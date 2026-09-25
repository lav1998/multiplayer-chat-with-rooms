# Multiplayer Chat with Rooms

## What the application does
This is a lightweight, real-time multiplayer chat application. It allows users to join distinct, named chat rooms and instantly exchange messages with other users currently in the same room.

## Features currently implemented
- **Real-time Messaging:** Low-latency communication powered by WebSockets.
- **Room Isolation:** Messages are exclusively broadcast to users residing in the specific room.
- **Robust Broadcasting:** Gracefully handles unexpected client disconnections during active server broadcasts.
- **Structured Payloads:** Uses JSON for messaging, with server-generated UTC timestamps to guarantee chronological consistency.
- **Validation:** Dual-layer (frontend and backend) validation to prevent empty or whitespace-only spam.
- **Clean UI:** A modern, auto-scrolling, responsive HTML/CSS frontend with clear status indicators and system event announcements.

## Tech Stack
- **Backend:** Python 3, FastAPI, Uvicorn
- **Frontend:** Vanilla HTML5, CSS3, JavaScript, native WebSockets API
- **Dependencies:** Standard library (JSON, Datetime) + FastAPI/Uvicorn only.

## Simple Architecture Explanation
The application uses an in-memory `ConnectionManager`. When a client joins a room, their browser initiates a native `WebSocket` connection to the FastAPI backend. The server intercepts this connection and stores the WebSocket object in a Python `Set` mapped to the room's name in a central dictionary.

When a user sends a message, the server parses the payload, validates it, injects a server-side timestamp, formats it as JSON, and iterates through the `Set` of WebSockets for that specific room, transmitting the message to each connected client. The frontend then parses the JSON and injects the message into the DOM.

## Project Structure
```text
multiplayer-chat-with-rooms/
│
├── main.py              # The FastAPI backend and WebSocket ConnectionManager
├── index.html           # The frontend UI, styles, and client-side logic
├── requirements.txt     # Python dependencies list
└── .gitignore           # Git ignore configurations
```

## Setup and Run Instructions (Windows)
1. **Open a terminal (PowerShell or Command Prompt)** in the project directory.
2. **Create a virtual environment (Optional but recommended):**
   ```bash
   python -m venv venv
   .\venv\Scripts\activate
   ```
3. **Install dependencies:**
   ```bash
   pip install -r requirements.txt
   ```
4. **Start the server:**
   ```bash
   python -m uvicorn main:app --reload
   ```
5. **Open your browser** and navigate to `http://localhost:8000`.

## How to use the application
1. In the browser, enter a **Username** (e.g., "Alice") and a **Room** (e.g., "general").
2. Click **Join Room**.
3. Open a second browser tab (or window), navigate to the same URL, and enter a different Username but the *same* Room.
4. Type a message in the input box and press **Send** or hit `Enter`. The message will instantly appear for both users.
5. Click **Leave** to disconnect from the room.

## Known Limitations
*   **Single-Instance Prototype:** Room state and active connections are held entirely in server memory (RAM). This means the application currently only works if deployed as a single process/instance.
*   **Ephemeral Data:** There is no database. If the server restarts, all rooms and connections are immediately lost.
*   **No Chat History:** Users only see messages that were broadcast *while* they are connected. They cannot see messages sent before they joined the room.

## Production Considerations
To evolve this prototype into a production-ready application, the following architectural changes would be required:
1. **Messaging for Multiple Instances:** To scale horizontally across multiple servers, the in-memory dictionary must be replaced by a centralized message broker (like Redis Pub/Sub or RabbitMQ) so instances can share messages across the cluster.
2. **Persistence:** If retrieving historical chat logs is required, incoming messages must be saved to a database (e.g., PostgreSQL, Cassandra, or MongoDB) asynchronously before or while being broadcast.
3. **Authentication & Authorization:** The current system blindly trusts the username in the URL. A production system requires JWT tokens or session cookies to verify the identity of the WebSocket connector.
4. **Monitoring & Rate Limiting:** Introduce connection limits, rate limiting (to prevent message spam), and monitoring (e.g., Prometheus) to track active socket counts and server health.

---
*Made by Lav Ram Gabri using Google Antigravity*
