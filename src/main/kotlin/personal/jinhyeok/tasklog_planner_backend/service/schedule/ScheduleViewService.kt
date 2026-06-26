package personal.jinhyeok.tasklog_planner_backend.service.schedule

import org.springframework.stereotype.Service
import personal.jinhyeok.jooq.Tables.PLAN
import personal.jinhyeok.jooq.Tables.TASK
import personal.jinhyeok.jooq.Tables.TASK_CATEGORY
import personal.jinhyeok.jooq.Tables.TASK_SCHEDULE
import personal.jinhyeok.jooq.Tables.TASK_SESSION
import personal.jinhyeok.jooq.Tables.WORK_LOG
import personal.jinhyeok.jooq.enums.PlanType as JPlanType
import personal.jinhyeok.jooq.tables.records.PlanRecord
import personal.jinhyeok.jooq.tables.records.TaskRecord
import personal.jinhyeok.jooq.tables.records.TaskScheduleRecord
import personal.jinhyeok.jooq.tables.records.TaskSessionRecord
import personal.jinhyeok.tasklog_planner_backend.repository.category.TaskCategoryRepository
import personal.jinhyeok.tasklog_planner_backend.repository.notification.NotificationRepository
import personal.jinhyeok.tasklog_planner_backend.repository.plan.PlanRepository
import personal.jinhyeok.tasklog_planner_backend.repository.schedule.TaskScheduleRepository
import personal.jinhyeok.tasklog_planner_backend.repository.session.TaskSessionRepository
import personal.jinhyeok.tasklog_planner_backend.repository.task.TaskRepository
import personal.jinhyeok.tasklog_planner_backend.repository.user.AppUserRepository
import personal.jinhyeok.tasklog_planner_backend.repository.worklog.WorkLogRepository
import personal.jinhyeok.tasklog_planner_backend.service.support.TimeSupport
import personal.jinhyeok.tasklog_planner_backend.service.support.toDto
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.RunningSessionSummaryDto
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.ScheduleSessionSummaryDto
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.SimpleTaskDto
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.TaskDto
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.WeeklyBoardScheduleDto
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.WeeklyDayGroupDto
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.MonthlyScheduleInvestmentDto
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.MonthlyTaskDetailDto
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.MonthlyTaskInvestmentDto
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.QuarterlyScheduleSummaryDto
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.WeeklyScheduleSummaryDto
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.YearlyScheduleSummaryDto
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields

@Service
class ScheduleViewService(
    private val appUserRepository: AppUserRepository,
    private val planRepository: PlanRepository,
    private val taskCategoryRepository: TaskCategoryRepository,
    private val taskRepository: TaskRepository,
    private val taskScheduleRepository: TaskScheduleRepository,
    private val taskSessionRepository: TaskSessionRepository,
    private val workLogRepository: WorkLogRepository,
    private val notificationRepository: NotificationRepository,
    private val timeSupport: TimeSupport,
) {
    fun yearlySummary(year: Int): YearlyScheduleSummaryDto {
        val userEmail = appUserRepository.currentUserEmail()
        val yearPlan = planRepository.findYearPlan(userEmail, year)
        val weekPlans = yearPlan?.let { weekPlansForPlan(it) }.orEmpty()
        val schedules = schedulesForPlans(userEmail, weekPlans)
        val logs = logsForSchedules(userEmail, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31), schedules)
        val weekPlanCount = weekPlans.size
        return YearlyScheduleSummaryDto(
            year = year,
            weekPlanCount = weekPlanCount,
            plannedMinutesPerWeek = perWeek(plannedMinutesByDistinctTask(schedules), weekPlanCount),
            investedMinutesPerWeek = perWeek(workedMinutes(logs), weekPlanCount),
        )
    }

    fun monthlyScheduleInvestments(from: String, to: String): List<MonthlyScheduleInvestmentDto> {
        val userEmail = appUserRepository.currentUserEmail()
        return monthRange(from, to).map { month ->
            val weekPlans = weekPlansForMonth(userEmail, month)
            val schedules = schedulesForPlans(userEmail, weekPlans)
            val logs = logsForSchedules(userEmail, month.atDay(1), month.atEndOfMonth(), schedules)
            val weekPlanCount = weekPlans.size
            MonthlyScheduleInvestmentDto(
                month = month.format(YEAR_MONTH_FORMATTER),
                weekPlanCount = weekPlanCount,
                plannedMinutesPerWeek = plannedMinutesByDistinctTask(schedules),
                investedMinutesPerWeek = workedMinutes(logs),
            )
        }
    }

    fun monthlyTaskDetails(from: String, to: String): List<MonthlyTaskDetailDto> {
        val userEmail = appUserRepository.currentUserEmail()
        return monthRange(from, to).map { month ->
            val weekPlans = weekPlansForMonth(userEmail, month)
            val schedules = schedulesForPlans(userEmail, weekPlans)
            val logs = logsForSchedules(userEmail, month.atDay(1), month.atEndOfMonth(), schedules)
            val weekPlanCount = weekPlans.size
            val schedulesByTask = schedules.groupBy { it.get(TASK_SCHEDULE.TASK_ID) }
            val logsByTask = logs.groupBy { it.get(WORK_LOG.TASK_ID) }
            val tasks = schedulesByTask.keys.map { taskId ->
                val task = taskRepository.findById(taskId)
                MonthlyTaskInvestmentDto(
                    taskCode = task.get(TASK.TASK_CODE),
                    title = task.get(TASK.TITLE),
                    plannedMinutesPerWeek = task.get(TASK.PLANNED_MINUTES),
                    investedMinutesPerWeek = workedMinutes(logsByTask[taskId].orEmpty()),
                )
            }.sortedBy { it.taskCode }

            MonthlyTaskDetailDto(
                month = month.format(YEAR_MONTH_FORMATTER),
                taskCount = tasks.size,
                weekPlanCount = weekPlanCount,
                plannedMinutesPerWeek = plannedMinutesByDistinctTask(schedules),
                investedMinutesPerWeek = workedMinutes(logs),
                tasks = tasks,
            )
        }
    }

    fun quarterlySummaryByPlanId(planId: Long): QuarterlyScheduleSummaryDto {
        val userEmail = appUserRepository.currentUserEmail()
        val plan = planRepository.findById(userEmail, planId)
        val weekPlans = weekPlansForPlan(plan)
        val schedules = schedulesForPlans(userEmail, weekPlans)
        val logs = logsForSchedules(userEmail, plan.get(PLAN.START_DATE), plan.get(PLAN.END_DATE), schedules)
        val weekPlanCount = weekPlans.size
        return QuarterlyScheduleSummaryDto(
            planId = plan.get(PLAN.PLAN_ID),
            planCode = plan.get(PLAN.PLAN_CODE),
            weekPlanCount = weekPlanCount,
            plannedMinutesPerWeek = perWeek(plannedMinutesByDistinctTask(schedules), weekPlanCount),
            investedMinutesPerWeek = perWeek(workedMinutes(logs), weekPlanCount),
        )
    }

    fun weeklySummaryByPlanId(planId: Long): WeeklyScheduleSummaryDto {
        val userEmail = appUserRepository.currentUserEmail()
        val plan = planRepository.findById(userEmail, planId)
        val schedules = schedulesForPlan(userEmail, plan)
        val logs = logsForSchedules(userEmail, plan.get(PLAN.START_DATE), plan.get(PLAN.END_DATE), schedules)
        val plannedMinutes = plannedMinutesByDistinctTask(schedules)
        val investedMinutes = workedMinutes(logs)
        return WeeklyScheduleSummaryDto(
            planId = plan.get(PLAN.PLAN_ID),
            planCode = plan.get(PLAN.PLAN_CODE),
            plannedMinutes = plannedMinutes,
            investedMinutes = investedMinutes,
            progressRate = ratio(investedMinutes, plannedMinutes),
            missingWorkLogDays = missingWorkLogDays(plan.get(PLAN.START_DATE), plan.get(PLAN.END_DATE), schedules, logs),
            unreadNotificationCount = notificationRepository.unreadCountBetween(
                userEmail,
                plan.get(PLAN.START_DATE).atStartOfDay().atOffset(timeSupport.offset()),
                plan.get(PLAN.END_DATE).plusDays(1).atStartOfDay().atOffset(timeSupport.offset()),
            ),
            runningSession = runningSessionSummary(userEmail),
        )
    }

    fun weeklyDailySchedules(from: LocalDate, to: LocalDate): List<WeeklyBoardScheduleDto> {
        val userEmail = appUserRepository.currentUserEmail()
        val schedules = taskSchedulesOnly(taskScheduleRepository.findBetweenOrdered(userEmail, from, to), isTaskOnly = false)
        val sessions = sessionsForSchedules(userEmail, schedules)
        val logs = logsForSchedules(userEmail, from, to, schedules)
        val sessionsBySchedule = sessions.groupBy { it.get(TASK_SESSION.SCHEDULE_ID) }
        val logsBySchedule = logs.groupBy { it.get(WORK_LOG.SCHEDULE_ID) }
        return schedules.map { schedule ->
            val task = taskRepository.findById(schedule.get(TASK_SCHEDULE.TASK_ID))
            val scheduleSessions = sessionsBySchedule[schedule.get(TASK_SCHEDULE.SCHEDULE_ID)].orEmpty()
            val scheduleLogs = logsBySchedule[schedule.get(TASK_SCHEDULE.SCHEDULE_ID)].orEmpty()
            val latestSession = scheduleSessions.maxByOrNull { it.get(TASK_SESSION.CREATED_AT) }
            WeeklyBoardScheduleDto(
                scheduleCode = schedule.get(TASK_SCHEDULE.SCHEDULE_CODE),
                scheduledDate = schedule.get(TASK_SCHEDULE.SCHEDULED_DATE),
                startTime = schedule.get(TASK_SCHEDULE.START_TIME),
                endTime = schedule.get(TASK_SCHEDULE.END_TIME),
                plannedHours = timeSupport.minutesToHours(schedule.get(TASK_SCHEDULE.PLANNED_MINUTES)),
                actualHours = timeSupport.minutesToHours(workedMinutes(scheduleLogs)),
                hasActualLog = scheduleLogs.isNotEmpty(),
                memo = schedule.get(TASK_SCHEDULE.MEMO),
                task = task.toSimpleTask(),
                session = latestSession?.let { ScheduleSessionSummaryDto(it.get(TASK_SESSION.SESSION_CODE), it.get(TASK_SESSION.STATUS).toDto(), it.get(TASK_SESSION.ACCUMULATED_MINUTES)) },
            )
        }
    }

    fun yearlySchedule(year: Int): Map<String, Any?> {
        val userEmail = appUserRepository.currentUserEmail()
        val yearStart = LocalDate.of(year, 1, 1)
        val yearEnd = LocalDate.of(year, 12, 31)
        val yearPlan = planRepository.findYearPlan(userEmail, year)
        val schedules = yearPlan?.let { schedulesForPlan(userEmail, it) } ?: taskSchedulesOnly(taskScheduleRepository.findBetween(userEmail, yearStart, yearEnd))
        val logs = logsForSchedules(userEmail, yearStart, yearEnd, schedules)
        val planned = timeSupport.minutesToHours(schedules.sumOf { it.get(TASK_SCHEDULE.PLANNED_MINUTES) })
        val worked = timeSupport.minutesToHours(logs.sumOf { it.get(WORK_LOG.WORKED_MINUTES) })
        return mapOf(
            "year" to year,
            "yearPlan" to yearPlan?.let { mapOf("planCode" to it.get(PLAN.PLAN_CODE), "title" to it.get(PLAN.TITLE)) },
            "summary" to summary(planned, worked),
            "monthlyTotals" to (1..12).mapNotNull { monthTotal(it, schedules, logs) },
            "monthGroups" to schedules
                .groupBy { it.get(TASK_SCHEDULE.SCHEDULED_DATE).monthValue }
                .map { (month, rows) -> mapOf("month" to month, "tasks" to rows.map { taskRepository.findById(it.get(TASK_SCHEDULE.TASK_ID)).toDto() }.distinctBy { it.taskCode }) },
        )
    }

    fun quarterlySchedule(planCode: String): Map<String, Any> =
        quarterSummary(planCode) + quarterMonths(planCode)

    fun quarterSummary(planCode: String): Map<String, Any> {
        val userEmail = appUserRepository.currentUserEmail()
        val plan = planRepository.findByCode(userEmail, planCode)
        val schedules = schedulesForPlan(userEmail, plan)
        val logs = logsForSchedules(userEmail, plan.get(PLAN.START_DATE), plan.get(PLAN.END_DATE), schedules)
        val planned = timeSupport.minutesToHours(schedules.sumOf { it.get(TASK_SCHEDULE.PLANNED_MINUTES) })
        val worked = timeSupport.minutesToHours(logs.sumOf { it.get(WORK_LOG.WORKED_MINUTES) })
        return mapOf(
            "quarterPlan" to mapOf("planCode" to plan.get(PLAN.PLAN_CODE), "title" to plan.get(PLAN.TITLE)),
            "summary" to summary(planned, worked),
        )
    }

    fun quarterMonths(planCode: String): Map<String, Any> {
        val userEmail = appUserRepository.currentUserEmail()
        val plan = planRepository.findByCode(userEmail, planCode)
        val schedules = schedulesForPlan(userEmail, plan)
        val logs = logsForSchedules(userEmail, plan.get(PLAN.START_DATE), plan.get(PLAN.END_DATE), schedules)
        return mapOf(
            "monthlyTotals" to (plan.get(PLAN.START_DATE).monthValue..plan.get(PLAN.END_DATE).monthValue).mapNotNull { monthTotal(it, schedules, logs) },
            "monthGroups" to schedules.groupBy { it.get(TASK_SCHEDULE.SCHEDULED_DATE).monthValue }.map { (month, monthSchedules) ->
                mapOf(
                    "month" to month,
                    "weekGroups" to monthSchedules.groupBy { it.get(TASK_SCHEDULE.SCHEDULED_DATE).get(WeekFields.ISO.weekOfMonth()) }.map { (weekNo, weekSchedules) ->
                        val weekLogs = logs.filter { log -> weekSchedules.any { it.get(TASK_SCHEDULE.SCHEDULE_ID) == log.get(WORK_LOG.SCHEDULE_ID) } }
                        mapOf(
                            "weekNo" to weekNo,
                            "startDate" to weekSchedules.minOf { it.get(TASK_SCHEDULE.SCHEDULED_DATE) },
                            "endDate" to weekSchedules.maxOf { it.get(TASK_SCHEDULE.SCHEDULED_DATE) },
                            "plannedHours" to timeSupport.minutesToHours(weekSchedules.sumOf { it.get(TASK_SCHEDULE.PLANNED_MINUTES) }),
                            "workedHours" to timeSupport.minutesToHours(weekLogs.sumOf { it.get(WORK_LOG.WORKED_MINUTES) }),
                            "tasks" to weekSchedules.map { taskRepository.findById(it.get(TASK_SCHEDULE.TASK_ID)).toDto() }.distinctBy { it.taskCode },
                        )
                    },
                )
            },
        )
    }

    fun weeklySchedule(planCode: String): Map<String, Any?> {
        val userEmail = appUserRepository.currentUserEmail()
        val plan = planRepository.findByCode(userEmail, planCode)
        val schedules = schedulesForPlan(userEmail, plan, false)
        val logs = logsForSchedules(userEmail, plan.get(PLAN.START_DATE), plan.get(PLAN.END_DATE), schedules)
        val planned = timeSupport.minutesToHours(schedules.sumOf { it.get(TASK_SCHEDULE.PLANNED_MINUTES) })
        val worked = timeSupport.minutesToHours(logs.sumOf { it.get(WORK_LOG.WORKED_MINUTES) })
        val runningSession = runningSessionSummary(userEmail)
        return mapOf(
            "weekPlan" to mapOf("planCode" to plan.get(PLAN.PLAN_CODE), "title" to plan.get(PLAN.TITLE), "startDate" to plan.get(PLAN.START_DATE), "endDate" to plan.get(PLAN.END_DATE)),
            "summary" to summary(planned, worked) + mapOf(
                "missingWorkLogDays" to missingWorkLogDays(userEmail, plan.get(PLAN.START_DATE), plan.get(PLAN.END_DATE)),
                "notificationCount" to notificationRepository.unreadCount(userEmail),
                "currentTaskTitle" to runningSession?.taskTitle,
            ),
            "runningSession" to runningSession,
            "dayGroups" to (0..6).map { plan.get(PLAN.START_DATE).plusDays(it.toLong()) }.map { day ->
                val daySchedules = schedules.filter { it.get(TASK_SCHEDULE.SCHEDULED_DATE) == day }.sortedBy { it.get(TASK_SCHEDULE.START_TIME) }
                val dayLogs = logs.filter { it.get(WORK_LOG.WORKED_DATE) == day }
                WeeklyDayGroupDto(
                    date = day,
                    dayLabel = timeSupport.dayOfWeekLabel(day),
                    plannedHours = timeSupport.minutesToHours(daySchedules.sumOf { it.get(TASK_SCHEDULE.PLANNED_MINUTES) }),
                    workedHours = timeSupport.minutesToHours(dayLogs.sumOf { it.get(WORK_LOG.WORKED_MINUTES) }),
                    schedules = daySchedules.map { schedule ->
                        val task = taskRepository.findById(schedule.get(TASK_SCHEDULE.TASK_ID))
                        val session = taskSessionRepository.findLatestByScheduleId(schedule.get(TASK_SCHEDULE.SCHEDULE_ID))
                        WeeklyBoardScheduleDto(
                            scheduleCode = schedule.get(TASK_SCHEDULE.SCHEDULE_CODE),
                            scheduledDate = schedule.get(TASK_SCHEDULE.SCHEDULED_DATE),
                            startTime = schedule.get(TASK_SCHEDULE.START_TIME),
                            endTime = schedule.get(TASK_SCHEDULE.END_TIME),
                            plannedHours = timeSupport.minutesToHours(schedule.get(TASK_SCHEDULE.PLANNED_MINUTES)),
                            actualHours = workedHours(task.get(TASK.TASK_ID), schedule.get(TASK_SCHEDULE.SCHEDULE_ID), schedule.get(TASK_SCHEDULE.SCHEDULED_DATE)),
                            hasActualLog = dayLogs.any { it.get(WORK_LOG.SCHEDULE_ID) == schedule.get(TASK_SCHEDULE.SCHEDULE_ID) },
                            memo = schedule.get(TASK_SCHEDULE.MEMO),
                            task = task.toSimpleTask(),
                            session = session?.let { ScheduleSessionSummaryDto(it.get(TASK_SESSION.SESSION_CODE), it.get(TASK_SESSION.STATUS).toDto(), it.get(TASK_SESSION.ACCUMULATED_MINUTES)) },
                        )
                    },
                )
            },
        )
    }

    private fun monthTotal(
        month: Int,
        schedules: List<TaskScheduleRecord>,
        logs: List<personal.jinhyeok.jooq.tables.records.WorkLogRecord>,
    ): Map<String, Any>? {
        val monthSchedules = schedules.filter { it.get(TASK_SCHEDULE.SCHEDULED_DATE).monthValue == month }
        val monthLogs = logs.filter { it.get(WORK_LOG.WORKED_DATE).monthValue == month }
        val scheduledMinutes = monthSchedules.sumOf { it.get(TASK_SCHEDULE.PLANNED_MINUTES) }
        val workedMinutes = monthLogs.sumOf { it.get(WORK_LOG.WORKED_MINUTES) }
        if (scheduledMinutes == 0 && workedMinutes == 0) return null
        val planned = timeSupport.minutesToHours(scheduledMinutes)
        val worked = timeSupport.minutesToHours(workedMinutes)
        return mapOf("month" to month, "plannedHours" to planned, "workedHours" to worked, "varianceHours" to worked.subtract(planned))
    }

    private fun schedulesForPlan(
        userEmail: String,
        plan: PlanRecord,
        isTask: Boolean = true,
    ): List<TaskScheduleRecord> =
        taskSchedulesOnly(taskScheduleRepository.findByPlanIds(userEmail, planScopeIds(plan)), isTask)

    private fun schedulesForPlans(userEmail: String, plans: List<PlanRecord>): List<TaskScheduleRecord> =
        taskSchedulesOnly(taskScheduleRepository.findByPlanIds(userEmail, plans.map { it.get(PLAN.PLAN_ID) }))

    private fun sessionsForSchedules(userEmail: String, schedules: List<TaskScheduleRecord>): List<TaskSessionRecord> =
        taskSessionRepository.findByScheduleIds(userEmail, schedules.map { it.get(TASK_SCHEDULE.SCHEDULE_ID) })

    private fun weekPlansForPlan(plan: PlanRecord): List<PlanRecord> =
        planRepository.findByIds(planScopeIds(plan)).filter { it.get(PLAN.PLAN_TYPE) == JPlanType.WEEK }

    private fun weekPlansForMonth(userEmail: String, month: YearMonth): List<PlanRecord> {
        val monthStart = month.atDay(1)
        val monthEnd = month.atEndOfMonth()
        val monthPlans = planRepository.findByTypeBetween(userEmail, JPlanType.MONTH, monthStart, monthEnd)
        val monthPlanIds = monthPlans.map { it.get(PLAN.PLAN_ID) }.toSet()
        return monthPlans
            .flatMap { planRepository.findByIds(planScopeIds(it)) }
            .filter { it.get(PLAN.PLAN_TYPE) == JPlanType.WEEK }
            .filter { it.get(PLAN.PARENT_PLAN_ID) in monthPlanIds || it.get(PLAN.START_DATE).monthValue == month.monthValue }
            .distinctBy { it.get(PLAN.PLAN_ID) }
            .sortedBy { it.get(PLAN.START_DATE) }
    }

    private fun plannedMinutesByDistinctTask(schedules: List<TaskScheduleRecord>): Int =
        schedules.map { it.get(TASK_SCHEDULE.TASK_ID) }
            .distinct()
            .map { taskRepository.findById(it) }
            .filter { it.get(TASK.IS_TASK) }
            .sumOf { it.get(TASK.PLANNED_MINUTES) }

    private fun workedMinutes(logs: List<personal.jinhyeok.jooq.tables.records.WorkLogRecord>): Int =
        logs.sumOf { it.get(WORK_LOG.WORKED_MINUTES) }

    private fun taskSchedulesOnly(schedules: List<TaskScheduleRecord>, isTaskOnly: Boolean = true): List<TaskScheduleRecord> =
        schedules.filter { schedule -> if (isTaskOnly) taskRepository.findById(schedule.get(TASK_SCHEDULE.TASK_ID)).get(TASK.IS_TASK) else true }

    private fun perWeek(minutes: Int, weekPlanCount: Int): Int =
        if (weekPlanCount == 0) 0 else minutes / weekPlanCount

    private fun ratio(numerator: Int, denominator: Int): BigDecimal =
        if (denominator == 0) BigDecimal.ZERO else BigDecimal.valueOf(numerator.toLong())
            .divide(BigDecimal.valueOf(denominator.toLong()), 4, RoundingMode.HALF_UP)

    private fun missingWorkLogDays(
        start: LocalDate,
        end: LocalDate,
        schedules: List<TaskScheduleRecord>,
        logs: List<personal.jinhyeok.jooq.tables.records.WorkLogRecord>,
    ): Int {
        val scheduledDays = schedules.map { it.get(TASK_SCHEDULE.SCHEDULED_DATE) }.toSet()
        val loggedDays = logs.map { it.get(WORK_LOG.WORKED_DATE) }.toSet()
        return generateSequence(start) { it.plusDays(1) }
            .takeWhile { !it.isAfter(end) }
            .count { it in scheduledDays && it !in loggedDays }
    }

    private fun monthRange(from: String, to: String): List<YearMonth> {
        val start = YearMonth.parse(from, YEAR_MONTH_FORMATTER)
        val end = YearMonth.parse(to, YEAR_MONTH_FORMATTER)
        if (end.isBefore(start)) return emptyList()
        return generateSequence(start) { it.plusMonths(1) }
            .takeWhile { !it.isAfter(end) }
            .toList()
    }

    private fun planScopeIds(plan: PlanRecord): List<Long> {
        val planId = plan.get(PLAN.PLAN_ID)
        return listOf(planId) + planRepository.descendantIds(planId)
    }

    private fun logsForSchedules(
        userEmail: String,
        start: LocalDate,
        end: LocalDate,
        schedules: List<TaskScheduleRecord>,
    ): List<personal.jinhyeok.jooq.tables.records.WorkLogRecord> {
        val scheduleIds = schedules.map { it.get(TASK_SCHEDULE.SCHEDULE_ID) }.toSet()
        if (scheduleIds.isEmpty()) return emptyList()
        return workLogRepository.findBetween(userEmail, start, end)
            .filter { it.get(WORK_LOG.SCHEDULE_ID) in scheduleIds }
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

    private fun summary(planned: BigDecimal, worked: BigDecimal): Map<String, Any> =
        mapOf("plannedHours" to planned, "workedHours" to worked, "progressRate" to timeSupport.progressRate(worked, planned), "varianceHours" to worked.subtract(planned))

    private fun taskVariance(task: TaskRecord): Map<String, Any> {
        val worked = timeSupport.minutesToHours(workLogRepository.sumMinutesByTask(task.get(TASK.TASK_ID)))
        val planned = timeSupport.minutesToHours(task.get(TASK.PLANNED_MINUTES))
        return mapOf(
            "taskCode" to task.get(TASK.TASK_CODE),
            "title" to task.get(TASK.TITLE),
            "categoryPath" to taskCategoryRepository.pathById(task.get(TASK.CATEGORY_ID)),
            "plannedHours" to planned,
            "workedHours" to worked,
            "varianceHours" to worked.subtract(planned),
        )
    }

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
            categoryCode = taskCategoryRepository.findById(get(TASK.CATEGORY_ID))?.get(TASK_CATEGORY.CATEGORY_CODE) ?: "",
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

    companion object {
        private val YEAR_MONTH_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
    }
}
