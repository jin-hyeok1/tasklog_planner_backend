package personal.jinhyeok.tasklog_planner_backend.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class BearerTokenAuthenticationFilter(
    private val authTokenService: AuthTokenService,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        if (SecurityContextHolder.getContext().authentication == null) {
            extractToken(request.getHeader("Authorization"))?.let { token ->
                authTokenService.authenticate(token)?.let { principal ->
                    SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(
                        principal.email,
                        null,
                        listOf(SimpleGrantedAuthority("ROLE_${principal.role}")),
                    )
                }
            }
        }
        filterChain.doFilter(request, response)
    }

    private fun extractToken(authorization: String?): String? =
        authorization?.trim()
            ?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }
            ?.substringAfter("Bearer ")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
}
