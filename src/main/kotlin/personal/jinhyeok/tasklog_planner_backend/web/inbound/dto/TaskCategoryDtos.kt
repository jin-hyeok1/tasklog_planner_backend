package personal.jinhyeok.tasklog_planner_backend.web.inbound.dto

data class TaskCategoryDto(
    val categoryCode: String,
    val parentCategoryCode: String? = null,
    val categoryName: String,
    val categoryLevel: Int,
    val sortOrder: Int,
    val enabled: Boolean,
)

data class TaskCategoryTreeDto(
    val categoryCode: String,
    val parentCategoryCode: String? = null,
    val categoryName: String,
    val categoryLevel: Int,
    val sortOrder: Int,
    val enabled: Boolean,
    val children: List<TaskCategoryTreeDto> = emptyList(),
)

data class SaveTaskCategoryRequest(
    val parentCategoryCode: String? = null,
    val categoryName: String? = null,
    val categoryLevel: Int? = null,
    val sortOrder: Int? = null,
    val enabled: Boolean? = null,
)
