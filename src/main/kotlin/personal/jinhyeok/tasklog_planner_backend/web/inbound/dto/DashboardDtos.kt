package personal.jinhyeok.tasklog_planner_backend.web.inbound.dto

import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetTime

data class DashboardTodayScheduleDto(
    val scheduleCode: String,
    val task: SimpleTaskDto,
    val scheduledDate: LocalDate,
    val startTime: OffsetTime,
    val endTime: OffsetTime,
    val plannedHours: BigDecimal,
    val actualHours: BigDecimal,
    val sessionStatus: TaskSessionStatus? = null,
)

data class WeeklyBoardScheduleDto(
    val scheduleCode: String,
    val scheduledDate: LocalDate,
    val startTime: OffsetTime,
    val endTime: OffsetTime,
    val plannedHours: BigDecimal,
    val actualHours: BigDecimal,
    val hasActualLog: Boolean,
    val memo: String? = null,
    val task: SimpleTaskDto,
    val session: ScheduleSessionSummaryDto? = null,
)

data class WeeklyDayGroupDto(
    val date: LocalDate,
    val dayLabel: String,
    val plannedHours: BigDecimal,
    val workedHours: BigDecimal,
    val schedules: List<WeeklyBoardScheduleDto>,
)
