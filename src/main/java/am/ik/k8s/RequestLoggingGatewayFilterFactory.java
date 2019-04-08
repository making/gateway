package am.ik.k8s;

import java.time.LocalDateTime;

import is.tagomor.woothee.Classifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;

import static org.springframework.http.HttpHeaders.REFERER;

@Component
public class RequestLoggingGatewayFilterFactory extends AbstractGatewayFilterFactory {
	private final Logger log = LoggerFactory.getLogger("RTR");

	@Override
	public GatewayFilter apply(Object config) {
		return (exchange, chain) -> {
			long begin = System.nanoTime();
			return chain.filter(exchange) //
					.doFinally(__ -> {
						long elapsed = (System.nanoTime() - begin) / 1_000_000;
						ServerHttpRequest request = exchange.getRequest();
						ServerHttpResponse response = exchange.getResponse();
						LocalDateTime now = LocalDateTime.now();
						HttpMethod method = request.getMethod();
						RequestPath path = request.getPath();
						HttpStatus code = response.getStatusCode();
						int statusCode = code == null ? 0 : code.value();
						HttpHeaders headers = request.getHeaders();
						String host = headers.getHost().getHostString();
						String address = request.getRemoteAddress().getHostString();
						String userAgent = headers.getFirst(HttpHeaders.USER_AGENT);
						String referer = headers.getFirst(REFERER);
						boolean crawler = userAgent == null
								|| userAgent.toLowerCase().contains("bot")
								|| Classifier.isCrawler(userAgent);
						log.info(
								"date:{}\tmethod:{}\tpath:{}\tstatus:{}\thost:{}\taddress:{}\tresponse_time:{}ms\tcrawler:{}\tuser-agent:{}\treferer:{}",
								now, method, path, statusCode, host, address, elapsed,
								crawler, userAgent, referer);
					});
		};
	}
}
