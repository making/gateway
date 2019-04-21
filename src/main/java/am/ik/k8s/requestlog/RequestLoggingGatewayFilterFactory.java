package am.ik.k8s.requestlog;

import java.time.OffsetDateTime;
import java.util.Objects;

import is.tagomor.woothee.Classifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import static org.springframework.http.HttpHeaders.REFERER;

@Component
public class RequestLoggingGatewayFilterFactory extends AbstractGatewayFilterFactory {
	private final Logger log = LoggerFactory.getLogger("RTR");
	private final KafkaTemplate<String, RequestLog> kafkaTemplate;

	public RequestLoggingGatewayFilterFactory(
			KafkaTemplate<String, RequestLog> kafkaTemplate) {
		this.kafkaTemplate = kafkaTemplate;
	}

	@Override
	public GatewayFilter apply(Object config) {
		return (exchange, chain) -> {
			long begin = System.nanoTime();
			return chain.filter(exchange) //
					.doFinally(__ -> {
						long elapsed = (System.nanoTime() - begin) / 1_000_000;
						ServerHttpRequest request = exchange.getRequest();
						ServerHttpResponse response = exchange.getResponse();
						OffsetDateTime now = OffsetDateTime.now();
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
						RequestLog requestLog = new RequestLogBuilder()
								.setDate(now.toString())
								.setMethod(Objects.toString(method, ""))
								.setPath(path.value()).setStatus(statusCode).setHost(host)
								.setAddress(address).setElapsed(elapsed)
								.setUserAgent(userAgent).setReferer(referer)
								.setCrawler(crawler).createRequestLog();
						log.info("{}", requestLog);
						Mono.defer(() -> Mono.fromFuture(this.kafkaTemplate
								.send(this.kafkaTemplate.getDefaultTopic(),
										requestLog.getAddress(), requestLog)
								.completable())) //
								.subscribeOn(Schedulers.parallel()) //
								.subscribe();
					});
		};
	}
}
