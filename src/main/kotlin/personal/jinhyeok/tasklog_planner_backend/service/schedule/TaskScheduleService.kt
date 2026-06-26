package personal.jinhyeok.tasklog_planner_backend.service.schedule

import personal.jinhyeok.jooq.Tables.TASK
import personal.jinhyeok.jooq.Tables.TASK_SCHEDULE
import personal.jinhyeok.jooq.tables.records.TaskScheduleRecord
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import personal.jinhyeok.tasklog_planner_backend.enumeration.ApiCode
import personal.jinhyeok.tasklog_planner_backend.exception.ApiException
import personal.jinhyeok.tasklog_planner_backend.repository.plan.PlanRepository
import personal.jinhyeok.tasklog_planner_backend.repository.schedule.TaskScheduleRepository
import personal.jinhyeok.tasklog_planner_backend.repository.schedule.TaskScheduleSaveCommand
import personal.jinhyeok.tasklog_planner_backend.repository.schedule.TaskScheduleUpdateCommand
import personal.jinhyeok.tasklog_planner_backend.repository.session.TaskSessionRepository
import personal.jinhyeok.tasklog_planner_backend.repository.task.TaskRepository
import personal.jinhyeok.tasklog_planner_backend.repository.user.AppUserRepository
import personal.jinhyeok.tasklog_planner_backend.service.support.TimeSupport
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.SaveTaskScheduleRequest
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.TaskScheduleDto

@Service
class TaskScheduleService(
    private val appUserRepository: AppUserRepository,
    private val planRepository: PlanRepository,
    private val taskRepository: TaskRepository,
    private val taskScheduleRepository: TaskScheduleRepository,
    private val taskSessionRepository: TaskSessionRepository,
    private val timeSupport: TimeSupport,
) {
    @Transactional
    fun createSchedule(request: SaveTaskScheduleRequest): TaskScheduleDto {
        val userEmail = appUserRepository.currentUserEmail()
        val planId = planRepository.idByCode(userEmail, request.planCode ?: timeSupport.missing("planCode"))
        val task = taskRepository.findByCode(userEmail, request.taskCode ?: timeSupport.missing("taskCode"))
        val startTime = request.startTime ?: timeSupport.missing("startTime")
        val endTime = request.endTime ?: timeSupport.missing("endTime")
        if (!startTime.isBefore(endTime)) throw ApiException(ApiCode.BAD_REQUEST, "startTime must be before endTime")
        return taskScheduleRepository.insert(
            TaskScheduleSaveCommand(
                userEmail = userEmail,
                taskId = task.get(TASK.TASK_ID),
                planId = planId,
                scheduleCode = timeSupport.nextCode("SCHEDULE"),
                scheduledDate = request.scheduledDate ?: timeSupport.missing("scheduledDate"),
                startTime = startTime,
                endTime = endTime,
                plannedMinutes = timeSupport.minutesBetween(startTime, endTime),
                memo = request.memo,
            ),
        ).toDto()
    }

    @Transactional
    fun updateSchedule(scheduleCode: String, request: SaveTaskScheduleRequest): TaskScheduleDto {
        val userEmail = appUserRepository.currentUserEmail()
        val current = taskScheduleRepository.findByCode(userEmail, scheduleCode)
        val startTime = request.startTime ?: current.get(TASK_SCHEDULE.START_TIME)
        val endTime = request.endTime ?: current.get(TASK_SCHEDULE.END_TIME)
        if (!startTime.isBefore(endTime)) throw ApiException(ApiCode.BAD_REQUEST, "startTime must be before endTime")
        return taskScheduleRepository.update(
            scheduleCode,
            TaskScheduleUpdateCommand(
                userEmail = userEmail,
                taskId = request.taskCode?.let { taskRepository.findByCode(userEmail, it).get(TASK.TASK_ID) } ?: current.get(TASK_SCHEDULE.TASK_ID),
                planId = request.planCode?.let { planRepository.idByCode(userEmail, it) } ?: current.get(TASK_SCHEDULE.PLAN_ID),
                scheduledDate = request.scheduledDate ?: current.get(TASK_SCHEDULE.SCHEDULED_DATE),
                startTime = startTime,
                endTime = endTime,
                plannedMinutes = timeSupport.minutesBetween(startTime, endTime),
                memo = request.memo ?: current.get(TASK_SCHEDULE.MEMO),
                updatedAt = timeSupport.now(),
            ),
        ).toDto()
    }

    @Transactional
    fun deleteSchedule(scheduleCode: String): Map<String, String> {
        val schedule = taskScheduleRepository.findByCode(appUserRepository.currentUserEmail(), scheduleCode)
        taskSessionRepository.deleteByScheduleId(schedule.get(TASK_SCHEDULE.SCHEDULE_ID))
        taskScheduleRepository.deleteById(schedule.get(TASK_SCHEDULE.SCHEDULE_ID))
        return mapOf("deletedScheduleCode" to scheduleCode)
    }

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
