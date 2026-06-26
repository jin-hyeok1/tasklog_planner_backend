package personal.jinhyeok.tasklog_planner_backend.repository.user

import org.jooq.DSLContext
import org.jooq.InsertSetMoreStep
import org.jooq.Record
import org.jooq.TableField
import org.springframework.stereotype.Repository
import personal.jinhyeok.jooq.Tables.APP_USER
import personal.jinhyeok.jooq.enums.UserRole
import personal.jinhyeok.jooq.tables.records.AppUserRecord
import personal.jinhyeok.tasklog_planner_backend.enumeration.ApiCode
import personal.jinhyeok.tasklog_planner_backend.exception.ApiException
import personal.jinhyeok.tasklog_planner_backend.security.CurrentUserResolver
import java.time.OffsetTime

@Repository
class AppUserRepository(
    private val dsl: DSLContext,
    private val currentUserResolver: CurrentUserResolver,
) {
    fun findByEmail(email: String): AppUserRecord? =
        dsl.selectFrom(APP_USER).where(APP_USER.EMAIL.eq(email)).fetchOne()

    fun findByUsername(username: String): AppUserRecord? =
        dsl.selectFrom(APP_USER).where(APP_USER.USERNAME.eq(username)).fetchOne()

    fun existsByEmail(email: String): Boolean =
        dsl.fetchExists(APP_USER, APP_USER.EMAIL.eq(email))

    fun existsByUsername(username: String): Boolean =
        dsl.fetchExists(APP_USER, APP_USER.USERNAME.eq(username))

    fun createUser(appUser: AppUserBase): Long = dsl.insertInto(APP_USER)
        .set(APP_USER.EMAIL, appUser.email)
        .set(APP_USER.USER_CODE, appUser.userCode)
        .set(APP_USER.USERNAME, appUser.username)
        .set(APP_USER.DISPLAY_NAME, appUser.displayName)
        .set(APP_USER.PASSWORD, appUser.password)
        .set(APP_USER.ROLE, appUser.role)
        .set(APP_USER.TIMEZONE, appUser.timezone)
        .set(APP_USER.WEEKLY_TARGET_MINUTES, appUser.weeklyTargetMinutes)
        .setNullable(APP_USER.DAILY_START_TIME, appUser.dailyStartTime)
        .setNullable(APP_USER.DAILY_END_TIME, appUser.dailyEndTime)
        .set(APP_USER.NOTIFICATION_ENABLED, appUser.notificationEnabled)
        .execute().toLong()

    fun currentUser(): AppUserRecord {
        val email = currentUserResolver.currentUserEmail()
        return findByEmail(email)
            ?: throw ApiException(ApiCode.NOT_FOUND, "User not found: $email")
    }

    fun currentUserEmail(): String = currentUserResolver.currentUserEmail()

    fun updateCurrent(request: AppUserUpdate): AppUserRecord {
        val current = currentUser()
        val email = current.get(APP_USER.EMAIL)
        return dsl.update(APP_USER)
            .set(APP_USER.EMAIL, request.email ?: email)
            .set(APP_USER.USERNAME, request.username ?: current.get(APP_USER.USERNAME))
            .set(APP_USER.DISPLAY_NAME, request.displayName ?: current.get(APP_USER.DISPLAY_NAME))
            .set(APP_USER.TIMEZONE, request.timezone ?: current.get(APP_USER.TIMEZONE))
            .set(
                APP_USER.WEEKLY_TARGET_MINUTES,
                request.weeklyTargetMinutes ?: current.get(APP_USER.WEEKLY_TARGET_MINUTES)
            )
            .set(APP_USER.DAILY_START_TIME, request.dailyStartTime ?: current.get(APP_USER.DAILY_START_TIME))
            .set(APP_USER.DAILY_END_TIME, request.dailyEndTime ?: current.get(APP_USER.DAILY_END_TIME))
            .set(
                APP_USER.NOTIFICATION_ENABLED,
                request.notificationEnabled ?: current.get(APP_USER.NOTIFICATION_ENABLED)
            )
            .where(APP_USER.EMAIL.eq(email))
            .returning()
            .fetchOne() ?: throw ApiException(ApiCode.NOT_FOUND, "User not found: $email")
    }

    fun updateCurrentPassword(encodedPassword: String): Int {
        val current = currentUser()
        val email = current.get(APP_USER.EMAIL)
        return dsl.update(APP_USER)
            .set(APP_USER.PASSWORD, encodedPassword)
            .where(APP_USER.EMAIL.eq(email))
            .execute()
    }
}

data class AppUserUpdate(
    val email: String?,
    val username: String?,
    val displayName: String?,
    val timezone: String?,
    val weeklyTargetMinutes: Int?,
    val dailyStartTime: OffsetTime?,
    val dailyEndTime: OffsetTime?,
    val notificationEnabled: Boolean?,
)

data class AppUserBase(
    val email: String,
    val username: String,
    val userCode: String,
    val displayName: String,
    val password: String,
    val role: UserRole,
    val timezone: String,
    val weeklyTargetMinutes: Int,
    val dailyStartTime: OffsetTime?,
    val dailyEndTime: OffsetTime?,
    val notificationEnabled: Boolean,
)

fun <R: Record, T> InsertSetMoreStep<R>.setNullable(field: TableField<R, T>, value: T?): InsertSetMoreStep<R> =
    if (value == null) this else this.set(field, value)
