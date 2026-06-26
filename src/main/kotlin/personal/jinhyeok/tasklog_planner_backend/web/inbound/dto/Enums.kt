package personal.jinhyeok.tasklog_planner_backend.web.inbound.dto

enum class UserRole { ADMIN, USER }
enum class PlanType { YEAR, QUARTER, MONTH, WEEK }
enum class PlanStatus { ACTIVE, INACTIVE, COMPLETED, CANCELLED }
enum class TaskStatus { TODO, IN_PROGRESS, DONE, HOLD, DELAYED }
enum class TaskPriority { LOW, MEDIUM, HIGH, URGENT }
enum class TaskSessionStatus { RUNNING, PAUSED, FINISHED }
enum class WorkLogSource { MANUAL, SESSION }
enum class NotificationType { INFO, WARNING, DANGER }
