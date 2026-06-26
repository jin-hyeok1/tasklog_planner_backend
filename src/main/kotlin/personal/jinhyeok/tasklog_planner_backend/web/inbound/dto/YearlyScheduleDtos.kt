package personal.jinhyeok.tasklog_planner_backend.web.inbound.dto

data class YearlyScheduleSummaryDto(
    val year: Int,
    val weekPlanCount: Int,
    val plannedMinutesPerWeek: Int,
    val investedMinutesPerWeek: Int,
)

data class MonthlyScheduleInvestmentDto(
    val month: String,
    val weekPlanCount: Int,
    val plannedMinutesPerWeek: Int,
    val investedMinutesPerWeek: Int,
)

data class MonthlyTaskDetailDto(
    val month: String,
    val taskCount: Int,
    val weekPlanCount: Int,
    val plannedMinutesPerWeek: Int,
    val investedMinutesPerWeek: Int,
    val tasks: List<MonthlyTaskInvestmentDto>,
)

data class MonthlyTaskInvestmentDto(
    val taskCode: String,
    val title: String,
    val plannedMinutesPerWeek: Int,
    val investedMinutesPerWeek: Int,
)
