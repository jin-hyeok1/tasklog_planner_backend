package personal.jinhyeok.tasklog_planner_backend.service.plan

import personal.jinhyeok.jooq.Tables.PLAN
import org.jooq.Condition
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import personal.jinhyeok.tasklog_planner_backend.repository.plan.PlanRepository
import personal.jinhyeok.tasklog_planner_backend.repository.plan.PlanSaveCommand
import personal.jinhyeok.tasklog_planner_backend.repository.plan.PlanUpdateCommand
import personal.jinhyeok.tasklog_planner_backend.repository.schedule.TaskScheduleRepository
import personal.jinhyeok.tasklog_planner_backend.repository.user.AppUserRepository
import personal.jinhyeok.tasklog_planner_backend.service.support.TimeSupport
import personal.jinhyeok.tasklog_planner_backend.service.support.toDto
import personal.jinhyeok.tasklog_planner_backend.service.support.toJooq
import personal.jinhyeok.tasklog_planner_backend.enumeration.ApiCode
import personal.jinhyeok.tasklog_planner_backend.exception.ApiException
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.PlanDto
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.PlanStatus
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.PlanTreeDto
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.PlanType
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.SavePlanRequest

@Service
class PlanService(
    private val appUserRepository: AppUserRepository,
    private val planRepository: PlanRepository,
    private val taskScheduleRepository: TaskScheduleRepository,
    private val timeSupport: TimeSupport,
) {
    fun planTree(year: Int?): List<PlanTreeDto> {
        val rows = planRepository.findTreeCandidates(appUserRepository.currentUserEmail(), year)
        val byParent = rows.groupBy { it.get(PLAN.PARENT_PLAN_ID) }
        fun build(parentId: Long?): List<PlanTreeDto> =
            byParent[parentId].orEmpty().map { row ->
                val dto = row.toDto()
                PlanTreeDto(
                    planCode = dto.planCode,
                    parentPlanCode = dto.parentPlanCode,
                    planType = dto.planType,
                    title = dto.title,
                    startDate = dto.startDate,
                    endDate = dto.endDate,
                    plannedHours = dto.plannedHours,
                    status = dto.status,
                    description = dto.description,
                    children = build(row.get(PLAN.PLAN_ID)),
                )
            }
        return build(null)
    }

    fun plans(planType: PlanType?, parentPlanCode: String?, year: Int?): List<PlanDto> {
        val userEmail = appUserRepository.currentUserEmail()
        var condition: Condition = org.jooq.impl.DSL.trueCondition()
        if (planType != null) condition = condition.and(PLAN.PLAN_TYPE.eq(planType.toJooq()))
        if (parentPlanCode != null) condition = condition.and(PLAN.PARENT_PLAN_ID.eq(planRepository.idByCode(userEmail, parentPlanCode)))
        if (year != null) condition = condition.and(planRepository.overlapsYear(year))
        return planRepository.findAll(userEmail, condition).map { it.toDto() }
    }

    @Transactional
    fun createPlan(request: SavePlanRequest): PlanDto {
        val planType = request.planType ?: timeSupport.missing("planType")
        val startDate = request.startDate ?: timeSupport.missing("startDate")
        val endDate = request.endDate ?: timeSupport.missing("endDate")
        if (endDate.isBefore(startDate)) throw ApiException(ApiCode.BAD_REQUEST, "endDate must be greater than or equal to startDate")
        val userEmail = appUserRepository.currentUserEmail()
        val row = planRepository.insert(
            PlanSaveCommand(
                userEmail = userEmail,
                parentPlanId = request.parentSelection?.parentPlanCode?.let { planRepository.idByCode(userEmail, it) },
                planCode = timeSupport.nextPlanCode(planType, startDate),
                planType = planType.toJooq(),
                title = request.title ?: timeSupport.missing("title"),
                startDate = startDate,
                endDate = endDate,
                status = (request.status ?: PlanStatus.ACTIVE).toJooq(),
                description = request.description,
            ),
        )
        return row.toDto()
    }

    @Transactional
    fun updatePlan(planCode: String, request: SavePlanRequest): PlanDto {
        val userEmail = appUserRepository.currentUserEmail()
        val current = planRepository.findByCode(userEmail, planCode)
        val startDate = request.startDate ?: current.get(PLAN.START_DATE)
        val endDate = request.endDate ?: current.get(PLAN.END_DATE)
        if (endDate.isBefore(startDate)) throw ApiException(ApiCode.BAD_REQUEST, "endDate must be greater than or equal to startDate")
        val row = planRepository.update(
            planCode,
            PlanUpdateCommand(
                userEmail = userEmail,
                parentPlanId = request.parentSelection?.parentPlanCode?.let { planRepository.idByCode(userEmail, it) } ?: current.get(PLAN.PARENT_PLAN_ID),
                planType = (request.planType ?: current.get(PLAN.PLAN_TYPE).toDto()).toJooq(),
                title = request.title ?: current.get(PLAN.TITLE),
                startDate = startDate,
                endDate = endDate,
                status = (request.status ?: current.get(PLAN.STATUS).toDto()).toJooq(),
                description = request.description ?: current.get(PLAN.DESCRIPTION),
                updatedAt = timeSupport.now(),
            ),
        )
        return row.toDto()
    }

    @Transactional
    fun deletePlan(planCode: String, cascade: Boolean): Map<String, List<String>> {
        val target = planRepository.findByCode(appUserRepository.currentUserEmail(), planCode)
        val ids = if (cascade) planRepository.descendantIds(target.get(PLAN.PLAN_ID)) + target.get(PLAN.PLAN_ID) else listOf(target.get(PLAN.PLAN_ID))
        return mapOf("deletedPlanCodes" to planRepository.deleteByIds(ids))
    }

    private fun personal.jinhyeok.jooq.tables.records.PlanRecord.toDto(): PlanDto =
        PlanDto(
            planId = get(PLAN.PLAN_ID),
            planCode = get(PLAN.PLAN_CODE),
            parentPlanCode = planRepository.findById(get(PLAN.PARENT_PLAN_ID))?.get(PLAN.PLAN_CODE),
            planType = get(PLAN.PLAN_TYPE).toDto(),
            title = get(PLAN.TITLE),
            startDate = get(PLAN.START_DATE),
            endDate = get(PLAN.END_DATE),
            plannedHours = timeSupport.minutesToHours(taskScheduleRepository.sumPlannedMinutesByPlanIds(planScopeIds(get(PLAN.PLAN_ID)))),
            status = get(PLAN.STATUS).toDto(),
            description = get(PLAN.DESCRIPTION),
            createdAt = get(PLAN.CREATED_AT),
            updatedAt = get(PLAN.UPDATED_AT),
        )

    private fun planScopeIds(planId: Long): List<Long> =
        listOf(planId) + planRepository.descendantIds(planId)
}
