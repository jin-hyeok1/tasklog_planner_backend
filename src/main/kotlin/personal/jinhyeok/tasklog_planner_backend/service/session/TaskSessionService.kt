package personal.jinhyeok.tasklog_planner_backend.service.session

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import personal.jinhyeok.jooq.Tables.TASK
import personal.jinhyeok.jooq.Tables.TASK_SCHEDULE
import personal.jinhyeok.jooq.Tables.TASK_SESSION
import personal.jinhyeok.jooq.Tables.WORK_LOG
import personal.jinhyeok.jooq.enums.TaskSessionStatus as JTaskSessionStatus
import personal.jinhyeok.jooq.enums.TaskStatus as JTaskStatus
import personal.jinhyeok.jooq.enums.WorkLogSource as JWorkLogSource
import personal.jinhyeok.jooq.tables.records.TaskSessionRecord
import personal.jinhyeok.tasklog_planner_backend.enumeration.ApiCode
import personal.jinhyeok.tasklog_planner_backend.exception.ApiException
import personal.jinhyeok.tasklog_planner_backend.repository.schedule.TaskScheduleRepository
import personal.jinhyeok.tasklog_planner_backend.repository.session.TaskSessionRepository
import personal.jinhyeok.tasklog_planner_backend.repository.session.TaskSessionSaveCommand
import personal.jinhyeok.tasklog_planner_backend.repository.task.TaskRepository
import personal.jinhyeok.tasklog_planner_backend.repository.user.AppUserRepository
import personal.jinhyeok.tasklog_planner_backend.repository.worklog.WorkLogRepository
import personal.jinhyeok.tasklog_planner_backend.repository.worklog.WorkLogSaveCommand
import personal.jinhyeok.tasklog_planner_backend.service.support.TimeSupport
import personal.jinhyeok.tasklog_planner_backend.service.support.toDto
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.FinishTaskSessionRequest
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.StartTaskSessionRequest
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.TaskSessionDto
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.WorkLogDto
import java.time.LocalDate

@Service
class TaskSessionService(
    private val appUserRepository: AppUserRepository,
    private val taskRepository: TaskRepository,
    private val taskScheduleRepository: TaskScheduleRepository,
    private val taskSessionRepository: TaskSessionRepository,
    private val workLogRepository: WorkLogRepository,
    private val timeSupport: TimeSupport,
) {
    @Transactional
    fun startSession(request: StartTaskSessionRequest): Map<String, Any?> {
        val userEmail = appUserRepository.currentUserEmail()
        val task = taskRepository.findByCode(userEmail, request.taskCode ?: timeSupport.missing("taskCode"))
        val scheduleId = request.scheduleCode?.let { taskScheduleRepository.findByCode(userEmail, it).get(TASK_SCHEDULE.SCHEDULE_ID) }
        val paused = pauseRunningSession(userEmail)
        val now = timeSupport.now()
        val row = taskSessionRepository.insert(
            TaskSessionSaveCommand(
                userEmail = userEmail,
                taskId = task.get(TASK.TASK_ID),
                scheduleId = scheduleId,
                sessionCode = timeSupport.nextCode("SESSION"),
                workedDate = request.workedDate ?: LocalDate.now(),
                now = now,
            ),
        )
        if (task.get(TASK.STATUS) == JTaskStatus.TODO) {
            taskRepository.updateStatus(task.get(TASK.TASK_ID), JTaskStatus.IN_PROGRESS, timeSupport.now())
        }
        return mapOf("startedSession" to row.toSummary(), "pausedSession" to paused?.toSummary())
    }

    @Transactional
    fun pauseSession(sessionCode: String): TaskSessionDto {
        val session = taskSessionRepository.findByCode(appUserRepository.currentUserEmail(), sessionCode)
        if (session.get(TASK_SESSION.STATUS) != JTaskSessionStatus.RUNNING) throw ApiException(ApiCode.CONFLICT, "Session is not RUNNING")
        val now = timeSupport.now()
        val accumulated = session.get(TASK_SESSION.ACCUMULATED_MINUTES) + timeSupport.minutesBetween(session.get(TASK_SESSION.LAST_STARTED_AT) ?: session.get(TASK_SESSION.STARTED_AT), now)
        return taskSessionRepository.pause(sessionCode, accumulated, now).toDto()
    }

    @Transactional
    fun resumeSession(sessionCode: String): Map<String, Any?> {
        val userEmail = appUserRepository.currentUserEmail()
        val session = taskSessionRepository.findByCode(userEmail, sessionCode)
        if (session.get(TASK_SESSION.STATUS) != JTaskSessionStatus.PAUSED) throw ApiException(ApiCode.CONFLICT, "Session is not PAUSED")
        val paused = pauseRunningSession(userEmail)
        val row = taskSessionRepository.resume(sessionCode, timeSupport.now())
        return mapOf("resumedSession" to row.toSummary(), "pausedSession" to paused?.toSummary())
    }

    @Transactional
    fun finishSession(sessionCode: String, request: FinishTaskSessionRequest): Map<String, Any> {
        val session = taskSessionRepository.findByCode(appUserRepository.currentUserEmail(), sessionCode)
        if (session.get(TASK_SESSION.STATUS) == JTaskSessionStatus.FINISHED) throw ApiException(ApiCode.CONFLICT, "Session is already FINISHED")
        val now = timeSupport.now()
        val accumulated = session.get(TASK_SESSION.ACCUMULATED_MINUTES) +
            if (session.get(TASK_SESSION.STATUS) == JTaskSessionStatus.RUNNING) timeSupport.minutesBetween(session.get(TASK_SESSION.LAST_STARTED_AT) ?: session.get(TASK_SESSION.STARTED_AT), now) else 0
        val finished = taskSessionRepository.finish(sessionCode, accumulated, now)
        val workLog = createSessionWorkLog(finished, request.memo)
        return mapOf("session" to finished.toSummary(), "workLog" to workLog)
    }

    private fun pauseRunningSession(userEmail: String): TaskSessionRecord? =
        taskSessionRepository.findRunning(userEmail)?.let {
            val now = timeSupport.now()
            val accumulated = it.get(TASK_SESSION.ACCUMULATED_MINUTES) + timeSupport.minutesBetween(it.get(TASK_SESSION.LAST_STARTED_AT) ?: it.get(TASK_SESSION.STARTED_AT), now)
            taskSessionRepository.pause(it.get(TASK_SESSION.SESSION_CODE), accumulated, now)
        }

    private fun createSessionWorkLog(session: TaskSessionRecord, memo: String?): WorkLogDto {
        val userEmail = appUserRepository.currentUserEmail()
        val taskId = session.get(TASK_SESSION.TASK_ID)
        val workedDate = session.get(TASK_SESSION.WORKED_DATE)
        return workLogRepository.insert(
            WorkLogSaveCommand(
                userEmail = userEmail,
                taskId = taskId,
                scheduleId = session.get(TASK_SESSION.SCHEDULE_ID),
                workedDate = workedDate,
                sequenceNo = workLogRepository.nextSequence(taskId, workedDate),
                startedAt = session.get(TASK_SESSION.STARTED_AT),
                endedAt = session.get(TASK_SESSION.FINISHED_AT) ?: timeSupport.now(),
                workedMinutes = session.get(TASK_SESSION.ACCUMULATED_MINUTES).coerceAtLeast(1),
                memo = memo ?: "TaskSession 종료",
                source = JWorkLogSource.SESSION,
            ),
        ).let {
            WorkLogDto(
                taskCode = taskRepository.findById(it.get(WORK_LOG.TASK_ID)).get(TASK.TASK_CODE),
                scheduleCode = taskScheduleRepository.findById(it.get(WORK_LOG.SCHEDULE_ID))?.get(TASK_SCHEDULE.SCHEDULE_CODE),
                workedDate = it.get(WORK_LOG.WORKED_DATE),
                sequenceNo = it.get(WORK_LOG.SEQUENCE_NO),
                startedAt = it.get(WORK_LOG.STARTED_AT),
                endedAt = it.get(WORK_LOG.ENDED_AT),
                workedMinutes = it.get(WORK_LOG.WORKED_MINUTES),
                memo = it.get(WORK_LOG.MEMO),
                source = it.get(WORK_LOG.SOURCE).toDto(),
                createdAt = it.get(WORK_LOG.CREATED_AT),
                updatedAt = it.get(WORK_LOG.UPDATED_AT),
            )
        }
    }

    private fun TaskSessionRecord.toDto(): TaskSessionDto =
        TaskSessionDto(
            sessionCode = get(TASK_SESSION.SESSION_CODE),
            scheduleCode = taskScheduleRepository.findById(get(TASK_SESSION.SCHEDULE_ID))?.get(TASK_SCHEDULE.SCHEDULE_CODE),
            taskCode = taskRepository.findById(get(TASK_SESSION.TASK_ID)).get(TASK.TASK_CODE),
            workedDate = get(TASK_SESSION.WORKED_DATE),
            status = get(TASK_SESSION.STATUS).toDto(),
            startedAt = get(TASK_SESSION.STARTED_AT),
            lastStartedAt = get(TASK_SESSION.LAST_STARTED_AT),
            pausedAt = get(TASK_SESSION.PAUSED_AT),
            finishedAt = get(TASK_SESSION.FINISHED_AT),
            accumulatedMinutes = get(TASK_SESSION.ACCUMULATED_MINUTES),
            createdAt = get(TASK_SESSION.CREATED_AT),
            updatedAt = get(TASK_SESSION.UPDATED_AT),
        )

    private fun TaskSessionRecord.toSummary(): Map<String, Any> =
        mapOf(
            "sessionCode" to get(TASK_SESSION.SESSION_CODE),
            "status" to get(TASK_SESSION.STATUS).toDto(),
            "accumulatedMinutes" to get(TASK_SESSION.ACCUMULATED_MINUTES),
        )
}
