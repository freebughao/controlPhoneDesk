package com.controlphonedesk.config;

import java.io.File;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SqliteDataSourceConfig {
    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.driver-class-name:org.sqlite.JDBC}")
    private String driverClassName;

    @Bean
    public DataSource dataSource() {
        ensureDirectory(url);
        return DataSourceBuilder.create()
            .url(url)
            .driverClassName(driverClassName)
            .build();
    }

    private void ensureDirectory(String jdbcUrl) {
        if (jdbcUrl == null || !jdbcUrl.startsWith("jdbc:sqlite:")) {
            return;
        }
        String path = jdbcUrl.substring("jdbc:sqlite:".length());
        if (path.startsWith("file:")) {
            path = path.substring("file:".length());
        }
        if (path.isBlank() || path.equals(":memory:")) {
            return;
        }
        File file = new File(path);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
    }
}
