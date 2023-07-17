package io.github.nioertel.perf.jdbc.micrometer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.BDDAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.jayway.jsonpath.JsonPath;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import io.github.nioertel.perf.jdbc.hikari.EnhancedHikariDataSource;
import io.github.nioertel.perf.jdbc.micrometer.test.TestApp;
import io.github.nioertel.perf.jdbc.tracker.IdentifiableConnection;
import io.github.nioertel.perf.jdbc.utils.DateHelper;

@SpringBootTest(//
		classes = TestApp.class, //
		webEnvironment = WebEnvironment.RANDOM_PORT, //
		properties = { //
				"management.endpoints.web.exposure.include=*", //
				"spring.jackson.serialization.indent_output=true", //
				"spring.datasource.type=io.github.nioertel.perf.jdbc.hikari.EnhancedHikariDataSource", //
				"spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false;MODE=MSSQLServer", //
				"spring.datasource.driver-class-name=org.h2.Driver", //
				"spring.datasource.username=sa", //
				"spring.datasource.password=sa", //
				"spring.datasource.name=test-1", //
				"spring.datasource.hikari.pool-name=test-1", //
				"spring.datasource.hikari.minimum-idle=1", //
				"spring.datasource.hikari.maximum-pool-size=1"//
		})
class HikariJdbcMetricsEndpointTest {

	@Autowired
	private List<HikariDataSource> autoConfiguredDataSources;

	@Autowired
	private HikariJdbcMetricsEndpoint hikariMetricsEndpoint;

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void actuatorShouldBePresent() {
		ResponseEntity<String> health = restTemplate.getForEntity("/actuator/health", String.class, Map.of());
		BDDAssertions.assertThat(health.getStatusCode()).isSameAs(HttpStatus.OK);
		BDDAssertions.assertThat(JsonPath.<String>read(health.getBody(), "status")).isEqualTo("UP");
	}

	@Test
	void actuatorShouldPublishHikariJdbcMetricsEndpoint() {
		ResponseEntity<String> health = restTemplate.getForEntity("/actuator/hikariJdbcMetrics", String.class, Map.of());
		BDDAssertions.assertThat(health.getStatusCode()).isSameAs(HttpStatus.OK);
		BDDAssertions.assertThat((Map<?, ?>) JsonPath.parse(health.getBody()).json()).allSatisfy((k, v) -> {
			BDDAssertions.assertThat(k).isEqualTo("connectionStatsByPool");
		});
	}

	@Test
	void actuatorShouldPublishHikariJdbcMetricsForAutoConfiguredDataSource() throws SQLException {
		LocalDateTime beforeAcquire = LocalDateTime.now(DateHelper.EUROPE_BERLIN).truncatedTo(ChronoUnit.MILLIS);
		try (IdentifiableConnection c = (IdentifiableConnection) autoConfiguredDataSources.get(0).getConnection()) {
			LocalDateTime acquired = LocalDateTime.now(DateHelper.EUROPE_BERLIN).truncatedTo(ChronoUnit.MILLIS);
			try (Statement s = c.createStatement()) {
				BDDAssertions.assertThat(s.execute("SELECT 5")).isTrue();
				ResultSet r = s.getResultSet();
				BDDAssertions.assertThat(r.next()).isTrue();
				BDDAssertions.assertThat(r.getInt(1)).isEqualTo(5);
			}
			// connection should be visible in Actuator endpoint
			ResponseEntity<String> health = restTemplate.getForEntity("/actuator/hikariJdbcMetrics", String.class, Map.of());
			BDDAssertions.assertThat(health.getStatusCode()).isSameAs(HttpStatus.OK);
			System.out.println(health.getBody());
			verifyPoolMetricsForOneActiveConnection(health.getBody(), beforeAcquire, acquired, "test-1");
		}
	}

	@Test
	void actuatorShouldPublishHikariJdbcMetricsForManuallyAddedDataSource() throws SQLException {
		HikariConfig config = new HikariConfig();
		config.setDriverClassName("org.h2.Driver");
		config.setConnectionTestQuery("SELECT 1");
		config.setJdbcUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false;MODE=MSSQLServer");
		config.setUsername("sa");
		config.setPassword("sa");
		config.setMinimumIdle(1);
		config.setPoolName("test-2");
		config.setMaximumPoolSize(1);

		try (EnhancedHikariDataSource dataSource = new EnhancedHikariDataSource(config)) {
			hikariMetricsEndpoint.registerAdditionalDatasSource(dataSource);

			LocalDateTime beforeAcquire = LocalDateTime.now(DateHelper.EUROPE_BERLIN).truncatedTo(ChronoUnit.MILLIS);
			try (IdentifiableConnection c = (IdentifiableConnection) dataSource.getConnection()) {
				LocalDateTime acquired = LocalDateTime.now(DateHelper.EUROPE_BERLIN).truncatedTo(ChronoUnit.MILLIS);
				try (Statement s = c.createStatement()) {
					BDDAssertions.assertThat(s.execute("SELECT 5")).isTrue();
					ResultSet r = s.getResultSet();
					BDDAssertions.assertThat(r.next()).isTrue();
					BDDAssertions.assertThat(r.getInt(1)).isEqualTo(5);
				}
				// connection should be visible in Actuator endpoint
				ResponseEntity<String> health = restTemplate.getForEntity("/actuator/hikariJdbcMetrics", String.class, Map.of());
				BDDAssertions.assertThat(health.getStatusCode()).isSameAs(HttpStatus.OK);
				System.out.println(health.getBody());
				verifyPoolMetricsForOneActiveConnection(health.getBody(), beforeAcquire, acquired, "test-2");
			}
			// connection should have disappeared from Actuator
			ResponseEntity<String> health = restTemplate.getForEntity("/actuator/hikariJdbcMetrics", String.class, Map.of());
			BDDAssertions.assertThat(health.getStatusCode()).isSameAs(HttpStatus.OK);
			verifyPoolMetricsForNoActiveConnection(health.getBody(), "test-2");
		}
	}

	private static void verifyPoolMetricsForOneActiveConnection(String responseBody, LocalDateTime beforeAcquire, LocalDateTime acquired,
			String poolName) {
		BDDAssertions.assertThat(JsonPath.<Map<?, ?>>read(responseBody, "connectionStatsByPool"))//
				.anySatisfy((k, v) -> {
					BDDAssertions.assertThat(k).isEqualTo(poolName);
					BDDAssertions.assertThat((Map<?, ?>) v)//
							.hasSize(3)//
							.anySatisfy((k2, v2) -> {
								BDDAssertions.assertThat(k2).isEqualTo("poolName");
								BDDAssertions.assertThat(v2).isEqualTo(poolName);
							})//
							.anySatisfy((k2, v2) -> {
								BDDAssertions.assertThat(k2).isEqualTo("activeConnections");
								BDDAssertions.assertThat((List<?>) v2)//
										.hasSize(1)//
										.anySatisfy(v3 -> {
											BDDAssertions.assertThat((Map<?, ?>) v3)//
													.anySatisfy((k4, v4) -> {
														BDDAssertions.assertThat(k4).isEqualTo("bindingThreadId");
														BDDAssertions.assertThat(v4).isEqualTo(1);
													})//
													.anySatisfy((k4, v4) -> {
														BDDAssertions.assertThat(k4).isEqualTo("bindingThreadName");
														BDDAssertions.assertThat(v4).isEqualTo("main");
													})//
													.anySatisfy((k4, v4) -> {
														BDDAssertions.assertThat(k4).isEqualTo("acquired");
														LocalDateTime acquiredFromActuator = LocalDateTime.parse((String) v4);
														BDDAssertions.assertThat(acquiredFromActuator).isBetween(beforeAcquire, acquired);
													});
										});
							})//
							.anySatisfy((k2, v2) -> {
								BDDAssertions.assertThat(k2).isEqualTo("pendingConnections");
								BDDAssertions.assertThat((List<?>) v2).isEmpty();
							});
				});
	}

	private static void verifyPoolMetricsForNoActiveConnection(String responseBody, String poolName) {
		BDDAssertions.assertThat(JsonPath.<Map<?, ?>>read(responseBody, "connectionStatsByPool"))//
				.anySatisfy((k, v) -> {
					BDDAssertions.assertThat(k).isEqualTo(poolName);
					BDDAssertions.assertThat((Map<?, ?>) v)//
							.hasSize(3)//
							.anySatisfy((k2, v2) -> {
								BDDAssertions.assertThat(k2).isEqualTo("poolName");
								BDDAssertions.assertThat(v2).isEqualTo(poolName);
							})//
							.anySatisfy((k2, v2) -> {
								BDDAssertions.assertThat(k2).isEqualTo("activeConnections");
								BDDAssertions.assertThat((List<?>) v2).isEmpty();
							})//
							.anySatisfy((k2, v2) -> {
								BDDAssertions.assertThat(k2).isEqualTo("pendingConnections");
								BDDAssertions.assertThat((List<?>) v2).isEmpty();
							});
				});
	}

}
