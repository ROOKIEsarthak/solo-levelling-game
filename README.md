# Solo Levelling

Turn real life into an RPG — quests, XP, ranks, and attributes for career, fitness, and discipline.

Offline-first Android MVP inspired by Solo Leveling. Meaningful actions become quests that award XP, raise attributes, and unlock ranks. Lifetime progress is permanent; an append-only XP ledger is the source of truth. Single local player — no backend or remote auth.

## Features

- **Onboarding** — name and weekly priorities; seeds today’s quest plan
- **Dashboard** — level, rank, XP bar, streak, today’s quests, adaptive suggestions
- **Quests** — complete / undo with TIMER, COUNT, and METRIC verification
- **Character** — profile, lifetime XP, ledger history, seven attributes (STR, END, INT, VIT, DISC, FOC, WIS)
- **Life modules** — DSA, workouts, nutrition, focus, journal, boss quests, skills, career nodes
- **Analytics** — weekly review, adaptive XP suggestions, JSON export
- **Achievements & streaks** — updated via domain events; grace days and recovery quests
- **Daily quest generation** — `DailyQuestWorker` schedules today’s plan
- **Day boundary** — `DayBoundaryWorker` handles midnight streak miss / recovery
- **Settings** — goals, schedule, targets; **Reset** clears progress and keeps name + configs
- **Anti-farming** — daily XP cap, idempotent completion, undo window

## Tech stack

| Layer | Choice |
|--------|--------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Persistence | `JsonDatabase` — JSON under `filesDir/db/` (Gson; **not Room**) |
| Async | Coroutines |
| Background | WorkManager |
| DI | Manual `AppContainer` |
| Architecture | Layered monolith + in-process EventBus |

## Architecture

```
UI (Compose + ViewModels)
    ↓
AppContainer (manual DI)
    ↓
Domain services + event handlers
    ↓
JsonDatabase (filesDir/db/*.json + tasks/)
```

- **Services** — quests, progression (`ProgressionService`), onboarding, modules, analytics, seasons, day boundary
- **EventBus** — `QuestCompleted`, `XpAwarded`, `LevelUp`, `StreakUpdated`, … → handlers for streaks, achievements, boss progress, notifications, sync outbox, seasons
- **V3 ports (offline no-ops)** — `MetricIngestPort`, `CalendarPort`, `SyncTransportPort`

Full product/technical spec: [`app/src/main/java/com/example/solo_levelling/docs/app-architecture.md`](app/src/main/java/com/example/solo_levelling/docs/app-architecture.md)

## Local data

Private app storage: `filesDir/db/`

| Path | Role |
|------|------|
| `user.json` | Profile + user configs (kept on reset) |
| `progress.json` | Level, XP, rank, streak, attributes |
| `xp_ledger.json` | Append-only XP source of truth |
| `tasks/task-*.json` | Quest instances (one file per quest) |
| `workouts.json`, `nutrition.json`, `journal.json`, … | Module tables |

**Reset** (Settings) calls `clearProgressTables()` — wipes XP, quests, streaks, achievements, and module logs; preserves name, priorities, timezone, and settings in `user.json`; then re-seeds baselines.

## Requirements

- **JDK 17 or 21** recommended (JDK 11+ builds; unit tests / Robolectric are unreliable on newer JDKs such as 25)
- Android Studio (or Android SDK)
- Device/emulator on **API 23+** (`minSdk 23`, `targetSdk 37`)
- Package: `com.example.solo_levelling`

## Build & run

```bash
./gradlew assembleDebug    # build debug APK
./gradlew installDebug     # install on connected device/emulator
./gradlew test             # JVM unit tests
```

Or open the project in Android Studio and run the `app` configuration.

If you previously installed a Room-based build, prefer a clean reinstall (`adb uninstall com.example.solo_levelling` then `installDebug`) so the JSON store is used.

## Project layout

```
app/src/main/java/com/example/solo_levelling/
├── core/          # SystemDefaults, AppClock, EventBus, DomainEvent
├── data/          # JsonDatabase, JsonFileIO, entities, seed
├── domain/        # services, handlers, pure logic, ports
├── ui/            # Compose screens + ViewModels (incl. Settings)
├── work/          # DailyQuestWorker, DayBoundaryWorker
├── notifications/
└── docs/          # architecture + implementation coverage
```

## Status

**Offline architecture complete** for product scenarios — quests, XP ledger, streaks, day-boundary workers, verification, modules, analytics, and Settings all run locally.

V3 network integrations remain **deferred** behind ports (wearables / Health Connect, calendar OAuth, backend sync). Scorecard: [`implementation-coverage.md`](app/src/main/java/com/example/solo_levelling/docs/implementation-coverage.md).
