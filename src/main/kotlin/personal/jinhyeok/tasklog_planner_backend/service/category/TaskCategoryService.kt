package personal.jinhyeok.tasklog_planner_backend.service.category

import personal.jinhyeok.jooq.Tables.TASK_CATEGORY
import personal.jinhyeok.jooq.tables.records.TaskCategoryRecord
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import personal.jinhyeok.tasklog_planner_backend.repository.category.TaskCategoryRepository
import personal.jinhyeok.tasklog_planner_backend.repository.category.TaskCategorySaveCommand
import personal.jinhyeok.tasklog_planner_backend.repository.category.TaskCategoryUpdateCommand
import personal.jinhyeok.tasklog_planner_backend.repository.user.AppUserRepository
import personal.jinhyeok.tasklog_planner_backend.service.support.TimeSupport
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.SaveTaskCategoryRequest
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.TaskCategoryDto
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.TaskCategoryTreeDto

@Service
class TaskCategoryService(
    private val appUserRepository: AppUserRepository,
    private val taskCategoryRepository: TaskCategoryRepository,
    private val timeSupport: TimeSupport,
) {
    fun categoryTree(enabled: Boolean?, categoryLevel: Short?): List<TaskCategoryTreeDto> {
        val rows = taskCategoryRepository.findTreeCandidates(appUserRepository.currentUserEmail(), enabled = enabled, categoryLevel = categoryLevel)
        val byParent = rows.groupBy { it.get(TASK_CATEGORY.PARENT_CATEGORY_ID) }
        fun build(parentId: Long?): List<TaskCategoryTreeDto> =
            byParent[parentId].orEmpty().map { row ->
                val dto = row.toDto()
                TaskCategoryTreeDto(dto.categoryCode, dto.parentCategoryCode, dto.categoryName, dto.categoryLevel, dto.sortOrder, dto.enabled, build(row.get(TASK_CATEGORY.CATEGORY_ID)))
            }
        return build(null)
    }

    @Transactional
    fun createCategory(request: SaveTaskCategoryRequest): TaskCategoryDto {
        val userEmail = appUserRepository.currentUserEmail()
        val categoryLevel = request.categoryLevel ?: 1
        return taskCategoryRepository.insert(
            TaskCategorySaveCommand(
                userEmail = userEmail,
                parentCategoryId = parentCategoryId(userEmail, categoryLevel, request.parentCategoryCode),
                categoryCode = timeSupport.nextCategoryCode(),
                categoryName = request.categoryName ?: timeSupport.missing("categoryName"),
                categoryLevel = categoryLevel,
                sortOrder = request.sortOrder ?: 0,
                enabled = request.enabled ?: true,
            ),
        ).toDto()
    }

    @Transactional
    fun updateCategory(categoryCode: String, request: SaveTaskCategoryRequest): TaskCategoryDto {
        val userEmail = appUserRepository.currentUserEmail()
        val current = taskCategoryRepository.findByCode(userEmail, categoryCode)
        val categoryLevel = request.categoryLevel ?: current.get(TASK_CATEGORY.CATEGORY_LEVEL).toInt()
        return taskCategoryRepository.update(
            categoryCode,
            TaskCategoryUpdateCommand(
                userEmail = userEmail,
                parentCategoryId = parentCategoryId(userEmail, categoryLevel, request.parentCategoryCode, current.get(TASK_CATEGORY.PARENT_CATEGORY_ID)),
                categoryName = request.categoryName ?: current.get(TASK_CATEGORY.CATEGORY_NAME),
                categoryLevel = categoryLevel,
                sortOrder = request.sortOrder ?: current.get(TASK_CATEGORY.SORT_ORDER),
                enabled = request.enabled ?: current.get(TASK_CATEGORY.ENABLED),
                updatedAt = timeSupport.now(),
            ),
        ).toDto()
    }

    @Transactional
    fun deleteCategory(categoryCode: String, cascade: Boolean): Map<String, List<String>> {
        val target = taskCategoryRepository.findByCode(appUserRepository.currentUserEmail(), categoryCode)
        val targetId = target.get(TASK_CATEGORY.CATEGORY_ID)
        val ids = if (cascade) taskCategoryRepository.descendantIds(targetId) + targetId else listOf(targetId)
        return mapOf("deletedCategoryCodes" to taskCategoryRepository.deleteByIds(ids))
    }

    private fun TaskCategoryRecord.toDto(): TaskCategoryDto =
        TaskCategoryDto(
            categoryCode = get(TASK_CATEGORY.CATEGORY_CODE),
            parentCategoryCode = taskCategoryRepository.findById(get(TASK_CATEGORY.PARENT_CATEGORY_ID))?.get(TASK_CATEGORY.CATEGORY_CODE),
            categoryName = get(TASK_CATEGORY.CATEGORY_NAME),
            categoryLevel = get(TASK_CATEGORY.CATEGORY_LEVEL).toInt(),
            sortOrder = get(TASK_CATEGORY.SORT_ORDER),
            enabled = get(TASK_CATEGORY.ENABLED),
        )

    private fun parentCategoryId(
        userEmail: String,
        categoryLevel: Int,
        parentCategoryCode: String?,
        currentParentCategoryId: Long? = null,
    ): Long? {
        if (categoryLevel <= 1) return null
        return parentCategoryCode
            ?.takeIf { it.isNotBlank() }
            ?.let { taskCategoryRepository.idByCode(userEmail, it) }
            ?: currentParentCategoryId
    }
}
