package personal.jinhyeok.tasklog_planner_backend.repository

data class PageableRecord<T>(
    val data: List<T>,
    val totalCount: Int,
    val totalPage: Int,
)
