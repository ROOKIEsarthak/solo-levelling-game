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
- **Achievements & streaks** — updated via domain events; streak break UX via `StreakRecoveryHost` (Recovery Quest feature removed)
- **Daily quest generation** — `DailyQuestWorker` + day-boundary catch-up
- **Day boundary** — next-local-midnight `DayBoundaryCoordinator` / `DayBoundaryWorker` (not a fixed 24h interval)
- **Settings** — goals, schedule, targets; **Reset** clears progress and keeps name + configs
- **Anti-farming** — daily XP cap, idempotent completion, undo window

## Tech stack

| Layer | Choice |
|--------|--------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Persistence | `JsonDatabase` — JSON under `filesDir/db/` (Gson; **not Room**) |
| Async | Coroutines |
| Background | WorkManager (one-time midnight boundary + daily quest gen) |
| DI | Manual `AppContainer` |
| Architecture | Layered monolith + EventBus for optional side effects |

## Architecture

```
UI (Compose + thin ViewModels)
    ↓
AppContainer (manual DI)
    ↓
Domain services + PostQuestCompletionCoordinator + handlers
    ↓
JsonDatabase (filesDir/db/*.json + tasks/)
```

### CURRENT ownership (canonical)

| Concern | Owner |
|---------|--------|
| Day boundary / midnight | `DayBoundaryCoordinator` + `DayBoundaryService`; Worker is thin |
| Active module reads | `ActiveProgressionReader` + `ModuleScope` |
| Boss progress | `BossProgressHandler` + `BossProgressLogic` |
| Quest XP / status | `QuestCompletionService` |
| Critical post-complete | `PostQuestCompletionCoordinator` (streak, boss, achievements, deps, season) |
| Optional side effects | EventBus → notifications / UI overlays |
| Sync | **None** (outbox removed until a real transport exists) |

### FUTURE product intent (not current)

Auth, remote backend, Room/DataStore, real sync transport.

- **EventBus** — optional/async listeners after critical post-completion orchestration
- **V3 ports (offline no-ops)** — `MetricIngestPort`, `CalendarPort`

Canonical technical reference: [`docs/architecture-and-system-design.md`](docs/architecture-and-system-design.md). Product/technical spec (PRD, prefer code when they disagree): [`app/src/main/java/com/example/solo_levelling/docs/app-architecture.md`](app/src/main/java/com/example/solo_levelling/docs/app-architecture.md)

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
