package personal.jinhyeok.tasklog_planner_backend.service.support

import personal.jinhyeok.jooq.enums.NotificationType as JNotificationType
import personal.jinhyeok.jooq.enums.PlanStatus as JPlanStatus
import personal.jinhyeok.jooq.enums.PlanType as JPlanType
import personal.jinhyeok.jooq.enums.TaskPriority as JTaskPriority
import personal.jinhyeok.jooq.enums.TaskSessionStatus as JTaskSessionStatus
import personal.jinhyeok.jooq.enums.TaskStatus as JTaskStatus
import personal.jinhyeok.jooq.enums.UserRole as JUserRole
import personal.jinhyeok.jooq.enums.WorkLogSource as JWorkLogSource
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.NotificationType
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.PlanStatus
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.PlanType
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.TaskPriority
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.TaskSessionStatus
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.TaskStatus
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.UserRole
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.WorkLogSource

fun JUserRole.toDto(): UserRole = UserRole.valueOf(name)
fun JPlanType.toDto(): PlanType = PlanType.valueOf(name)
fun PlanType.toJooq(): JPlanType = JPlanType.valueOf(name)
fun JPlanStatus.toDto(): PlanStatus = PlanStatus.valueOf(name)
fun PlanStatus.toJooq(): JPlanStatus = JPlanStatus.valueOf(name)
fun JTaskStatus.toDto(): TaskStatus = TaskStatus.valueOf(name)
fun TaskStatus.toJooq(): JTaskStatus = JTaskStatus.valueOf(name)
fun JTaskPriority.toDto(): TaskPriority = TaskPriority.valueOf(name)
fun TaskPriority.toJooq(): JTaskPriority = JTaskPriority.valueOf(name)
fun JTaskSessionStatus.toDto(): TaskSessionStatus = TaskSessionStatus.valueOf(name)
fun JWorkLogSource.toDto(): WorkLogSource = WorkLogSource.valueOf(name)
fun WorkLogSource.toJooq(): JWorkLogSource = JWorkLogSource.valueOf(name)
fun JNotificationType.toDto(): NotificationType = NotificationType.valueOf(name)
