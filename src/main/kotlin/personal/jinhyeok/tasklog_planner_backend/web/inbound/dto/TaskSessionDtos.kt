package personal.jinhyeok.tasklog_planner_backend.web.inbound.dto

import java.time.LocalDate
import java.time.OffsetDateTime

data class TaskSessionDto(
    val sessionCode: String,
    val scheduleCode: String? = null,
    val taskCode: String,
    val workedDate: LocalDate,
    val status: TaskSessionStatus,
    val startedAt: OffsetDateTime,
    val lastStartedAt: OffsetDateTime? = null,
    val pausedAt: OffsetDateTime? = null,
    val finishedAt: OffsetDateTime? = null,
    val accumulatedMinutes: Int,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)

data class StartTaskSessionRequest(
    val taskCode: String? = null,
    val scheduleCode: String? = null,
    val workedDate: LocalDate? = null,
)

data class FinishTaskSessionRequest(val memo: String? = null)

data class RunningSessionSummaryDto(
    val sessionCode: String,
    val taskCode: String,
    val scheduleCode: String? = null,
    val taskTitle: String,
    val categoryPath: List<String> = emptyList(),
    val startedAt: OffsetDateTime,
    val elapsedMinutes: Int,
    val status: TaskSessionStatus,
)

data class ScheduleSessionSummaryDto(
    val sessionCode: String,
    val status: TaskSessionStatus,
    val accumulatedMinutes: Int,
)
