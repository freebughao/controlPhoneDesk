package com.controlphonedesk.web;

import com.controlphonedesk.AppProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    private final AppProperties properties;

    public CorsConfig(AppProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = properties.getCors().getAllowedOrigins().toArray(new String[0]);
        String[] patterns = properties.getCors().getAllowedOriginPatterns().toArray(new String[0]);
        var registration = registry.addMapping("/api/**")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .exposedHeaders("Authorization")
            .allowCredentials(true);
        if (patterns.length > 0) {
            registration.allowedOriginPatterns(patterns);
        } else {
            registration.allowedOrigins(origins);
        }
    }
}
