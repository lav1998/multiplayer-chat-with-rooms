from typing import Dict, Set
from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from fastapi.responses import HTMLResponse
import os
from datetime import datetime, timezone

app = FastAPI()

class ConnectionManager:
    def __init__(self):
        # Maps room names to sets of active WebSocket connections
        self.rooms: Dict[str, Set[WebSocket]] = {}

    async def connect(self, websocket: WebSocket, room: str):
        await websocket.accept()
        if room not in self.rooms:
            self.rooms[room] = set()
        self.rooms[room].add(websocket)

    def disconnect(self, websocket: WebSocket, room: str):
        if room in self.rooms:
            self.rooms[room].discard(websocket)
            # Optionally clean up empty rooms
            if not self.rooms[room]:
                del self.rooms[room]

    async def broadcast(self, message: dict, room: str):
        if room in self.rooms:
            dead_connections = set()
            for connection in self.rooms[room]:
                try:
                    await connection.send_json(message)
                except Exception:
                    dead_connections.add(connection)
            
            for dead in dead_connections:
                self.disconnect(dead, room)

manager = ConnectionManager()

@app.get("/", response_class=HTMLResponse)
async def read_root():
    # Serve the basic index.html file
    file_path = os.path.join(os.path.dirname(__file__), "index.html")
    with open(file_path, "r", encoding="utf-8") as f:
        html_content = f.read()
    return HTMLResponse(content=html_content)

@app.websocket("/ws/{room}/{username}")
async def websocket_endpoint(websocket: WebSocket, room: str, username: str):
    await manager.connect(websocket, room)
    
    join_msg = {
        "type": "system",
        "username": "System",
        "text": f"{username} joined the room.",
        "timestamp": datetime.now(timezone.utc).isoformat()
    }
    await manager.broadcast(join_msg, room)
    
    try:
        while True:
            data = await websocket.receive_text()
            cleaned_data = data.strip()
            if not cleaned_data:
                continue
            
            chat_msg = {
                "type": "chat",
                "username": username,
                "text": cleaned_data,
                "timestamp": datetime.now(timezone.utc).isoformat()
            }
            await manager.broadcast(chat_msg, room)
    except WebSocketDisconnect:
        manager.disconnect(websocket, room)
        leave_msg = {
            "type": "system",
            "username": "System",
            "text": f"{username} left the room.",
            "timestamp": datetime.now(timezone.utc).isoformat()
        }
        await manager.broadcast(leave_msg, room)
