package io.github.nioertel.perf.jdbc.micrometer;

import java.sql.Connection;
import java.util.List;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;

import com.zaxxer.hikari.HikariConfig;

import io.github.nioertel.perf.jdbc.hikari.EnhancedHikariDataSource;

@ConditionalOnWebApplication
@ConditionalOnClass({ Connection.class })
@AutoConfiguration(after = DataSourceAutoConfiguration.class)
public class JdbcMetricsActuatorEndpointsAutoConfiguration {

	@ConditionalOnClass({ HikariConfig.class })
	@Bean
	HikariJdbcMetricsEndpoint hikariJdbcMetricsEndpoint(List<EnhancedHikariDataSource> hikariDataSources) {
		return new HikariJdbcMetricsEndpoint(hikariDataSources);
	}
}
