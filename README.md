# Distributed Online Gambling Platform

A distributed systems project built from scratch in Java, implementing a fully functional online gambling platform on top of a custom **MapReduce framework** and a multi-node **Master/Worker architecture**. All communication between components is done exclusively through **TCP sockets**, with no external libraries or databases.

---

## Overview

The platform supports two modes of operation:

- **Player mode** — browse and filter available games, place bets, add balance
- **Manager mode** — add/remove games, modify game properties, view profit/loss statistics per game and per player

The system is designed to scale horizontally across an arbitrary number of machines (Workers), with all game data distributed across nodes via consistent hashing and all query processing parallelised via MapReduce.

---

## Architecture

```
[Manager Console]          [Player App]
       |                        |
       |          TCP           |
       └──────────┬─────────────┘
                  ▼
           [Master :5000]
          /      |       \
    TCP  /    TCP|        \ TCP
        ▼        ▼         ▼
  [Worker 1] [Worker 2] [Worker N]
        \        |         /
    TCP  \    TCP|        / TCP
          ▼      ▼       ▼
              [Reducer :7000]
                  |
             TCP callback
                  |
           [Master :5000]
```

### Components

| Component | Role |
|---|---|
| **Master** | Central TCP server. Routes manager operations to the correct Worker via consistent hashing (`H(GameName) % N`). Coordinates MapReduce fan-out for player searches. Maintains a waiting room for in-flight search requests. |
| **ClientHandler** | One thread per connected client (player, manager, or Reducer callback). Handles routing, MapReduce orchestration, and `wait`/`notify` synchronisation. |
| **Worker** | TCP server that stores a partition of the game catalogue in memory. Runs the `map()` phase of MapReduce on its local data. Handles game management operations from the Manager. |
| **WorkerHandler** | One thread per request received by a Worker. Executes `map()`, sends results directly to the Reducer, and ACKs the Master. |
| **Reducer** | TCP server that collects `map()` results from all Workers, runs `reduce()` when all expected results have arrived, and sends the final result back to the Master via a callback connection. |
| **ReducerHandler** | One thread per incoming connection to the Reducer. Handles both `REGISTER_MAP` messages from the Master and `MAP_RESULT` messages from Workers. |
| **Secure Random Generator** | Separate multithreaded TCP server that continuously generates cryptographically secure random numbers used during bet resolution, implementing a producer-consumer buffer. |

---

## MapReduce — Search Flow

When a player searches for games using filters (risk level, betting category, minimum stars), the system executes a full MapReduce pipeline:

```
1. Master generates a unique mapId
2. Master registers (mapId, workerCount) with Reducer
3. Master fans out MAP_TASK to all Workers in parallel (one thread per Worker)
4. Each Worker runs map() — emits (mapId, Game) for every game matching the filters
5. Each Worker sends MAP_RESULT directly to Reducer, then ACKs Master
6. Reducer accumulates results; when received == expected, triggers reduce()
7. reduce() de-duplicates the game list by name
8. Reducer sends REDUCER_CALLBACK to Master:5000
9. Master wakes the waiting ClientHandler thread via notify()
10. ClientHandler sends the final game list to the Player
```

Multiple concurrent searches run fully independently, each identified by their `mapId`.

---

## Game Distribution

Games are distributed across Workers using consistent hashing:

```java
NodeId = Math.abs(GameName.hashCode()) % numberOfWorkers
```

This ensures each game always lands on — and is always retrieved from — the same Worker, with no coordination overhead.

---

## Synchronisation

All synchronisation is implemented manually using Java's built-in primitives, as required by the project specification. No classes from `java.util.concurrent` are used.

| Scenario | Mechanism |
|---|---|
| Concurrent bets on the same game | `synchronized` on the `Worker` instance |
| Concurrent map result collection in Reducer | `synchronized` on the `Reducer` instance |
| Waiting for Reducer callback in Master | `wait()` / `notify()` on `SearchResult` |
| Secure RNG buffer (producer-consumer) | `wait()` / `notify()` on the shared queue |
| Shared mapId counter on Master | `synchronized` method |
| Waiting room map on Master | `synchronized` methods |

---

## Game Data Format

Games are registered via JSON files:

```json
{
  "GameName": "DragonSlots",
  "ProviderName": "ProviderA",
  "Stars": 4,
  "NoOfVotes": 230,
  "GameLogo": "/usr/bin/images/dragon_slots.png",
  "MinBet": 0.1,
  "MaxBet": 10.0,
  "RiskLevel": "low",
  "HashKey": "dragonkey123"
}
```

`BettingCategory` and `Jackpot` are computed by the system and are not part of the file.

---

## Project Structure

```
distributed-gambling-platform/
├── src/
│   ├── shared/
│   │   └── Request.java            # Serializable message envelope (all TCP comms)
│   ├── model/
│   │   └── Game.java               # Game data model
│   ├── master/
│   │   ├── Master.java             # TCP server, shared state, routing
│   │   ├── ClientHandler.java      # Per-client thread, MapReduce orchestration
│   │   └── SearchResult.java       # wait/notify lock object for search results
│   ├── worker/
│   │   ├── Worker.java             # TCP server, in-memory game store
│   │   └── WorkerHandler.java      # Per-request thread, map() execution
│   ├── reducer/
│   │   ├── Reducer.java            # TCP server, shared state, reduce logic
│   │   └── ReducerHandler.java     # Per-connection thread, result collection
│   ├── manager/
│   │   └── ManagerConsole.java     # CLI for game management
│   ├── player/
│   │   └── PlayerApp.java          # CLI for player interactions
│   └── randomgenerator/
│       └── RandomGenerator.java    # Secure RNG TCP server
└── resources/
    ├── config.properties           # Ports and addresses for all components
    └── games/                      # Sample game JSON files
```

---

## Configuration

All components are configured via `resources/config.properties`. Set the following properties before running:

```properties
# Master configuration
master.port=5000
master.host=localhost

# Reducer configuration
reducer.port=7000
reducer.host=localhost

# Worker configuration (one per node)
worker.port=6000
worker.host=localhost

# Random Generator configuration
randomgen.port=8000
randomgen.host=localhost
```

---

## Running the Project

Each component is an independent Java process and can run on a separate machine. Before starting, make sure all addresses and ports are correctly set in `resources/config.properties`.

Start the components in the following order so that each server is ready before the next one tries to connect to it:

1. **Reducer** — must be up first, as Workers send their map results directly to it
   ```bash
   java -cp bin src.reducer.Reducer
   ```

2. **Secure Random Generator** — required for bet resolution
   ```bash
   java -cp bin src.randomgenerator.RandomGenerator
   ```

3. **Workers** — one process per node, each listening on its own port
   ```bash
   java -cp bin src.worker.Worker 1  # Worker 1
   java -cp bin src.worker.Worker 2  # Worker 2
   # ... add more Workers as needed
   ```

4. **Master** — connects to Workers and Reducer on startup
   ```bash
   java -cp bin src.master.Master <number_of_workers>
   ```

5. **Manager Console** — connects to the Master to register games
   ```bash
   java -cp bin src.manager.ManagerConsole
   ```

6. **Player App** — connects to the Master to search and play games
   ```bash
   java -cp bin src.player.PlayerApp
   ```

The number of Workers is arbitrary and configured at startup — no code changes are needed to add more nodes.

---

## Technical Constraints

This project was built under the following constraints, as defined by the course specification:

- **No external libraries** — only Java standard library (`ServerSocket`, object streams)
- **No database** — all game data and statistics stored in memory (`HashMap`)
- **No `java.util.concurrent`** — synchronisation via `synchronized`, `wait`, `notify` only
- **TCP only** — all inter-component communication via raw TCP sockets
- **No disk writes** — except for game logo files

---

## Features

### Player Features
- Browse available games with detailed information
- Filter games by:
  - Risk level (low, medium, high)
  - Betting category
  - Minimum star rating
- Place bets on games
- Add balance to account
- View bet history and results

### Manager Features
- Add new games to the platform
- Remove existing games
- Modify game properties (odds, limits, ratings)
- View profit/loss statistics per game
- View player-level statistics
- Monitor system health

### System Features
- Horizontal scaling across arbitrary number of nodes
- Consistent hashing for deterministic game distribution
- MapReduce-based parallel search across all nodes
- Cryptographically secure random number generation for fair bet resolution
- Manual thread synchronisation for fine-grained control
- Producer-consumer pattern for secure random number generation

---

## Course Context

Built as the final project for the **Distributed Systems** course. The project covers:

- Distributed system design and component decomposition
- The MapReduce programming model (parallel map, shuffle, reduce)
- Multithreaded Java server design
- TCP socket programming
- Manual synchronisation with Java monitors
- Consistent hashing for data partitioning
- Producer-consumer concurrency pattern (Secure RNG)

---

## License

This project is provided as-is for educational purposes.
