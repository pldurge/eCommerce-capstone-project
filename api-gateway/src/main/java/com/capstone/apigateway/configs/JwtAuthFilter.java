package com.capstone.apigateway.configs;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthFilter extends AbstractGatewayFilterFactory<JwtAuthFilter.Config> {

    private static final String BLACKLIST_PREFIX = "blacklist:";

    private final JwtUtil utils;
    private final ReactiveStringRedisTemplate redisTemplate;

    public JwtAuthFilter(JwtUtil utils,  ReactiveStringRedisTemplate redisTemplate) {
        super(Config.class);
        this.utils = utils;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String authHeader = exchange.getRequest().getHeaders()
                    .getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return unauthorized(exchange, "Missing or Invalid Authorization Header");
            }

            String token = authHeader.substring(7);

            if (!utils.isTokenValid(token)) {
                return unauthorized(exchange, "Token Invalid or Expired : Please Log in Again");
            }

            // Check Redis blacklist — rejects tokens invalidated by logout
            return redisTemplate.hasKey(BLACKLIST_PREFIX + token)
                    .flatMap(blacklisted -> {
                        if (Boolean.TRUE.equals(blacklisted)) {
                            return unauthorized(exchange, "Token has been invalidated. Please login again.");
                        }

                        String username = utils.extractUsername(token);
                        String role = utils.extractRole(token);

                        var mutatedRequest = exchange.getRequest().mutate()
                                .header("X-User-Name", username)
                                .header("X-User-Role", role)
                                .build();

                        return chain.filter(exchange.mutate().request(mutatedRequest).build());
                    });
        };
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("content-type", "application/json");
        var buffer = exchange.getResponse().bufferFactory()
                .wrap(("{\"error\": \"" + message + "\"}").getBytes());
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    public static class Config {}
}
