package io.github.nioertel.perf.jdbc.hikari;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.assertj.core.api.BDDAssertions;
import org.junit.jupiter.api.Test;

import com.zaxxer.hikari.HikariConfig;

import io.github.nioertel.perf.jdbc.tracker.IdentifiableConnection;
import io.github.nioertel.perf.jdbc.tracker.JdbcConnectionMetrics;
import io.github.nioertel.perf.jdbc.tracker.JdbcConnectionStatsSnapshot;

class EnhancedHikariDataSourceTest {

	static {
		if (null == System.getProperty("org.slf4j.simpleLogger.defaultLogLevel")) {
			System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
		}
	}

	private static final String CONNECTION_CLOSE = "java.sql.Connection.close()";

	@Test
	void testCreateConnection() throws SQLException {
		HikariConfig config = new HikariConfig();
		config.setDriverClassName("org.h2.Driver");
		config.setConnectionTestQuery("SELECT 1");
		config.setJdbcUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false;MODE=MSSQLServer");
		config.setUsername("sa");
		config.setPassword("sa");
		config.setMinimumIdle(1);
		config.setPoolName("test-1");
		config.setMaximumPoolSize(1);

		JdbcConnectionMetrics connectionMetrics = null;
		try (EnhancedHikariDataSource dataSource = new EnhancedHikariDataSource(config)) {
			connectionMetrics = dataSource.getConnectionMetrics();
			BDDAssertions.assertThat(connectionMetrics.getPoolName()).isEqualTo("test-1");
			BDDAssertions.assertThat(connectionMetrics.getInvocationCount(CONNECTION_CLOSE)).isEqualTo(0L);
			try (IdentifiableConnection c = (IdentifiableConnection) dataSource.getConnection()) {
				try (Statement s = c.createStatement()) {
					BDDAssertions.assertThat(s.execute("SELECT 5")).isTrue();
					ResultSet r = s.getResultSet();
					BDDAssertions.assertThat(r.next()).isTrue();
					BDDAssertions.assertThat(r.getInt(1)).isEqualTo(5);
				}

				List<JdbcConnectionStatsSnapshot> statsSnapshot = connectionMetrics.getActiveConnections();
				BDDAssertions.assertThat(statsSnapshot).hasSize(1);
				BDDAssertions.assertThat(statsSnapshot).anySatisfy(stat -> {
					BDDAssertions.assertThat(stat.getBindingThreadId()).isEqualTo(Thread.currentThread().getId());
					BDDAssertions.assertThat(stat.getBindingThreadName()).isEqualTo(Thread.currentThread().getName());
					BDDAssertions.assertThat(stat.getConnectionInstanceIdentifier()).isEqualTo(c.getIdentifier());
				});
			}
			BDDAssertions.assertThat(connectionMetrics.getInvocationCount(CONNECTION_CLOSE)).isEqualTo(1L);
		}
		BDDAssertions.assertThat(connectionMetrics.getInvocationCount(CONNECTION_CLOSE)).isEqualTo(1L);
	}

	// TODO: verify that timed out connections are also closed when they go back into the pool

}
