package personal.jinhyeok.tasklog_planner_backend.service.task

import personal.jinhyeok.jooq.Tables.TASK
import personal.jinhyeok.jooq.Tables.TASK_CATEGORY
import personal.jinhyeok.jooq.Tables.TASK_SCHEDULE
import personal.jinhyeok.jooq.tables.records.TaskRecord
import personal.jinhyeok.jooq.tables.records.TaskScheduleRecord
import org.jooq.Condition
import org.jooq.impl.DSL
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import personal.jinhyeok.tasklog_planner_backend.repository.category.TaskCategoryRepository
import personal.jinhyeok.tasklog_planner_backend.repository.plan.PlanRepository
import personal.jinhyeok.tasklog_planner_backend.repository.schedule.TaskScheduleRepository
import personal.jinhyeok.tasklog_planner_backend.repository.session.TaskSessionRepository
import personal.jinhyeok.tasklog_planner_backend.repository.task.TaskRepository
import personal.jinhyeok.tasklog_planner_backend.repository.task.TaskSaveCommand
import personal.jinhyeok.tasklog_planner_backend.repository.task.TaskUpdateCommand
import personal.jinhyeok.tasklog_planner_backend.repository.user.AppUserRepository
import personal.jinhyeok.tasklog_planner_backend.repository.worklog.WorkLogRepository
import personal.jinhyeok.tasklog_planner_backend.service.support.TimeSupport
import personal.jinhyeok.tasklog_planner_backend.service.support.toDto
import personal.jinhyeok.tasklog_planner_backend.service.support.toJooq
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.SaveTaskRequest
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.TaskDto
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.TaskPriority
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.TaskScheduleDto
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.TaskStatus
import java.time.LocalDate

@Service
class TaskService(
    private val appUserRepository: AppUserRepository,
    private val planRepository: PlanRepository,
    private val taskCategoryRepository: TaskCategoryRepository,
    private val taskRepository: TaskRepository,
    private val taskScheduleRepository: TaskScheduleRepository,
    private val taskSessionRepository: TaskSessionRepository,
    private val workLogRepository: WorkLogRepository,
    private val timeSupport: TimeSupport,
) {
    fun tasks(status: TaskStatus?, categoryCode: String?, startDate: LocalDate?, endDate: LocalDate?): List<TaskDto> {
        val userEmail = appUserRepository.currentUserEmail()
        var condition: Condition = DSL.trueCondition()
        if (status != null) condition = condition.and(TASK.STATUS.eq(status.toJooq()))
        if (categoryCode != null) {
            val categoryId = taskCategoryRepository.idByCode(userEmail, categoryCode)
            val categoryIds = listOf(categoryId) + taskCategoryRepository.descendantIds(categoryId)
            condition = condition.and(TASK.CATEGORY_ID.`in`(categoryIds))
        }
        if (startDate != null) condition = condition.and(TASK.DUE_DATE.isNull.or(TASK.DUE_DATE.ge(startDate)))
        if (endDate != null) condition = condition.and(TASK.START_DATE.isNull.or(TASK.START_DATE.le(endDate)))
        return taskRepository.findAll(userEmail, condition).map { it.toDto() }
    }

    fun taskDetail(taskCode: String): TaskDto =
        taskRepository.findByCode(appUserRepository.currentUserEmail(), taskCode).toDto()

    fun taskSchedules(taskCode: String): List<TaskScheduleDto> {
        val task = taskRepository.findByCode(appUserRepository.currentUserEmail(), taskCode)
        return taskScheduleRepository.findByTaskId(task.get(TASK.TASK_ID)).map { it.toDto() }
    }

    @Transactional
    fun createTask(request: SaveTaskRequest): TaskDto {
        val userEmail = appUserRepository.currentUserEmail()
        val categoryCode = request.categoryCode ?: timeSupport.missing("categoryCode")
        return taskRepository.insert(
            TaskSaveCommand(
                userEmail = userEmail,
                categoryId = taskCategoryRepository.idByCode(userEmail, categoryCode),
                taskCode = timeSupport.nextCode("TASK"),
                title = request.title ?: timeSupport.missing("title"),
                description = request.description,
                plannedMinutes = request.plannedHours?.let(timeSupport::hoursToMinutes) ?: 0,
                startDate = request.startDate,
                dueDate = request.dueDate,
                status = (request.status ?: TaskStatus.TODO).toJooq(),
                priority = (request.priority ?: TaskPriority.MEDIUM).toJooq(),
                isTask = request.isTask,
            ),
        ).toDto()
    }

    @Transactional
    fun updateTask(taskCode: String, request: SaveTaskRequest): TaskDto {
        val userEmail = appUserRepository.currentUserEmail()
        val current = taskRepository.findByCode(userEmail, taskCode)
        return taskRepository.update(
            taskCode,
            TaskUpdateCommand(
                userEmail = userEmail,
                categoryId = request.categoryCode?.let { taskCategoryRepository.idByCode(userEmail, it) } ?: current.get(TASK.CATEGORY_ID),
                title = request.title ?: current.get(TASK.TITLE),
                description = request.description ?: current.get(TASK.DESCRIPTION),
                plannedMinutes = request.plannedHours?.let(timeSupport::hoursToMinutes) ?: current.get(TASK.PLANNED_MINUTES),
                startDate = request.startDate ?: current.get(TASK.START_DATE),
                dueDate = request.dueDate ?: current.get(TASK.DUE_DATE),
                status = (request.status ?: current.get(TASK.STATUS).toDto()).toJooq(),
                priority = (request.priority ?: current.get(TASK.PRIORITY).toDto()).toJooq(),
                updatedAt = timeSupport.now(),
                isTask = request.isTask,
            ),
        ).toDto()
    }

    @Transactional
    fun deleteTask(taskCode: String, cascade: Boolean): Map<String, String> {
        val task = taskRepository.findByCode(appUserRepository.currentUserEmail(), taskCode)
        val taskId = task.get(TASK.TASK_ID)
        if (cascade) {
            workLogRepository.deleteByTaskId(taskId)
            taskSessionRepository.deleteByTaskId(taskId)
            taskScheduleRepository.deleteByTaskId(taskId)
        }
        taskRepository.delete(taskId)
        return mapOf("deletedTaskCode" to taskCode)
    }

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

    private fun TaskScheduleRecord.toDto(): TaskScheduleDto =
        TaskScheduleDto(
            scheduleCode = get(TASK_SCHEDULE.SCHEDULE_CODE),
            planCode = planRepository.findById(get(TASK_SCHEDULE.PLAN_ID))?.get(personal.jinhyeok.jooq.Tables.PLAN.PLAN_CODE) ?: "",
            taskCode = taskRepository.findById(get(TASK_SCHEDULE.TASK_ID)).get(TASK.TASK_CODE),
            scheduledDate = get(TASK_SCHEDULE.SCHEDULED_DATE),
            startTime = get(TASK_SCHEDULE.START_TIME),
            endTime = get(TASK_SCHEDULE.END_TIME),
            plannedHours = timeSupport.minutesToHours(get(TASK_SCHEDULE.PLANNED_MINUTES)),
            memo = get(TASK_SCHEDULE.MEMO),
            createdAt = get(TASK_SCHEDULE.CREATED_AT),
            updatedAt = get(TASK_SCHEDULE.UPDATED_AT),
        )
}
