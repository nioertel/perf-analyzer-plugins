package io.github.nioertel.perf.executor.actuator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.github.nioertel.perf.executor.ActiveTaskSnapshot;
import io.github.nioertel.perf.executor.ExecutionMetrics;
import io.github.nioertel.perf.executor.ExecutorStateInfoExtractor;

@Endpoint(id = "executorInsights")
public class ExecutorInsightsEndpoint {

	private final Map<String, ExecutionMetrics> executionMetricsByProvider;

	private final Map<String, ExecutorStateInfoExtractor> executorStateInfosByProvider;

	ExecutorInsightsEndpoint(List<ExecutorStateInfoExtractor> executorStateInfos, List<ExecutionMetrics> executionMetrics) {
		this.executionMetricsByProvider = executionMetrics.stream().collect(Collectors.toMap(//
				m -> m.getName(), //
				Function.identity()//
		));
		this.executorStateInfosByProvider = executorStateInfos.stream().collect(Collectors.toMap(//
				m -> m.getName(), //
				Function.identity()//
		));
	}

	@ReadOperation
	public ExecutionMetricsOverall executionMetrics() {
		return ExecutionMetricsOverall.fromExecutionMetricsByProvider(executorStateInfosByProvider, executionMetricsByProvider);
	}

	public static class ExecutionMetricsOverall {

		private final Map<String, ExecutionMetricsForProvider> executionMetricsByProvider;

		@JsonCreator
		private ExecutionMetricsOverall(//
				@JsonProperty("executionMetricsByProvider") Map<String, ExecutionMetricsForProvider> executionMetricsByProvider) {
			this.executionMetricsByProvider = new HashMap<>(executionMetricsByProvider);
		}

		public static ExecutionMetricsOverall fromExecutionMetricsByProvider(Map<String, ExecutorStateInfoExtractor> executorStateInfosByProvider,
				Map<String, ExecutionMetrics> executionMetricsByProvider) {
			return new ExecutionMetricsOverall(//
					executionMetricsByProvider.entrySet().stream()//
							.map(entry -> ExecutionMetricsForProvider.fromExecutionMetrics(//
									entry.getKey(), //
									executorStateInfosByProvider.get(entry.getKey()), //
									entry.getValue().getActiveTasks()))//
							.collect(Collectors.toMap(e -> e.getProviderName(), e -> e)));
		}

		public Map<String, ExecutionMetricsForProvider> getExecutionMetricsByProvider() {
			return Collections.unmodifiableMap(executionMetricsByProvider);
		}

	}

	public static class ExecutionMetricsForProvider {

		private final String providerName;

		private final Map<String, Object> executorState;

		private final List<ActiveTaskSnapshot> activeTasks;

		@JsonCreator
		private ExecutionMetricsForProvider(//
				@JsonProperty("providerName") String providerName, //
				@JsonProperty("executorState") Map<String, Object> executorState, //
				@JsonProperty("activeTasks") List<ActiveTaskSnapshot> activeTasks) {
			this.providerName = providerName;
			this.executorState = new LinkedHashMap<>(executorState);
			this.activeTasks = new ArrayList<>(activeTasks);
		}

		public static ExecutionMetricsForProvider fromExecutionMetrics(String providerName, ExecutorStateInfoExtractor executorStateExtractor,
				List<ActiveTaskSnapshot> activeTasks) {
			Map<String, Object> executorState;
			if (null == executorStateExtractor) {
				executorState = null;
			} else {
				executorState = executorStateExtractor.getExecutorStateInfo();
			}
			return new ExecutionMetricsForProvider(//
					providerName, //
					executorState, //
					activeTasks);
		}

		public String getProviderName() {
			return providerName;
		}

		public Map<String, Object> getExecutorState() {
			return executorState;
		}

		public List<ActiveTaskSnapshot> getActiveTasks() {
			return Collections.unmodifiableList(activeTasks);
		}

	}

}
