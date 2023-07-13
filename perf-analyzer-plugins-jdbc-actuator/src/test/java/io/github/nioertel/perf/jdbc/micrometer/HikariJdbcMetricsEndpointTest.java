package io.github.nioertel.perf.jdbc.micrometer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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

import io.github.nioertel.perf.jdbc.hikari.EnhancedHikariDataSource;
import io.github.nioertel.perf.jdbc.micrometer.test.TestApp;
import io.github.nioertel.perf.jdbc.tracker.IdentifiableConnection;

@SpringBootTest(//
		classes = TestApp.class, //
		webEnvironment = WebEnvironment.RANDOM_PORT, //
		properties = { //
				"management.endpoints.web.exposure.include=*", //
				"spring.jackson.serialization.indent_output=true"//
		})
class HikariJdbcMetricsEndpointTest {

	// @Autowired
	// private HikariJdbcMetricsEndpoint hikariMetricsEndpoint;

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
	void actuatorShouldPublishHikariJdbcMetrics() throws SQLException {
		HikariConfig config = new HikariConfig();
		config.setDriverClassName("org.h2.Driver");
		config.setConnectionTestQuery("SELECT 1");
		config.setJdbcUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false;MODE=MSSQLServer");
		config.setUsername("sa");
		config.setPassword("sa");
		config.setMinimumIdle(1);
		config.setPoolName("test-1");
		config.setMaximumPoolSize(1);

		// TODO: this data source must be Spring Boot managed...
		try (EnhancedHikariDataSource dataSource = new EnhancedHikariDataSource(config)) {
			try (IdentifiableConnection c = (IdentifiableConnection) dataSource.getConnection()) {
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
				BDDAssertions.assertThat(JsonPath.<Map<?, ?>>read(health.getBody(), "connectionStatsByPool")).isEmpty();
			}
			// connection should have disappeared from Actuator
			ResponseEntity<String> health = restTemplate.getForEntity("/actuator/hikariJdbcMetrics", String.class, Map.of());
			BDDAssertions.assertThat(health.getStatusCode()).isSameAs(HttpStatus.OK);
			System.out.println(health.getBody());
			BDDAssertions.assertThat(JsonPath.<Map<?, ?>>read(health.getBody(), "connectionStatsByPool")).isEmpty();
		}
	}

}
