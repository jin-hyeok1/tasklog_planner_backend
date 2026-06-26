package personal.jinhyeok.tasklog_planner_backend.web.inbound

import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import personal.jinhyeok.tasklog_planner_backend.service.schedule.ScheduleViewService
import java.time.LocalDate

@RestController
class ScheduleViewController(private val service: ScheduleViewService) {
    @GetMapping("/api/yearly-schedules/{year}")
    fun yearly(@PathVariable year: Int): BaseResponseEntity =
        BaseResponseEntity.single(service.yearlySchedule(year))

    @GetMapping("/api/yearly-schedules/{year}/summary")
    fun yearlySummary(@PathVariable year: Int): BaseResponseEntity =
        BaseResponseEntity.single(service.yearlySummary(year))

    @GetMapping("/api/yearly-schedules/monthly-investments")
    fun monthlyInvestments(
        @RequestParam from: String,
        @RequestParam to: String,
    ): BaseResponseEntity = BaseResponseEntity.list(service.monthlyScheduleInvestments(from, to))

    @GetMapping("/api/yearly-schedules/monthly-tasks")
    fun monthlyTasks(
        @RequestParam from: String,
        @RequestParam to: String,
    ): BaseResponseEntity = BaseResponseEntity.list(service.monthlyTaskDetails(from, to))

    @GetMapping("/api/quarterly-schedules/{planCode}")
    fun quarterly(@PathVariable planCode: String): BaseResponseEntity =
        BaseResponseEntity.single(service.quarterlySchedule(planCode))

    @GetMapping("/api/quarterly-schedules/summary")
    fun quarterlySummaryByPlanId(@RequestParam planId: Long): BaseResponseEntity =
        BaseResponseEntity.single(service.quarterlySummaryByPlanId(planId))

    @GetMapping("/api/quarterly-schedules/{planCode}/summary")
    fun quarterSummary(@PathVariable planCode: String): BaseResponseEntity =
        BaseResponseEntity.single(service.quarterSummary(planCode))

    @GetMapping("/api/quarterly-schedules/{planCode}/months")
    fun quarterMonths(@PathVariable planCode: String): BaseResponseEntity =
        BaseResponseEntity.single(service.quarterMonths(planCode))

    @GetMapping("/api/weekly-schedules/{planCode}")
    fun weekly(@PathVariable planCode: String): BaseResponseEntity =
        BaseResponseEntity.single(service.weeklySchedule(planCode))

    @GetMapping("/api/weekly-schedules/summary")
    fun weeklySummaryByPlanId(@RequestParam planId: Long): BaseResponseEntity =
        BaseResponseEntity.single(service.weeklySummaryByPlanId(planId))

    @GetMapping("/api/weekly-schedules/daily-schedules")
    fun weeklyDailySchedules(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
    ): BaseResponseEntity = BaseResponseEntity.list(service.weeklyDailySchedules(from, to))
}
