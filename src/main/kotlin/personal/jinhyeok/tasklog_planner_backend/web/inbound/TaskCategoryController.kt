package personal.jinhyeok.tasklog_planner_backend.web.inbound

import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import personal.jinhyeok.tasklog_planner_backend.service.category.TaskCategoryService
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.SaveTaskCategoryRequest

@RestController
@RequestMapping("/api/task-categories")
class TaskCategoryController(private val service: TaskCategoryService) {
    @GetMapping("/tree")
    fun tree(
        @RequestParam(required = false) enabled: Boolean?,
        @RequestParam(required = false, name = "category-level") categoryLevel: Short?,
        ): BaseResponseEntity =
        BaseResponseEntity.list(service.categoryTree(enabled, categoryLevel))

    @PostMapping
    fun create(@RequestBody request: SaveTaskCategoryRequest): BaseResponseEntity =
        BaseResponseEntity.created(service.createCategory(request))

    @PutMapping("/{categoryCode}")
    fun update(@PathVariable categoryCode: String, @RequestBody request: SaveTaskCategoryRequest): BaseResponseEntity =
        BaseResponseEntity.single(service.updateCategory(categoryCode, request))

    @DeleteMapping("/{categoryCode}")
    fun delete(
        @PathVariable categoryCode: String,
        @RequestParam(defaultValue = "false") cascade: Boolean,
    ): BaseResponseEntity = BaseResponseEntity.single(service.deleteCategory(categoryCode, cascade))
}
