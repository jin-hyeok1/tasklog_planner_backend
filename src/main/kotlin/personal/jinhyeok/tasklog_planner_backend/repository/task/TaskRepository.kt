package personal.jinhyeok.tasklog_planner_backend.repository.task

import personal.jinhyeok.jooq.Tables.TASK
import personal.jinhyeok.jooq.enums.TaskPriority as JTaskPriority
import personal.jinhyeok.jooq.enums.TaskStatus as JTaskStatus
import personal.jinhyeok.jooq.tables.records.TaskRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import personal.jinhyeok.tasklog_planner_backend.enumeration.ApiCode
import personal.jinhyeok.tasklog_planner_backend.exception.ApiException
import personal.jinhyeok.tasklog_planner_backend.repository.user.setNullable

@Repository
class TaskRepository(private val dsl: DSLContext) {
    fun findByCode(userEmail: String, taskCode: String): TaskRecord =
        dsl.selectFrom(TASK).where(TASK.USER_EMAIL.eq(userEmail)).and(TASK.TASK_CODE.eq(taskCode)).fetchOne()
            ?: throw ApiException(ApiCode.NOT_FOUND, "Task not found: $taskCode")

    fun findById(taskId: Long): TaskRecord =
        dsl.selectFrom(TASK).where(TASK.TASK_ID.eq(taskId)).fetchOne()
            ?: throw ApiException(ApiCode.NOT_FOUND, "Task not found")

    fun findAll(userEmail: String, condition: Condition): List<TaskRecord> =
        dsl.selectFrom(TASK).where(TASK.USER_EMAIL.eq(userEmail)).and(condition).orderBy(TASK.TASK_CODE.asc()).fetch()

    fun findAllByUser(userEmail: String): List<TaskRecord> =
        dsl.selectFrom(TASK).where(TASK.USER_EMAIL.eq(userEmail)).fetch()

    fun findAllByStatus(userEmail: String, status: JTaskStatus): List<TaskRecord> =
        dsl.selectFrom(TASK).where(TASK.USER_EMAIL.eq(userEmail)).and(TASK.STATUS.eq(status)).fetch()

    fun insert(command: TaskSaveCommand): TaskRecord =
        dsl.insertInto(TASK)
            .set(TASK.USER_EMAIL, command.userEmail)
            .set(TASK.CATEGORY_ID, command.categoryId)
            .set(TASK.TASK_CODE, command.taskCode)
            .set(TASK.TITLE, command.title)
            .set(TASK.DESCRIPTION, command.description)
            .set(TASK.PLANNED_MINUTES, command.plannedMinutes)
            .set(TASK.START_DATE, command.startDate)
            .set(TASK.DUE_DATE, command.dueDate)
            .set(TASK.STATUS, command.status)
            .set(TASK.PRIORITY, command.priority)
            .setNullable(TASK.IS_TASK, command.isTask)
            .returning()
            .fetchOne() ?: throw ApiException(ApiCode.INTERNAL_ERROR, "Failed to create task")

    fun update(taskCode: String, command: TaskUpdateCommand): TaskRecord =
        dsl.update(TASK)
            .set(TASK.CATEGORY_ID, command.categoryId)
            .set(TASK.TITLE, command.title)
            .set(TASK.DESCRIPTION, command.description)
            .set(TASK.PLANNED_MINUTES, command.plannedMinutes)
            .set(TASK.START_DATE, command.startDate)
            .set(TASK.DUE_DATE, command.dueDate)
            .set(TASK.STATUS, command.status)
            .set(TASK.PRIORITY, command.priority)
            .set(TASK.UPDATED_AT, command.updatedAt)
            .set(TASK.IS_TASK, command.isTask)
            .where(TASK.TASK_CODE.eq(taskCode))
            .and(TASK.USER_EMAIL.eq(command.userEmail))
            .returning()
            .fetchOne() ?: throw ApiException(ApiCode.NOT_FOUND, "Task not found: $taskCode")

    fun delete(taskId: Long): Int = dsl.deleteFrom(TASK).where(TASK.TASK_ID.eq(taskId)).execute()

    fun updateStatus(taskId: Long, status: JTaskStatus, updatedAt: java.time.OffsetDateTime): Int =
        dsl.update(TASK).set(TASK.STATUS, status).set(TASK.UPDATED_AT, updatedAt).where(TASK.TASK_ID.eq(taskId)).execute()
}

data class TaskSaveCommand(
    val userEmail: String,
    val categoryId: Long,
    val taskCode: String,
    val title: String,
    val description: String?,
    val plannedMinutes: Int,
    val startDate: java.time.LocalDate?,
    val dueDate: java.time.LocalDate?,
    val status: JTaskStatus,
    val priority: JTaskPriority,
    val isTask: Boolean?
)

data class TaskUpdateCommand(
    val userEmail: String,
    val categoryId: Long,
    val title: String,
    val description: String?,
    val plannedMinutes: Int,
    val startDate: java.time.LocalDate?,
    val dueDate: java.time.LocalDate?,
    val status: JTaskStatus,
    val priority: JTaskPriority,
    val updatedAt: java.time.OffsetDateTime,
    val isTask: Boolean?,
)
