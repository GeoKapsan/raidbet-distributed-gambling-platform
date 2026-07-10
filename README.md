# RaidBet: A Distributed Online Gambling Platform

A Distributed Systems project built from scratch in Java, implementing a fully functional online gambling platform on top of a custom **MapReduce framework** and a multi-node **Master/Worker architecture**. All communication between components is done exclusively through **TCP sockets**, with no external libraries or databases.

>**Frontend:** A companion mobile/web frontend for this backend is available at https://github.com/GeoKapsan/distributed-gambling-platform-android

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

| Component                   | Role                                                                                                                                                                                                                                  |
| --------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Master**                  | Central TCP server. Routes operations to the correct Worker via consistent hashing (`H(GameName) % N`). Coordinates MapReduce fan-out for player searches. Maintains a waiting room for in-flight search requests (SavedMasterState). |
| **ClientHandler**           | One thread per connected client (Player, Manager, or Reducer callback). Handles routing, MapReduce orchestration, and `wait`/`notify` synchronisation.                                                                                |
| **Worker**                  | TCP server that stores a partition of the game catalogue in memory. Runs the `map()` phase of MapReduce on its local data. Handles game management operations from the Manager.                                                       |
| **WorkerHandler**           | One thread per request received by a Worker. Executes `map()`, sends results directly to the Reducer, and ACKs the Master.                                                                                                            |
| **Reducer**                 | TCP server that collects `map()` results from all Workers, runs `reduce()` when all expected results have arrived, and sends the final result back to the Master via a callback connection.                                           |
| **ReducerHandler**          | One thread per incoming connection to the Reducer. Handles both `REGISTER_MAP` messages (message that initiates MapReduce operation) from the Master and `MAP_RESULT` messages from Workers.                                          |
| **Secure Random Generator** | Separate multithreaded TCP server that continuously generates cryptographically secure random numbers used during bet resolution, implementing a producer-consumer buffer. One Random Number Generator per Game.                      |

---

## MapReduce — Search Flow

When a player searches for games using filters (risk level, betting category, minimum stars), the system executes a full MapReduce pipeline:

```
1. Master generates a unique mapId
2. Master registers (mapId, workerCount) with Reducer
3. Master fans out MAP_TASK to all Workers in parallel (one thread per Worker) and freezes another thread waiting for Reducer's callback
4. Each Worker runs map() — emits (mapId, Game) for every game matching the filters
5. Each Worker sends MAP_RESULT directly to Reducer, then ACKs Master
6. Reducer accumulates results; when received == expected, triggers reduce()
7. reduce() de-duplicates the game list by name
8. Reducer sends REDUCER_CALLBACK to Master:5000
9. Master wakes the waiting ClientHandler thread via notify()
10. ClientHandler sends the final game list to the Player
```

Multiple concurrent searches run fully independently, each identified by their `mapId`.

This specific MapReduce flow happens for computing Provider and Player profit.

---

## Game Distribution

Games are distributed across Workers using consistent hashing:

```java
NodeId = Math.abs(GameName.hashCode()) % numberOfWorkers
```

This ensures each game always lands on — and is always retrieved from — the same Worker, with no coordination overhead.

---

## Game Data Format

Games are registered via JSON files:

```json
{  
  "GameName": "CosmicCrash",  
  "ProviderName": "ProviderA",  
  "Stars": 2,  
  "NoOfVotes": 60,  
  "GameLogo": "src/main/resources/images/cosmic_crash_logo.png",  
  "MinBet": 5.0,  
  "MaxBet": 500.0,  
  "RiskLevel": "high",  
  "HashKey": "cosmickey111"  
}
```

`BettingCategory` and `Jackpot` are computed by the system and are not part of the file.

---
## Configuration

All components are configured via `resources/config.properties`. Set the following properties before running:

```properties
# Master configuration
master.host=localhost  
master.port=5001  

# Worker configuration
worker.count=3  
worker.0.host=localhost  
worker.1.host=localhost  
worker.2.host=localhost  
worker.port=6001  

# Reducer configuration
reducer.host=localhost  
reducer.port=7001  

# Secure Random Generator configuration
srg.host=localhost  
srg.port=8001
```

---

## Running the Project

Each component is an independent Java process and can run on a separate machine. Before starting, make sure all addresses and ports are correctly set in `resources/config.properties`.

There is no specific order to run the components but all the components need to be active so the system works properly.

To run the project in a single machine, run **run.sh** script.

The number of Workers is arbitrary and configured at startup through the configuration file so no code changes are needed to add more nodes.

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

### System Features
- Horizontal scaling across arbitrary number of nodes
- Consistent hashing for deterministic game distribution
- MapReduce-based parallel search across all nodes
- Cryptographically secure random number generation for fair bet resolution
- Manual thread synchronisation for fine-grained control
- Producer-consumer pattern for secure random number generation

---

## Course Context

Built as the final project for the **Distributed Systems (3664)** course.

The course is a 3rd year mandatory course for the **Department of Informatics** (https://www.dept.aueb.gr/cs) of the **Athens University of Economics and Business** (https://www.aueb.gr).

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

