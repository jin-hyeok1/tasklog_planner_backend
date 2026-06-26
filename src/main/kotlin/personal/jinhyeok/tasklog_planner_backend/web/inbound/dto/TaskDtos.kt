package personal.jinhyeok.tasklog_planner_backend.web.inbound.dto

import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime

data class TaskDto(
    val taskCode: String,
    val categoryCode: String,
    val title: String,
    val description: String? = null,
    val plannedHours: BigDecimal,
    val startDate: LocalDate? = null,
    val dueDate: LocalDate? = null,
    val status: TaskStatus,
    val priority: TaskPriority,
    val createdAt: OffsetDateTime? = null,
    val updatedAt: OffsetDateTime? = null,
    val isTask: Boolean,
)

data class SaveTaskRequest(
    val categoryCode: String? = null,
    val title: String? = null,
    val description: String? = null,
    val plannedHours: BigDecimal? = null,
    val startDate: LocalDate? = null,
    val dueDate: LocalDate? = null,
    val status: TaskStatus? = null,
    val priority: TaskPriority? = null,
    val isTask: Boolean? = null
)

data class SimpleTaskDto(
    val taskCode: String,
    val title: String,
    val status: TaskStatus,
    val priority: TaskPriority? = null,
    val categoryPath: List<String> = emptyList(),
    val isTask: Boolean,
)
