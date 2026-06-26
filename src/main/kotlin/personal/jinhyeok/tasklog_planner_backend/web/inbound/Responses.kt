package personal.jinhyeok.tasklog_planner_backend.web.inbound

import jakarta.servlet.http.HttpServletRequest
import personal.jinhyeok.tasklog_planner_backend.enumeration.ApiCode
import personal.jinhyeok.tasklog_planner_backend.service.PageableData
import java.time.Instant

abstract class BaseResponse {
    @JvmField
    var code: Int = ApiCode.OK.code

    @JvmField
    var message: String = ApiCode.OK.message

    @JvmField
    var timestamp: Instant = Instant.now()
}

class ErrorResponse() : BaseResponse() {
    @JvmField
    var errorMessage: String? = null

    @JvmField
    var path: String? = null

    @JvmField
    var details: Map<String, String>? = null

    constructor(apiCode: ApiCode, errorMessage: String?, request: HttpServletRequest, details: Map<String, String>? = null) : this() {
        this.code = apiCode.code
        this.message = apiCode.message
        this.errorMessage = errorMessage ?: apiCode.message
        this.path = request.requestURI
        this.details = details
    }
}

class SingleResponse<T>() : BaseResponse() {
    @JvmField
    var data: T? = null

    constructor(data: T) : this() {
        this.data = data
    }
}

class ListResponse<T>() : BaseResponse() {
    @JvmField
    var data: List<T>? = null

    @JvmField
    var totalCount: Int = 0

    constructor(data: Collection<T>) : this() {
        this.data = ArrayList(data)
        this.totalCount = data.size
    }

    constructor(data: Array<T>) : this() {
        this.data = data.toList()
        this.totalCount = data.size
    }
}

class PageResponse<T>() : BaseResponse() {
    @JvmField
    var data: List<T>? = null

    @JvmField
    var totalCount: Int = 0

    @JvmField
    var totalPage: Int = 0

    @JvmField
    var currentPage: Int = 0

    @JvmField
    var pageSize: Int = 0

    constructor(data: PageableData<T>) : this() {
        this.data = data.data
        this.totalCount = data.totalCount
        this.totalPage = data.totalPage
        this.currentPage = data.currentPage
        this.pageSize = data.pageSize
    }
}
