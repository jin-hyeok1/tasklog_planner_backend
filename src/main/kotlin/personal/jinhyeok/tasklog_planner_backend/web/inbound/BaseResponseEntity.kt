package personal.jinhyeok.tasklog_planner_backend.web.inbound

import jakarta.servlet.http.HttpServletRequest
import personal.jinhyeok.tasklog_planner_backend.enumeration.ApiCode
import personal.jinhyeok.tasklog_planner_backend.service.PageableData
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

class BaseResponseEntity : ResponseEntity<BaseResponse> {

    constructor(status: HttpStatus) : super(status)

    constructor(body: BaseResponse?, status: HttpStatus) : super(body, status) {
        body?.applyCode(ApiCode.from(status))
    }

    constructor(headers: HttpHeaders, status: HttpStatus) : super(headers, status)

    constructor(body: BaseResponse?, headers: HttpHeaders?, rawStatus: Int) : super(body, headers, rawStatus) {
        body?.applyCode(ApiCode.from(HttpStatus.valueOf(rawStatus)))
    }

    constructor(body: BaseResponse?, headers: HttpHeaders?, statusCode: HttpStatus) : super(body, headers, statusCode) {
        body?.applyCode(ApiCode.from(statusCode))
    }

    private constructor(body: BaseResponse?, status: HttpStatus, apiCode: ApiCode) : super(body, status) {
        body?.applyCode(apiCode)
    }

    companion object {
        fun empty(): BaseResponseEntity = BaseResponseEntity(HttpStatus.OK)

        fun created(): BaseResponseEntity = BaseResponseEntity(HttpStatus.CREATED)

        fun emptyNoContent(): BaseResponseEntity = BaseResponseEntity(HttpStatus.NO_CONTENT)

        fun <T> created(body: T): BaseResponseEntity = BaseResponseEntity(SingleResponse(body), HttpStatus.CREATED)

        fun <T> single(body: T): BaseResponseEntity = BaseResponseEntity(SingleResponse(body), HttpStatus.OK)

        fun <T> page(body: PageableData<T>): BaseResponseEntity = BaseResponseEntity(PageResponse(body), HttpStatus.OK)

        fun <T> list(body: Collection<T>): BaseResponseEntity = BaseResponseEntity(ListResponse(body), HttpStatus.OK)

        fun <T> list(body: Array<T>): BaseResponseEntity = BaseResponseEntity(ListResponse(body), HttpStatus.OK)

        fun error(
            apiCode: ApiCode,
            request: HttpServletRequest,
            errorMessage: String? = null,
            details: Map<String, String>? = null,
        ): BaseResponseEntity =
            BaseResponseEntity(ErrorResponse(apiCode, errorMessage, request, details), apiCode.httpStatus, apiCode)
    }
}

private fun BaseResponse.applyCode(apiCode: ApiCode) {
    code = apiCode.code
    message = apiCode.message
}
