package personal.jinhyeok.tasklog_planner_backend.web.inbound

import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import personal.jinhyeok.tasklog_planner_backend.service.dashboard.DashboardService
import java.time.LocalDate

@RestController
@RequestMapping("/api/dashboard")
class DashboardController(private val service: DashboardService) {
    @GetMapping("/weekly-summary")
    fun weeklySummary(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) weekStartDate: LocalDate,
    ): BaseResponseEntity = BaseResponseEntity.single(service.weeklySummary(weekStartDate))

    @GetMapping("/today")
    fun today(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
    ): BaseResponseEntity = BaseResponseEntity.single(service.today(date))

    @GetMapping("/today/schedules")
    fun todaySchedules(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
    ): BaseResponseEntity = BaseResponseEntity.list(service.todaySchedules(date))
}
