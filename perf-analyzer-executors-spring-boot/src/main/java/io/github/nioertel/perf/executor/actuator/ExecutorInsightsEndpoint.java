package io.github.nioertel.perf.executor.actuator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import io.github.nioertel.perf.executor.ActiveTaskSnapshot;
import io.github.nioertel.perf.executor.ExecutionMetrics;

@Endpoint(id = "executorInsights")
public class ExecutorInsightsEndpoint {

	private final Map<String, ExecutionMetrics> executionMetricsByProvider;

	ExecutorInsightsEndpoint(List<ExecutionMetrics> executionMetrics) {
		this.executionMetricsByProvider = executionMetrics.stream().collect(Collectors.toMap(//
				m -> m.getName(), //
				Function.identity()//
		));
	}

	@ReadOperation
	public ExecutionMetricsOverall executionMetrics() {
		return ExecutionMetricsOverall.fromExecutionMetricsByProvider(executionMetricsByProvider);
	}

	public static class ExecutionMetricsOverall {

		private final Map<String, ExecutionMetricsForProvider> executionMetricsByProvider;

		private ExecutionMetricsOverall(Map<String, ExecutionMetricsForProvider> executionMetricsByProvider) {
			this.executionMetricsByProvider = new HashMap<>(executionMetricsByProvider);
		}

		public static ExecutionMetricsOverall fromExecutionMetricsByProvider(Map<String, ExecutionMetrics> executionMetricsByProvider) {
			return new ExecutionMetricsOverall(//
					executionMetricsByProvider.entrySet().stream()//
							.map(entry -> ExecutionMetricsForProvider.fromExecutionMetrics(//
									entry.getKey(), //
									entry.getValue().getActiveTasks()))//
							.collect(Collectors.toMap(e -> e.getProviderName(), e -> e)));
		}

		public Map<String, ExecutionMetricsForProvider> getExecutionMetricsByProvider() {
			return Collections.unmodifiableMap(executionMetricsByProvider);
		}

	}

	public static class ExecutionMetricsForProvider {

		private final String providerName;

		private final List<ActiveTaskSnapshot> activeTasks;

		private ExecutionMetricsForProvider(String providerName, List<ActiveTaskSnapshot> activeTasks) {
			this.providerName = providerName;
			this.activeTasks = new ArrayList<>(activeTasks);
		}

		public static ExecutionMetricsForProvider fromExecutionMetrics(String providerName, List<ActiveTaskSnapshot> activeTasks) {
			return new ExecutionMetricsForProvider(//
					providerName, //
					activeTasks);
		}

		public String getProviderName() {
			return providerName;
		}

		public List<ActiveTaskSnapshot> getActiveTasks() {
			return Collections.unmodifiableList(activeTasks);
		}

	}

}
