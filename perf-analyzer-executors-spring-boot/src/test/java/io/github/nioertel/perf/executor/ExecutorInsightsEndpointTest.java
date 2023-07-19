package io.github.nioertel.perf.executor;

import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

import io.github.nioertel.perf.executor.actuator.ExecutorInsightsEndpoint;
import io.github.nioertel.perf.executor.actuator.ExecutorInsightsEndpoint.ExecutionMetricsOverall;
import io.github.nioertel.perf.executor.spring.ExecutionTrackingTaskDecoratorSpring;
import io.github.nioertel.perf.executor.spring.ThreadPoolTaskExecutorStateInfoExtractor;
import io.github.nioertel.perf.executor.test.TestApp;
import io.github.nioertel.perf.utils.ThrowingSupplier;

@SpringBootTest(//
		classes = TestApp.class, //
		webEnvironment = WebEnvironment.RANDOM_PORT, //
		properties = { //
				"logging.level.io=DEBUG", //
				"management.endpoints.web.exposure.include=*", //
				"spring.jackson.serialization.indent_output=true" //
		})
@Import(ExecutorInsightsEndpointTest.Beans.class)
class ExecutorInsightsEndpointTest {

	static class Beans {

		private static final String TASK_EXECUTOR_NAME = "test-1";

		@Bean
		ExecutionTrackingTaskDecoratorSpring executionTrackingTaskDecoratorSpring() {
			return new ExecutionTrackingTaskDecoratorSpring(TASK_EXECUTOR_NAME);
		}

		@Bean
		TaskExecutor executorService(ExecutionTrackingTaskDecoratorSpring executionTrackingTaskDecoratorSpring) {
			ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
			executor.setThreadNamePrefix(TASK_EXECUTOR_NAME);
			executor.setTaskDecorator(executionTrackingTaskDecoratorSpring);
			executor.initialize();
			executor.setAwaitTerminationSeconds(30);
			executor.setWaitForTasksToCompleteOnShutdown(true);
			return executor;
		}

		@Bean
		ExecutorStateInfoExtractor executorStateInfoExtractor(ThreadPoolTaskExecutor executorService) {
			return new ThreadPoolTaskExecutorStateInfoExtractor(TASK_EXECUTOR_NAME, executorService);
		}
	}

	public static class TestRunnable implements Runnable {

		private AtomicBoolean coreLogicFinished = new AtomicBoolean();

		private Semaphore canFinishExecution = new Semaphore(0);

		private int result;

		private void doSomethingExpensive() {
			int hash = 0;
			for (int i = 0; i < 10_000; ++i) {
				hash += ("run-" + i).hashCode();
			}
			this.result = hash;
			try {
				TimeUnit.MILLISECONDS.sleep(10L);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		@Override
		public synchronized void run() {
			doSomethingExpensive();
			coreLogicFinished.set(true);

			try {
				canFinishExecution.acquire();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} finally {
				coreLogicFinished.set(false);
			}
		}

		public int getResult() {
			return result;
		}

		public void continueRunning() {
			canFinishExecution.release();
		}

		public void reset() {
			continueRunning();
			blockUntilExecutionFinished(10L, TimeUnit.SECONDS);
		}

		public boolean blockUntilCoreLogicFinished(long timeout, TimeUnit unit) {
			long latestEndOfBlocking = System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(timeout, unit);
			while (latestEndOfBlocking > System.currentTimeMillis()) {
				if (coreLogicFinished.get()) {
					return true;
				} else {
					try {
						TimeUnit.MILLISECONDS.sleep(Math.min(latestEndOfBlocking - System.currentTimeMillis(), 10L));
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						return coreLogicFinished.get();
					}
				}
			}
			return false;
		}

		public boolean blockUntilExecutionFinished(long timeout, TimeUnit unit) {
			long latestEndOfBlocking = System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(timeout, unit);
			while (latestEndOfBlocking > System.currentTimeMillis()) {
				if (!coreLogicFinished.get()) {
					return true;
				} else {
					try {
						TimeUnit.MILLISECONDS.sleep(Math.min(latestEndOfBlocking - System.currentTimeMillis(), 10L));
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						return !coreLogicFinished.get();
					}
				}
			}
			return false;
		}
	}

	@Autowired
	private ExecutorInsightsEndpoint executorInsightsEndpoint;

	@Autowired
	private ObjectMapper om;

	@Autowired
	private ThreadPoolTaskExecutor executorService;

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
		BDDAssertions.assertThat((Map<?, ?>) JsonPath.parse(health.getBody()).json()).allSatisfy((k, v) -> {
			BDDAssertions.assertThat(k).isEqualTo("executionMetricsByProvider");
		});
	}

	@Test
	void runningTaskShouldAppearInMetricsViaActuator() throws JsonProcessingException {
		runningTaskShouldAppearInMetrics(() -> {
			ResponseEntity<String> health = restTemplate.getForEntity("/actuator/executorInsights", String.class, Map.of());
			BDDAssertions.assertThat(health.getStatusCode()).isSameAs(HttpStatus.OK);
			System.out.println(health.getBody());
			return om.readValue(health.getBody(), ExecutionMetricsOverall.class);
		});
	}

	@Test
	void runningTaskShouldAppearInMetricsViaEndpointBean() throws JsonProcessingException {
		runningTaskShouldAppearInMetrics(() -> {
			return executorInsightsEndpoint.executionMetrics();
		});
	}

	private void runningTaskShouldAppearInMetrics(ThrowingSupplier<ExecutionMetricsOverall, JsonProcessingException> executionMetricsRetriever)
			throws JsonProcessingException {
		TestRunnable task = new TestRunnable();
		try {
			runningTaskShouldAppearInMetrics(task, executionMetricsRetriever);
		} finally {
			task.reset();
		}
	}

	private void runningTaskShouldAppearInMetrics(TestRunnable task,
			ThrowingSupplier<ExecutionMetricsOverall, JsonProcessingException> executionMetricsRetriever) throws JsonProcessingException {
		executorService.submit(task);
		BDDAssertions.assertThat(task.blockUntilCoreLogicFinished(30, TimeUnit.SECONDS))
				.describedAs("Expecting task to start within 30 seconds. Issue with test setup.").isTrue();

		ExecutionMetricsOverall executionMetricsOverall = executionMetricsRetriever.get();
		BDDAssertions.assertThat(executionMetricsOverall.getExecutionMetricsByProvider())//
				.hasSize(1)//
				.anySatisfy((k, v) -> {
					BDDAssertions.assertThat(k).isEqualTo(Beans.TASK_EXECUTOR_NAME);
					BDDAssertions.assertThat(v.getProviderName()).isEqualTo(Beans.TASK_EXECUTOR_NAME);
					BDDAssertions.assertThat((Map<?, ?>) v.getExecutorState())//
							.hasSize(7)//
							.anySatisfy((infoKey, infoValue) -> {
								BDDAssertions.assertThat(infoKey).isEqualTo("activeCount");
								BDDAssertions.assertThat(infoValue).isEqualTo(1);
							});
					BDDAssertions.assertThat(v.getActiveTasks())//
							.hasSize(1)//
							.allSatisfy(activeTask -> {
								BDDAssertions.assertThat(activeTask.getTaskName()).isEqualTo(TestRunnable.class.getName());
								BDDAssertions.assertThat(activeTask.getState()).isSameAs(ActiveTaskStats.State.RUNNING);
								BDDAssertions.assertThat(activeTask.getConsumedCpuTimeMillis()).isGreaterThan(0L);
							});
				});

		task.continueRunning();
		BDDAssertions.assertThat(task.blockUntilExecutionFinished(30, TimeUnit.SECONDS))
				.describedAs("Expecting task to start within 30 seconds. Issue with test setup.").isTrue();

		executionMetricsOverall = executionMetricsRetriever.get();
		BDDAssertions.assertThat(executionMetricsOverall.getExecutionMetricsByProvider())//
				.hasSize(1)//
				.anySatisfy((k, v) -> {
					BDDAssertions.assertThat(k).isEqualTo(Beans.TASK_EXECUTOR_NAME);
					BDDAssertions.assertThat(v.getProviderName()).isEqualTo(Beans.TASK_EXECUTOR_NAME);
					BDDAssertions.assertThat((Map<?, ?>) v.getExecutorState())//
							.hasSize(7)//
							.anySatisfy((infoKey, infoValue) -> {
								BDDAssertions.assertThat(infoKey).isEqualTo("activeCount");
								BDDAssertions.assertThat(infoValue).isEqualTo(0);
							});
					BDDAssertions.assertThat(v.getActiveTasks()).isEmpty();
				});
	}

}
