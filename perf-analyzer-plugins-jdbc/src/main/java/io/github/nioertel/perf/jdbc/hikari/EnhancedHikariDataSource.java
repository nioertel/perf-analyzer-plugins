package io.github.nioertel.perf.jdbc.hikari;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import io.github.nioertel.perf.jdbc.tracker.IdentifiableConnection;
import io.github.nioertel.perf.jdbc.tracker.JdbcConnectionMetrics;
import io.github.nioertel.perf.jdbc.tracker.JdbcConnectionTracker;

public class EnhancedHikariDataSource extends HikariDataSource {

	private static final Logger LOGGER = LoggerFactory.getLogger(EnhancedHikariDataSource.class);

	private final JdbcConnectionTracker jdbcConnectionTracker;

	public EnhancedHikariDataSource() {
		LOGGER.info("Creating data source [n/a].");
		this.jdbcConnectionTracker = new JdbcConnectionTracker("n/a");
	}

	public EnhancedHikariDataSource(HikariConfig config) {
		super(config);
		String poolName = config.getPoolName();
		if (null == poolName) {
			poolName = "n/a";
		}
		LOGGER.info("Creating data source [{}].", poolName);
		this.jdbcConnectionTracker = new JdbcConnectionTracker(poolName);
	}

	public JdbcConnectionMetrics getConnectionMetrics() {
		return jdbcConnectionTracker.getConnectionMetrics();
	}

	@Override
	public IdentifiableConnection getConnection() throws SQLException {
		return jdbcConnectionTracker.getConnection(super::getConnection);
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setPoolName(String name) {
		super.setPoolName(name);
		jdbcConnectionTracker.setPoolName(name);
	}
}
