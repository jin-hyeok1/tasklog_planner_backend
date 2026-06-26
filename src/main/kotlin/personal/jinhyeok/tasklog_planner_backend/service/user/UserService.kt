package personal.jinhyeok.tasklog_planner_backend.service.user

import org.springframework.stereotype.Service
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import personal.jinhyeok.jooq.Tables.APP_USER
import personal.jinhyeok.jooq.enums.UserRole as JUserRole
import personal.jinhyeok.tasklog_planner_backend.repository.user.AppUserRepository
import personal.jinhyeok.tasklog_planner_backend.repository.user.AppUserBase
import personal.jinhyeok.tasklog_planner_backend.repository.user.AppUserUpdate
import personal.jinhyeok.tasklog_planner_backend.security.AuthTokenService
import personal.jinhyeok.tasklog_planner_backend.service.support.TimeSupport
import personal.jinhyeok.tasklog_planner_backend.service.support.toDto
import personal.jinhyeok.tasklog_planner_backend.enumeration.ApiCode
import personal.jinhyeok.tasklog_planner_backend.exception.ApiException
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.UpdateUserRequest
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.UserDto
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.AuthResponse
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.LoginRequest
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.SignUpRequest

@Service
class UserService(
    private val appUserRepository: AppUserRepository,
    private val authTokenService: AuthTokenService,
    private val timeSupport: TimeSupport,
) {
    private val passwordEncoder = BCryptPasswordEncoder()

    fun signUp(request: SignUpRequest): AuthResponse {
        val email: String = requiredString(request.email, "email")
        val username: String = requiredString(request.username, "username")
        val displayName: String = requiredString(request.displayName, "displayName")
        val rawPassword: String = requiredString(request.password, "password")
        if (appUserRepository.existsByEmail(email)) throw ApiException(ApiCode.CONFLICT, "Email already exists: $email")
        if (appUserRepository.existsByUsername(username)) throw ApiException(ApiCode.CONFLICT, "Username already exists: $username")

        val user = AppUserBase(
            email = email,
            username = username,
            userCode = timeSupport.nextCode("USER"),
            displayName = displayName,
            password = hashPassword(rawPassword),
            role = JUserRole.USER,
            timezone = request.timezone?.takeIf { it.isNotBlank() } ?: "Asia/Seoul",
            weeklyTargetMinutes = request.weeklyTargetHours?.let(timeSupport::hoursToMinutes) ?: 3600,
            dailyStartTime = request.dailyStartTime,
            dailyEndTime = request.dailyEndTime,
            notificationEnabled = request.notificationEnabled ?: true,
        )
        appUserRepository.createUser(user)
        return authResponse(appUserRepository.findByEmail(email)!!)
    }

    fun login(request: LoginRequest): AuthResponse {
        val email: String = requiredString(request.email, "email")
        val rawPassword: String = requiredString(request.password, "password")
        val user = appUserRepository.findByEmail(email) ?: throw ApiException(ApiCode.UNAUTHORIZED, "Invalid email or password")
        if (!passwordEncoder.matches(rawPassword, user.get(APP_USER.PASSWORD))) {
            throw ApiException(ApiCode.UNAUTHORIZED, "Invalid email or password")
        }
        return authResponse(user)
    }

    fun getMe(): UserDto = appUserRepository.currentUser().toDto(timeSupport)

    fun updateMe(request: UpdateUserRequest): UserDto {
        return appUserRepository.updateCurrent(
            AppUserUpdate(
                email = request.email,
                username = request.username,
                displayName = request.displayName,
                timezone = request.timezone,
                weeklyTargetMinutes = request.weeklyTargetHours?.let(timeSupport::hoursToMinutes),
                dailyStartTime = request.dailyStartTime,
                dailyEndTime = request.dailyEndTime,
                notificationEnabled = request.notificationEnabled,
            ),
        ).toDto(timeSupport)
    }

    private fun authResponse(user: personal.jinhyeok.jooq.tables.records.AppUserRecord): AuthResponse {
        val issued = authTokenService.issue(user.get(APP_USER.EMAIL), user.get(APP_USER.ROLE).name)
        return AuthResponse(
            accessToken = issued.accessToken,
            expiresAt = issued.expiresAt.atOffset(timeSupport.offset()),
            user = user.toDto(timeSupport),
        )
    }

    private fun requiredString(value: String?, fieldName: String): String =
        value?.trim()?.takeIf { it.isNotBlank() }
            ?: throw ApiException(ApiCode.MISSING_PARAMETER, "$fieldName is required", mapOf(fieldName to "required"))

    private fun hashPassword(rawPassword: String): String =
        requireNotNull(passwordEncoder.encode(rawPassword)) { "Password encoder returned null" }
}

private fun personal.jinhyeok.jooq.tables.records.AppUserRecord.toDto(timeSupport: TimeSupport): UserDto =
    UserDto(
        userCode = get(APP_USER.USER_CODE),
        email = get(APP_USER.EMAIL),
        username = get(APP_USER.USERNAME),
        displayName = get(APP_USER.DISPLAY_NAME),
        role = get(APP_USER.ROLE).toDto(),
        timezone = get(APP_USER.TIMEZONE),
        weeklyTargetHours = timeSupport.minutesToHours(get(APP_USER.WEEKLY_TARGET_MINUTES)),
        dailyStartTime = get(APP_USER.DAILY_START_TIME),
        dailyEndTime = get(APP_USER.DAILY_END_TIME),
        notificationEnabled = get(APP_USER.NOTIFICATION_ENABLED),
        createdAt = get(APP_USER.CREATED_AT),
        updatedAt = get(APP_USER.UPDATED_AT),
    )
