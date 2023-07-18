package io.github.nioertel.perf.executor.actuator;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

import io.github.nioertel.perf.executor.ExecutionMetrics;

@ConditionalOnWebApplication
@ConditionalOnClass({ ThreadPoolExecutor.class })
public class ExecutionTrackingActuatorEndpointsAutoConfiguration {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionTrackingActuatorEndpointsAutoConfiguration.class);

	@ConditionalOnBean({ ExecutionMetrics.class })
	@Bean
	ExecutorInsightsEndpoint executorInsightsEndpoint(List<ExecutionMetrics> executionMetrics) {
		LOGGER.info("Creating executor insights actuator endpoint based on {} execution metrics providers.", executionMetrics.size());
		return new ExecutorInsightsEndpoint(executionMetrics);
	}
}
