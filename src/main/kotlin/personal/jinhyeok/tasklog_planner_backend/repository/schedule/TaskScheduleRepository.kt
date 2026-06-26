package personal.jinhyeok.tasklog_planner_backend.repository.schedule

import personal.jinhyeok.jooq.Tables.TASK_SCHEDULE
import personal.jinhyeok.jooq.tables.records.TaskScheduleRecord
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import personal.jinhyeok.tasklog_planner_backend.enumeration.ApiCode
import personal.jinhyeok.tasklog_planner_backend.exception.ApiException
import java.time.LocalDate
import java.time.OffsetTime

@Repository
class TaskScheduleRepository(private val dsl: DSLContext) {
    fun findByCode(userEmail: String, scheduleCode: String): TaskScheduleRecord =
        dsl.selectFrom(TASK_SCHEDULE).where(TASK_SCHEDULE.USER_EMAIL.eq(userEmail)).and(TASK_SCHEDULE.SCHEDULE_CODE.eq(scheduleCode)).fetchOne()
            ?: throw ApiException(ApiCode.NOT_FOUND, "TaskSchedule not found: $scheduleCode")

    fun findById(scheduleId: Long?): TaskScheduleRecord? =
        scheduleId?.let { dsl.selectFrom(TASK_SCHEDULE).where(TASK_SCHEDULE.SCHEDULE_ID.eq(it)).fetchOne() }

    fun findByTaskId(taskId: Long): List<TaskScheduleRecord> =
        dsl.selectFrom(TASK_SCHEDULE).where(TASK_SCHEDULE.TASK_ID.eq(taskId)).orderBy(TASK_SCHEDULE.SCHEDULED_DATE, TASK_SCHEDULE.START_TIME).fetch()

    fun findByPlanIds(userEmail: String, planIds: Collection<Long>): List<TaskScheduleRecord> {
        if (planIds.isEmpty()) return emptyList()
        return dsl.selectFrom(TASK_SCHEDULE)
            .where(TASK_SCHEDULE.USER_EMAIL.eq(userEmail))
            .and(TASK_SCHEDULE.PLAN_ID.`in`(planIds))
            .orderBy(TASK_SCHEDULE.SCHEDULED_DATE.asc(), TASK_SCHEDULE.START_TIME.asc())
            .fetch()
    }

    fun findBetween(userEmail: String, start: LocalDate, end: LocalDate): List<TaskScheduleRecord> =
        dsl.selectFrom(TASK_SCHEDULE).where(TASK_SCHEDULE.USER_EMAIL.eq(userEmail)).and(TASK_SCHEDULE.SCHEDULED_DATE.between(start, end)).fetch()

    fun findBetweenOrdered(userEmail: String, start: LocalDate, end: LocalDate): List<TaskScheduleRecord> =
        dsl.selectFrom(TASK_SCHEDULE)
            .where(TASK_SCHEDULE.USER_EMAIL.eq(userEmail))
            .and(TASK_SCHEDULE.SCHEDULED_DATE.between(start, end))
            .orderBy(TASK_SCHEDULE.SCHEDULED_DATE.asc(), TASK_SCHEDULE.START_TIME.asc(), TASK_SCHEDULE.SCHEDULE_CODE.asc())
            .fetch()

    fun findToday(userEmail: String, date: LocalDate): List<TaskScheduleRecord> =
        dsl.selectFrom(TASK_SCHEDULE)
            .where(TASK_SCHEDULE.USER_EMAIL.eq(userEmail))
            .and(TASK_SCHEDULE.SCHEDULED_DATE.eq(date))
            .orderBy(TASK_SCHEDULE.START_TIME.asc(), TASK_SCHEDULE.SCHEDULE_CODE.asc())
            .fetch()

    fun existsByDate(userEmail: String, date: LocalDate): Boolean =
        dsl.fetchExists(TASK_SCHEDULE, TASK_SCHEDULE.USER_EMAIL.eq(userEmail).and(TASK_SCHEDULE.SCHEDULED_DATE.eq(date)))

    fun sumPlannedMinutesByPlanId(planId: Long): Int =
        dsl.select(DSL.coalesce(DSL.sum(TASK_SCHEDULE.PLANNED_MINUTES), 0))
            .from(TASK_SCHEDULE)
            .where(TASK_SCHEDULE.PLAN_ID.eq(planId))
            .fetchOne(0, Int::class.java) ?: 0

    fun sumPlannedMinutesByPlanIds(planIds: Collection<Long>): Int {
        if (planIds.isEmpty()) return 0
        return dsl.select(DSL.coalesce(DSL.sum(TASK_SCHEDULE.PLANNED_MINUTES), 0))
            .from(TASK_SCHEDULE)
            .where(TASK_SCHEDULE.PLAN_ID.`in`(planIds))
            .fetchOne(0, Int::class.java) ?: 0
    }

    fun deleteByTaskId(taskId: Long): Int = dsl.deleteFrom(TASK_SCHEDULE).where(TASK_SCHEDULE.TASK_ID.eq(taskId)).execute()
    fun deleteById(scheduleId: Long): Int = dsl.deleteFrom(TASK_SCHEDULE).where(TASK_SCHEDULE.SCHEDULE_ID.eq(scheduleId)).execute()

    fun insert(command: TaskScheduleSaveCommand): TaskScheduleRecord =
        dsl.insertInto(TASK_SCHEDULE)
            .set(TASK_SCHEDULE.USER_EMAIL, command.userEmail)
            .set(TASK_SCHEDULE.TASK_ID, command.taskId)
            .set(TASK_SCHEDULE.PLAN_ID, command.planId)
            .set(TASK_SCHEDULE.SCHEDULE_CODE, command.scheduleCode)
            .set(TASK_SCHEDULE.SCHEDULED_DATE, command.scheduledDate)
            .set(TASK_SCHEDULE.START_TIME, command.startTime)
            .set(TASK_SCHEDULE.END_TIME, command.endTime)
            .set(TASK_SCHEDULE.PLANNED_MINUTES, command.plannedMinutes)
            .set(TASK_SCHEDULE.MEMO, command.memo)
            .returning()
            .fetchOne() ?: throw ApiException(ApiCode.INTERNAL_ERROR, "Failed to create schedule")

    fun update(scheduleCode: String, command: TaskScheduleUpdateCommand): TaskScheduleRecord =
        dsl.update(TASK_SCHEDULE)
            .set(TASK_SCHEDULE.TASK_ID, command.taskId)
            .set(TASK_SCHEDULE.PLAN_ID, command.planId)
            .set(TASK_SCHEDULE.SCHEDULED_DATE, command.scheduledDate)
            .set(TASK_SCHEDULE.START_TIME, command.startTime)
            .set(TASK_SCHEDULE.END_TIME, command.endTime)
            .set(TASK_SCHEDULE.PLANNED_MINUTES, command.plannedMinutes)
            .set(TASK_SCHEDULE.MEMO, command.memo)
            .set(TASK_SCHEDULE.UPDATED_AT, command.updatedAt)
            .where(TASK_SCHEDULE.SCHEDULE_CODE.eq(scheduleCode))
            .and(TASK_SCHEDULE.USER_EMAIL.eq(command.userEmail))
            .returning()
            .fetchOne() ?: throw ApiException(ApiCode.NOT_FOUND, "TaskSchedule not found: $scheduleCode")
}

data class TaskScheduleSaveCommand(
    val userEmail: String,
    val taskId: Long,
    val planId: Long,
    val scheduleCode: String,
    val scheduledDate: LocalDate,
    val startTime: OffsetTime,
    val endTime: OffsetTime,
    val plannedMinutes: Int,
    val memo: String?,
)

data class TaskScheduleUpdateCommand(
    val userEmail: String,
    val taskId: Long,
    val planId: Long,
    val scheduledDate: LocalDate,
    val startTime: OffsetTime,
    val endTime: OffsetTime,
    val plannedMinutes: Int,
    val memo: String?,
    val updatedAt: java.time.OffsetDateTime,
)
