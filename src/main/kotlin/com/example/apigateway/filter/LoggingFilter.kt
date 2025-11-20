package com.example.apigateway.filter

import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class LoggingFilter : GlobalFilter, Ordered {

    private val logger = LoggerFactory.getLogger(LoggingFilter::class.java)

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val request = exchange.request
        val startTime = System.currentTimeMillis()

        logger.info("=== 요청 시작 ===")
        logger.info("Method: {}", request.method)
        logger.info("URI: {}", request.uri)
        logger.info("Path: {}", request.path)
        logger.info("Headers: {}", request.headers)
        logger.info("Query Params: {}", request.queryParams)

        return chain.filter(exchange)
            .doOnSuccess {
                val response = exchange.response
                val duration = System.currentTimeMillis() - startTime
                logger.info("=== 응답 완료 ===")
                logger.info("Status: {}", response.statusCode)
                logger.info("Headers: {}", response.headers)
                logger.info("처리 시간: {}ms", duration)
            }
            .doOnError { error ->
                val duration = System.currentTimeMillis() - startTime
                logger.error("=== 요청 처리 중 에러 발생 ===")
                logger.error("URI: {}", request.uri)
                logger.error("처리 시간: {}ms", duration)
                logger.error("에러 타입: {}", error.javaClass.simpleName)
                logger.error("에러 메시지: {}", error.message, error)
            }
            .then(Mono.fromRunnable {
                val response = exchange.response
                if (response.statusCode?.is5xxServerError == true) {
                    logger.error("=== 5xx 서버 에러 발생 ===")
                    logger.error("Status: {}", response.statusCode)
                    logger.error("Request URI: {}", request.uri)
                    logger.error("Request Method: {}", request.method)
                }
            })
    }

    override fun getOrder(): Int {
        return Ordered.HIGHEST_PRECEDENCE
    }
}
