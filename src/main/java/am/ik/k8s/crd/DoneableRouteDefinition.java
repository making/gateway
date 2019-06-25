package am.ik.k8s.crd;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;

public class DoneableRouteDefinition extends CustomResourceDoneable<RouteDefinition> {

	public DoneableRouteDefinition(RouteDefinition resource,
			Function<RouteDefinition, RouteDefinition> function) {
		super(resource, function);
	}
}
