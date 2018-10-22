package am.ik.k8s;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringCloudGatewayKubernetesApplication {

	public static void main(String[] args) {
		System.setProperty("reactor.netty.http.server.accessLogEnabled", "true");
		SpringApplication.run(SpringCloudGatewayKubernetesApplication.class, args);
	}
}
