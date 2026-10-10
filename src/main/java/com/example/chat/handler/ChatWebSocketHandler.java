package com.example.chat.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Service
public class ChatWebSocketHandler extends TextWebSocketHandler {

    // Maps room names to sets of active WebSocket sessions
    private final Map<String, Set<WebSocketSession>> rooms = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String room = getRoom(session);
        String username = getUsername(session);

        rooms.computeIfAbsent(room, k -> new CopyOnWriteArraySet<>()).add(session);

        String timestamp = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
        Map<String, String> joinMsg = Map.of(
                "type", "system",
                "username", "System",
                "text", username + " joined the room.",
                "timestamp", timestamp
        );

        broadcast(room, joinMsg);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String room = getRoom(session);
        String username = getUsername(session);

        String data = message.getPayload().trim();
        if (data.isEmpty()) {
            return;
        }

        String timestamp = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
        Map<String, String> chatMsg = Map.of(
                "type", "chat",
                "username", username,
                "text", data,
                "timestamp", timestamp
        );

        broadcast(room, chatMsg);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String room = getRoom(session);
        String username = getUsername(session);

        Set<WebSocketSession> roomSessions = rooms.get(room);
        if (roomSessions != null) {
            roomSessions.remove(session);
            if (roomSessions.isEmpty()) {
                rooms.remove(room);
            }
        }

        String timestamp = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
        Map<String, String> leaveMsg = Map.of(
                "type", "system",
                "username", "System",
                "text", username + " left the room.",
                "timestamp", timestamp
        );

        broadcast(room, leaveMsg);
    }

    private void broadcast(String room, Map<String, String> message) throws Exception {
        Set<WebSocketSession> roomSessions = rooms.get(room);
        if (roomSessions == null) return;

        String json = objectMapper.writeValueAsString(message);
        TextMessage textMessage = new TextMessage(json);

        for (WebSocketSession session : roomSessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(textMessage);
                } catch (Exception e) {
                    // Similar to python: ignore failed sends
                }
            }
        }
    }

    private String getRoom(WebSocketSession session) {
        String[] segments = session.getUri().getPath().split("/");
        return segments.length > 2 ? segments[2] : "unknown";
    }

    private String getUsername(WebSocketSession session) {
        String[] segments = session.getUri().getPath().split("/");
        return segments.length > 3 ? segments[3] : "unknown";
    }
}
