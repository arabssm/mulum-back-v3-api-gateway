package com.example.apigateway.filter

import com.example.apigateway.model.UserRole
import com.example.apigateway.util.JwtUtil
import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class AuthorizationFilter(
    private val jwtUtil: JwtUtil
) : GlobalFilter, Ordered {

    private val log = LoggerFactory.getLogger(AuthorizationFilter::class.java)

    companion object {
        private const val STUDENT_PREFIX = "std"
        private const val TEACHER_PREFIX = "tch"
        private const val ANONYMOUS_PREFIX = "ara"
    }

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val request = exchange.request
        val path = request.uri.path

        log.info("요청 처리 중: {}", path)

        val pathSegments = path.split("/").filter { it.isNotEmpty() }

        if (pathSegments.isEmpty()) {
            return chain.filter(exchange)
        }

        val firstSegment = pathSegments[0]

        val requiredRole = when (firstSegment) {
            STUDENT_PREFIX -> UserRole.STUDENT
            TEACHER_PREFIX -> UserRole.TEACHER
            ANONYMOUS_PREFIX -> UserRole.ANONYMOUS
            else -> UserRole.STUDENT_OR_TEACHER
        }

        log.info("필요한 권한: {}", requiredRole)

        if (requiredRole == UserRole.ANONYMOUS) {
            log.info("익명 접근 허용 (인증/인가 불필요)")
            return chain.filter(exchange)
        }

        val authHeader = request.headers.getFirst("Authorization")

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Authorization 헤더가 없거나 유효하지 않음")
            exchange.response.statusCode = HttpStatus.UNAUTHORIZED
            return exchange.response.setComplete()
        }

        val token = authHeader.substring(7)

        if (!jwtUtil.validateToken(token)) {
            log.warn("유효하지 않은 토큰")
            exchange.response.statusCode = HttpStatus.UNAUTHORIZED
            return exchange.response.setComplete()
        }

        val userRole = jwtUtil.getRoleFromToken(token)
        log.info("토큰의 사용자 권한: {}", userRole)

        val authorized = when (requiredRole) {
            UserRole.STUDENT -> userRole?.lowercase() == "student" || userRole?.lowercase() == "std"
            UserRole.TEACHER -> userRole?.lowercase() == "teacher" || userRole?.lowercase() == "tch"
            UserRole.STUDENT_OR_TEACHER -> {
                val r = userRole?.lowercase()
                r == "student" || r == "std" || r == "teacher" || r == "tch"
            }
            else -> true
        }

        if (!authorized) {
            log.warn("권한 없음. 필요한 권한: {}, 사용자 권한: {}", requiredRole, userRole)
            exchange.response.statusCode = HttpStatus.FORBIDDEN
            return exchange.response.setComplete()
        }

        log.info("인가 성공")

        // 토큰 클레임에서 사용자 정보와 팀 ID를 추출해 요청 헤더에 주입합니다.
        val claims = jwtUtil.getClaims(token)
        val user = try {
            claims?.get("user", String::class.java)
                ?: claims?.subject
                ?: claims?.get("username", String::class.java)
        } catch (_: Exception) {
            null
        }

        val teamId = try {
            claims?.get("teamId", String::class.java)
                ?: claims?.get("team_id", String::class.java)
                ?: claims?.get("team", String::class.java)
        } catch (_: Exception) {
            null
        }

        // X-User, X-Team-Id 헤더를 추가합니다. teamId가 없으면 헤더를 추가하지 않습니다.
        val requestBuilder = exchange.request.mutate()
            .header("X-User-Id", user ?: "unknown")

        if (teamId != null) {
            requestBuilder.header("X-Team-Id", teamId)
        }

        val mutatedRequest = requestBuilder.build()

        val mutatedExchange = exchange.mutate().request(mutatedRequest).build()
        return chain.filter(mutatedExchange)
    }

    override fun getOrder(): Int {
        return Ordered.HIGHEST_PRECEDENCE + 1
    }
}
