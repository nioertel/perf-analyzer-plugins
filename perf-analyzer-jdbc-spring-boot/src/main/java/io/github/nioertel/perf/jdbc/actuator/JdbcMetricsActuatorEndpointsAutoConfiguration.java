package io.github.nioertel.perf.jdbc.actuator;

import java.sql.Connection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
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
	JdbcConnectionMetricsSupplier hikariJdbcMetricsSupplier(List<EnhancedHikariDataSource> hikariDataSources) {
		return new JdbcConnectionMetricsSupplier(//
				hikariDataSources.stream()//
						.map(EnhancedHikariDataSource::getConnectionMetrics)//
						.collect(Collectors.toList())//
		);
	}

	@ConditionalOnBean({ JdbcConnectionMetricsSupplier.class })
	@Bean
	JdbcMetricsEndpoint jdbcMetricsEndpoint(List<JdbcConnectionMetricsSupplier> connectionMetricsSuppliers) {
		return new JdbcMetricsEndpoint(connectionMetricsSuppliers);
	}
}
