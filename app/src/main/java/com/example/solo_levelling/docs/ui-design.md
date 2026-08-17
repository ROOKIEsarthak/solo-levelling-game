# Solo Levelling — UI Design Document

**Status:** Source of truth for UI that matches the **current app**  
**Platform:** Android (Jetpack Compose + Material 3)  
**Related:** [`app-architecture.md`](./app-architecture.md), [`implementation-coverage.md`](./implementation-coverage.md)

This document describes the product as implemented today so screens, copy, and flows stay consistent with real behavior and data—not an abstract redesign brief alone.

---

## 1. Product framing

**Solo Levelling** is an offline, single-user life operating system styled as an RPG “system.”

| Layer | What it means in UI |
|--------|---------------------|
| Plan | Weekly quest schedule, workout routine, calorie/protein targets |
| Execute | Complete quests, log sets, log meals, log steps/weight/journal |
| Track | History lists, XP ledger, weekly review |
| Progress | Level, rank, attributes, streaks, achievements, season XP |

**Design tension (intentional):**  
Gamification is present (XP, rank, boss, ARISE) but should feel like a serious personal system—not a childish game.

**Primary daily jobs:**

1. See status in seconds (Dashboard).
2. Clear today’s quests.
3. Log workout sets and meals quickly (especially on phone at the gym / kitchen).

---

## 2. Design principles

1. **System-first** — Use SYSTEM / Hunter / Quest language where it already exists; don’t invent a second product vocabulary on the same screens.
2. **Cards + hierarchy** — One primary card or section answers the question; secondary detail below.
3. **Fast logging** — Prefer inline fields and short forms over multi-step wizards.
4. **Dark, readable** — Default dark theme; high contrast text; restrained accent.
5. **Monospace presence** — Cascadia Code reinforces “terminal / system” identity.
6. **Don’t hide RPG depth** — Character, Modules, Achievements stay reachable; they are not decorative.
7. **JSON is truth** — UI reflects persisted state after restart; never treat Compose state as permanent storage.
8. **Event-driven feedback** — Level/rank overlays and snackbars react to domain events, not fake local-only success.

---

## 3. Brand & visual system

### 3.1 Brand moments

| Surface | Copy | Notes |
|---------|------|--------|
| Splash | **ARISE** / **起きろ** | Full-screen; ~3.2s gate; audio `okiro_deep` |
| Dashboard title | **SYSTEM** | Top app bar |
| Onboarding | **SYSTEM INITIALIZATION** | 3-step wizard |
| Default player name | **Hunter** | Fallback when name empty |

### 3.2 Color

**Visual source:** Solo Leveling–inspired dark futuristic SYSTEM (original palette). Dynamic Material You color is **off** so the brand palette stays fixed.

**Splash (shared tokens):**

| Role | Hex | Use |
|------|-----|-----|
| Background | `#05070D` | Splash (`SplashBackground`) |
| Glow purple | `#7C6CFF` | Sparse accent |
| Glow cyan | `#67D4FF` | Secondary glow + XP |

**App theme (Material 3 dark — blue/cyan SYSTEM):**

| Token | Hex | Role |
|-------|-----|------|
| Background | `#05070D` | Screen backdrop |
| Secondary background | `#080C16` | Sidebar / elevated plane |
| Surface (card) | `#0D1320` | `surfaceContainer` |
| Surface-2 | `#111827` | Nested rows (`surfaceContainerHigh`) |
| Primary | `#4DA3FF` | Accents, selected chips, XP bar |
| Cyan | `#67D4FF` | Progress / glow |
| Secondary purple | `#7C6CFF` | Sparse only |
| On-primary | `#051018` | Text on primary |
| Muted text | `#9AA3B2` | `onSurfaceVariant` |
| Outline | white @ 12% | Card / chip borders |
| Success | `#4ADE80` | Weekly progress, cleared |
| Warning | `#F5C84C` | Boss progress |
| Error | `#E35D5D` | Reset / destructive |
| Sidebar | `#080C16` | Bottom nav / rail |
| Default mode | Dark | `darkTheme = true` |
| Dynamic color | Off | `dynamicColor = false` |

**Semantic guidance:**

| Role | Intent |
|------|--------|
| Background | Near-black navy |
| Surface | Elevated card |
| Primary accent | System blue |
| Success | Green (completed / cleared) |
| Warning | Amber (boss, caps) |
| Danger | Red (reset, delete) |
| Text | Near-white |
| Secondary text | Muted gray |

Avoid neon overload and heavy multi-stop gradients outside splash / level-up.

### 3.3 Typography

| Item | Spec |
|------|------|
| Family | **Cascadia Code** (`cascadia_code_regular` / `cascadia_code_bold`) |
| Scale | Full Material 3 type scale, Cascadia on every slot |
| Hierarchy (target) | Page ~22–28sp · Section ~16–20sp · Body ~14–16sp · Meta ~12–14sp |

Do not mix a second display font without an explicit brand change.

### 3.4 Components in use

| Pattern | Where |
|---------|--------|
| `Card` | Almost every content block |
| `FilterChip` | Tabs / day / priority filters |
| `AssistChip` | Attribute strip on Dashboard |
| `LinearProgressIndicator` | XP, weekly %, boss |
| `OutlinedTextField` + `Button` / `OutlinedButton` | Forms |
| `AlertDialog` | Reset confirmation |
| `Snackbar` | Quest errors / messages |
| Bottom `NavigationBar` | Six main tabs |
| Overlay card | Level-up / rank-up (`LevelUpHost`) |

There is **no shared `ui/components/` library** yet; patterns are screen-local. New shared pieces should match these Material 3 primitives.

### 3.5 Spacing (observed)

- Screen padding ≈ **16.dp**
- Card padding ≈ **12–16.dp**
- Vertical stack gap ≈ **8–12.dp**
- Chip / button row gap ≈ **8.dp**

---

## 4. Information architecture

```
Splash (ARISE)
    ↓
Onboarding (if !onboardingDone)
    ↓
┌─────────────────────────────────────────────┐
│  Bottom navigation (main tabs)              │
│                                             │
│  Home ──────── Dashboard (SYSTEM)           │
│  Quests ────── Quests hub                   │
│  Self ──────── Character                    │
│  Gym ───────── Fitness (Workout + Diet)     │
│  Life ──────── Life Modules                 │
│  Review ────── Weekly Review / Analytics    │
│                                             │
│  Stack routes (no bottom bar):              │
│    Achievements  ← Dashboard trophy         │
│    Settings      ← Dashboard gear           │
└─────────────────────────────────────────────┘
         + LevelUpHost overlay (global)
```

### 4.1 Tab labels (exact)

| Tab UI label | Route | Icon concept |
|--------------|-------|--------------|
| Home | `dashboard` | Home |
| Quests | `quests` | TaskAlt |
| Self | `character` | Person |
| Gym | `fitness` | FitnessCenter |
| Life | `modules` | Spa |
| Review | `analytics` | Analytics |

### 4.2 Mapping to “Personal System” vocabulary

Use this table when aligning external UI prompts with the app:

| Personal System term | App screen / concept |
|----------------------|----------------------|
| Dashboard | Home / SYSTEM |
| Tasks | Quests (generated; complete/undo) |
| Workout | Gym → Fitness section |
| Diet | Gym → Diet section |
| Progress | Self + Review (+ streak/XP on Home) |
| History | XP ledger, workout/diet history tabs |
| Settings | Settings stack route |
| Profile | Settings fields + Character card |

---

## 5. Global flows

### 5.1 First launch

```
App start → Bootstrap (seed DB)
         → WelcomeSplash (≥ 3.2s AND ready)
         → Onboarding (3 steps)
         → Dashboard
```

### 5.2 Returning user

```
App start → Splash gate → Dashboard
```

### 5.3 Feedback

| Event | UI |
|-------|-----|
| Quest complete failure | Snackbar message |
| Level up / rank up | Bottom overlay card (`LevelUpHost`) |
| Reset | Confirm dialog; destructive action |
| Adaptive suggestion | Dismiss on card |

### 5.4 Empty / loading / error (current vs target)

| State | Current | Target for polish |
|-------|---------|-------------------|
| Empty | Inline copy (“No workout for this date”, “No active boss”, …) | Same copy + primary CTA button |
| Loading | Mostly absent (flows emit when ready) | Subtle skeletons on Dashboard / Review |
| Error | Snackbars; avoid raw exceptions | Short human message + retry where save fails |

---

## 6. Screen specifications

### 6.1 Welcome Splash

**Purpose:** Brand gate while bootstrap finishes.

| Element | Spec |
|---------|------|
| Background | `#1A0A2E` + purple/cyan glow animation |
| Primary text | `ARISE` |
| Secondary | `起きろ` |
| Actions | None |
| Exit | Automatic when ready + min duration |

---

### 6.2 Onboarding — SYSTEM INITIALIZATION

**Purpose:** Create player identity and quest schedule seeds.

| Step | Title | Content | Primary action |
|------|-------|---------|----------------|
| 1 | Who is the Player? | Name field | Next |
| 2 | Weekly priorities | Chips: career, fitness, discipline, focus, health | Next / Back |
| 3 | Schedule days | Chips MON–SUN; helper: active days for career & fitness quests | Initialize System / Back |

---

### 6.3 Dashboard — SYSTEM

**Purpose:** Answer “How am I doing?” in a few seconds.

**Top bar:** Title `SYSTEM` · Achievements icon · Settings icon

**Block order (top → bottom):**

1. **Profile card**
   - Name (or Hunter)
   - Goal title (if set)
   - `LEVEL {n}  ·  RANK {r}`
   - XP progress bar: `{xpIntoLevel} / {xpNeed} XP  ·  streak {s}`
   - Optional: `{season} · {seasonXp} season XP`

2. **Attributes** — horizontal AssistChips: `{CODE} {value}`  
   Codes: STR, END, INT, VIT, DISC, FOC, WIS

3. **This week**
   - Progress bar = weekly quest completion %
   - `{pct}% quests · 7d XP: {xp}`

4. **Active Boss** (if any) — title, progress, `{current} / {target}`

5. **Recent Achievements** — list of keys/cards

6. **Quick actions** (row)
   - Workout · Steps · Journal  
   (Today these are quick-log shortcuts, not full editors.)

7. **Today's Quests**
   - Card: title · `+{xp} XP · {status}`
   - Action: **Clear** (complete) or label **Cleared**

8. **System Suggestions**
   - Title + detail · **Dismiss**

**Design notes:**

- Daily “personal OS %” from the external prompt is **not** a single hardcoded widget yet; weekly % + XP + quest clears are the live signals.
- Prefer keeping this scrollable single column; avoid denser dashboards that hide Clear.

---

### 6.4 Quests

**Purpose:** Browse and complete quest instances.

| Element | Spec |
|---------|------|
| Title | `Quests` |
| Subline | `Available today: +{n} XP` |
| Tabs | Today · Weekly · Milestones · Recovery · Bosses |
| Quest card | Title, type, XP, status, optional verification line |
| Actions | **Complete** · **Undo** (when completed) |
| Bosses tab | Boss card, progress bar, child quest checklist |
| Empty boss | `No active boss` |

**Important product constraint:**  
Quests are **system-generated**. There is no create/edit/delete task form in the current UI. Designers must not assume free-form task CRUD unless product scope expands.

---

### 6.5 Character (Self)

**Purpose:** Lifetime progression readout.

| Section | Content |
|---------|---------|
| Profile card | Name, Level · Rank, lifetime XP, bar to next level |
| Attributes | Per-code cards: value + lifetime attribute XP |
| XP History | Ledger rows: amount, source type, timestamp |

**Actions:** Read-only.

---

### 6.6 Fitness (Gym)

**Purpose:** Plan and log training; log nutrition. One route, two stacked sections.

#### Page header

`Fitness`

#### A. Fitness (Workout) card

| Sub-tab | Content |
|---------|---------|
| **Today summary** | Workout name or `Not logged` · exercise/set counts · optional diet tip line |
| **Routine** | Day chips Mon–Sun · workout name · planned exercises · add exercise form (name, target muscle, sets, rep min/max) · Save day · Mark rest · reorder/remove |
| **Log** | Date ← → · name · sets (weight × reps, optional RPE) · Add set · unplanned exercise · Save · Delete |
| **History** | Last ~14 days · Compare exercise |

Key empty copy: `No workout for this date` · rest-day helper about Save day / Add exercise.

**Mobile priority:** Large targets, inline set entry, minimal navigation depth (log stays on this screen).

#### B. Diet card

| Sub-tab | Content |
|---------|---------|
| **Today totals** | Calories / Protein / Carbs / Fat — or `No diet logged today` |
| **Log Food** | Date ← → · meals (custom names) · foods (qty, unit, optional macros) · day total |
| **History** | Last ~14 days · `No diet history yet` |

Actions: Add meal · Add food · Delete · Save food.

**Product rule:** Meal names are free-form (Breakfast, Pre-workout, Custom, …)—not limited to three meals.

---

### 6.7 Life Modules

**Purpose:** Secondary trackers that still feed the system.

| Section | Primary actions |
|---------|-----------------|
| Career | Advance nodes |
| DSA | Add problem · Attempt · Solve · Master |
| Focus | Start timer · Log now |
| Steps | Log steps |
| Weight | Log weight (kg) |
| Journal | Save journal |
| Routines | Chips: Wake · Sleep · Read · Meditate |
| Boss Quests | Create boss · +25% progress |
| Skills | Filter by domain (unlock copy when empty) |

Header: `Life Modules`

---

### 6.8 Weekly Review (Review tab)

**Purpose:** Reflect on the week and export data.

| Block | Content |
|-------|---------|
| Title | `Weekly Review` |
| Season line | `{name} · {n} season XP` |
| Score card | Personal score /100 · quests completed/total · XP · DSA · workouts · boss · attributes · recommendations |
| Adaptive suggestions | Title + detail cards |
| Action | **Export data** |

---

### 6.9 Achievements

**Purpose:** Catalog of defs with locked/unlocked state.  
Opened from Dashboard trophy. No bottom bar.

---

### 6.10 Settings

**Purpose:** Profile, goals, admin, reset.

| Field / control | Label |
|-----------------|-------|
| Name | Player name |
| Vision | Goal title (vision) |
| Nutrition | Calorie target · Protein target (g) |
| Activity | Step target |
| Schedule | Schedule days (1=Mon … 7=Sun, CSV) |
| Notifications | On / Off toggle |
| Actions | Save config · Regenerate today's quests · Rebuild XP from ledger · Export data · Reset all progress |
| Read-only | System caps · Recent outbox events |

**Reset dialog**

- Title: `Reset all progress?`
- Clears: XP, quests, streaks, achievements, module logs (workout/diet history, metrics, etc.)
- Preserves: name, configs, onboarding flag; workout **routine** survives reset
- Actions: Cancel · Reset

---

### 6.11 Level-up overlay

Global host above navigation.

| Event | Presentation |
|-------|----------------|
| Level up | Card: LEVEL UP messaging |
| Rank up | Card: RANK UP messaging |

Non-blocking; dismissible / auto-clear per implementation.

---

## 7. Interaction patterns

### 7.1 Date navigation

Used on workout log and diet log:

```
‹   {date}   ›
```

History is day-keyed JSON; UI must never assume “today only.”

### 7.2 Completion language

| Context | Preferred verb |
|---------|----------------|
| Dashboard quest | **Clear** |
| Quests screen | **Complete** / **Undo** |
| Completed state | **Cleared** / COMPLETED status |

### 7.3 Workout logging (UX contract)

```
Open Gym → Log tab → (optional) Start from routine
  → Inline weight/reps per set → Add set → Save
```

Avoid forcing Routine → Day → Exercise → Edit → Log as a long chain for the common path.

### 7.4 Diet logging (UX contract)

```
Add meal → Add food → quantity (+ optional macros) → Save
```

Single screen; no wizard.

---

## 8. Content & voice

| Do | Don’t |
|----|-------|
| SYSTEM, Hunter, Quest, XP, Rank, Boss | Generic “todo app” copy on the same screens |
| Short status lines (`+40 XP · ACTIVE`) | Long paragraphs on cards |
| Action verbs: Clear, Complete, Save, Log, Dismiss | Joke-y / meme gamification |
| Japanese only on splash (`起きろ`) | Random JP elsewhere unless intentional |

---

## 9. Accessibility

Target checklist (Material + Compose):

- Content descriptions on icon-only buttons (Achievements, Settings already labeled).
- Chip/button hit targets ≥ 48.dp where practical (critical on Gym set entry).
- Don’t rely on color alone for completed vs pending (use text status).
- Form fields have visible labels (OutlinedTextField labels).
- Sufficient contrast on dark surfaces; avoid low-contrast purple-on-purple body text.
- Snackbars and dialogs readable by screen readers (standard M3).

---

## 10. Responsive behavior

| Breakpoint | Layout |
|------------|--------|
| Phone (primary) | Bottom navigation + scroll columns |
| Tablet / wide (future) | Prefer `NavigationRail` or sidebar; keep same destinations |
| Landscape gym use | Keep Log tab usable; sticky save if added later |

Desktop sidebar from external prompts is **aspirational**; ship phone patterns first.

---

## 11. Data the UI must respect

UI must go through services / DAOs (via `AppContainer`), not raw files.

| Domain | On-disk (concept) | UI surfaces |
|--------|-------------------|-------------|
| User + configs | `user.json` | Settings, Dashboard name/goal |
| Progress | `progress.json` + `xp_ledger.json` | Dashboard, Character, Review |
| Quests | `tasks/task-*.json` | Dashboard, Quests |
| Workout routine | `workouts/routine.json` | Gym → Routine |
| Workout logs | `workouts/logs/{date}.json` | Gym → Log / History |
| Diet | `diet/logs/{date}.json` | Gym → Diet |
| Weight / steps | `metrics.json` | Life Modules; quick Steps on Home |

**Reset:** Clear tracking/progress; keep profile + configs; keep routine.

---

## 12. Design backlog (aligned to app, not a rewrite)

Prioritized polish that stays compatible with current architecture:

1. **Dashboard density** — Clearer “today” hierarchy (quests + gym/diet snapshot) without removing XP/rank.
2. **Gym split affordance** — Stronger visual separation of Workout vs Diet (still one tab if needed).
3. **Empty states + CTAs** — Every empty block gets one next action.
4. **Mobile set entry** — Larger inputs, previous performance reference, sticky Save.
5. **Loading skeletons** — Dashboard + Review.
6. **Toast consistency** — Success: workout saved, meal added, quest cleared.
7. **Optional calendar history** — Indicators for quest day / workout / diet (port exists as no-op today).
8. **Shared components** — Extract only when used 2+ times (`EmptyState`, `DateSelector`, `ProgressCard`).

Out of scope unless explicitly approved: new database, removing XP/quests, replacing EventBus, inventing freestyle Tasks CRUD without domain support.

---

## 13. Screen wireframe sketches (ASCII)

### Dashboard

```
┌─────────────────────────────┐
│ SYSTEM              🏆  ⚙   │
├─────────────────────────────┤
│ ┌ Hunter ───────────────┐   │
│ │ LEVEL 12 · RANK D     │   │
│ │ ████████░░  820/1000  │   │
│ │ streak 7              │   │
│ └───────────────────────┘   │
│ [STR 12][END 10][INT 14]…   │
│ ┌ This week ────────────┐   │
│ │ ███████░░  72% · XP   │   │
│ └───────────────────────┘   │
│ [Workout] [Steps] [Journal] │
│ Today's Quests              │
│ ┌ Clear DSA ──── [Clear] ┐  │
│ └────────────────────────┘  │
├─────────────────────────────┤
│ Home Quests Self Gym Life … │
└─────────────────────────────┘
```

### Gym (Workout Log focus)

```
┌─────────────────────────────┐
│ Fitness                     │
│ ┌ Fitness ──────────────┐   │
│ │ Today · Chest+Tris    │   │
│ │ [Routine][Log][Hist]  │   │
│ │ ‹ Aug 16 ›            │   │
│ │ Bench Press           │   │
│ │ 1  [60] kg  [10] reps │   │
│ │ 2  [60] kg  [ 9] reps │   │
│ │ [+ Add set]  [Save]   │   │
│ └───────────────────────┘   │
│ ┌ Diet ─────────────────┐   │
│ │ 1240 / 1800 kcal      │   │
│ │ [Log Food][History]   │   │
│ └───────────────────────┘   │
└─────────────────────────────┘
```

---

## 14. Definition of “UI close to the app”

A design is faithful when it:

- [ ] Uses the six-tab IA (or an explicit remapping table).
- [ ] Keeps SYSTEM / XP / Level / Rank / Quest / Boss language on core loops.
- [ ] Shows quests as completable items, not a blank task manager.
- [ ] Treats workout routine and workout log as separate modes.
- [ ] Allows arbitrary meal names and optional macros.
- [ ] Surfaces weight/steps/journal under Life (or clearly linked quick actions).
- [ ] Includes Settings reset semantics (progress gone, profile kept).
- [ ] Defaults to dark + Cascadia + card/chip patterns.
- [ ] Prioritizes gym/diet logging speed on mobile.

---

## 15. Document maintenance

| When | Update |
|------|--------|
| Nav labels or routes change | §4 |
| New screen ships | §6 |
| Theme tokens change | §3 |
| Quest CRUD or Diet split becomes real | §6.4 / §6.6 + §12 |

**Owner:** App maintainers  
**Last aligned to codebase:** 2026-08-16 (Solo Leveling blue/cyan progression redesign)
