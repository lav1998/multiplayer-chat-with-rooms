# Multiplayer Chat with Rooms (Java/Spring Boot)

## What the application does
This is a lightweight, real-time multiplayer chat application. It allows users to join distinct, named chat rooms and instantly exchange messages with other users currently in the same room.

*Note: This project was originally built in Python/FastAPI and has been migrated to Java/Spring Boot to demonstrate a simplified, single-file WebSocket server architecture.*

## Features currently implemented
- **Real-time Messaging:** Low-latency communication powered by Spring WebSockets.
- **Room Isolation:** Messages are exclusively broadcast to users residing in the specific room.
- **Robust Broadcasting:** Gracefully handles unexpected client disconnections during active server broadcasts.
- **Structured Payloads:** Uses JSON for messaging, with server-generated UTC timestamps to guarantee chronological consistency.
- **Validation:** Dual-layer (frontend and backend) validation to prevent empty or whitespace-only spam.
- **Clean UI:** A modern, auto-scrolling, responsive HTML/CSS frontend with clear status indicators and system event announcements.

## Tech Stack
- **Backend:** Java 17, Spring Boot (Web, WebSocket)
- **Frontend:** Vanilla HTML5, CSS3, JavaScript, native WebSockets API
- **Dependencies:** Spring Boot Starter Web, Spring Boot Starter WebSocket, Jackson (for JSON).

## Simple Architecture Explanation
The backend logic has been consolidated into a single file (`ChatApplication.java`). The application uses a custom `ChatHandler` extending `TextWebSocketHandler`. 

When a client joins a room, their browser initiates a native `WebSocket` connection to the Spring Boot backend (`/ws/{room}/{username}`). The handler intercepts this connection and stores the `WebSocketSession` in a thread-safe `CopyOnWriteArraySet`, which is mapped to the room's name in a `ConcurrentHashMap`.

When a user sends a message, the server parses the payload, validates it, injects a server-side timestamp, formats it as JSON using Jackson's `ObjectMapper`, and iterates through the `Set` of sessions for that specific room, transmitting the message to each connected client. The frontend then parses the JSON and injects the message into the DOM.

## Project Structure
```text
multiplayer-chat-with-rooms-java/
├── pom.xml                                           # Maven build configuration
├── src/main/java/.../ChatApplication.java            # Single-file Spring Boot app & WebSocket Handler
└── src/main/resources/static/index.html              # The frontend UI, styles, and client-side logic
```

## Setup and Run Instructions (Windows / macOS / Linux)

### Prerequisites
- Java 17 or higher installed.
- (Optional) Maven installed on your system (the project includes a Maven Wrapper).

### Running the Application

1. **Open a terminal (PowerShell or Command Prompt)** in the project directory.
2. **Run using the Maven Wrapper:**
   - On **Windows**:
     ```cmd
     .\mvnw.cmd spring-boot:run
     ```
   - On **macOS/Linux**:
     ```bash
     ./mvnw spring-boot:run
     ```
3. **Open your browser** and navigate to `http://localhost:8080`.

## How to use the application
1. In the browser, enter a **Username** (e.g., "Alice") and a **Room** (e.g., "general").
2. Click **Join Room**.
3. Open a second browser tab (or window), navigate to the same URL, and enter a different Username but the *same* Room.
4. Type a message in the input box and press **Send** or hit `Enter`. The message will instantly appear for both users.
5. Click **Leave** to disconnect from the room.

## Known Limitations
*   **Single-Instance Prototype:** Room state and active connections are held entirely in server memory (`ConcurrentHashMap`). This means the application currently only works if deployed as a single process/instance.
*   **Ephemeral Data:** There is no database. If the server restarts, all rooms and connections are immediately lost.
*   **No Chat History:** Users only see messages that were broadcast *while* they are connected. They cannot see messages sent before they joined the room.

## Production Considerations
To evolve this prototype into a production-ready application, the following architectural changes would be required:
1. **Messaging for Multiple Instances:** To scale horizontally across multiple servers, the in-memory map must be replaced by a centralized message broker (like Redis Pub/Sub, RabbitMQ, or STOMP with an external broker) so instances can share messages across the cluster.
2. **Persistence:** If retrieving historical chat logs is required, incoming messages must be saved to a database (e.g., PostgreSQL, Cassandra, or MongoDB) asynchronously before or while being broadcast.
3. **Authentication & Authorization:** The current system blindly trusts the username in the URL. A production system requires JWT tokens or session cookies to verify the identity of the WebSocket connector.
4. **Monitoring & Rate Limiting:** Introduce connection limits, rate limiting (to prevent message spam), and monitoring (e.g., Micrometer/Prometheus) to track active socket counts and server health.

---
*Made by Lav Ram Gabri using Google Antigravity*
