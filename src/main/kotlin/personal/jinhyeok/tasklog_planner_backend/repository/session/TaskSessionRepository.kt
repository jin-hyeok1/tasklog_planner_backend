package personal.jinhyeok.tasklog_planner_backend.repository.session

import personal.jinhyeok.jooq.Tables.TASK_SESSION
import personal.jinhyeok.jooq.enums.TaskSessionStatus as JTaskSessionStatus
import personal.jinhyeok.jooq.tables.records.TaskSessionRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import personal.jinhyeok.tasklog_planner_backend.enumeration.ApiCode
import personal.jinhyeok.tasklog_planner_backend.exception.ApiException

@Repository
class TaskSessionRepository(private val dsl: DSLContext) {
    fun findByCode(userEmail: String, sessionCode: String): TaskSessionRecord =
        dsl.selectFrom(TASK_SESSION).where(TASK_SESSION.USER_EMAIL.eq(userEmail)).and(TASK_SESSION.SESSION_CODE.eq(sessionCode)).fetchOne()
            ?: throw ApiException(ApiCode.NOT_FOUND, "TaskSession not found: $sessionCode")

    fun findLatestByScheduleId(scheduleId: Long): TaskSessionRecord? =
        dsl.selectFrom(TASK_SESSION).where(TASK_SESSION.SCHEDULE_ID.eq(scheduleId)).orderBy(TASK_SESSION.CREATED_AT.desc()).limit(1).fetchOne()

    fun findByScheduleIds(userEmail: String, scheduleIds: Collection<Long>): List<TaskSessionRecord> {
        if (scheduleIds.isEmpty()) return emptyList()
        return dsl.selectFrom(TASK_SESSION)
            .where(TASK_SESSION.USER_EMAIL.eq(userEmail))
            .and(TASK_SESSION.SCHEDULE_ID.`in`(scheduleIds))
            .fetch()
    }

    fun findRunning(userEmail: String): TaskSessionRecord? =
        dsl.selectFrom(TASK_SESSION).where(TASK_SESSION.USER_EMAIL.eq(userEmail)).and(TASK_SESSION.STATUS.eq(JTaskSessionStatus.RUNNING)).fetchOne()

    fun insert(command: TaskSessionSaveCommand): TaskSessionRecord =
        dsl.insertInto(TASK_SESSION)
            .set(TASK_SESSION.USER_EMAIL, command.userEmail)
            .set(TASK_SESSION.TASK_ID, command.taskId)
            .set(TASK_SESSION.SCHEDULE_ID, command.scheduleId)
            .set(TASK_SESSION.SESSION_CODE, command.sessionCode)
            .set(TASK_SESSION.WORKED_DATE, command.workedDate)
            .set(TASK_SESSION.STATUS, JTaskSessionStatus.RUNNING)
            .set(TASK_SESSION.STARTED_AT, command.now)
            .set(TASK_SESSION.LAST_STARTED_AT, command.now)
            .set(TASK_SESSION.ACCUMULATED_MINUTES, 0)
            .returning()
            .fetchOne() ?: throw ApiException(ApiCode.INTERNAL_ERROR, "Failed to start session")

    fun pause(sessionCode: String, accumulatedMinutes: Int, now: java.time.OffsetDateTime): TaskSessionRecord =
        dsl.update(TASK_SESSION)
            .set(TASK_SESSION.STATUS, JTaskSessionStatus.PAUSED)
            .set(TASK_SESSION.PAUSED_AT, now)
            .set(TASK_SESSION.ACCUMULATED_MINUTES, accumulatedMinutes)
            .set(TASK_SESSION.UPDATED_AT, now)
            .where(TASK_SESSION.SESSION_CODE.eq(sessionCode))
            .returning()
            .fetchOne() ?: throw ApiException(ApiCode.NOT_FOUND, "TaskSession not found: $sessionCode")

    fun resume(sessionCode: String, now: java.time.OffsetDateTime): TaskSessionRecord =
        dsl.update(TASK_SESSION)
            .set(TASK_SESSION.STATUS, JTaskSessionStatus.RUNNING)
            .set(TASK_SESSION.LAST_STARTED_AT, now)
            .set(TASK_SESSION.PAUSED_AT, null as java.time.OffsetDateTime?)
            .set(TASK_SESSION.UPDATED_AT, now)
            .where(TASK_SESSION.SESSION_CODE.eq(sessionCode))
            .returning()
            .fetchOne() ?: throw ApiException(ApiCode.NOT_FOUND, "TaskSession not found: $sessionCode")

    fun finish(sessionCode: String, accumulatedMinutes: Int, now: java.time.OffsetDateTime): TaskSessionRecord =
        dsl.update(TASK_SESSION)
            .set(TASK_SESSION.STATUS, JTaskSessionStatus.FINISHED)
            .set(TASK_SESSION.FINISHED_AT, now)
            .set(TASK_SESSION.ACCUMULATED_MINUTES, accumulatedMinutes)
            .set(TASK_SESSION.UPDATED_AT, now)
            .where(TASK_SESSION.SESSION_CODE.eq(sessionCode))
            .returning()
            .fetchOne() ?: throw ApiException(ApiCode.NOT_FOUND, "TaskSession not found: $sessionCode")

    fun deleteByTaskId(taskId: Long): Int = dsl.deleteFrom(TASK_SESSION).where(TASK_SESSION.TASK_ID.eq(taskId)).execute()
    fun deleteByScheduleId(scheduleId: Long): Int = dsl.deleteFrom(TASK_SESSION).where(TASK_SESSION.SCHEDULE_ID.eq(scheduleId)).execute()
}

data class TaskSessionSaveCommand(
    val userEmail: String,
    val taskId: Long,
    val scheduleId: Long?,
    val sessionCode: String,
    val workedDate: java.time.LocalDate,
    val now: java.time.OffsetDateTime,
)
