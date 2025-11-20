package com.example.apigateway.filter

import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.server.reactive.ServerHttpResponseDecorator
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.charset.StandardCharsets

@Component
class ErrorLoggingFilter : GlobalFilter, Ordered {

    private val logger = LoggerFactory.getLogger(ErrorLoggingFilter::class.java)

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val decoratedResponse = object : ServerHttpResponseDecorator(exchange.response) {
            override fun writeWith(body: org.reactivestreams.Publisher<out DataBuffer>): Mono<Void> {
                if (statusCode?.is5xxServerError == true) {
                    val flux = if (body is Flux<*>) {
                        body as Flux<DataBuffer>
                    } else {
                        Flux.from(body as org.reactivestreams.Publisher<DataBuffer>)
                    }

                    val modifiedBody = flux.map { dataBuffer ->
                        val content = ByteArray(dataBuffer.readableByteCount())
                        dataBuffer.read(content)
                        DataBufferUtils.release(dataBuffer)

                        val responseBody = String(content, StandardCharsets.UTF_8)
                        logger.error("=== 백엔드 5xx 에러 응답 ===")
                        logger.error("Status: {}", statusCode)
                        logger.error("Request URI: {}", exchange.request.uri)
                        logger.error("Request Path: {}", exchange.request.path)
                        logger.error("Backend URL: {}", exchange.getAttribute<Any>("org.springframework.cloud.gateway.support.ServerWebExchangeUtils.gatewayRequestUrl"))
                        logger.error("Response Body: {}", responseBody)

                        delegate.bufferFactory().wrap(content)
                    }

                    return super.writeWith(modifiedBody)
                }
                return super.writeWith(body)
            }
        }

        return chain.filter(exchange.mutate().response(decoratedResponse).build())
    }

    override fun getOrder(): Int {
        return Ordered.LOWEST_PRECEDENCE
    }
}

