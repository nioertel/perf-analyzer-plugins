# perf-analyzer-plugins
Application plugins for performance analysis.

## perf-analyzer-jdbc
### Goals of this plugin
- Provide a `java.sql.Connection` proxy that provides detailed insightes that may be useful when analyzing performance issues
- Add minimal performance overhead so this can be active by default in production
- Integrate with common connection pools, so far providing support for:
  - HikariCP via an extension of `com.zaxxer.hikari.HikariDataSource`

### Usage
Maven:
```xml
<dependency>
    <groupId>io.github.nioertel.perf</groupId>
    <artifactId>perf-analyzer-jdbc</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

Java Code Snippet:
```java
import io.github.nioertel.perf.jdbc.hikari.EnhancedHikariDataSource;

// ... boiler plate code

// setup HikariCP config
HikariConfig config = new HikariConfig();
config.setDriverClassName("org.h2.Driver");
config.setConnectionTestQuery("SELECT 1");
config.setJdbcUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false;MODE=MSSQLServer");
// ... further config settings

// instantiate and work with data source
try (EnhancedHikariDataSource dataSource = new EnhancedHikariDataSource(config)) {
    // ... business logic

    // access connection metrics
    JdbcConnectionMetrics connectionMetrics = dataSource.getConnectionMetrics();
}

// ... boiler plate code
```

## perf-analyzer-jdbc-spring-boot
### Goals of this plugin
- Provide Spring Boot integration of [perf-analyzer-jdbc](#perf-analyzer-jdbc), including
  - auto configuration for `Enhanced*DataSource` (such as EnhancedHikariDataSource) if `spring.datasource.type` is set accordingly
- Actuator endpoint `/actuator/jdbcMetrics`

### Usage
Maven:
```xml
<dependency>
    <groupId>io.github.nioertel.perf</groupId>
    <artifactId>perf-analyzer-jdbc-spring-boot</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

Spring Boot Config:
```properties
spring.datasource.type=io.github.nioertel.perf.jdbc.hikari.EnhancedHikariDataSource
# apply any other HikariCP specific settings as usual (spring.datasource.hikari.*)
```

Java Code Snippet:
```java
// TODO
```

## perf-analyzer-executors
TODO

## perf-analyzer-executors-spring-boot
TODO

## perf-analyzer-common
Shared utilities which are used across the different modules.

## perf-analyzer-test-helper
Tools for testing of the perf-analyzer-plugins. Only for internal use currently.
