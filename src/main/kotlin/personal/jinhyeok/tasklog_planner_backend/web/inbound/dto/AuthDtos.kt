package personal.jinhyeok.tasklog_planner_backend.web.inbound.dto

import java.time.OffsetDateTime
import java.time.OffsetTime

data class SignUpRequest(
    val email: String? = null,
    val username: String? = null,
    val displayName: String? = null,
    val password: String? = null,
    val timezone: String? = null,
    val weeklyTargetHours: java.math.BigDecimal? = null,
    val dailyStartTime: OffsetTime? = null,
    val dailyEndTime: OffsetTime? = null,
    val notificationEnabled: Boolean? = null,
)

data class LoginRequest(
    val email: String? = null,
    val password: String? = null,
)

data class AuthResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresAt: OffsetDateTime,
    val user: UserDto,
)
