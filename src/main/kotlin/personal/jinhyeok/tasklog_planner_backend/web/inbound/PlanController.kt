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
import personal.jinhyeok.tasklog_planner_backend.service.plan.PlanService
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.PlanType
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.SavePlanRequest

@RestController
@RequestMapping("/api/plans")
class PlanController(private val service: PlanService) {
    @GetMapping("/tree")
    fun tree(@RequestParam(required = false) year: Int?): BaseResponseEntity =
        BaseResponseEntity.list(service.planTree(year))

    @GetMapping
    fun plans(
        @RequestParam(required = false) planType: PlanType?,
        @RequestParam(required = false) parentPlanCode: String?,
        @RequestParam(required = false) year: Int?,
    ): BaseResponseEntity = BaseResponseEntity.list(service.plans(planType, parentPlanCode, year))

    @PostMapping
    fun create(@RequestBody request: SavePlanRequest): BaseResponseEntity =
        BaseResponseEntity.created(service.createPlan(request))

    @PutMapping("/{planCode}")
    fun update(@PathVariable planCode: String, @RequestBody request: SavePlanRequest): BaseResponseEntity =
        BaseResponseEntity.single(service.updatePlan(planCode, request))

    @DeleteMapping("/{planCode}")
    fun delete(
        @PathVariable planCode: String,
        @RequestParam(defaultValue = "false") cascade: Boolean,
    ): BaseResponseEntity = BaseResponseEntity.single(service.deletePlan(planCode, cascade))
}
