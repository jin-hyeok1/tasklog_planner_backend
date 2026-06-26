package personal.jinhyeok.tasklog_planner_backend.exception

import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import personal.jinhyeok.tasklog_planner_backend.enumeration.ApiCode
import personal.jinhyeok.tasklog_planner_backend.web.inbound.BaseResponseEntity

@RestControllerAdvice
class GlobalExceptionAdvisor {
    private val log = LoggerFactory.getLogger(GlobalExceptionAdvisor::class.java)

    @ExceptionHandler(ApiException::class)
    fun handleApiException(exception: ApiException, request: HttpServletRequest): BaseResponseEntity {
        log.warn(
            "API exception. method={}, path={}, query={}, code={}, message={}, details={}",
            request.method,
            request.requestURI,
            request.queryString,
            exception.apiCode.code,
            exception.message,
            exception.details,
        )
        return BaseResponseEntity.error(exception.apiCode, request, exception.message, exception.details)
    }

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingParameter(
        exception: MissingServletRequestParameterException,
        request: HttpServletRequest,
    ): BaseResponseEntity {
        val message = "${exception.parameterName} parameter is required"
        log.warn(
            "Missing request parameter. method={}, path={}, query={}, parameter={}, message={}",
            request.method,
            request.requestURI,
            request.queryString,
            exception.parameterName,
            message,
        )
        return BaseResponseEntity.error(
            ApiCode.MISSING_PARAMETER,
            request,
            message,
            mapOf(exception.parameterName to "required"),
        )
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class, HttpMessageNotReadableException::class)
    fun handleBadRequest(exception: Exception, request: HttpServletRequest): BaseResponseEntity {
        log.warn(
            "Bad request. method={}, path={}, query={}, message={}",
            request.method,
            request.requestURI,
            request.queryString,
            exception.message,
        )
        return BaseResponseEntity.error(ApiCode.BAD_REQUEST, request, exception.message)
    }

    @ExceptionHandler(Exception::class)
    fun handleException(exception: Exception, request: HttpServletRequest): BaseResponseEntity {
        log.error(
            "Unhandled exception. method={}, path={}, query={}, message={}",
            request.method,
            request.requestURI,
            request.queryString,
            exception.message,
            exception,
        )
        return BaseResponseEntity.error(ApiCode.INTERNAL_ERROR, request, exception.message)
    }
}
