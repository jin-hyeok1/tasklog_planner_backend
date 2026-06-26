package personal.jinhyeok.tasklog_planner_backend.web.inbound.dto

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.OffsetTime

data class UserDto(
    val userCode: String,
    val email: String,
    val username: String,
    val displayName: String,
    val role: UserRole,
    val timezone: String,
    val weeklyTargetHours: BigDecimal,
    val dailyStartTime: OffsetTime?,
    val dailyEndTime: OffsetTime?,
    val notificationEnabled: Boolean,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)

data class UpdateUserRequest(
    val email: String? = null,
    val username: String? = null,
    val displayName: String? = null,
    val timezone: String? = null,
    val weeklyTargetHours: BigDecimal? = null,
    val dailyStartTime: OffsetTime? = null,
    val dailyEndTime: OffsetTime? = null,
    val notificationEnabled: Boolean? = null,
)
