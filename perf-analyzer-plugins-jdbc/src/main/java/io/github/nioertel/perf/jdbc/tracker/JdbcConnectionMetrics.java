package io.github.nioertel.perf.jdbc.tracker;

import java.util.List;

public interface JdbcConnectionMetrics {

	static JdbcConnectionMetrics empty(String poolName) {
		return new JdbcConnectionMetrics() {

			@Override
			public String getPoolName() {
				return poolName;
			}

			@Override
			public long getInvocationCount(String methodName) {
				return 0L;
			}

			@Override
			public List<JdbcConnectionStatsSnapshot> getActiveConnections() {
				return List.of();
			}
		};
	}

	long getInvocationCount(String methodName);

	List<JdbcConnectionStatsSnapshot> getActiveConnections();

	String getPoolName();
}