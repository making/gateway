package am.ik.k8s.crd;

import java.util.HashMap;
import java.util.Map;

import io.fabric8.kubernetes.api.KubernetesResourceMappingProvider;
import io.fabric8.kubernetes.api.model.KubernetesResource;

public class RouteDefinitionMappingProvider implements KubernetesResourceMappingProvider {

	private Map<String, Class<? extends KubernetesResource>> mappings = new HashMap<String, Class<? extends KubernetesResource>>() {
		{
			put("gateway.ik.am/v1beta1#RouteDefinition", RouteDefinition.class);
			put("gateway.ik.am/v1beta1#RouteDefinitionList", RouteDefinitionList.class);
		}
	};

	public Map<String, Class<? extends KubernetesResource>> getMappings() {
		return this.mappings;
	}
}
