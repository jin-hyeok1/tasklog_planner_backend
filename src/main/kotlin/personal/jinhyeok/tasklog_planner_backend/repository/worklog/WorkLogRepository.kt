package personal.jinhyeok.tasklog_planner_backend.repository.worklog

import personal.jinhyeok.jooq.Tables.WORK_LOG
import personal.jinhyeok.jooq.enums.WorkLogSource as JWorkLogSource
import personal.jinhyeok.jooq.tables.records.WorkLogRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import personal.jinhyeok.tasklog_planner_backend.enumeration.ApiCode
import personal.jinhyeok.tasklog_planner_backend.exception.ApiException
import java.time.LocalDate

@Repository
class WorkLogRepository(private val dsl: DSLContext) {
    fun findBetween(userEmail: String, start: LocalDate, end: LocalDate): List<WorkLogRecord> =
        dsl.selectFrom(WORK_LOG).where(WORK_LOG.USER_EMAIL.eq(userEmail)).and(WORK_LOG.WORKED_DATE.between(start, end)).fetch()

    fun findAll(condition: Condition): List<WorkLogRecord> =
        dsl.selectFrom(WORK_LOG).where(condition).orderBy(WORK_LOG.WORKED_DATE.asc(), WORK_LOG.SEQUENCE_NO.asc()).fetch()

    fun findByKey(taskId: Long, workedDate: LocalDate, sequenceNo: Int): WorkLogRecord =
        dsl.selectFrom(WORK_LOG).where(WORK_LOG.TASK_ID.eq(taskId)).and(WORK_LOG.WORKED_DATE.eq(workedDate)).and(WORK_LOG.SEQUENCE_NO.eq(sequenceNo)).fetchOne()
            ?: throw ApiException(ApiCode.NOT_FOUND, "WorkLog not found")

    fun nextSequence(taskId: Long, workedDate: LocalDate): Int =
        dsl.select(DSL.max(WORK_LOG.SEQUENCE_NO)).from(WORK_LOG).where(WORK_LOG.TASK_ID.eq(taskId)).and(WORK_LOG.WORKED_DATE.eq(workedDate)).fetchOne(0, Int::class.java)?.plus(1) ?: 1

    fun sumMinutesByTask(taskId: Long): Int =
        dsl.select(DSL.coalesce(DSL.sum(WORK_LOG.WORKED_MINUTES), 0)).from(WORK_LOG).where(WORK_LOG.TASK_ID.eq(taskId)).fetchOne(0, Int::class.java) ?: 0

    fun sumMinutesBySchedule(taskId: Long, scheduleId: Long?, workedDate: LocalDate): Int =
        dsl.select(DSL.coalesce(DSL.sum(WORK_LOG.WORKED_MINUTES), 0)).from(WORK_LOG).where(WORK_LOG.TASK_ID.eq(taskId)).and(WORK_LOG.SCHEDULE_ID.eq(scheduleId)).and(WORK_LOG.WORKED_DATE.eq(workedDate)).fetchOne(0, Int::class.java) ?: 0

    fun sumMinutesBetween(userEmail: String, start: LocalDate, end: LocalDate): Int =
        dsl.select(DSL.coalesce(DSL.sum(WORK_LOG.WORKED_MINUTES), 0)).from(WORK_LOG).where(WORK_LOG.USER_EMAIL.eq(userEmail)).and(WORK_LOG.WORKED_DATE.between(start, end)).fetchOne(0, Int::class.java) ?: 0

    fun existsByDate(userEmail: String, date: LocalDate): Boolean =
        dsl.fetchExists(WORK_LOG, WORK_LOG.USER_EMAIL.eq(userEmail).and(WORK_LOG.WORKED_DATE.eq(date)))

    fun deleteByTaskId(taskId: Long): Int = dsl.deleteFrom(WORK_LOG).where(WORK_LOG.TASK_ID.eq(taskId)).execute()

    fun insert(command: WorkLogSaveCommand): WorkLogRecord =
        dsl.insertInto(WORK_LOG)
            .set(WORK_LOG.TASK_ID, command.taskId)
            .set(WORK_LOG.WORKED_DATE, command.workedDate)
            .set(WORK_LOG.SEQUENCE_NO, command.sequenceNo)
            .set(WORK_LOG.USER_EMAIL, command.userEmail)
            .set(WORK_LOG.SCHEDULE_ID, command.scheduleId)
            .set(WORK_LOG.STARTED_AT, command.startedAt)
            .set(WORK_LOG.ENDED_AT, command.endedAt)
            .set(WORK_LOG.WORKED_MINUTES, command.workedMinutes)
            .set(WORK_LOG.MEMO, command.memo)
            .set(WORK_LOG.SOURCE, command.source)
            .returning()
            .fetchOne() ?: throw ApiException(ApiCode.INTERNAL_ERROR, "Failed to create work log")

    fun update(taskId: Long, workedDate: LocalDate, sequenceNo: Int, command: WorkLogUpdateCommand): WorkLogRecord =
        dsl.update(WORK_LOG)
            .set(WORK_LOG.SCHEDULE_ID, command.scheduleId)
            .set(WORK_LOG.STARTED_AT, command.startedAt)
            .set(WORK_LOG.ENDED_AT, command.endedAt)
            .set(WORK_LOG.WORKED_MINUTES, command.workedMinutes)
            .set(WORK_LOG.MEMO, command.memo)
            .set(WORK_LOG.UPDATED_AT, command.updatedAt)
            .where(WORK_LOG.TASK_ID.eq(taskId))
            .and(WORK_LOG.WORKED_DATE.eq(workedDate))
            .and(WORK_LOG.SEQUENCE_NO.eq(sequenceNo))
            .returning()
            .fetchOne() ?: throw ApiException(ApiCode.NOT_FOUND, "WorkLog not found")

    fun delete(taskId: Long, workedDate: LocalDate, sequenceNo: Int): Int =
        dsl.deleteFrom(WORK_LOG)
            .where(WORK_LOG.TASK_ID.eq(taskId))
            .and(WORK_LOG.WORKED_DATE.eq(workedDate))
            .and(WORK_LOG.SEQUENCE_NO.eq(sequenceNo))
            .execute()
}

data class WorkLogSaveCommand(
    val userEmail: String,
    val taskId: Long,
    val scheduleId: Long?,
    val workedDate: LocalDate,
    val sequenceNo: Int,
    val startedAt: java.time.OffsetDateTime,
    val endedAt: java.time.OffsetDateTime,
    val workedMinutes: Int,
    val memo: String?,
    val source: JWorkLogSource,
)

data class WorkLogUpdateCommand(
    val scheduleId: Long?,
    val startedAt: java.time.OffsetDateTime,
    val endedAt: java.time.OffsetDateTime,
    val workedMinutes: Int,
    val memo: String?,
    val updatedAt: java.time.OffsetDateTime,
)
