package personal.jinhyeok.tasklog_planner_backend.enumeration

import org.springframework.http.HttpStatus

enum class ApiCode(
    val code: Int,
    val message: String,
    val httpStatus: HttpStatus,
) {
    OK(2000, "OK", HttpStatus.OK),
    CREATED(2010, "Created", HttpStatus.CREATED),
    NO_CONTENT(2040, "No Content", HttpStatus.NO_CONTENT),

    MISSING_PARAMETER(4001, "Required parameter is missing", HttpStatus.BAD_REQUEST),
    BAD_REQUEST(4002, "Bad request", HttpStatus.BAD_REQUEST),
    VALIDATION_FAILED(4003, "Validation failed", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(4011, "Unauthorized", HttpStatus.UNAUTHORIZED),
    NOT_FOUND(4041, "Resource not found", HttpStatus.NOT_FOUND),
    CONFLICT(4091, "Resource conflict", HttpStatus.CONFLICT),
    INTERNAL_ERROR(5001, "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR),
    ;

    companion object {
        fun from(status: HttpStatus): ApiCode = when (status) {
            HttpStatus.CREATED -> CREATED
            HttpStatus.NO_CONTENT -> NO_CONTENT
            HttpStatus.BAD_REQUEST -> BAD_REQUEST
            HttpStatus.UNAUTHORIZED -> UNAUTHORIZED
            HttpStatus.NOT_FOUND -> NOT_FOUND
            HttpStatus.CONFLICT -> CONFLICT
            HttpStatus.INTERNAL_SERVER_ERROR -> INTERNAL_ERROR
            else -> OK
        }
    }
}
