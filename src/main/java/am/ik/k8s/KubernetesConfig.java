package am.ik.k8s;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KubernetesConfig {

	@Bean(destroyMethod = "close")
	public KubernetesClient kubernetesClient() {
		Config config = new ConfigBuilder().build();
		return new DefaultKubernetesClient(config);
	}
}
