package personal.jinhyeok.tasklog_planner_backend.web.inbound.dto

import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime

data class PlanDto(
    val planId: Long,
    val planCode: String,
    val parentPlanCode: String? = null,
    val planType: PlanType,
    val title: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val plannedHours: BigDecimal,
    val status: PlanStatus,
    val description: String? = null,
    val createdAt: OffsetDateTime? = null,
    val updatedAt: OffsetDateTime? = null,
)

data class PlanTreeDto(
    val planCode: String,
    val parentPlanCode: String? = null,
    val planType: PlanType,
    val title: String,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val plannedHours: BigDecimal? = null,
    val status: PlanStatus? = null,
    val description: String? = null,
    val children: List<PlanTreeDto> = emptyList(),
)

data class ParentSelectionDto(val parentPlanCode: String? = null)

data class SavePlanRequest(
    val planType: PlanType? = null,
    val title: String? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val status: PlanStatus? = null,
    val description: String? = null,
    val parentSelection: ParentSelectionDto? = null,
)
