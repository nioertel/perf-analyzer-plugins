package io.github.nioertel.perf.jdbc.actuator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.nioertel.perf.jdbc.tracker.JdbcConnectionMetrics;

public class JdbcConnectionMetricsSupplier {

	private final List<JdbcConnectionMetrics> connectionMetrics;

	public JdbcConnectionMetricsSupplier(List<JdbcConnectionMetrics> connectionMetrics) {
		this.connectionMetrics = new ArrayList<>(connectionMetrics);
	}

	public List<JdbcConnectionMetrics> getConnectionMetrics() {
		return Collections.unmodifiableList(connectionMetrics);
	}

}
