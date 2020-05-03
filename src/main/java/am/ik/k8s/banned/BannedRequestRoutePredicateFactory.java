package am.ik.k8s.banned;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.cloud.gateway.handler.predicate.AbstractRoutePredicateFactory;
import org.springframework.cloud.gateway.handler.predicate.GatewayPredicate;
import org.springframework.cloud.gateway.handler.predicate.PathRoutePredicateFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import java.util.function.Predicate;

import static org.springframework.http.HttpHeaders.USER_AGENT;

@Component
public class BannedRequestRoutePredicateFactory extends AbstractRoutePredicateFactory<PathRoutePredicateFactory.Config> {
    private final MeterRegistry meterRegistry;

    public BannedRequestRoutePredicateFactory(MeterRegistry meterRegistry) {
        super(PathRoutePredicateFactory.Config.class);
        this.meterRegistry = meterRegistry;
    }

    @Override
    public Predicate<ServerWebExchange> apply(PathRoutePredicateFactory.Config config) {
        return new BannedCrawlerRoutePredicate(config, this.meterRegistry);
    }

    static class BannedCrawlerRoutePredicate implements GatewayPredicate {
        private final Predicate<ServerWebExchange> bannedPathPredicate;
        private final Counter bannedCounter;

        public BannedCrawlerRoutePredicate(PathRoutePredicateFactory.Config config, MeterRegistry meterRegistry) {
            this.bannedPathPredicate = new PathRoutePredicateFactory().apply(config);
            this.bannedCounter = meterRegistry.counter("gateway.banned");
        }

        @Override
        public boolean test(ServerWebExchange exchange) {
            final boolean banned = this.isBanned(exchange);
            if (banned) {
                this.bannedCounter.increment();
            }
            return banned;
        }

        public boolean isBanned(ServerWebExchange exchange) {
            final String userAgent = exchange.getRequest().getHeaders().getFirst(USER_AGENT);
            if (userAgent != null && userAgent.startsWith("Hatena::Russia::Crawler")) {
                return true;
            }
            return this.bannedPathPredicate.test(exchange);
        }


        @Override
        public String toString() {
            return "BannedRequest (" + this.bannedPathPredicate + ")";
        }
    }
}
