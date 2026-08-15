# Solo Levelling

Turn real life into an RPG — quests, XP, ranks, and attributes for career, fitness, and discipline.

Offline-first Android MVP inspired by Solo Leveling. Meaningful actions become quests that award XP, raise attributes, and unlock ranks. Lifetime progress is permanent; an append-only XP ledger is the source of truth.

## Features

- **Onboarding** — set your name and weekly priorities
- **Dashboard** — level, rank, XP bar, streak, today’s quests, adaptive suggestions
- **Quests** — complete or undo quest instances
- **Character** — profile, lifetime XP, and seven attributes (STR, END, INT, VIT, DISC, FOC, WIS)
- **Life modules** — DSA, workouts, nutrition, focus, journal, boss quests, skills
- **Analytics** — weekly review, adaptive XP suggestions, JSON export
- **Achievements & streaks** — unlocked via domain events
- **Daily quest generation** — WorkManager schedules today’s plan
- **Anti-farming** — daily XP cap, idempotent completion, undo window

## Tech stack

| Layer | Choice |
|--------|--------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Persistence | Room |
| Async | Coroutines |
| Background | WorkManager |
| Architecture | Layered monolith + in-process EventBus |

## Architecture

```
UI (Compose + ViewModels)
    ↓
AppContainer (manual DI)
    ↓
Domain services + event handlers
    ↓
Room (entities, DAOs, XP ledger)
```

Domain events (`QuestCompleted`, `XpAwarded`, `LevelUp`, …) flow through `EventBus`. Handlers update streaks and achievements asynchronously.

Full product/technical spec: [`app/src/main/java/com/example/solo_levelling/docs/app-architecture.md`](app/src/main/java/com/example/solo_levelling/docs/app-architecture.md)

## Requirements

- JDK 11+
- Android Studio (or Android SDK)
- Device/emulator on **API 23+** (`minSdk 23`, `targetSdk 37`)
- Package: `com.example.solo_levelling`

## Build & run

```bash
./gradlew assembleDebug    # build debug APK
./gradlew installDebug     # install on connected device/emulator
./gradlew test             # unit tests
```

Or open the project in Android Studio and run the `app` configuration.

## Project layout

```
app/src/main/java/com/example/solo_levelling/
├── core/          # config, EventBus, clock
├── data/          # Room DB, seed data
├── domain/        # services, handlers, models
├── ui/            # Compose screens + ViewModels
├── work/          # DailyQuestWorker
├── notifications/
└── docs/          # architecture / product spec
```

## Status

Local single-player MVP. No backend or auth yet — see the architecture doc for roadmap ideas.
