package personal.jinhyeok.tasklog_planner_backend.security

import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import personal.jinhyeok.tasklog_planner_backend.enumeration.ApiCode
import personal.jinhyeok.tasklog_planner_backend.exception.ApiException

@Component
class CurrentUserResolver {
    fun currentUserEmail(): String {
        val authentication: Authentication = SecurityContextHolder.getContext().authentication
            ?: throw ApiException(ApiCode.UNAUTHORIZED, "Authenticated user is required")
        val principal = authentication.principal
        return when (principal) {
            is String -> principal
            else -> authentication.name
        }.trim().takeIf { it.isNotBlank() }
            ?: throw ApiException(ApiCode.UNAUTHORIZED, "Authenticated user is required")
    }
}
