from typing import Dict, Set
from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from fastapi.responses import HTMLResponse
import os

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

    async def broadcast(self, message: str, room: str):
        if room in self.rooms:
            dead_connections = set()
            for connection in self.rooms[room]:
                try:
                    await connection.send_text(message)
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
    await manager.broadcast(f"[System] {username} joined the room.", room)
    try:
        while True:
            data = await websocket.receive_text()
            await manager.broadcast(f"{username}: {data}", room)
    except WebSocketDisconnect:
        manager.disconnect(websocket, room)
        await manager.broadcast(f"[System] {username} left the room.", room)
