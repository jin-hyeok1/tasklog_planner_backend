package personal.jinhyeok.tasklog_planner_backend.web.inbound.dto

import java.time.LocalDate
import java.time.OffsetDateTime

data class WorkLogDto(
    val taskCode: String,
    val scheduleCode: String? = null,
    val workedDate: LocalDate,
    val sequenceNo: Int,
    val startedAt: OffsetDateTime,
    val endedAt: OffsetDateTime,
    val workedMinutes: Int,
    val memo: String? = null,
    val source: WorkLogSource,
    val createdAt: OffsetDateTime? = null,
    val updatedAt: OffsetDateTime? = null,
)

data class SaveWorkLogRequest(
    val taskCode: String? = null,
    val scheduleCode: String? = null,
    val workedDate: LocalDate? = null,
    val startedAt: OffsetDateTime? = null,
    val endedAt: OffsetDateTime? = null,
    val memo: String? = null,
)
