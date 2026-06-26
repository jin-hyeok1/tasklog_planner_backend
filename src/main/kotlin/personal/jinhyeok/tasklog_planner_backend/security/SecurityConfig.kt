package personal.jinhyeok.tasklog_planner_backend.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import personal.jinhyeok.tasklog_planner_backend.enumeration.ApiCode

@Configuration
class SecurityConfig(
    private val bearerTokenAuthenticationFilter: BearerTokenAuthenticationFilter,
) {
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors { }
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers("/api/users/sign-up", "/api/users/login").permitAll()
                    .requestMatchers("/error").permitAll()
                    .anyRequest().authenticated()
            }
            .exceptionHandling {
                it.authenticationEntryPoint { request, response, authException ->
                    writeError(response, request, ApiCode.UNAUTHORIZED, authException)
                }
                it.accessDeniedHandler { request, response, accessDeniedException ->
                    writeError(response, request, ApiCode.UNAUTHORIZED, accessDeniedException)
                }
            }
            .addFilterBefore(bearerTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }

    private fun writeError(
        response: HttpServletResponse,
        request: HttpServletRequest,
        apiCode: ApiCode,
        exception: Exception,
    ) {
        response.status = apiCode.httpStatus.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        val message = exception.message?.replace("\"", "\\\"") ?: apiCode.message
        val path = request.requestURI.replace("\"", "\\\"")
        response.writer.use {
            it.write(
                """{"code":${apiCode.code},"message":"${apiCode.message}","errorMessage":"$message","path":"$path"}""",
            )
        }
    }
}
