package personal.jinhyeok.tasklog_planner_backend.service.worklog

import org.jooq.Condition
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import personal.jinhyeok.jooq.Tables.TASK
import personal.jinhyeok.jooq.Tables.TASK_SCHEDULE
import personal.jinhyeok.jooq.Tables.WORK_LOG
import personal.jinhyeok.jooq.enums.WorkLogSource as JWorkLogSource
import personal.jinhyeok.jooq.tables.records.WorkLogRecord
import personal.jinhyeok.tasklog_planner_backend.enumeration.ApiCode
import personal.jinhyeok.tasklog_planner_backend.exception.ApiException
import personal.jinhyeok.tasklog_planner_backend.repository.schedule.TaskScheduleRepository
import personal.jinhyeok.tasklog_planner_backend.repository.task.TaskRepository
import personal.jinhyeok.tasklog_planner_backend.repository.user.AppUserRepository
import personal.jinhyeok.tasklog_planner_backend.repository.worklog.WorkLogRepository
import personal.jinhyeok.tasklog_planner_backend.repository.worklog.WorkLogSaveCommand
import personal.jinhyeok.tasklog_planner_backend.repository.worklog.WorkLogUpdateCommand
import personal.jinhyeok.tasklog_planner_backend.service.support.TimeSupport
import personal.jinhyeok.tasklog_planner_backend.service.support.toDto
import personal.jinhyeok.tasklog_planner_backend.service.support.toJooq
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.SaveWorkLogRequest
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.WorkLogDto
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.WorkLogSource
import java.time.LocalDate

@Service
class WorkLogService(
    private val appUserRepository: AppUserRepository,
    private val taskRepository: TaskRepository,
    private val taskScheduleRepository: TaskScheduleRepository,
    private val workLogRepository: WorkLogRepository,
    private val timeSupport: TimeSupport,
) {
    fun workLogs(taskCode: String?, scheduleCode: String?, dateFrom: LocalDate?, dateTo: LocalDate?, source: WorkLogSource?): List<WorkLogDto> {
        val userEmail = appUserRepository.currentUserEmail()
        var condition: Condition = WORK_LOG.USER_EMAIL.eq(userEmail)
        if (taskCode != null) condition = condition.and(WORK_LOG.TASK_ID.eq(taskRepository.findByCode(userEmail, taskCode).get(TASK.TASK_ID)))
        if (scheduleCode != null) condition = condition.and(WORK_LOG.SCHEDULE_ID.eq(taskScheduleRepository.findByCode(userEmail, scheduleCode).get(TASK_SCHEDULE.SCHEDULE_ID)))
        if (dateFrom != null) condition = condition.and(WORK_LOG.WORKED_DATE.ge(dateFrom))
        if (dateTo != null) condition = condition.and(WORK_LOG.WORKED_DATE.le(dateTo))
        if (source != null) condition = condition.and(WORK_LOG.SOURCE.eq(source.toJooq()))
        return workLogRepository.findAll(condition).map { it.toDto() }
    }

    @Transactional
    fun createWorkLog(request: SaveWorkLogRequest): WorkLogDto {
        val userEmail = appUserRepository.currentUserEmail()
        val task = taskRepository.findByCode(userEmail, request.taskCode ?: timeSupport.missing("taskCode"))
        val scheduleId = request.scheduleCode?.let { taskScheduleRepository.findByCode(userEmail, it).get(TASK_SCHEDULE.SCHEDULE_ID) }
        val startedAt = request.startedAt ?: timeSupport.missing("startedAt")
        val endedAt = request.endedAt ?: timeSupport.missing("endedAt")
        if (!startedAt.isBefore(endedAt)) throw ApiException(ApiCode.BAD_REQUEST, "startedAt must be before endedAt")
        val workedDate = request.workedDate ?: startedAt.toLocalDate()
        val taskId = task.get(TASK.TASK_ID)
        return workLogRepository.insert(
            WorkLogSaveCommand(
                userEmail = userEmail,
                taskId = taskId,
                scheduleId = scheduleId,
                workedDate = workedDate,
                sequenceNo = workLogRepository.nextSequence(taskId, workedDate),
                startedAt = startedAt,
                endedAt = endedAt,
                workedMinutes = timeSupport.minutesBetween(startedAt, endedAt),
                memo = request.memo,
                source = JWorkLogSource.MANUAL,
            ),
        ).toDto()
    }

    @Transactional
    fun updateWorkLog(taskCode: String, workedDate: LocalDate, sequenceNo: Int, request: SaveWorkLogRequest): WorkLogDto {
        val userEmail = appUserRepository.currentUserEmail()
        val task = taskRepository.findByCode(userEmail, taskCode)
        val taskId = task.get(TASK.TASK_ID)
        val current = workLogRepository.findByKey(taskId, workedDate, sequenceNo)
        val startedAt = request.startedAt ?: current.get(WORK_LOG.STARTED_AT)
        val endedAt = request.endedAt ?: current.get(WORK_LOG.ENDED_AT)
        if (!startedAt.isBefore(endedAt)) throw ApiException(ApiCode.BAD_REQUEST, "startedAt must be before endedAt")
        return workLogRepository.update(
            taskId,
            workedDate,
            sequenceNo,
            WorkLogUpdateCommand(
                scheduleId = request.scheduleCode?.let { taskScheduleRepository.findByCode(userEmail, it).get(TASK_SCHEDULE.SCHEDULE_ID) } ?: current.get(WORK_LOG.SCHEDULE_ID),
                startedAt = startedAt,
                endedAt = endedAt,
                workedMinutes = timeSupport.minutesBetween(startedAt, endedAt),
                memo = request.memo ?: current.get(WORK_LOG.MEMO),
                updatedAt = timeSupport.now(),
            ),
        ).toDto()
    }

    @Transactional
    fun deleteWorkLog(taskCode: String, workedDate: LocalDate, sequenceNo: Int): Map<String, Boolean> {
        val task = taskRepository.findByCode(appUserRepository.currentUserEmail(), taskCode)
        val deleted = workLogRepository.delete(task.get(TASK.TASK_ID), workedDate, sequenceNo)
        if (deleted == 0) throw ApiException(ApiCode.NOT_FOUND, "WorkLog not found")
        return mapOf("deleted" to true)
    }

    private fun WorkLogRecord.toDto(): WorkLogDto =
        WorkLogDto(
            taskCode = taskRepository.findById(get(WORK_LOG.TASK_ID)).get(TASK.TASK_CODE),
            scheduleCode = taskScheduleRepository.findById(get(WORK_LOG.SCHEDULE_ID))?.get(TASK_SCHEDULE.SCHEDULE_CODE),
            workedDate = get(WORK_LOG.WORKED_DATE),
            sequenceNo = get(WORK_LOG.SEQUENCE_NO),
            startedAt = get(WORK_LOG.STARTED_AT),
            endedAt = get(WORK_LOG.ENDED_AT),
            workedMinutes = get(WORK_LOG.WORKED_MINUTES),
            memo = get(WORK_LOG.MEMO),
            source = get(WORK_LOG.SOURCE).toDto(),
            createdAt = get(WORK_LOG.CREATED_AT),
            updatedAt = get(WORK_LOG.UPDATED_AT),
        )
}
