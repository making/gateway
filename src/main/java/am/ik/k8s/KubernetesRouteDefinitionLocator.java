package am.ik.k8s;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.PostConstruct;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServiceSpec;
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
					String routes = annotations.get(KEY_PREFIX + "/routes");
					if (routes == null) {
						return;
					}
					String portName = annotations.get(KEY_PREFIX + "/port");
					ServiceSpec spec = service.getSpec();
					List<ServicePort> ports = spec.getPorts();
					String scheme = annotations.getOrDefault(KEY_PREFIX + "/scheme",
							"http");
					OptionalInt sp = determinePort(portName, ports);
					if (sp.isPresent()) {
						int port = sp.getAsInt();
						RouteDefinition routeDefinition = this.objectMapper
								.readValue(routes, RouteDefinition.class);
						routeDefinition.setId(id);
						if (routeDefinition.getUri() == null) {
							boolean useClusterIp = Boolean.parseBoolean(
									annotations.get(KEY_PREFIX + "/useClusterIP"));
							String host = useClusterIp ? spec.getClusterIP()
									: metadata.getName() + "." + metadata.getNamespace()
											+ ".svc.cluster.local";
							URI uri = UriComponentsBuilder.newInstance() //
									.scheme(scheme) //
									.host(host) //
									.port(port) //
									.build().toUri();
							routeDefinition.setUri(uri);
						}
						String yaml = this.objectMapper
								.writeValueAsString(routeDefinition);
						log.info("Update {}\t{}", id, yaml);
						this.routeDefinitions.put(routeDefinition.getId(),
								routeDefinition);
						this.eventPublisher.publishEvent(new RefreshRoutesEvent(this));
					}
					else {
						log.warn("Could not determine port => {}", id);
					}
				}
			}
			else if (action == Action.DELETED) {
				log.info("Delete {}", id);
				this.routeDefinitions.remove(id);
				this.eventPublisher.publishEvent(new RefreshRoutesEvent(this));
			}
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	static OptionalInt determinePort(String portName, List<ServicePort> ports) {
		Optional<ServicePort> sp = ports.stream()
				.filter(p -> Objects.equals(p.getName(), portName)).findAny();
		if (sp.isPresent()) {
			return OptionalInt.of(sp.get().getPort());
		}
		else if (ports.size() == 1) {
			return OptionalInt.of(ports.get(0).getPort());
		}
		return OptionalInt.empty();
	}

	@Override
	public void onClose(KubernetesClientException e) {
		log.debug("close");
	}
}
