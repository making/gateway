package am.ik.k8s;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.PostConstruct;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class KubernetesRouteDefinitionLocator
		implements RouteDefinitionLocator, Watcher<Service> {
	private static final Logger log = LoggerFactory
			.getLogger(KubernetesRouteDefinitionLocator.class);
	private static final String KEY_PREFIX = "spring.cloud.gateway";
	private final ConcurrentMap<String, RouteDefinition> routeDefinitions = new ConcurrentHashMap<>();
	private final ApplicationEventPublisher eventPublisher;
	private final KubernetesClient kubernetesClient;
	private final ObjectMapper objectMapper;

	public KubernetesRouteDefinitionLocator(ApplicationEventPublisher eventPublisher,
			KubernetesClient kubernetesClient) {
		this.eventPublisher = eventPublisher;
		this.kubernetesClient = kubernetesClient;
		this.objectMapper = new Jackson2ObjectMapperBuilder().factory(new YAMLFactory())
				.build();
	}

	@Override
	public Flux<RouteDefinition> getRouteDefinitions() {
		return Flux.fromIterable(this.routeDefinitions.values());
	}

	@PostConstruct
	public void watch() {
		this.kubernetesClient.services().inAnyNamespace().watch(this);
	}

	@Override
	public void eventReceived(Action action, Service service) {
		try {
			ObjectMeta metadata = service.getMetadata();
			String id = metadata.getNamespace() + "/" + metadata.getName();
			if (action == Action.ADDED || action == Action.MODIFIED) {
				Map<String, String> annotations = metadata.getAnnotations();
				if (annotations != null) {
					if (!annotations.containsKey(KEY_PREFIX + "/port")) {
						return;
					}
					String portName = annotations.get(KEY_PREFIX + "/port");
					Optional<ServicePort> sp = service.getSpec().getPorts().stream()
							.filter(p -> Objects.equals(p.getName(), portName)).findAny();
					if (sp.isPresent()) {
						ServicePort servicePort = sp.get();
						String routes = annotations.get(KEY_PREFIX + "/routes");
						RouteDefinition routeDefinition = this.objectMapper
								.readValue(routes, RouteDefinition.class);
						routeDefinition.setId(id);
						if (routeDefinition.getUri() == null) {
							String host = metadata.getName() + "."
									+ metadata.getNamespace() + ".svc.cluster.local";
							URI uri = UriComponentsBuilder.newInstance().scheme("http")
									.host(host).port(servicePort.getPort()).build()
									.toUri();
							routeDefinition.setUri(uri);
						}
						String yaml = this.objectMapper
								.writeValueAsString(routeDefinition);
						log.info("Update {}\t{}", service.getMetadata().getName(), yaml);
						this.routeDefinitions.put(routeDefinition.getId(),
								routeDefinition);
						this.eventPublisher.publishEvent(new RefreshRoutesEvent(this));
					}
				}
			}
			else if (action == Action.DELETED) {
				log.info("Delete {}\t{}", service.getMetadata().getName(), id);
				this.routeDefinitions.remove(id);
				this.eventPublisher.publishEvent(new RefreshRoutesEvent(this));
			}
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void onClose(KubernetesClientException e) {
		log.debug("close");
	}
}
