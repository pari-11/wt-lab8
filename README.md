# WT Lab 8: Real-time Cricket Score Management System

A Spring Boot application that simulates a live cricket scoring platform. It handles ongoing matches, ball-by-ball score updates, player statistics and match summaries. The data is served through REST APIs and shown on a responsive dashboard that updates live using Server-Sent Events (SSE).

## Running the Spring Boot app (`cricket-score-app/`)

Requirements: **Java 17+** (21 recommended) and **Maven 3.9+**.

```bash
cd cricket-score-app
mvn spring-boot:run
```

Then open **http://localhost:8080**.

To run the tests: `mvn test`.

On startup the app loads 4 teams and 4 matches: one completed, two live and one upcoming. A built-in simulator bowls one delivery in every live match every 3 seconds. You can change this in `src/main/resources/application.properties`.

## REST API

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/matches?status=LIVE` | All matches, with an optional status filter |
| GET | `/api/matches/{id}` | Full scorecard, live state and recent commentary |
| GET | `/api/matches/{id}/commentary?limit=50` | Ball-by-ball commentary |
| GET | `/api/matches/{id}/summary` | Result, scores, top batters and bowlers |
| POST | `/api/matches` | Create a match: `{"teamAId":"ind","teamBId":"nz","venue":"Chennai","overs":20}` |
| POST | `/api/matches/{id}/start` | Toss, then the match goes live |
| POST | `/api/matches/{id}/balls` | Record a delivery: `{"runs":4,"extra":"NONE","dismissal":null}` |
| GET | `/api/teams`, `/api/teams/{id}/players` | Teams and their squads with stats |
| GET | `/api/players`, `/api/players/{id}` | Player career statistics |
| GET | `/api/players/leaderboard?category=runs` | Leaderboard by `runs`, `wickets`, `sixes` or `strikeRate` |
| GET | `/api/stream` | SSE stream that sends a `match` event after every score change |
| GET/POST | `/api/simulator`, `/api/simulator/pause`, `/api/simulator/resume` | Control the live feed |

`extra` can be `NONE`, `WIDE`, `NO_BALL`, `BYE` or `LEG_BYE`. `dismissal` can be `BOWLED`, `CAUGHT`, `LBW`, `RUN_OUT`, `STUMPED`, or `null`.

## Project structure

```
cricket-score-app/src/main/java/com/wtlab/cricket/
  model/       Team, Player, Match, Innings (scoring rules), BallEvent, ...
  repository/  Thread-safe in-memory stores
  service/     ScoringService, MatchService, StatsService, MatchSimulator,
               LiveUpdatePublisher (SSE), MatchViewMapper, DataSeeder
  web/         REST controllers and the global exception handler
  dto/         Request bodies and JSON views
src/main/resources/static/   Dashboard (HTML/CSS/JS)
```

## `frontend/`

The dashboard with a lightweight Node.js server (`node server.js`, then open http://localhost:8080) that serves the same `/api` endpoints as the Spring Boot backend.
