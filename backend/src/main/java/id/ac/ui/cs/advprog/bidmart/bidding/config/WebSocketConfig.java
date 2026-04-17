package id.ac.ui.cs.advprog.bidmart.bidding.config;

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
        // endpoint utama untuk handshake koneksi dari frontend
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*"); // mengizinkan koneksi dari domain mana saja (frontend)
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // prefix untuk jalur broadcast dari server ke client (subscribe)
        registry.enableSimpleBroker("/topic");

        // prefix untuk jalur request dari client ke server (publish)
        registry.setApplicationDestinationPrefixes("/app");
    }
}