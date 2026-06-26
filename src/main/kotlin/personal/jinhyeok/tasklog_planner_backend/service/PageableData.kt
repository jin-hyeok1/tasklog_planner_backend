package personal.jinhyeok.tasklog_planner_backend.service

import personal.jinhyeok.tasklog_planner_backend.repository.PageableRecord
import java.util.function.Function

data class PageableData<T>(
    val data: List<T>,
    val totalCount: Int,
    val totalPage: Int,
    val currentPage: Int,
    val pageSize: Int,
) {
    companion object {
        fun <E, T> from(
            pageableRecord: PageableRecord<E>,
            currentPage: Int,
            pageSize: Int,
            mapper: Function<E, T>,
        ): PageableData<T> =
            PageableData(
                data = pageableRecord.data.map { mapper.apply(it) },
                totalCount = pageableRecord.totalCount,
                totalPage = pageableRecord.totalPage,
                currentPage = currentPage,
                pageSize = pageSize,
            )
    }
}
