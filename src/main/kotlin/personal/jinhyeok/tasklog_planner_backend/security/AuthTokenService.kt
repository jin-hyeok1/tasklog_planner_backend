package personal.jinhyeok.tasklog_planner_backend.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import personal.jinhyeok.tasklog_planner_backend.web.inbound.dto.UserRole
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class TokenPrincipal(
    val email: String,
    val role: String,
    val expiresAt: Instant,
)

data class IssuedToken(
    val accessToken: String,
    val expiresAt: Instant,
)

@Component
class AuthTokenService(
    @Value("\${app.auth-token-secret:change-me}") private val secret: String,
    @Value("\${app.auth-token-ttl-seconds:86400}") private val ttlSeconds: Long,
) {
    fun issue(email: String, role: String): IssuedToken {
        val now = Instant.now()
        val expiresAt = now.plusSeconds(ttlSeconds)
        val header = base64Url("""{"alg":"HS256","typ":"JWT"}""")
        val payload = base64Url(
            """{"sub":"${escape(email)}","role":"${escape(role)}","iat":${now.epochSecond},"exp":${expiresAt.epochSecond}}""",
        )
        val signature = sign("$header.$payload")
        return IssuedToken("$header.$payload.$signature", expiresAt)
    }

    fun authenticate(token: String): TokenPrincipal? {
        val parts = token.split('.')
        if (parts.size != 3) return null
        val signingInput = "${parts[0]}.${parts[1]}"
        val expectedSignature = sign(signingInput)
        if (!MessageDigest.isEqual(expectedSignature.toByteArray(StandardCharsets.UTF_8), parts[2].toByteArray(StandardCharsets.UTF_8))) return null

        val payload = runCatching { String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8) }.getOrNull() ?: return null
        val email = claim(payload, "sub") ?: return null
        val role = claim(payload, "role") ?: UserRole.USER.name
        val exp = claim(payload, "exp")?.toLongOrNull() ?: return null
        if (Instant.now().epochSecond >= exp) return null
        return TokenPrincipal(email = email, role = role, expiresAt = Instant.ofEpochSecond(exp))
    }

    private fun sign(input: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        return base64Url(mac.doFinal(input.toByteArray(StandardCharsets.UTF_8)))
    }

    private fun claim(payload: String, key: String): String? {
        val regex = Regex(""""$key"\s*:\s*("([^"]*)"|(\d+))""")
        val match = regex.find(payload) ?: return null
        return match.groupValues[2].ifBlank { match.groupValues[3] }.takeIf { it.isNotBlank() }
    }

    private fun escape(value: String): String = value.replace("\\", "\\\\").replace("\"", "\\\"")

    private fun base64Url(value: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun base64Url(value: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(value)
}
