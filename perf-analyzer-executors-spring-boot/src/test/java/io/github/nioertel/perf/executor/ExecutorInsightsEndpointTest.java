package io.github.nioertel.perf.executor;

import java.util.Map;

import org.assertj.core.api.BDDAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.jayway.jsonpath.JsonPath;

import io.github.nioertel.perf.executor.actuator.ExecutorInsightsEndpoint;
import io.github.nioertel.perf.executor.spring.ExecutionTrackingTaskDecoratorSpring;
import io.github.nioertel.perf.executor.test.TestApp;

@SpringBootTest(//
		classes = TestApp.class, //
		webEnvironment = WebEnvironment.RANDOM_PORT, //
		properties = { //
				"logging.level.*=DEBUG", //
				"management.endpoints.web.exposure.include=*", //
				"spring.jackson.serialization.indent_output=true" //
		})
@Import(ExecutorInsightsEndpointTest.Beans.class)
class ExecutorInsightsEndpointTest {

	static class Beans {

		@Bean
		static ExecutionTrackingTaskDecoratorSpring executionTrackingTaskDecoratorSpring() {
			return new ExecutionTrackingTaskDecoratorSpring("test-1");
		}

		@Bean
		static TaskExecutor executorService(ExecutionTrackingTaskDecoratorSpring executionTrackingTaskDecoratorSpring) {
			ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
			executor.setTaskDecorator(executionTrackingTaskDecoratorSpring);
			executor.initialize();
			return executor;
		}
	}

	@Autowired
	private ExecutorInsightsEndpoint executorInsightsEndpoint;

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void actuatorShouldBePresent() {
		ResponseEntity<String> health = restTemplate.getForEntity("/actuator/health", String.class, Map.of());
		BDDAssertions.assertThat(health.getStatusCode()).isSameAs(HttpStatus.OK);
		BDDAssertions.assertThat(JsonPath.<String>read(health.getBody(), "status")).isEqualTo("UP");
	}

	@Test
	void actuatorShouldPublishjdbcMetricsEndpoint() {
		ResponseEntity<String> health = restTemplate.getForEntity("/actuator/executorInsights", String.class, Map.of());
		BDDAssertions.assertThat(health.getStatusCode()).isSameAs(HttpStatus.OK);
		System.out.println(health.getBody());
		BDDAssertions.assertThat((Map<?, ?>) JsonPath.parse(health.getBody()).json()).allSatisfy((k, v) -> {
			BDDAssertions.assertThat(k).isEqualTo("executionMetricsByProvider");
		});
	}

	// TODO: Add the real tests
}
