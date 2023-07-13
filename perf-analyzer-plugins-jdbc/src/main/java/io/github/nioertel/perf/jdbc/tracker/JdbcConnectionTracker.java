package io.github.nioertel.perf.jdbc.tracker;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import io.github.nioertel.perf.jdbc.utils.ThrowingSupplier;

public class JdbcConnectionTracker {

	private final JdbcConnectionMetricsTracker jdbcConnectionMetrics;

	private final JdbcConnectionMetricsHousekeeper jdbcConnectionMetricsHousekeeper;

	private final String poolName;

	private final AtomicLong connectionInstanceIdGenerator = new AtomicLong();

	public JdbcConnectionTracker(String poolName) {
		this.poolName = poolName;
		this.jdbcConnectionMetrics = new JdbcConnectionMetricsTracker(poolName);
		this.jdbcConnectionMetricsHousekeeper = new JdbcConnectionMetricsHousekeeper();
		jdbcConnectionMetricsHousekeeper.start(jdbcConnectionMetrics, 30L, TimeUnit.SECONDS);
	}

	public String getPoolName() {
		return poolName;
	}

	public JdbcConnectionMetrics getConnectionMetrics() {
		return jdbcConnectionMetrics;
	}

	public IdentifiableConnection getConnection(ThrowingSupplier<Connection, SQLException> connectionSupplier) throws SQLException {
		long connectionInstanceId = connectionInstanceIdGenerator.incrementAndGet();
		jdbcConnectionMetrics.prepareForBinding(connectionInstanceId);

		Connection delegate;
		try {
			delegate = connectionSupplier.get();
		} catch (Exception e) {
			jdbcConnectionMetrics.release(connectionInstanceId);
			throw e;
		}

		IdentifiableConnection connectionProxy = (IdentifiableConnection) Proxy.newProxyInstance(//
				IdentifiableConnection.class.getClassLoader(), //
				new Class[] { IdentifiableConnection.class }, //
				new JdbcConnectionInvocationHandler(//
						jdbcConnectionMetrics, //
						delegate, //
						connectionInstanceId));
		jdbcConnectionMetrics.bindAcquiredConnection(connectionProxy);
		return connectionProxy;
	}

}
