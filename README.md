# 🎨 Labyrinthe de Mots – JavaFX Client

This is the **JavaFX-based client** for the multiplayer *Labyrinthe de Mots* game. It provides an interactive graphical interface inspired by Pac-Man, allowing players to connect to a server and play in real-time.

## 🖥️ Features

- 🌈 JavaFX GUI with animated tile-based maze rendering
- 🎮 User input handling (keyboard/mouse)
- 📡 TCP communication with the game server
- 🔄 Real-time update of player state and game progress
- 🧭 Highlights possible paths and discovered words
- 🧠 Visual feedback for scoring and results

---

## 🧱 Architecture

- **Language**: Java  
- **Framework**: JavaFX  
- **Communication**: TCP sockets with the game server  
- **Role**: Thin client – all core game logic lives on the server  

---

## 🌐 Multiplayer Workflow

1. Launch the client and connect to the server via IP/port
2. Wait for another player to join the session
3. Receive generated maze, word positions, and turn updates
4. Play turn-based actions within time constraints
5. View scores and game results on finish

---

---

## 🧩 Notes

- The client does not contain any core logic (e.g. maze generation or scoring). It purely displays data received from the server and sends back player actions.
- A working server must be running and accessible for multiplayer gameplay.

---

**Université de Tunis El Manar – ISI | 2024–2025**
