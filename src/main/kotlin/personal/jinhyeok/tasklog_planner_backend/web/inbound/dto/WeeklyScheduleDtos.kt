package personal.jinhyeok.tasklog_planner_backend.web.inbound.dto

import java.math.BigDecimal

data class WeeklyScheduleSummaryDto(
    val planId: Long,
    val planCode: String,
    val plannedMinutes: Int,
    val investedMinutes: Int,
    val progressRate: BigDecimal,
    val missingWorkLogDays: Int,
    val unreadNotificationCount: Int,
    val runningSession: RunningSessionSummaryDto? = null,
)
