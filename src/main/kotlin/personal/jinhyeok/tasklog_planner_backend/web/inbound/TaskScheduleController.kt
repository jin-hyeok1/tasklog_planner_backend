package personal.jinhyeok.tasklog_planner_backend.web.inbound

import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import personal.jinhyeok.tasklog_planner_backend.service.schedule.TaskScheduleService
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.SaveTaskScheduleRequest

@RestController
@RequestMapping("/api/task-schedules")
class TaskScheduleController(private val service: TaskScheduleService) {
    @PostMapping
    fun create(@RequestBody request: SaveTaskScheduleRequest): BaseResponseEntity =
        BaseResponseEntity.created(service.createSchedule(request))

    @PutMapping("/{scheduleCode}")
    fun update(@PathVariable scheduleCode: String, @RequestBody request: SaveTaskScheduleRequest): BaseResponseEntity =
        BaseResponseEntity.single(service.updateSchedule(scheduleCode, request))

    @DeleteMapping("/{scheduleCode}")
    fun delete(@PathVariable scheduleCode: String): BaseResponseEntity =
        BaseResponseEntity.single(service.deleteSchedule(scheduleCode))
}
