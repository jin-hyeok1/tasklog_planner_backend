package personal.jinhyeok.tasklog_planner_backend.web.inbound.dto

import java.time.OffsetDateTime

data class NotificationDto(
    val notificationCode: String,
    val title: String,
    val message: String,
    val notificationType: NotificationType,
    val readYn: Boolean,
    val createdAt: OffsetDateTime,
)
