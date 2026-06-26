package personal.jinhyeok.tasklog_planner_backend.exception

import personal.jinhyeok.tasklog_planner_backend.enumeration.ApiCode

class ApiException(
    val apiCode: ApiCode,
    override val message: String = apiCode.message,
    val details: Map<String, String>? = null,
) : RuntimeException(message)
