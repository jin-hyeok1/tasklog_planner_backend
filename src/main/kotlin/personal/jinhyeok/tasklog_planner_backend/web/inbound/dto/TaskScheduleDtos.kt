package personal.jinhyeok.tasklog_planner_backend.web.inbound.dto

import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.OffsetTime

data class TaskScheduleDto(
    val scheduleCode: String,
    val planCode: String,
    val taskCode: String,
    val scheduledDate: LocalDate,
    val startTime: OffsetTime,
    val endTime: OffsetTime,
    val plannedHours: BigDecimal,
    val memo: String? = null,
    val createdAt: OffsetDateTime? = null,
    val updatedAt: OffsetDateTime? = null,
)

data class SaveTaskScheduleRequest(
    val planCode: String? = null,
    val taskCode: String? = null,
    val scheduledDate: LocalDate? = null,
    val startTime: OffsetTime? = null,
    val endTime: OffsetTime? = null,
    val memo: String? = null,
)
