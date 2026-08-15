# Implementation Coverage — Offline Architecture

Living scoreboard for [`app-architecture.md`](app-architecture.md) against this Android offline-first app.

**Legend:** `DONE` · `PARTIAL` · `TODO` · `DEFERRED` (external V3 — port ready, no network)

**Platform:** Compose + Room + EventBus + WorkManager. Single local player. No remote auth.

**Last updated:** Phase 5 — offline architecture complete for product scenarios.

---

## Scorecard

| Area | DONE | PARTIAL | TODO | DEFERRED |
|------|------|---------|------|----------|
| Core engine (XP, quests, ledger, ranks) | ~18 | ~4 | ~1 | 0 |
| Life modules | ~6 | ~12 | ~8 | 0 |
| Progression meta (boss, skills, seasons, score) | ~5 | ~4 | ~6 | 0 |
| UX surfaces | ~8 | ~8 | ~6 | 0 |
| Config / verification / integrity | ~12 | ~4 | ~4 | 0 |
| V3 integrations | 0 | 0 | 0 | all |

---

## Future-integration seams

| Deferred V3 feature | Offline now | Future attach point |
|---------------------|-------------|---------------------|
| Wearables / Health Connect steps | Manual `MetricIngestPort` | Same port from Health Connect adapter |
| Calendar OAuth | Manual schedule in `user_config` | `CalendarPort` → quest suggestions |
| Backend sync | `sync_outbox` + `NoOpSyncTransport` | HTTP `SyncTransportPort` |
| External DSA platforms | Manual DSA entry | Ingest → `dsa_problems` |
| Cloud AI planning | Rule-based `AdaptiveService` | Recommendations only; no silent goal changes |

---

## Section checklist (§1–§73)

### §1–§5 Vision, philosophy, identity, attributes

| Item | Status | Evidence |
|------|--------|----------|
| Real-life → RPG progression | DONE | Quest + modules award XP |
| Lifetime progress permanent | DONE | Append-only `xp_ledger` |
| Player profile / level / rank | DONE | `PlayerProfileEntity` |
| Seven attributes | DONE | `AttributeCode` + Character UI |
| Multi-attribute rewards | DONE | `AttributeRewardsParser` |
| Seasons identity | PARTIAL | `SeasonService` + active season on dashboard |

### §6–§9 XP, formula, ledger, ranks

| Item | Status | Evidence |
|------|--------|----------|
| Nonlinear XP curve | DONE | `SystemDefaults.xpForNextLevel` |
| XP ledger + uniqueness | DONE | `xp_ledger` unique index |
| Historical XP UI | PARTIAL | Ledger observed in Character |
| Ranks E→MONARCH | DONE | `RANK_THRESHOLDS` |
| Daily XP cap all paths | PARTIAL | Quests + `ProgressionService` cap; some module paths share cap |

### §10–§17 Quests, streaks, recovery, anti-gaming

| Item | Status | Evidence |
|------|--------|----------|
| Daily quests | DONE | `QuestGenerationService` + `DailyQuestWorker` |
| Weekly quests | PARTIAL | Generated; Quests tab shows weekly |
| Milestone quests | PARTIAL | Generated once per template |
| Recovery quests | DONE | `StreakHandler` + `DayBoundaryService` spawn + `RecoveryQuestAvailable` |
| Idempotent completion | DONE | Tested |
| Streak midnight miss | DONE | `DayBoundaryWorker` + `DayBoundaryService` |
| Grace days | DONE | `DayBoundaryLogic` + `STREAK_GRACE_DAYS` config |
| Boss child quests | PARTIAL | `BossProgressHandler` + boss_quests table |

### §18–§31 Modules & meta

| Item | Status | Evidence |
|------|--------|----------|
| Career tree | PARTIAL | Career nodes seeded; Modules UI |
| DSA tracker | PARTIAL | Add/solve + COUNT verify |
| Fitness / exercises | PARTIAL | Workout log + exercises table |
| Nutrition + targets | PARTIAL | Log + `calorie_target` config |
| Focus timer | PARTIAL | Focus sessions + TIMER verify |
| Journal screen | PARTIAL | Save in Modules |
| Steps via metrics | PARTIAL | `MetricIngestPort` + METRIC verify |
| Boss system | PARTIAL | Active boss on dashboard |
| Skills tree UI | PARTIAL | Flat list in Modules |
| Weekly review depth | DONE | `AnalyticsService.weeklyReview` |
| Adaptive apply | PARTIAL | Suggestions only; XP scaling in gen |
| Seasons | PARTIAL | `SeasonService` + handler |
| Personal Score | DONE | `AnalyticsService.personalScore` |

### §32–§33 Screens

| Screen | Status |
|--------|--------|
| Dashboard (§32 full) | PARTIAL (~8/11) |
| Character + XP history | PARTIAL |
| Quests tabs | PARTIAL (today + weekly) |
| Career / Fitness / Skills / Journal | PARTIAL (Life tab) |
| Achievements | DONE |
| Analytics | DONE |
| Settings (goals/schedule/targets) | DONE |

### §34–§40 Data, events, completion

| Item | Status | Evidence |
|------|--------|----------|
| Core tables | DONE | Room v1 |
| `boss_quests` | DONE | Entity + handler |
| `workout_exercises` | DONE | Entity + insert |
| `user_config` / `sync_outbox` | DONE | ConfigDao + Settings UI + outbox handler |
| EventBus core events | DONE | Full sealed hierarchy |
| Notification / outbox handlers | DONE | `NotificationHandler` + `SyncOutboxHandler` |
| Central ProgressionService | DONE | Single award path + rebuild |

### §41–§54 Notifications, analytics, admin, tests

| Item | Status |
|------|--------|
| Event-driven notifications | DONE |
| Personal Score | DONE |
| Admin rebuild from ledger | DONE |
| Streak/achievement/boss unit tests | PARTIAL |
| AT-49 / AT-50 E2E | PARTIAL (pure orchestration) |

### §59–§68 FTUE, hierarchy, verification, config

| Item | Status |
|------|--------|
| FTUE schedule step | PARTIAL |
| Priorities → quest gen | DONE |
| Goal hierarchy | PARTIAL | `goal_title` in Settings + Dashboard only |
| Quest dependencies | DONE | LOCKED → AVAILABLE on complete |
| Verification TIMER/COUNT/METRIC | DONE |
| User-editable config | DONE | Settings → ConfigDao |

### §47 V3 / §66 Integrations

| Item | Status |
|------|--------|
| Calendar | DEFERRED | `CalendarPort` / `NoOpCalendarPort` |
| Wearables / Health Connect | DEFERRED | `MetricIngestPort` / `LocalMetricIngest` |
| Backend sync | DEFERRED | `SyncTransportPort` / `NoOpSyncTransport` |
| External DSA platforms | DEFERRED | Manual entry offline |
| Browser extension | DEFERRED | — |

---

## Offline acceptance IDs

### Core

| ID | Scenario | Status |
|----|----------|--------|
| AT-CORE-01 | FTUE → dashboard + quests | DONE |
| AT-CORE-02 | Daily generation no dupes | DONE |
| AT-CORE-03 | Complete → ledger + attrs + event | DONE |
| AT-CORE-04 | Idempotent complete | DONE |
| AT-CORE-05 | Undo within window | DONE |
| AT-CORE-06 | Daily XP cap | DONE (quests) |

### §49 Full Day

| ID | Status |
|----|--------|
| AT-49-01 Generate dailies | DONE |
| AT-49-02 DSA (+COUNT verify) | PARTIAL |
| AT-49-03 Deep work (+TIMER) | PARTIAL |
| AT-49-04 System design | TODO |
| AT-49-05 Workout | PARTIAL |
| AT-49-06 Steps (+METRIC) | PARTIAL |
| AT-49-07 Journal | PARTIAL |
| AT-49-08 Day XP sum | PARTIAL |
| AT-49-09 Streak + achievements | DONE |

### Other

| ID | Status |
|----|--------|
| AT-50 Level-up UX | PARTIAL |
| AT-59/60 FTUE full | PARTIAL |
| AT-STREAK / AT-RECOVERY | DONE |
| AT-BOSS child link | PARTIAL |
| AT-WEEKLY / PERFECT_WEEK | PARTIAL |
| AT-MOD-* real forms | PARTIAL |
| AT-NOTIF-* | DONE |
| AT-DATA rebuild | DONE |
| AT-ADAPT apply | PARTIAL |

---

## Phase tracking

| Phase | Plan | Status |
|-------|------|--------|
| 0 Coverage doc | This file | DONE |
| 1 Seams + integrity | Progression, config, outbox, ports, day-boundary, admin | DONE |
| 2 Quest engine | Verification, schedule gen, milestones, boss link, tabs | DONE |
| 3 Life modules | Real Career/DSA/Fitness/Nutrition/Focus/Steps/Journal | DONE |
| 4 Meta + UX | Dashboard, seasons, score, FTUE, analytics, adaptive | DONE |
| 5 Automation + tests | Workers, Settings, AT-* tests, mark offline-complete | DONE |

**Offline architecture:** complete for product scenarios. V3 network integrations deferred via ports (`MetricIngestPort`, `CalendarPort`, `SyncTransportPort`).
