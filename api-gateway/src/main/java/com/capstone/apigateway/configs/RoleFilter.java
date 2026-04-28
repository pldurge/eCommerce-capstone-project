package com.capstone.apigateway.configs;

import lombok.Getter;
import lombok.Setter;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class RoleFilter extends AbstractGatewayFilterFactory<RoleFilter.Config> {

    public RoleFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config){
        return (exchange, chain) -> {
            // X-User-Role is set by JwtAuthFilter earlier in the chain
            String userRole = exchange.getRequest().getHeaders()
                    .getFirst("X-User-Role");

            if (userRole == null || !userRole.equals(config.getRequiredRole())) {
                return forbidden(exchange,
                        "Access denied: requires role " + config.getRequiredRole()
                                + ", but got " + (userRole != null ? userRole : "none"));
            }

            return chain.filter(exchange);
        };
    }

    private Mono<Void> forbidden(ServerWebExchange exchange, String reason) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        var buffer = exchange.getResponse().bufferFactory()
                .wrap(("{\"error\": \"" + reason + "\"}").getBytes());
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Getter
    @Setter
    public static class Config {
        private String requiredRole;
    }
}
