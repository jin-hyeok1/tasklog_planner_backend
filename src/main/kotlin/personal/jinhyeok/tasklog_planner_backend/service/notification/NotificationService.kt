package personal.jinhyeok.tasklog_planner_backend.service.notification

import org.springframework.stereotype.Service
import personal.jinhyeok.tasklog_planner_backend.repository.notification.NotificationRepository
import personal.jinhyeok.tasklog_planner_backend.repository.user.AppUserRepository
import personal.jinhyeok.tasklog_planner_backend.service.support.toDto
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.NotificationDto

@Service
class NotificationService(
    private val appUserRepository: AppUserRepository,
    private val notificationRepository: NotificationRepository,
) {
    fun notifications(readYn: Boolean?, limit: Int?): List<NotificationDto> =
        notificationRepository.findAll(appUserRepository.currentUserEmail(), readYn, limit).map { row ->
            NotificationDto(
                notificationCode = row.notificationCode,
                title = row.title,
                message = row.message,
                notificationType = row.notificationType.toDto(),
                readYn = row.readYn,
                createdAt = row.createdAt,
            )
        }
}
