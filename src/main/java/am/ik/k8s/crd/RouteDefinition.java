package am.ik.k8s.crd;

import java.util.StringJoiner;

import io.fabric8.kubernetes.client.CustomResource;

public class RouteDefinition extends CustomResource {
	private RouteDefinitionSpec spec;

	public RouteDefinitionSpec getSpec() {
		return spec;
	}

	public void setSpec(RouteDefinitionSpec spec) {
		this.spec = spec;
	}

	@Override
	public String toString() {
		return new StringJoiner(", ", RouteDefinition.class.getSimpleName() + "[", "]")
				.add("namespace=" + getMetadata().getNamespace()) //
				.add("name=" + getMetadata().getName()) //
				.add("spec=" + spec) //
				.toString();
	}
}
