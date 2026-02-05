package com.controlphonedesk.web;

import com.controlphonedesk.AppProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final ScrcpyWebSocketProxyHandler scrcpyHandler;
    private final AppProperties properties;
    private final com.controlphonedesk.auth.WebSocketAuthInterceptor authInterceptor;

    public WebSocketConfig(
        ScrcpyWebSocketProxyHandler scrcpyHandler,
        AppProperties properties,
        com.controlphonedesk.auth.WebSocketAuthInterceptor authInterceptor
    ) {
        this.scrcpyHandler = scrcpyHandler;
        this.properties = properties;
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        String[] origins = properties.getCors().getAllowedOrigins().toArray(new String[0]);
        String[] patterns = properties.getCors().getAllowedOriginPatterns().toArray(new String[0]);
        var registration = registry.addHandler(scrcpyHandler, "/ws/scrcpy")
            .addInterceptors(authInterceptor);
        if (patterns.length > 0) {
            registration.setAllowedOriginPatterns(patterns);
        } else {
            registration.setAllowedOrigins(origins);
        }
    }
}
