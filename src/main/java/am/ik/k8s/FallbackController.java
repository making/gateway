package am.ik.k8s;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpStatus.*;

@RestController
public class FallbackController {
	@RequestMapping("/error/404")
	public ResponseEntity error404() {
		return ResponseEntity.status(NOT_FOUND)
				.body(new ClassPathResource("static/error/404.html"));
	}

	@RequestMapping("/error/500")
	public ResponseEntity error500() {
		return ResponseEntity.status(INTERNAL_SERVER_ERROR)
				.body(new ClassPathResource("static/error/500.html"));
	}

	@RequestMapping("/error/503")
	public ResponseEntity error503() {
		return ResponseEntity.status(SERVICE_UNAVAILABLE)
				.body(new ClassPathResource("static/error/503.html"));
	}

	@RequestMapping("/error/504")
	public ResponseEntity error504() {
		return ResponseEntity.status(GATEWAY_TIMEOUT)
				.body(new ClassPathResource("static/error/504.html"));
	}
}
