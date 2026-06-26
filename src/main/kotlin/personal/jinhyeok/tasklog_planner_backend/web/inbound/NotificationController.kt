package personal.jinhyeok.tasklog_planner_backend.web.inbound

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import personal.jinhyeok.tasklog_planner_backend.service.notification.NotificationService

@RestController
@RequestMapping("/api/notifications")
class NotificationController(private val service: NotificationService) {
    @GetMapping
    fun notifications(
        @RequestParam(required = false) readYn: Boolean?,
        @RequestParam(required = false) limit: Int?,
    ): BaseResponseEntity = BaseResponseEntity.list(service.notifications(readYn, limit))
}
