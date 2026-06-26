package personal.jinhyeok.tasklog_planner_backend.web.inbound

import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import personal.jinhyeok.tasklog_planner_backend.service.session.TaskSessionService
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.FinishTaskSessionRequest
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.StartTaskSessionRequest

@RestController
@RequestMapping("/api/task-sessions")
class TaskSessionController(private val service: TaskSessionService) {
    @PostMapping("/start")
    fun start(@RequestBody request: StartTaskSessionRequest): BaseResponseEntity =
        BaseResponseEntity.single(service.startSession(request))

    @PostMapping("/{sessionCode}/pause")
    fun pause(@PathVariable sessionCode: String): BaseResponseEntity =
        BaseResponseEntity.single(service.pauseSession(sessionCode))

    @PostMapping("/{sessionCode}/resume")
    fun resume(@PathVariable sessionCode: String): BaseResponseEntity =
        BaseResponseEntity.single(service.resumeSession(sessionCode))

    @PostMapping("/{sessionCode}/finish")
    fun finish(
        @PathVariable sessionCode: String,
        @RequestBody(required = false) request: FinishTaskSessionRequest?,
    ): BaseResponseEntity = BaseResponseEntity.single(service.finishSession(sessionCode, request ?: FinishTaskSessionRequest()))
}
