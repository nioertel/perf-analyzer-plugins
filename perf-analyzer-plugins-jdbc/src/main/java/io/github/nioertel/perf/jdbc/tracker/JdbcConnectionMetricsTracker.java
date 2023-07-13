package io.github.nioertel.perf.jdbc.tracker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class JdbcConnectionMetricsTracker implements JdbcConnectionMetrics {

	private static final Logger LOGGER = LoggerFactory.getLogger(JdbcConnectionMetricsTracker.class);

	private final String poolName;

	private final Map<String, AtomicLong> methodInvocationCounter = new ConcurrentHashMap<>();

	// TODO: add housekeeping for outdated entries (in case we have a bug)
	private final Map<Long, JdbcConnectionStats> activeConnections = new ConcurrentHashMap<>();

	JdbcConnectionMetricsTracker(String poolName) {
		this.poolName = poolName;
	}

	void prepareForBinding(long connectionInstanceId) {
		Thread currentThread = Thread.currentThread();
		JdbcConnectionStats stats = new JdbcConnectionStats(//
				currentThread.getId(), //
				currentThread.getName(), //
				System.currentTimeMillis());
		activeConnections.put(connectionInstanceId, stats);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Going to acquire connection {} ({}).", connectionInstanceId, stats);
		}
	}

	void release(long connectionInstanceId) {
		JdbcConnectionStats stats = activeConnections.get(connectionInstanceId);
		if (null == stats) {
			LOGGER.warn("Not tracking connection {} as binding was not prepared.", connectionInstanceId);
			return;
		}
		stats.setReleasedTimestamp(System.currentTimeMillis());
		activeConnections.remove(connectionInstanceId);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Released connection {} ({}).", connectionInstanceId, stats.getDetails());
		}
	}

	void bindAcquiredConnection(IdentifiableConnection connection) {
		long connectionIdentifier = connection.getIdentifier();
		JdbcConnectionStats stats = activeConnections.get(connectionIdentifier);
		if (null == stats) {
			LOGGER.warn("Not tracking connection {} ({}) as binding was not prepared.", connectionIdentifier, connection);
			return;
		}
		stats.setAcquiredTimestamp(System.currentTimeMillis());
		stats.setConnection(connection);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Bound connection {} ({}).", connectionIdentifier, stats);
		}
	}

	void connectionReleaseStart(IdentifiableConnection connection) {
		long connectionIdentifier = connection.getIdentifier();
		JdbcConnectionStats stats = activeConnections.get(connectionIdentifier);
		if (null == stats) {
			LOGGER.warn("Not tracking connection {} ({}) as binding was not prepared.", connectionIdentifier, connection);
			return;
		}
		stats.setReleaseStartTimestamp(System.currentTimeMillis());
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Going to release connection {} ({}).", connectionIdentifier, stats);
		}
	}

	void connectionReleased(IdentifiableConnection connection) {
		release(connection.getIdentifier());
	}

	void connectionReleaseFailed(IdentifiableConnection connection) {
		release(connection.getIdentifier());
	}

	void updateInvocationCounter(String methodIdentifier) {
		methodInvocationCounter.computeIfAbsent(methodIdentifier, m -> new AtomicLong()).incrementAndGet();
	}

	/**
	 * <li>Remove connection stats that have no connection assigned (i.e. week reference is empty)</li>
	 * <li>Remove connection stats for which there is another connection stat that references the same physical connection
	 * and has a later acquired timestamp</li>
	 * <li>Remove all connections if the tracker has a size > 10k (and log some relevant details to identify what
	 * happened)</li>
	 * </ul>
	 */
	void cleanup() {
		
	}

	@Override
	public String getPoolName() {
		return poolName;
	}

	@Override
	public long getInvocationCount(String methodName) {
		AtomicLong count = methodInvocationCounter.get(methodName);
		if (null == count) {
			return 0L;
		} else {
			return count.longValue();
		}
	}

	@Override
	public List<JdbcConnectionStatsSnapshot> getActiveConnections() {
		List<JdbcConnectionStatsSnapshot> result = new ArrayList<>();
		for (JdbcConnectionStats stats : activeConnections.values()) {
			result.add(stats.snapshot());
		}
		return result;
	}

}