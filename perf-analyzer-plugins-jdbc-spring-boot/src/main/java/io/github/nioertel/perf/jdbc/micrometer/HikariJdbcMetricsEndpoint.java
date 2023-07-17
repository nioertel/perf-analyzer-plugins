package io.github.nioertel.perf.jdbc.micrometer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.lang.Nullable;

import io.github.nioertel.perf.jdbc.hikari.EnhancedHikariDataSource;
import io.github.nioertel.perf.jdbc.tracker.JdbcConnectionMetrics;
import io.github.nioertel.perf.jdbc.tracker.JdbcConnectionStatsSnapshot;
import io.github.nioertel.perf.utils.DateHelper;

@Endpoint(id = "hikariJdbcMetrics")
public class HikariJdbcMetricsEndpoint {

	private final Map<String, JdbcConnectionMetrics> connectionMetricsByPool;

	HikariJdbcMetricsEndpoint(List<EnhancedHikariDataSource> hikariDataSources) {
		this.connectionMetricsByPool = hikariDataSources.stream().collect(Collectors.toMap(//
				ds -> ds.getPoolName(), //
				ds -> ds.getConnectionMetrics()//
		));
	}

	private static boolean shouldIncludeStacktraces(Boolean includeStacktraces) {
		if (null == includeStacktraces) {
			return false;
		} else {
			return includeStacktraces.booleanValue();
		}
	}

	public void registerAdditionalDatasSource(EnhancedHikariDataSource dataSource) {
		connectionMetricsByPool.put(dataSource.getPoolName(), dataSource.getConnectionMetrics());
	}

	@ReadOperation
	public JdbcConnectionStatsDescriptors hikariJdbcMetrics(@Nullable Boolean includeStacktraces) {
		return hikariJdbcMetrics(null, null, includeStacktraces);
	}

	@ReadOperation
	public JdbcConnectionStatsDescriptors hikariJdbcMetrics(@Selector @Nullable String poolName, Boolean includeStacktraces) {
		return hikariJdbcMetrics(poolName, null, includeStacktraces);
	}

	@ReadOperation
	public JdbcConnectionStatsDescriptors hikariJdbcMetrics(@Selector @Nullable String poolName, @Selector @Nullable Long threadId,
			Boolean includeStacktraces) {
		JdbcConnectionStatsDescriptors jdbcConnectionStatsDescriptors;
		if (null == poolName) {
			jdbcConnectionStatsDescriptors = JdbcConnectionStatsDescriptors.fromConnectionStats(//
					connectionMetricsByPool, //
					shouldIncludeStacktraces(includeStacktraces));
		} else {
			jdbcConnectionStatsDescriptors = JdbcConnectionStatsDescriptors.fromConnectionStats(//
					Map.of(poolName, connectionMetricsByPool.getOrDefault(poolName, JdbcConnectionMetrics.empty(poolName))), //
					shouldIncludeStacktraces(includeStacktraces));
		}

		if (null == threadId) {
			return jdbcConnectionStatsDescriptors;
		} else {
			return jdbcConnectionStatsDescriptors.filterForThread(threadId);
		}
	}

	public static class JdbcConnectionStatsDescriptors {

		private final Map<String, JdbcConnectionStatsDescriptor> connectionStatsByPool;

		private JdbcConnectionStatsDescriptors(Map<String, JdbcConnectionStatsDescriptor> connectionStatsByPool) {
			this.connectionStatsByPool = new HashMap<>(connectionStatsByPool);
		}

		public static JdbcConnectionStatsDescriptors fromConnectionStats(Map<String, JdbcConnectionMetrics> connectionMetricsByPool,
				boolean includeStackTraces) {
			return new JdbcConnectionStatsDescriptors(//
					connectionMetricsByPool.entrySet().stream()//
							.map(entry -> JdbcConnectionStatsDescriptor.fromConnectionStats(//
									entry.getKey(), //
									entry.getValue().getActiveConnections(), //
									includeStackTraces))//
							.collect(Collectors.toMap(e -> e.getPoolName(), e -> e)));
		}

		public JdbcConnectionStatsDescriptors filterForThread(long threadId) {
			return new JdbcConnectionStatsDescriptors(connectionStatsByPool.entrySet().stream().collect(Collectors.toMap(//
					e -> e.getKey(), //
					e -> e.getValue().filterForThread(threadId)//
			)));
		}

		public Map<String, JdbcConnectionStatsDescriptor> getConnectionStatsByPool() {
			return Collections.unmodifiableMap(connectionStatsByPool);
		}

	}

	public static class JdbcConnectionStatsDescriptor {

		private final String poolName;

		private final List<ActiveJdbcConnectionInsights> activeConnections;

		private final List<PendingJdbcConnectionInsights> pendingConnections;

		private JdbcConnectionStatsDescriptor(String poolName, List<ActiveJdbcConnectionInsights> activeConnections,
				List<PendingJdbcConnectionInsights> pendingConnections) {
			this.poolName = poolName;
			this.activeConnections = new ArrayList<>(activeConnections);
			this.pendingConnections = new ArrayList<>(pendingConnections);
		}

		public static JdbcConnectionStatsDescriptor fromConnectionStats(String poolName, List<JdbcConnectionStatsSnapshot> connectionStats,
				boolean includeStackTraces) {
			// TODO: include stack trace infos if requested
			return new JdbcConnectionStatsDescriptor(//
					poolName, //
					connectionStats.stream()//
							.filter(c -> 0 != c.getAcquiredTimestamp())//
							.map(ActiveJdbcConnectionInsights::fromConnectionStats)//
							.collect(Collectors.toList()), //
					connectionStats.stream()//
							.filter(c -> 0 == c.getAcquiredTimestamp())//
							.map(PendingJdbcConnectionInsights::fromConnectionStats)//
							.collect(Collectors.toList()));
		}

		public JdbcConnectionStatsDescriptor filterForThread(long threadId) {
			return new JdbcConnectionStatsDescriptor(//
					poolName, //
					activeConnections.stream().filter(c -> c.getBindingThreadId() == threadId).collect(Collectors.toList()), //
					pendingConnections.stream().filter(c -> c.getBindingThreadId() == threadId).collect(Collectors.toList()));
		}

		public String getPoolName() {
			return poolName;
		}

		public List<ActiveJdbcConnectionInsights> getActiveConnections() {
			return Collections.unmodifiableList(activeConnections);
		}

		public List<PendingJdbcConnectionInsights> getPendingConnections() {
			return Collections.unmodifiableList(pendingConnections);
		}

	}

	public static class PendingJdbcConnectionInsights {

		private final long bindingThreadId;

		private final String bindingThreadName;

		private final LocalDateTime acquireStart;

		public PendingJdbcConnectionInsights(long bindingThreadId, String bindingThreadName, LocalDateTime acquireStart) {
			this.bindingThreadId = bindingThreadId;
			this.bindingThreadName = bindingThreadName;
			this.acquireStart = acquireStart;
		}

		public static PendingJdbcConnectionInsights fromConnectionStats(JdbcConnectionStatsSnapshot connectionStats) {
			return new PendingJdbcConnectionInsights(//
					connectionStats.getBindingThreadId(), //
					connectionStats.getBindingThreadName(), //
					DateHelper.toLocalDateTime(connectionStats.getAcquireStartTimestamp()));
		}

		public long getBindingThreadId() {
			return bindingThreadId;
		}

		public String getBindingThreadName() {
			return bindingThreadName;
		}

		public LocalDateTime getAcquireStart() {
			return acquireStart;
		}

	}

	public static class ActiveJdbcConnectionInsights {

		private final long connectionInstanceIdentifier;

		private final long bindingThreadId;

		private final String bindingThreadName;

		private final LocalDateTime acquireStart;

		private final LocalDateTime acquired;

		private final long activeSeconds;

		public ActiveJdbcConnectionInsights(long connectionInstanceIdentifier, long bindingThreadId, String bindingThreadName,
				LocalDateTime acquireStart, LocalDateTime acquired, long activeSeconds) {
			this.connectionInstanceIdentifier = connectionInstanceIdentifier;
			this.bindingThreadId = bindingThreadId;
			this.bindingThreadName = bindingThreadName;
			this.acquireStart = acquireStart;
			this.acquired = acquired;
			this.activeSeconds = activeSeconds;
		}

		public static ActiveJdbcConnectionInsights fromConnectionStats(JdbcConnectionStatsSnapshot connectionStats) {
			return new ActiveJdbcConnectionInsights(//
					connectionStats.getConnectionInstanceIdentifier(), //
					connectionStats.getBindingThreadId(), //
					connectionStats.getBindingThreadName(), //
					DateHelper.toLocalDateTime(connectionStats.getAcquireStartTimestamp()), //
					DateHelper.toLocalDateTime(connectionStats.getAcquiredTimestamp()), //
					(System.currentTimeMillis() - connectionStats.getAcquiredTimestamp()) / 1_000L);
		}

		public long getConnectionInstanceIdentifier() {
			return connectionInstanceIdentifier;
		}

		public long getBindingThreadId() {
			return bindingThreadId;
		}

		public String getBindingThreadName() {
			return bindingThreadName;
		}

		public LocalDateTime getAcquireStart() {
			return acquireStart;
		}

		public LocalDateTime getAcquired() {
			return acquired;
		}

		public long getActiveSeconds() {
			return activeSeconds;
		}

	}

}
