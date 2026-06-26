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
import personal.jinhyeok.tasklog_planner_backend.service.worklog.WorkLogService
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.SaveWorkLogRequest
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.WorkLogSource
import java.time.LocalDate

@RestController
@RequestMapping("/api/work-logs")
class WorkLogController(private val service: WorkLogService) {
    @GetMapping
    fun workLogs(
        @RequestParam(required = false) taskCode: String?,
        @RequestParam(required = false) scheduleCode: String?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) dateFrom: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) dateTo: LocalDate?,
        @RequestParam(required = false) source: WorkLogSource?,
    ): BaseResponseEntity = BaseResponseEntity.list(service.workLogs(taskCode, scheduleCode, dateFrom, dateTo, source))

    @PostMapping
    fun create(@RequestBody request: SaveWorkLogRequest): BaseResponseEntity =
        BaseResponseEntity.created(service.createWorkLog(request))

    @PutMapping("/{taskCode}/{workedDate}/{sequenceNo}")
    fun update(
        @PathVariable taskCode: String,
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) workedDate: LocalDate,
        @PathVariable sequenceNo: Int,
        @RequestBody request: SaveWorkLogRequest,
    ): BaseResponseEntity = BaseResponseEntity.single(service.updateWorkLog(taskCode, workedDate, sequenceNo, request))

    @DeleteMapping("/{taskCode}/{workedDate}/{sequenceNo}")
    fun delete(
        @PathVariable taskCode: String,
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) workedDate: LocalDate,
        @PathVariable sequenceNo: Int,
    ): BaseResponseEntity = BaseResponseEntity.single(service.deleteWorkLog(taskCode, workedDate, sequenceNo))
}
