package com.example.chat.config;

import com.example.chat.handler.ChatWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;

    public WebSocketConfig(ChatWebSocketHandler chatWebSocketHandler) {
        this.chatWebSocketHandler = chatWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Registering the handler for paths like /ws/{room}/{username}
        // Wait, Spring's Raw WebSocketHandler mapping uses simple Ant paths, but for path variables we typically need a handshake interceptor, or map all to a wildcard and parse URI in the handler.
        // Spring WebSocket Registry doesn't support {room} {username} path variables out of the box like REST controllers do, we must map with trailing wildcard or parse the URI manually in the handler.
        // Wait, we can map to "/ws/*/*" and then extract the room and username from the URI in the handler.
        registry.addHandler(chatWebSocketHandler, "/ws/*/*")
                .setAllowedOrigins("*");
    }
}
