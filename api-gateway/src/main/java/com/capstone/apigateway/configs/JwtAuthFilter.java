package com.capstone.apigateway.configs;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends AbstractGatewayFilterFactory<JwtAuthFilter.Config> {

    private final JwtUtil utils;

    public JwtAuthFilter() {
        super(Config.class);
        this.utils = null;
    }

    @Override
    public GatewayFilter apply(Config config){
        return (exchange, chain) ->{
            String authHeader = exchange.getRequest().getHeaders()
                    .getFirst(HttpHeaders.AUTHORIZATION);

            if(authHeader == null || !authHeader.startsWith("Bearer ")){
                return unauthorized(exchange, "Missing or Invalid Authorization Header");
            }

            String token = authHeader.substring(7);

            if(!utils.isTokenValid(token)) {
                return unauthorized(exchange, "Token Invalid or Expired");
            }

            String username = utils.extractUsername(token);

            String role = utils.extractRole(token);

            var mutatedRequest = exchange.getRequest().mutate()
                    .header("X-User-Name", username)
                    .header("X-User-Role", role)
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
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
