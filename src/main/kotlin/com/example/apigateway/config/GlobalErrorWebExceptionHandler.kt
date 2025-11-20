package com.example.apigateway.config

import org.slf4j.LoggerFactory
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler
import org.springframework.core.annotation.Order
import org.springframework.core.io.buffer.DataBufferFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.net.ConnectException
import java.nio.charset.StandardCharsets

@Component
@Order(-1)
class GlobalErrorWebExceptionHandler : ErrorWebExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalErrorWebExceptionHandler::class.java)

    override fun handle(exchange: ServerWebExchange, ex: Throwable): Mono<Void> {
        logger.error("=== Global Exception Handler ===")
        logger.error("요청 URI: {}", exchange.request.uri)
        logger.error("요청 Path: {}", exchange.request.path)
        logger.error("요청 Method: {}", exchange.request.method)
        logger.error("예외 타입: {}", ex.javaClass.name)
        logger.error("예외 메시지: {}", ex.message)

        // 원인(cause)도 로깅
        if (ex.cause != null) {
            logger.error("예외 원인: {}", ex.cause?.javaClass?.name)
            logger.error("원인 메시지: {}", ex.cause?.message)
        }

        // 스택 트레이스를 로그에 기록
        logger.error("스택 트레이스:", ex)

        val response = exchange.response
        val bufferFactory: DataBufferFactory = response.bufferFactory()

        val errorResponse = when (ex) {
            is ConnectException -> {
                logger.error("백엔드 서비스 연결 실패: {}", ex.message)
                response.statusCode = HttpStatus.SERVICE_UNAVAILABLE
                """{"error": "Service Unavailable", "message": "백엔드 서비스에 연결할 수 없습니다.", "details": "${ex.message}"}"""
            }
            is ResponseStatusException -> {
                response.statusCode = ex.statusCode
                """{"error": "${ex.statusCode}", "message": "${ex.reason}", "details": "${ex.message}"}"""
            }
            else -> {
                logger.error("처리되지 않은 예외 발생", ex)
                response.statusCode = HttpStatus.INTERNAL_SERVER_ERROR
                """{"error": "Internal Server Error", "message": "서버 내부 오류가 발생했습니다.", "details": "${ex.message}", "type": "${ex.javaClass.simpleName}"}"""
            }
        }

        response.headers.contentType = MediaType.APPLICATION_JSON

        val dataBuffer = bufferFactory.wrap(errorResponse.toByteArray(StandardCharsets.UTF_8))
        return response.writeWith(Mono.just(dataBuffer))
    }
}
