package am.ik.k8s.teapot;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class TeapotGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    @Override
    public GatewayFilter apply(Object config) {
        return new TeapotGatewayFilter();
    }

    static class TeapotGatewayFilter implements GatewayFilter {

        @Override
        public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
            return Mono.fromRunnable(() -> {
                exchange.getResponse().setStatusCode(HttpStatus.I_AM_A_TEAPOT);
            });
        }

        @Override
        public String toString() {
            return "[Teapot]";
        }
    }
}