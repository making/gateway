package am.ik.k8s.config;

import am.ik.cloud.gateway.locator.KubernetesRouteDefinitionLocator;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class KubernetesConfig {

	@Bean
	public KubernetesClient kubernetesClient() {
		Config config = new ConfigBuilder().build();
		return new DefaultKubernetesClient(config);
	}

	@Bean
	public RouteDefinitionLocator kubernetesRouteDefinitionLocator(KubernetesClient kubernetesClient,
			ApplicationEventPublisher eventPublisher) {
		return new KubernetesRouteDefinitionLocator(kubernetesClient, eventPublisher);
	}
}
