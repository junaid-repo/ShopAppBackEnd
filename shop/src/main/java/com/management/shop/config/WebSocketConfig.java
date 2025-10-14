package com.management.shop.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // The endpoint clients will connect to
        registry.addEndpoint("/ws")
                // Allow connections from any origin, crucial for development
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Messages sent to destinations with this prefix will be routed to the message broker (e.g., /topic)
        registry.enableSimpleBroker("/topic");
        // Messages sent to destinations with this prefix are for application-level processing (e.g., @MessageMapping)
        registry.setApplicationDestinationPrefixes("/app");
    }
}