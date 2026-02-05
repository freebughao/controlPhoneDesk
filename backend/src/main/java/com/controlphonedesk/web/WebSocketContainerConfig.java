package com.controlphonedesk.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
public class WebSocketContainerConfig {
    /**
     * 调整 WebSocket 容器的消息缓冲区大小，防止大视频帧被截断。
     */
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxBinaryMessageBufferSize(5 * 1024 * 1024);
        container.setMaxTextMessageBufferSize(1 * 1024 * 1024);
        return container;
    }
}
