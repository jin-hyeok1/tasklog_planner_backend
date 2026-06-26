package personal.jinhyeok.tasklog_planner_backend.web.inbound.dto

data class QuarterlyScheduleSummaryDto(
    val planId: Long,
    val planCode: String,
    val weekPlanCount: Int,
    val plannedMinutesPerWeek: Int,
    val investedMinutesPerWeek: Int,
)
