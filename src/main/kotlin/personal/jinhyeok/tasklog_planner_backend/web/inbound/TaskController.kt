package personal.jinhyeok.tasklog_planner_backend.web.inbound

import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import personal.jinhyeok.tasklog_planner_backend.service.task.TaskService
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.SaveTaskRequest
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.TaskStatus
import java.time.LocalDate

@RestController
@RequestMapping("/api/tasks")
class TaskController(private val service: TaskService) {
    @GetMapping
    fun tasks(
        @RequestParam(required = false) status: TaskStatus?,
        @RequestParam(required = false) categoryCode: String?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate?,
    ): BaseResponseEntity = BaseResponseEntity.list(service.tasks(status, categoryCode, startDate, endDate))

    @GetMapping("/{taskCode}")
    fun detail(@PathVariable taskCode: String): BaseResponseEntity =
        BaseResponseEntity.single(service.taskDetail(taskCode))

    @GetMapping("/{taskCode}/schedules")
    fun schedules(@PathVariable taskCode: String): BaseResponseEntity =
        BaseResponseEntity.list(service.taskSchedules(taskCode))

    @PostMapping
    fun create(@RequestBody request: SaveTaskRequest): BaseResponseEntity =
        BaseResponseEntity.created(service.createTask(request))

    @PutMapping("/{taskCode}")
    fun update(@PathVariable taskCode: String, @RequestBody request: SaveTaskRequest): BaseResponseEntity =
        BaseResponseEntity.single(service.updateTask(taskCode, request))

    @DeleteMapping("/{taskCode}")
    fun delete(
        @PathVariable taskCode: String,
        @RequestParam(defaultValue = "false") cascade: Boolean,
    ): BaseResponseEntity = BaseResponseEntity.single(service.deleteTask(taskCode, cascade))
}
