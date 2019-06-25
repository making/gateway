package am.ik.k8s.crd;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.StringJoiner;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

@JsonDeserialize(using = JsonDeserializer.None.class)
public class RouteDefinitionSpec implements KubernetesResource {
	private static final Logger log = LoggerFactory.getLogger(RouteDefinitionSpec.class);
	private String serviceName;
	private String portName;
	private Route route;
	private String scheme = "http";
	private boolean useClusterIp = false;

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getPortName() {
		return portName;
	}

	public void setPortName(String portName) {
		this.portName = portName;
	}

	public Route getRoute() {
		return route;
	}

	public void setRoute(Route route) {
		this.route = route;
	}

	public String getScheme() {
		return scheme;
	}

	public void setScheme(String scheme) {
		this.scheme = scheme;
	}

	public static Logger getLog() {
		return log;
	}

	public boolean isUseClusterIp() {
		return useClusterIp;
	}

	public void setUseClusterIp(boolean useClusterIp) {
		this.useClusterIp = useClusterIp;
	}

	public Optional<RouteDefinition> toRouteDefinition(String id, String name,
			String namespace, BiFunction<String, String, Service> findService) {
		final RouteDefinition routeDefinition = new RouteDefinition();
		final String serviceName = Objects.toString(this.serviceName, name);
		final URI uri;
		final Service service = findService.apply(serviceName, namespace);
		if (StringUtils.isEmpty(this.route.uri)) {
			if (service == null) {
				log.warn("Service({}) is not found in {}.", serviceName, namespace);
				return Optional.empty();
			}
			final ServiceSpec serviceSpec = service.getSpec();
			final String host = this.isUseClusterIp() ? serviceSpec.getClusterIP()
					: serviceName + "." + namespace + ".svc.cluster.local";
			final OptionalInt sp = determinePort(this.portName, serviceSpec.getPorts());
			if (sp.isPresent()) {
				uri = UriComponentsBuilder.newInstance().scheme(this.scheme).host(host)
						.port(sp.getAsInt()).build().toUri();
			}
			else {
				log.warn("Could not determine port => {}", id);
				return Optional.empty();
			}
		}
		else {
			uri = URI.create(this.route.uri);
		}
		routeDefinition.setId(id);
		routeDefinition.setUri(uri);
		routeDefinition.setFilters(this.route.filters.stream()
				.filter(x -> x instanceof String || x instanceof Map) //
				.map(x -> createDefinition(x, FilterDefinition::new, (n, args) -> {
					final FilterDefinition definition = new FilterDefinition();
					definition.setName(n);
					definition.setArgs(args);
					return definition;
				})) //
				.collect(Collectors.toList()));
		routeDefinition.setPredicates(this.route.predicates.stream()
				.filter(x -> x instanceof String || x instanceof Map) //
				.map(x -> createDefinition(x, PredicateDefinition::new, (n, args) -> {
					final PredicateDefinition definition = new PredicateDefinition();
					definition.setName(n);
					definition.setArgs(args);
					return definition;
				})) //
				.collect(Collectors.toList()));
		return Optional.of(routeDefinition);
	}

	private static <T> T createDefinition(Object def,
			Function<String, T> createWithString,
			BiFunction<String, Map<String, String>, T> createWithNameAndArgs) {
		if (def instanceof String) {
			return createWithString.apply((String) def);
		}
		else {
			final String name = Objects.toString(((Map) def).get("name"), "null");
			final Map<String, String> args = new LinkedHashMap<>();
			final Object o = ((Map) def).get("args");
			if (o instanceof Map) {
				((Map) o).forEach((k, v) -> args.put(Objects.toString(k, "null"),
						Objects.toString(v, "null")));
			}
			return createWithNameAndArgs.apply(name, args);
		}
	}

	private static OptionalInt determinePort(String portName, List<ServicePort> ports) {
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

	public static class Route {
		private String uri;
		private List<Object> predicates;
		private List<Object> filters;

		public String getUri() {
			return uri;
		}

		public void setUri(String uri) {
			this.uri = uri;
		}

		public List<Object> getPredicates() {
			return predicates;
		}

		public void setPredicates(List<Object> predicates) {
			this.predicates = predicates;
		}

		public List<Object> getFilters() {
			return filters;
		}

		public void setFilters(List<Object> filters) {
			this.filters = filters;
		}

		@Override
		public String toString() {
			return new StringJoiner(", ", Route.class.getSimpleName() + "[", "]")
					.add("uri='" + uri + "'") //
					.add("predicates=" + predicates) //
					.add("filters=" + filters) //
					.toString();
		}
	}

	@Override
	public String toString() {
		return new StringJoiner(", ", RouteDefinitionSpec.class.getSimpleName() + "[",
				"]") //
						.add("serviceName='" + serviceName + "'") //
						.add("portName='" + portName + "'") //
						.add("route=" + route) //
						.add("scheme='" + scheme + "'") //
						.toString();
	}
}
