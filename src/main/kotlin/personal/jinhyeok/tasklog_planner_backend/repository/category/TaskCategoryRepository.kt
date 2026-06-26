package personal.jinhyeok.tasklog_planner_backend.repository.category

import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.SelectConditionStep
import org.springframework.stereotype.Repository
import personal.jinhyeok.jooq.Tables.TASK_CATEGORY
import personal.jinhyeok.jooq.tables.records.TaskCategoryRecord
import personal.jinhyeok.tasklog_planner_backend.enumeration.ApiCode
import personal.jinhyeok.tasklog_planner_backend.exception.ApiException

@Repository
class TaskCategoryRepository(private val dsl: DSLContext) {
    fun findByCode(userEmail: String, categoryCode: String): TaskCategoryRecord =
        dsl.selectFrom(TASK_CATEGORY).where(TASK_CATEGORY.USER_EMAIL.eq(userEmail)).and(TASK_CATEGORY.CATEGORY_CODE.eq(categoryCode)).fetchOne()
            ?: throw ApiException(ApiCode.NOT_FOUND, "TaskCategory not found: $categoryCode")

    fun findById(categoryId: Long?): TaskCategoryRecord? =
        categoryId?.let { dsl.selectFrom(TASK_CATEGORY).where(TASK_CATEGORY.CATEGORY_ID.eq(it)).fetchOne() }

    fun findTreeCandidates(userEmail: String, enabled: Boolean?, categoryLevel: Short?): List<TaskCategoryRecord> =
        dsl.selectFrom(TASK_CATEGORY)
            .where(TASK_CATEGORY.USER_EMAIL.eq(userEmail).let { if (enabled == null) it else it.and(TASK_CATEGORY.ENABLED.eq(enabled)) })
            .andWithNullSkip(TASK_CATEGORY.CATEGORY_LEVEL, categoryLevel)
            .orderBy(TASK_CATEGORY.SORT_ORDER.asc(), TASK_CATEGORY.CATEGORY_NAME.asc())
            .fetch()

    fun idByCode(userEmail: String, categoryCode: String): Long = findByCode(userEmail, categoryCode).get(TASK_CATEGORY.CATEGORY_ID)

    fun descendantIds(parentId: Long): List<Long> =
        dsl.selectFrom(TASK_CATEGORY).where(TASK_CATEGORY.PARENT_CATEGORY_ID.eq(parentId)).fetch().flatMap {
            descendantIds(it.get(TASK_CATEGORY.CATEGORY_ID)) + it.get(TASK_CATEGORY.CATEGORY_ID)
        }

    fun deleteByIds(ids: List<Long>): List<String> {
        val codes = dsl.select(TASK_CATEGORY.CATEGORY_CODE).from(TASK_CATEGORY).where(TASK_CATEGORY.CATEGORY_ID.`in`(ids)).fetch(TASK_CATEGORY.CATEGORY_CODE)
        dsl.deleteFrom(TASK_CATEGORY).where(TASK_CATEGORY.CATEGORY_ID.`in`(ids)).execute()
        return codes
    }

    fun pathById(categoryId: Long): List<String> {
        val result = ArrayDeque<String>()
        var row = findById(categoryId)
        while (row != null) {
            result.addFirst(row.get(TASK_CATEGORY.CATEGORY_NAME))
            row = findById(row.get(TASK_CATEGORY.PARENT_CATEGORY_ID))
        }
        return result.toList()
    }

    fun insert(command: TaskCategorySaveCommand): TaskCategoryRecord =
        dsl.insertInto(TASK_CATEGORY)
            .set(TASK_CATEGORY.USER_EMAIL, command.userEmail)
            .set(TASK_CATEGORY.PARENT_CATEGORY_ID, command.parentCategoryId)
            .set(TASK_CATEGORY.CATEGORY_CODE, command.categoryCode)
            .set(TASK_CATEGORY.CATEGORY_NAME, command.categoryName)
            .set(TASK_CATEGORY.CATEGORY_LEVEL, command.categoryLevel.toShort())
            .set(TASK_CATEGORY.SORT_ORDER, command.sortOrder)
            .set(TASK_CATEGORY.ENABLED, command.enabled)
            .returning()
            .fetchOne() ?: throw ApiException(ApiCode.INTERNAL_ERROR, "Failed to create category")

    fun update(categoryCode: String, command: TaskCategoryUpdateCommand): TaskCategoryRecord =
        dsl.update(TASK_CATEGORY)
            .set(TASK_CATEGORY.PARENT_CATEGORY_ID, command.parentCategoryId)
            .set(TASK_CATEGORY.CATEGORY_NAME, command.categoryName)
            .set(TASK_CATEGORY.CATEGORY_LEVEL, command.categoryLevel.toShort())
            .set(TASK_CATEGORY.SORT_ORDER, command.sortOrder)
            .set(TASK_CATEGORY.ENABLED, command.enabled)
            .set(TASK_CATEGORY.UPDATED_AT, command.updatedAt)
            .where(TASK_CATEGORY.CATEGORY_CODE.eq(categoryCode))
            .and(TASK_CATEGORY.USER_EMAIL.eq(command.userEmail))
            .returning()
            .fetchOne() ?: throw ApiException(ApiCode.NOT_FOUND, "TaskCategory not found: $categoryCode")
}

data class TaskCategorySaveCommand(
    val userEmail: String,
    val parentCategoryId: Long?,
    val categoryCode: String,
    val categoryName: String,
    val categoryLevel: Int,
    val sortOrder: Int,
    val enabled: Boolean,
)

data class TaskCategoryUpdateCommand(
    val userEmail: String,
    val parentCategoryId: Long?,
    val categoryName: String,
    val categoryLevel: Int,
    val sortOrder: Int,
    val enabled: Boolean,
    val updatedAt: java.time.OffsetDateTime,
)

fun <R: Record, T> SelectConditionStep<R>.andWithNullSkip(field: Field<T>, value: T): SelectConditionStep<R> =
    if (value == null) this else this.and(field.eq(value))
