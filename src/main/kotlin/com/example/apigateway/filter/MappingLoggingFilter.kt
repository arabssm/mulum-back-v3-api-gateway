package com.example.apigateway.filter

import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.route.Route
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.net.URI

@Component
class MappingLoggingFilter : GlobalFilter, Ordered {

    private val log = LoggerFactory.getLogger(MappingLoggingFilter::class.java)

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val request = exchange.request
        val originalPath = request.uri.path

        // Try to get resolved route and request URL attributes after route resolution
        val routeAttr = exchange.attributes[ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR] as? Route
        val requestUrlAttr = exchange.attributes[ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR] as? URI

        if (routeAttr != null && requestUrlAttr != null) {
            log.info("[게이트웨이 매핑] 들어온경로='{}' -> 라우트Id='{}' -> 전달경로='{}'", originalPath, routeAttr.id, requestUrlAttr)
        } else {
            // If route or requestUrl not available yet, log that mapping not resolved yet
            log.info("[게이트웨이 매핑] 들어온경로='{}' -> 매핑 미확정", originalPath)
        }

        return chain.filter(exchange).doOnSuccess {
            // after filter chain completes, attempt to log resolved values again
            val r = exchange.attributes[ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR] as? Route
            val u = exchange.attributes[ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR] as? URI
            if (r != null && u != null) {
                log.info("[게이트웨이 매핑 - 완료] 들어온경로='{}' -> 라우트Id='{}' -> 전달경로='{}'", originalPath, r.id, u)
            }
        }
    }

    override fun getOrder(): Int {
        // Should run after routing filters (LOW precedence). Use HIGHEST + 1000 so it runs later.
        return Ordered.LOWEST_PRECEDENCE - 1
    }
}
