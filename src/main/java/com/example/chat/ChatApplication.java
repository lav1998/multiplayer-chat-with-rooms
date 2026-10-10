package com.example.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@SpringBootApplication
@Configuration
@EnableWebSocket
public class ChatApplication implements WebSocketConfigurer {

    public static void main(String[] args) {
        SpringApplication.run(ChatApplication.class, args);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Registering the handler mapped to the /ws/{room}/{username} URL pattern
        registry.addHandler(new ChatHandler(), "/ws/*/*").setAllowedOrigins("*");
    }

    // A single simple nested class handles the WebSocket connections
    static class ChatHandler extends TextWebSocketHandler {
        private final Map<String, Set<WebSocketSession>> rooms = new ConcurrentHashMap<>();
        private final ObjectMapper mapper = new ObjectMapper();

        @Override
        public void afterConnectionEstablished(WebSocketSession session) {
            String room = getSegment(session, 2);
            String user = getSegment(session, 3);
            
            // Add session to the room
            rooms.computeIfAbsent(room, k -> new CopyOnWriteArraySet<>()).add(session);
            broadcast(room, "system", "System", user + " joined the room.");
        }

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) {
            if (!message.getPayload().isBlank()) {
                broadcast(getSegment(session, 2), "chat", getSegment(session, 3), message.getPayload());
            }
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
            String room = getSegment(session, 2);
            String user = getSegment(session, 3);
            
            // Remove session from the room
            Set<WebSocketSession> roomSessions = rooms.get(room);
            if (roomSessions != null) {
                roomSessions.remove(session);
                if (roomSessions.isEmpty()) {
                    rooms.remove(room); // Cleanup empty rooms
                }
            }
            broadcast(room, "system", "System", user + " left the room.");
        }

        private void broadcast(String room, String type, String username, String text) {
            Set<WebSocketSession> roomSessions = rooms.get(room);
            if (roomSessions == null) return;
            
            try {
                // Build simple JSON response
                String json = mapper.writeValueAsString(Map.of(
                        "type", type, 
                        "username", username,
                        "text", text, 
                        "timestamp", Instant.now().toString()
                ));
                TextMessage msg = new TextMessage(json);
                
                // Send to all connected clients in the room
                for (WebSocketSession s : roomSessions) {
                    if (s.isOpen()) s.sendMessage(msg);
                }
            } catch (Exception ignored) {
                // Ignore failed deliveries
            }
        }

        private String getSegment(WebSocketSession session, int index) {
            String[] segments = session.getUri().getPath().split("/");
            return segments.length > index ? segments[index] : "unknown";
        }
    }
}
