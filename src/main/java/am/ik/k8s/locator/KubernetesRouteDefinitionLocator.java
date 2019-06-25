package am.ik.k8s.locator;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.PostConstruct;

import am.ik.k8s.crd.DoneableRouteDefinition;
import am.ik.k8s.crd.RouteDefinitionList;
import am.ik.k8s.crd.RouteDefinitionSpec;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
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
import org.springframework.stereotype.Component;

@Component
public class KubernetesRouteDefinitionLocator
		implements RouteDefinitionLocator, Watcher<am.ik.k8s.crd.RouteDefinition> {
	private static final Logger log = LoggerFactory
			.getLogger(KubernetesRouteDefinitionLocator.class);
	private final ConcurrentMap<String, RouteDefinition> routeDefinitions = new ConcurrentHashMap<>();
	private final ApplicationEventPublisher eventPublisher;
	private final KubernetesClient kubernetesClient;

	public KubernetesRouteDefinitionLocator(ApplicationEventPublisher eventPublisher,
			KubernetesClient kubernetesClient) {
		this.eventPublisher = eventPublisher;
		this.kubernetesClient = kubernetesClient;
	}

	@Override
	public Flux<RouteDefinition> getRouteDefinitions() {
		return Flux.fromIterable(this.routeDefinitions.values());
	}

	@PostConstruct
	public void watch() {
		final CustomResourceDefinition crd = this.kubernetesClient
				.customResourceDefinitions().withName("routedefinitions.gateway.ik.am")
				.get();
		this.kubernetesClient
				.customResources(crd, am.ik.k8s.crd.RouteDefinition.class,
						RouteDefinitionList.class, DoneableRouteDefinition.class)
				.inAnyNamespace().watch(this);
	}

	@Override
	public void eventReceived(Action action, am.ik.k8s.crd.RouteDefinition resource) {
		final ObjectMeta metadata = resource.getMetadata();
		final String id = routeId(metadata);
		if (action == Action.ADDED || action == Action.MODIFIED) {
			final RouteDefinitionSpec spec = resource.getSpec();
			final String name = metadata.getName();
			final String namespace = metadata.getNamespace();
			final Optional<RouteDefinition> rd = spec.toRouteDefinition(id, name,
					namespace, (n, ns) -> kubernetesClient.services().inNamespace(ns)
							.withName(n).get());
			if (rd.isPresent()) {
				final RouteDefinition routeDefinition = rd.get();
				log.info("Update {}\t{}", id, routeDefinition);
				this.routeDefinitions.put(routeDefinition.getId(), routeDefinition);
				this.eventPublisher.publishEvent(new RefreshRoutesEvent(this));
			}
			else {
				log.warn("Failed to update {}", id);
			}
		}
		else if (action == Action.DELETED) {
			log.info("Delete {}", id);
			this.routeDefinitions.remove(id);
			this.eventPublisher.publishEvent(new RefreshRoutesEvent(this));
		}
	}

	@Override
	public void onClose(KubernetesClientException e) {
		log.debug("close");
	}

	private static String routeId(ObjectMeta metadata) {
		return metadata.getNamespace() + "/" + metadata.getName();
	}
}
