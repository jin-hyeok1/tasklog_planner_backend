package personal.jinhyeok.tasklog_planner_backend.service.dashboard

import org.springframework.stereotype.Service
import personal.jinhyeok.jooq.Tables.APP_USER
import personal.jinhyeok.jooq.Tables.TASK
import personal.jinhyeok.jooq.Tables.TASK_SCHEDULE
import personal.jinhyeok.jooq.Tables.TASK_SESSION
import personal.jinhyeok.jooq.Tables.WORK_LOG
import personal.jinhyeok.jooq.enums.TaskStatus as JTaskStatus
import personal.jinhyeok.jooq.tables.records.TaskRecord
import personal.jinhyeok.tasklog_planner_backend.repository.category.TaskCategoryRepository
import personal.jinhyeok.tasklog_planner_backend.repository.schedule.TaskScheduleRepository
import personal.jinhyeok.tasklog_planner_backend.repository.session.TaskSessionRepository
import personal.jinhyeok.tasklog_planner_backend.repository.task.TaskRepository
import personal.jinhyeok.tasklog_planner_backend.repository.user.AppUserRepository
import personal.jinhyeok.tasklog_planner_backend.repository.worklog.WorkLogRepository
import personal.jinhyeok.tasklog_planner_backend.service.support.TimeSupport
import personal.jinhyeok.tasklog_planner_backend.service.support.toDto
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.DashboardTodayScheduleDto
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.RunningSessionSummaryDto
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.SimpleTaskDto
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.TaskDto
import java.math.BigDecimal
import java.time.LocalDate

@Service
class DashboardService(
    private val appUserRepository: AppUserRepository,
    private val taskCategoryRepository: TaskCategoryRepository,
    private val taskRepository: TaskRepository,
    private val taskScheduleRepository: TaskScheduleRepository,
    private val taskSessionRepository: TaskSessionRepository,
    private val workLogRepository: WorkLogRepository,
    private val timeSupport: TimeSupport,
) {
    fun weeklySummary(weekStartDate: LocalDate): Map<String, Any?> {
        val userEmail = appUserRepository.currentUserEmail()
        val weekEndDate = weekStartDate.plusDays(6)
        val schedules = taskScheduleRepository.findBetween(userEmail, weekStartDate, weekEndDate)
        val logs = workLogRepository.findBetween(userEmail, weekStartDate, weekEndDate)
        val planned = timeSupport.minutesToHours(schedules.sumOf { it.get(TASK_SCHEDULE.PLANNED_MINUTES) })
        val worked = timeSupport.minutesToHours(logs.sumOf { it.get(WORK_LOG.WORKED_MINUTES) })
        return mapOf(
            "weekStartDate" to weekStartDate,
            "weekEndDate" to weekEndDate,
            "plannedHours" to planned,
            "workedHours" to worked,
            "progressRate" to timeSupport.progressRate(worked, planned),
            "targetHours" to timeSupport.minutesToHours(appUserRepository.currentUser().get(APP_USER.WEEKLY_TARGET_MINUTES)),
            "missingWorkLogDays" to missingWorkLogDays(userEmail, weekStartDate, weekEndDate),
            "runningSession" to runningSessionSummary(userEmail),
            "delayedTasks" to taskRepository.findAllByStatus(userEmail, JTaskStatus.DELAYED).map { it.toDto() },
        )
    }

    fun today(date: LocalDate): Map<String, Any> {
        val userEmail = appUserRepository.currentUserEmail()
        return mapOf(
            "date" to date,
            "plannedHours" to timeSupport.minutesToHours(taskScheduleRepository.findToday(userEmail, date).sumOf { it.get(TASK_SCHEDULE.PLANNED_MINUTES) }),
            "workedHours" to timeSupport.minutesToHours(workLogRepository.findBetween(userEmail, date, date).sumOf { it.get(WORK_LOG.WORKED_MINUTES) }),
            "scheduleCount" to taskScheduleRepository.findToday(userEmail, date).size,
        )
    }

    fun todaySchedules(date: LocalDate): List<DashboardTodayScheduleDto> {
        val userEmail = appUserRepository.currentUserEmail()
        return taskScheduleRepository.findToday(userEmail, date).map { schedule ->
            val task = taskRepository.findById(schedule.get(TASK_SCHEDULE.TASK_ID))
            DashboardTodayScheduleDto(
                scheduleCode = schedule.get(TASK_SCHEDULE.SCHEDULE_CODE),
                task = task.toSimpleTask(),
                scheduledDate = schedule.get(TASK_SCHEDULE.SCHEDULED_DATE),
                startTime = schedule.get(TASK_SCHEDULE.START_TIME),
                endTime = schedule.get(TASK_SCHEDULE.END_TIME),
                plannedHours = timeSupport.minutesToHours(schedule.get(TASK_SCHEDULE.PLANNED_MINUTES)),
                actualHours = workedHours(task.get(TASK.TASK_ID), schedule.get(TASK_SCHEDULE.SCHEDULE_ID), schedule.get(TASK_SCHEDULE.SCHEDULED_DATE)),
                sessionStatus = taskSessionRepository.findLatestByScheduleId(schedule.get(TASK_SCHEDULE.SCHEDULE_ID))?.get(TASK_SESSION.STATUS)?.toDto(),
            )
        }
    }

    private fun missingWorkLogDays(userEmail: String, start: LocalDate, end: LocalDate): Int =
        generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(end) }.count { day ->
            day.isBefore(LocalDate.now()) &&
                taskScheduleRepository.existsByDate(userEmail, day) &&
                !workLogRepository.existsByDate(userEmail, day)
        }

    private fun runningSessionSummary(userEmail: String): RunningSessionSummaryDto? =
        taskSessionRepository.findRunning(userEmail)?.let {
            val task = taskRepository.findById(it.get(TASK_SESSION.TASK_ID))
            RunningSessionSummaryDto(
                sessionCode = it.get(TASK_SESSION.SESSION_CODE),
                taskCode = task.get(TASK.TASK_CODE),
                scheduleCode = taskScheduleRepository.findById(it.get(TASK_SESSION.SCHEDULE_ID))?.get(TASK_SCHEDULE.SCHEDULE_CODE),
                taskTitle = task.get(TASK.TITLE),
                categoryPath = taskCategoryRepository.pathById(task.get(TASK.CATEGORY_ID)),
                startedAt = it.get(TASK_SESSION.LAST_STARTED_AT) ?: it.get(TASK_SESSION.STARTED_AT),
                elapsedMinutes = it.get(TASK_SESSION.ACCUMULATED_MINUTES) + timeSupport.minutesBetween(it.get(TASK_SESSION.LAST_STARTED_AT) ?: it.get(TASK_SESSION.STARTED_AT), timeSupport.now()),
                status = it.get(TASK_SESSION.STATUS).toDto(),
            )
        }

    private fun workedHours(taskId: Long, scheduleId: Long?, workedDate: LocalDate): BigDecimal =
        timeSupport.minutesToHours(workLogRepository.sumMinutesBySchedule(taskId, scheduleId, workedDate))

    private fun TaskRecord.toSimpleTask(): SimpleTaskDto =
        SimpleTaskDto(
            taskCode = get(TASK.TASK_CODE),
            title = get(TASK.TITLE),
            status = get(TASK.STATUS).toDto(),
            priority = get(TASK.PRIORITY).toDto(),
            categoryPath = taskCategoryRepository.pathById(get(TASK.CATEGORY_ID)),
            isTask = get(TASK.IS_TASK),
        )

    private fun TaskRecord.toDto(): TaskDto =
        TaskDto(
            taskCode = get(TASK.TASK_CODE),
            categoryCode = "",
            title = get(TASK.TITLE),
            description = get(TASK.DESCRIPTION),
            plannedHours = timeSupport.minutesToHours(get(TASK.PLANNED_MINUTES)),
            startDate = get(TASK.START_DATE),
            dueDate = get(TASK.DUE_DATE),
            status = get(TASK.STATUS).toDto(),
            priority = get(TASK.PRIORITY).toDto(),
            createdAt = get(TASK.CREATED_AT),
            updatedAt = get(TASK.UPDATED_AT),
            isTask = get(TASK.IS_TASK),
        )
}
