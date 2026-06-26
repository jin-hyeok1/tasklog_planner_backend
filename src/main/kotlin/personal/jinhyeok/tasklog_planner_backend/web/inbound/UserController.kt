package personal.jinhyeok.tasklog_planner_backend.web.inbound

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import personal.jinhyeok.tasklog_planner_backend.service.user.UserService
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.LoginRequest
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.UpdateUserRequest
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.SignUpRequest

@RestController
@RequestMapping("/api/users")
class UserController(private val service: UserService) {

    @PostMapping("/sign-up")
    fun signUp(@RequestBody request: SignUpRequest): BaseResponseEntity =
        BaseResponseEntity.created(service.signUp(request))

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): BaseResponseEntity =
        BaseResponseEntity.single(service.login(request))

    @GetMapping("/me")
    fun me(): BaseResponseEntity = BaseResponseEntity.single(service.getMe())

    @PutMapping("/me")
    fun updateMe(@RequestBody request: UpdateUserRequest): BaseResponseEntity =
        BaseResponseEntity.single(service.updateMe(request))
}
