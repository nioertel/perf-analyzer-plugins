package io.github.nioertel.perf.jdbc.datasource;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import io.github.nioertel.perf.jdbc.hikari.EnhancedHikariDataSource;

abstract class EnhancedDataSourceConfiguration {

	private static final Logger LOGGER = LoggerFactory.getLogger(EnhancedDataSourceConfiguration.class);

	/**
	 * Enhanced Hikari DataSource configuration.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(EnhancedHikariDataSource.class)
	@ConditionalOnMissingBean(DataSource.class)
	@ConditionalOnProperty(name = "spring.datasource.type", havingValue = "io.github.nioertel.perf.jdbc.hikari.EnhancedHikariDataSource",
			matchIfMissing = true)
	static class Hikari {

		@Bean
		@ConfigurationProperties(prefix = "spring.datasource.hikari")
		EnhancedHikariDataSource dataSource(DataSourceProperties properties) {
			LOGGER.info("Initializing enhanced Hikari data source.");
			EnhancedHikariDataSource dataSource = properties.initializeDataSourceBuilder().type(EnhancedHikariDataSource.class).build();
			if (StringUtils.hasText(properties.getName())) {
				dataSource.setPoolName(properties.getName());
			}
			return dataSource;
		}

	}
}
