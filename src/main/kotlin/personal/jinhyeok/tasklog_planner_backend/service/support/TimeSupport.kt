package personal.jinhyeok.tasklog_planner_backend.service.support

import org.springframework.stereotype.Component
import personal.jinhyeok.tasklog_planner_backend.enumeration.ApiCode
import personal.jinhyeok.tasklog_planner_backend.exception.ApiException
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.ZoneOffset
import java.time.temporal.WeekFields

@Component
class TimeSupport {
    fun offset(): ZoneOffset = ZoneOffset.ofHours(9)
    fun now(): OffsetDateTime = OffsetDateTime.now(ZoneOffset.ofHours(9))
    fun minutesToHours(minutes: Int): BigDecimal = BigDecimal.valueOf(minutes.toLong()).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP).stripTrailingZeros()
    fun hoursToMinutes(hours: BigDecimal): Int = hours.multiply(BigDecimal.valueOf(60)).setScale(0, RoundingMode.HALF_UP).toInt()
    fun progressRate(worked: BigDecimal, planned: BigDecimal): Int = if (planned.compareTo(BigDecimal.ZERO) == 0) 0 else worked.multiply(BigDecimal.valueOf(100)).divide(planned, 0, RoundingMode.HALF_UP).toInt()
    fun minutesBetween(start: OffsetDateTime, end: OffsetDateTime): Int = Duration.between(start, end).toMinutes().toInt().coerceAtLeast(0)
    fun minutesBetween(start: LocalTime, end: LocalTime): Int = Duration.between(start, end).toMinutes().toInt()
    fun minutesBetween(start: OffsetTime, end: OffsetTime): Int = Duration.between(start, end).toMinutes().toInt()
    fun toOffsetTime(time: LocalTime): OffsetTime = OffsetTime.of(time, offset())
    fun toLocalTime(time: OffsetTime?): LocalTime? = time?.toLocalTime()
    fun dayOfWeekLabel(date: LocalDate): String = listOf("월", "화", "수", "목", "금", "토", "일")[date.dayOfWeek.value - 1]
    fun nextCode(prefix: String): String = "$prefix-${System.currentTimeMillis()}"
    fun nextCategoryCode(): String = nextCode("CATEGORY")
    fun nextPlanCode(planType: personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.PlanType, startDate: LocalDate): String = when (planType) {
        personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.PlanType.YEAR -> "PLAN-${startDate.year}-${System.currentTimeMillis()}"
        personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.PlanType.QUARTER -> "PLAN-${startDate.year}-Q${((startDate.monthValue - 1) / 3) + 1}-${System.currentTimeMillis()}"
        personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.PlanType.MONTH -> "PLAN-${startDate.year}-${startDate.monthValue.toString().padStart(2, '0')}-${System.currentTimeMillis()}"
        personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.PlanType.WEEK -> "PLAN-${startDate.year}-W${startDate.get(WeekFields.ISO.weekOfWeekBasedYear())}-${System.currentTimeMillis()}"
    }
    fun missing(name: String): Nothing = throw ApiException(ApiCode.MISSING_PARAMETER, "$name is required", mapOf(name to "required"))
}
