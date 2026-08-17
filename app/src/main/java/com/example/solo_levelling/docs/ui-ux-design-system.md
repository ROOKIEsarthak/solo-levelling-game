# Solo Levelling — UI/UX Design System (Current Behavior)

**Status:** Evidence-based documentation of the UI as implemented today  
**Platform:** Android (Jetpack Compose + Material 3)  
**Scope:** Current behavior only — not aspirational redesign  
**Related:** [`app-architecture.md`](./app-architecture.md), [`implementation-coverage.md`](./implementation-coverage.md), [`ui-design.md`](./ui-design.md) (older / partially stale)

This document describes how the app looks, feels, and guides users based on inspection of Compose screens, theme tokens, and navigation wiring. Assumptions are labeled as **inferred**; everything else is **observed**.

---

## Table of contents

1. [Application overview](#1-application-overview)
2. [Overall design philosophy](#2-overall-design-philosophy)
3. [Design system](#3-design-system)
4. [Layout system](#4-layout-system)
5. [Navigation & information architecture](#5-navigation--information-architecture)
6. [Individual screen analysis](#6-individual-screen-analysis)
7. [Component-level design analysis](#7-component-level-design-analysis)
8. [Interaction design](#8-interaction-design)
9. [Motion & animation](#9-motion--animation)
10. [Forms & input UX](#10-forms--input-ux)
11. [Feedback & system states](#11-feedback--system-states)
12. [Gamification / progress UX](#12-gamification--progress-ux)
13. [Accessibility](#13-accessibility)
14. [Responsive design](#14-responsive-design)
15. [Design tokens & styling architecture](#15-design-tokens--styling-architecture)
16. [UX patterns & design patterns](#16-ux-patterns--design-patterns)
17. [Consistency analysis](#17-consistency-analysis)
18. [UX strengths](#18-ux-strengths)
19. [UX/UI issues & improvements](#19-uxui-issues--improvements)
20. [Developer design guide](#20-developer-design-guide)
21. [Codebase reference map](#21-codebase-reference-map)
22. [Final design system summary](#22-final-design-system-summary)

---

## 1. Application overview

### Observed product purpose

**Solo Levelling** is an offline, single-user life OS styled as an RPG “SYSTEM.” Users plan (modules, schedules, targets), execute (quests, workouts, meals, life logs), and track progression (XP, level, rank, streaks, achievements).

### Frontend architecture (observed)

| Layer | Implementation |
|--------|----------------|
| UI | Jetpack Compose screens under `ui/` |
| Theme | Material 3 via `SololevellingTheme` |
| Navigation | Single `NavHost` in `SoloLevellingAppRoot` |
| State | ViewModels + `collectAsStateWithLifecycle` (most screens); some screens use local `remember` + `LaunchedEffect` |
| Feedback | Global `SnackbarHost`; event-driven overlays (`LevelUpHost`, `StreakRecoveryHost`) via domain EventBus |
| Persistence | JSON DB; UI reflects persisted state after restart |

### High-level UI structure map

```
MainActivity
└── SololevellingTheme
    └── SoloLevellingAppRoot
        ├── WelcomeSplash          (gate: bootstrap ready + ≥3.2s)
        └── Scaffold
            ├── FAB (Dashboard only)
            ├── Bottom NavigationBar  OR  NavigationRail (≥840dp)
            ├── SnackbarHost
            └── Box
                ├── NavHost (routes below)
                ├── LevelUpHost
                └── StreakRecoveryHost
```

### Major screens / routes

| Route | Screen | Nav tier |
|-------|--------|----------|
| `onboarding` | OnboardingScreen | Gate (bar hidden) |
| `dashboard` | DashboardScreen | Primary |
| `career` | CareerScreen | Primary (if career module on) |
| `fitness` | FitnessScreen(Workout) | Primary (if workout on) |
| `nutrition` | FitnessScreen(Diet) | Primary (if diet on) |
| `more` | MoreScreen | Primary (always) |
| `quests` | QuestsScreen | Secondary |
| `history` | HistoryScreen | Secondary |
| `character` | CharacterScreen | Secondary |
| `achievements` | AchievementsScreen | Secondary |
| `modules` | ModulesScreen | Secondary |
| `analytics` | AnalyticsScreen | Secondary |
| `settings` | SettingsScreen | Secondary |

### Reusable UI components

**Observed:** There is **no** `ui/components/` package. Reuse is limited to:

- Theme tokens (`ui/theme/`)
- Material 3 primitives (`Card`, `FilterChip`, `Button`, `OutlinedButton`, `OutlinedTextField`, `LinearProgressIndicator`, `AlertDialog`, …)
- File-local private composables (e.g. `QuestCard`, `CareerCard`, `MoreRow`, `DateStrip`)

### Styling technology

- Compose modifiers + `MaterialTheme.colorScheme` / `typography`
- Raw theme color vals for success/warning/glow
- No CSS, Tailwind, or styled-components
- No custom `Shapes` theme object

---

## 2. Overall design philosophy

### Observed visual style

- **Dark, futuristic “SYSTEM” UI** — near-black navy backgrounds, cyan/blue accents, sparse purple glow on brand moments
- **Gamified productivity** — RPG vocabulary (SYSTEM, Hunter, Missions, LEVEL UP) layered on logging/tracking workflows
- **Monospace identity** — Cascadia Code on the full Material 3 type scale
- **Flat outlined cards** — depth via surface tiers + 1.dp borders, not shadows
- **ALL-CAPS section labels** with letter-spacing on many hub screens

### Visual hierarchy (observed)

1. Brand / system chrome (`SYSTEM`, `◈ SYSTEM`, splash ARISE)
2. Page titles (`headlineSmall` + bold)
3. Highlight cards (priority / level) with primary-tinted borders
4. Standard content cards
5. Meta text (`onSurfaceVariant`, `bodySmall` / `labelSmall`)

### Consistency mechanisms (observed)

- Shared dark color scheme applied once at activity root
- Repeated card recipe: `surfaceContainer` + `BorderStroke(1.dp, outline)`
- Repeated chip recipe: selected fill `primary @ 0.15α`
- Module flags control which primary tabs and dashboard sections appear
- Global snackbar + event overlays for cross-screen feedback

### How UI communicates purpose (observed)

Splash (“ARISE” / “起きろ”), onboarding (“SYSTEM INITIALIZATION”), dashboard title “SYSTEM”, More hub “◈ SYSTEM”, and motivational copy from `SystemMessages` frame the app as a personal RPG system rather than a generic tracker.

### Observed vs inferred

| | |
|--|--|
| **Observed** | Dark M3 theme; Cascadia; card + chip recipes; module-gated tabs; XP/level/streak UI; event overlays |
| **Inferred intention** | Feel like a serious Solo Leveling–inspired “system interface,” not a playful cartoon game; keep daily logging fast under that skin |

---

## 3. Design system

### 3.1 Colors

**Defined in:** `ui/theme/Color.kt`  
**Mapped in:** `ui/theme/Theme.kt` → `DarkColorScheme`  
**Applied via:** `SololevellingTheme(darkTheme = true, dynamicColor = false)` — dynamic color parameter is ignored.

#### Raw tokens

| Token | Hex | Role (observed usage) |
|-------|-----|------------------------|
| `SystemBackground` | `#05070D` | Screen backdrop; overlay scrim |
| `SystemSecondaryBackground` | `#080C16` | M3 `surface` |
| `SystemForeground` | `#F5F4F7` | Primary text |
| `SystemSurface` | `#0D1320` | Cards (`surfaceContainer`) |
| `SystemSurface2` | `#111827` | Nested/highlight cards (`surfaceContainerHigh`) |
| `SystemMuted` | `#1A2233` | Highest surface / secondary containers |
| `SystemMutedForeground` | `#9AA3B2` | Secondary text |
| `SystemPrimary` | `#4DA3FF` | Accents, selected chips, CTAs |
| `SystemOnPrimary` | `#051018` | Text on primary buttons |
| `SystemCyan` / `SystemTertiary` / `GlowCyan` | `#67D4FF` | XP glow, progress accents |
| `SystemSecondary` / `GlowPurple` | `#7C6CFF` | Splash glow / purple accent |
| `SystemOutline` | white @ ~12% | Card/chip borders |
| `SystemAccent` | `#152238` | Primary/tertiary containers |
| `SystemSuccess` | `#4ADE80` | Completed, unlocked, positive metrics |
| `SystemWarning` | `#F5C84C` | Boss progress |
| `SystemError` | `#E35D5D` | Errors, destructive actions |
| `SystemSidebar` | `#080C16` | Nav bar/rail |
| `SplashBackground` | `#05070D` | Splash (= background) |
| `SystemOnSuccess` / `SystemOnWarning` | defined | **Not used in UI screens observed** |

#### Semantic roles in UI

| Role | Color | Where |
|------|-------|-------|
| Background | `#05070D` | Scaffold, splash, overlays |
| Card surface | `#0D1320` | Most cards |
| Highlight surface | `#111827` | Priority cards, nested blocks |
| Primary accent | `#4DA3FF` | Buttons, chips, borders, labels |
| XP / glow | `#67D4FF` | XP bars, streak text accents, level number |
| Success | `#4ADE80` | Cleared quests, unlocked badges, “NOW” analytics |
| Warning | `#F5C84C` | Boss progress bar |
| Error | `#E35D5D` | Validation text, reset button |
| Muted text | `#9AA3B2` | Subtitles, meta |

#### Gradients / transparency (observed)

- Splash: radial purple → cyan → transparent, pulsed alpha
- Level-up / streak overlays: `SystemBackground` @ 0.88–0.92α + cyan radial glow
- Chip selected: `primary.copy(alpha = 0.15f)`
- Nav indicator: `primary.copy(alpha = 0.18f)`
- Accent card borders: `primary.copy(alpha = 0.35f–0.5f)`
- Locked achievements: `surfaceContainer.copy(alpha = 0.5f)`

**Light scheme:** stub only (`primary`/`secondary`/`tertiary`). App always defaults to dark; light is not a productized mode.

### 3.2 Typography

**Defined in:** `ui/theme/Type.kt`  
**Font files:** `res/font/cascadia_code_regular.ttf`, `cascadia_code_bold.ttf`

| Aspect | Observed |
|--------|----------|
| Family | Cascadia Code on all 15 M3 slots |
| Sizes / line heights | Material 3 defaults (not customized in code) |
| Page titles | Typically `headlineSmall` + `FontWeight.Bold` |
| Section labels | Bold + `letterSpacing` 2–6.sp, often ALL CAPS |
| Body | `bodySmall` / `bodyMedium` |
| Meta | `labelSmall` / `onSurfaceVariant` |
| Splash | Custom: ARISE 52.sp / 起きろ 30.sp, letterSpacing 8 / 4 |
| Level number | `displayLarge` on level-up overlay |

Hierarchy is established by size slot + weight + letter-spacing + color (primary vs onSurface vs onSurfaceVariant), not by a second font family.

### 3.3 Spacing

No formal spacing tokens. Observed conventions:

| Context | Typical values |
|---------|----------------|
| Screen padding | **16.dp** (onboarding **24.dp**) |
| Section / LazyColumn gaps | **10–14.dp** |
| Card inner padding | **12–16.dp** |
| Chip / button row gaps | **4–8.dp** |
| Primary button height | **48.dp** (some finish actions **52.dp**) |
| Overlay padding | **28–32.dp** |

### 3.4 Borders & radius

| Pattern | Observed |
|---------|----------|
| Card border | `BorderStroke(1.dp, outline)` or primary @ 0.35–0.5α |
| Theme shapes | Not customized — M3 Card defaults (~12.dp) |
| Explicit radius | `RoundedCornerShape(8.dp)` nested blocks; `10.dp` dashboard mission rows |
| Chips | Default M3 chip shape |

### 3.5 Shadows & elevation

| Pattern | Observed |
|---------|----------|
| `Modifier.shadow` | **None** in `ui/` |
| Card elevation | Not used; outlined flat cards |
| Text shadow | Splash only (purple/cyan glow blur) |
| Depth language | Surface tiering + border alpha, not elevation |

---

## 4. Layout system

### Global page layout

`Scaffold` in `SoloLevellingAppRoot.kt`:

- `containerColor = colorScheme.background`
- Content area: `Row` with optional `NavigationRail` + `Box` containing `NavHost` + overlays
- Bottom bar when `currentRoute ∈ primaryRoutes` and width &lt; 840dp

### Header / navigation

| Surface | Layout |
|---------|--------|
| Phone | Bottom `NavigationBar`, `surfaceContainerLow @ 0.95α` |
| Wide (≥840dp) | Side `NavigationRail` with “◈ SYSTEM”, tabs, Settings shortcut, LVL + XP bar |
| Dashboard | In-screen `TopAppBar` (“SYSTEM” + trophy + settings) |
| Most other screens | No TopAppBar — title text at top of scroll/column |

### Content containers

- Primary pattern: single-column `LazyColumn` or `verticalScroll` `Column`
- Full-width cards stacked vertically
- Horizontal scroll only for chip rows / date strips
- No multi-column grids except Character attribute pairs (`Row` + `weight(1f)`)

### Fixed vs fluid

| Element | Behavior |
|---------|----------|
| Nav bar / rail | Fixed chrome |
| FAB | Fixed; Dashboard-only |
| Overlays | Full-screen fixed |
| Screen content | Fluid scroll |
| Deep links | Not implemented |

### Vertical / horizontal rhythm

- Vertical: consistent 12–16.dp stacking on most screens
- Horizontal: 16.dp screen inset; cards fill width

**Files:** `SoloLevellingAppRoot.kt`, each `*Screen.kt`

---

## 5. Navigation & information architecture

### Conceptual IA

```
Splash
  └─ Onboarding (if !onboardingDone)
       └─ Main shell
            ├─ Home (Dashboard) ──► Achievements, Settings, Quests, Fitness, Nutrition, Career
            ├─ Career*            (* module-gated)
            ├─ Workout*           → FitnessScreen(Workout)
            ├─ Diet*              → FitnessScreen(Diet)
            └─ More
                 ├─ Missions (Quests)
                 ├─ Progress (Analytics)
                 ├─ Self Attributes (Character)
                 ├─ History
                 ├─ Life (Modules)
                 ├─ Achievements
                 └─ Settings
            + LevelUpHost / StreakRecoveryHost (event overlays)
```

### Pattern → Purpose → Implementation → UX impact

| Pattern | Purpose | Implementation | UX impact |
|---------|---------|----------------|-----------|
| Module-gated primary tabs | Show only enabled life areas | `buildMainTabs(EnabledModules)` | Nav stays relevant; fewer dead tabs |
| More hub | Secondary destinations without overflowing bottom bar | `MoreScreen` card list | Quests/Character/etc. one tap away but not primary |
| Dual hubs | Fast paths from Home and More | Dashboard CTAs + More rows | Slightly redundant entries; high discoverability for core actions |
| Tab state restore | Preserve scroll/state per tab | `popUpTo(start) { saveState }`, `restoreState` | Switching tabs feels sticky |
| Disabled-module redirect | Avoid broken tabs after toggles | `redirectForDisabledModuleRoute` | Lands on Dashboard if module turned off |
| Event overlays | Celebrate / recover without route change | `LevelUpHost`, `StreakRecoveryHost` | Strong feedback; can interrupt any screen |
| FAB quick actions | Fast logging from Home | Dropdown on Dashboard when bar shown | Gym/kitchen shortcuts |
| System back only for secondary | Stack navigation | No in-app Up buttons on secondary screens | Users rely on gesture/back |

### Consistency

Primary tab switching is consistent. Secondary screens consistently hide the bottom bar. In-app back affordances are **not** consistent (only Dashboard has a TopAppBar; secondary screens have none).

**Deep links:** None in `AndroidManifest.xml` / NavHost.

---

## 6. Individual screen analysis

> Responsive note for all screens: app-level rail at ≥840dp; individual screens are single-column phone layouts. No per-screen tablet layouts were found.

### 6.1 Welcome Splash

| | |
|--|--|
| **Purpose** | Brand gate while DB bootstrap finishes |
| **User goal** | Wait for app readiness |
| **Entry** | Cold start |
| **Exit** | Auto when `ready && elapsed ≥ 3200ms` |

**Visual structure:** Full-screen `SplashBackground`; radial glow; centered “ARISE” + “起きろ”.

**Components:** Custom Canvas glow; Text with Cascadia; MediaPlayer `okiro_deep`.

**UX flow:** Non-interactive. Audio ~450ms after appear. Min display 3.2s.

**File:** `ui/WelcomeSplash.kt`

---

### 6.2 Onboarding — SYSTEM INITIALIZATION

| | |
|--|--|
| **Purpose** | Capture name, modules, career/fitness/diet profile |
| **User goal** | Initialize the system and enter Dashboard |
| **Entry** | First launch when `onboardingDone != true` |
| **Exit** | “Initialize System” → complete onboarding → Dashboard (onboarding popped) |

**Visual structure:** 24.dp padded scroll; step header; one large card; Back / Next footer.

**Steps (dynamic via `buildOnboardingSteps`):**

1. NAME  
2. GOALS (module selection)  
3. CAREER_INTENT / CAREER_PROFILE / CAREER_ASSESS (if career)  
4. WORKOUT_BODY / WORKOUT_PLAN (if workout)  
5. DIET_NUTRITION (if diet)  
6. SUMMARY  

**Components:** `ModuleGoalCard`, `ChipRow`, `BodyFields`, `OutlinedTextField`, `FilterChip`, primary/outlined buttons.

**UX flow:** Next disabled until `isOnboardingStepValid`; live BMI/BMR/TDEE preview on diet; career assessment preview; final Initialize writes via `OnboardingService`.

**File:** `ui/onboarding/OnboardingScreen.kt`

---

### 6.3 Dashboard — SYSTEM

| | |
|--|--|
| **Purpose** | Answer “what should I do / how am I doing?” |
| **User goal** | See priority, progress, clear missions, jump to modules |
| **Entry** | Default after onboarding; Home tab |
| **Exit** | Achievements, Settings, Career, Workout, Diet, Quests; FAB |

**Visual structure (top → bottom):** TopAppBar → greeting/date → TODAY’S PRIORITY → YOUR PROGRESS → SUGGESTIONS → LEVEL/XP → DAILY PROGRESS → TODAY’S MISSIONS → workout/nutrition cards (module-gated) → CURRENT STREAK → NEXT UNLOCK.

**Components:** `TopAppBar`, Cards, `Button`/`OutlinedButton`, `LinearProgressIndicator`, mission rows (`RoundedCornerShape(10.dp)`).

**UX flow:** Primary CTA “START” on priority; Clear on missions; START WORKOUT / LOG MEAL; dismiss suggestions; snackbar on mission complete via `SystemMessages.missionComplete`.

**File:** `ui/dashboard/DashboardScreen.kt`, `DashboardViewModel.kt`

---

### 6.4 Quests — Missions

| | |
|--|--|
| **Purpose** | Browse/complete/undo quests and bosses |
| **Entry** | More → Missions; Dashboard; FAB; streak recovery exit |
| **Exit** | System back |

**Visual structure:** Title + available XP → FilterChip tabs → quest list or bosses UI.

**Components:** `QuestCard`, `BossesTab`, chips, complete/undo buttons, boss progress (`SystemWarning`).

**UX flow:** Complete → snackbar with XP or error (cap, not found, etc.); Undo on completed.

**File:** `ui/quests/QuestsScreen.kt`

---

### 6.5 Fitness — Workout & Diet

| | |
|--|--|
| **Purpose** | Log workouts/routines/splits and meals/macros |
| **Entry** | Workout/Diet tabs; Dashboard; FAB; History links |
| **Exit** | System back / tab switch |

**Visual structure:** Title by tab → `FitnessSection` and/or `DietSection` cards with nested FilterChip sub-tabs (Routine | Log | History / Log Food | History).

**Components:** `DateStrip`, `MacroProgressRow`, text fields, dropdowns, chips, progress bars, 48–52.dp buttons.

**UX flow:** Heavy form logging; validation via `EntryValidation` → snackbar; PR callouts; empty states (“YOUR SYSTEM IS READY”, “NO MEALS LOGGED”).

**Responsive:** Date/chip rows scroll horizontally.

**File:** `ui/fitness/FitnessScreen.kt` (shared by `fitness` and `nutrition` routes)

---

### 6.6 Career

| | |
|--|--|
| **Purpose** | Career roadmap, DSA pipeline, system design concepts |
| **Entry** | Career tab; Dashboard priority |
| **Exit** | Tab switch / back |

**Visual structure:** Title → chips (Roadmap / DSA / System Design) → stacked `CareerCard`s.

**UX flow:** DSA Attempt → Solve → Master; add problem form; SD concept status buttons; snackbars.

**File:** `ui/career/CareerScreen.kt`

---

### 6.7 Character — Self Attributes

| | |
|--|--|
| **Purpose** | Read-only progression readout |
| **Entry** | More → Self Attributes |
| **Exit** | System back |

**Visual structure:** Profile card → CAREER/FITNESS sections → CONSISTENCY → SYSTEM STATS 2-col grid → XP History.

**UX flow:** Display only; empty copy when no profile sections/ledger.

**File:** `ui/character/CharacterScreen.kt`

---

### 6.8 Analytics — Progress

| | |
|--|--|
| **Purpose** | Level/XP, period scores, before vs now, weekly review, export |
| **Entry** | More → Progress |
| **Exit** | System back |

**Visual structure:** “YOUR PROGRESS” + cards (Level, Period Scores, Before vs Now, This Week, Export).

**UX flow:** Read-only; Export JSON via share intent + snackbar “Export ready”.

**File:** `ui/analytics/AnalyticsScreen.kt`

---

### 6.9 Achievements

| | |
|--|--|
| **Purpose** | Locked/unlocked achievement catalog |
| **Entry** | Dashboard trophy; More |
| **Exit** | System back |

**Visual structure:** Header + count → `LazyColumn` of cards (locked dimmed).

**UX flow:** Read-only; UNLOCKED (`SystemSuccess`) / LOCKED (`onSurfaceVariant`).

**File:** `ui/achievements/AchievementsScreen.kt`

---

### 6.10 History

| | |
|--|--|
| **Purpose** | Recent XP, workouts; quick links to log screens |
| **Entry** | More → History |
| **Exit** | Links to Workout/Diet; system back |

**Visual structure:** `HistoryCard` sections; clickable primary text links.

**File:** `ui/history/HistoryScreen.kt`

---

### 6.11 Modules — Life

| | |
|--|--|
| **Purpose** | Focus timer, steps/weight, journal, routines, bosses, skills (+ legacy career/DSA sections still present) |
| **Entry** | More → Life; FAB Add Weight |
| **Exit** | System back |

**Visual structure:** Long stacked `ModuleCard` forms/lists.

**UX flow:** Chip toggles, timer, form submits → snackbars; validation errors.

**File:** `ui/modules/ModulesScreen.kt`

---

### 6.12 More

| | |
|--|--|
| **Purpose** | Secondary navigation hub |
| **Entry** | More tab |
| **Exit** | Any of 7 destinations |

**Visual structure:** “◈ SYSTEM” + “More” + seven `MoreRow` cards (Missions, Progress, Self Attributes, History, Life, Achievements, Settings).

**File:** `ui/more/MoreScreen.kt`

---

### 6.13 Settings

| | |
|--|--|
| **Purpose** | Profile, module toggles, targets, splits, notifications, maintenance, reset |
| **Entry** | More; Dashboard gear; rail Settings |
| **Exit** | After reset → Dashboard (stack cleared); system back otherwise |

**Visual structure:** Scroll of `SettingsCard`s; On/Off button pairs; chip split editor; destructive reset.

**UX flow:** `AlertDialog` for module disable + reset confirm; save/export snackbars; inline split validation errors.

**File:** `ui/settings/SettingsScreen.kt`

---

### 6.14 Level-up overlay

| | |
|--|--|
| **Purpose** | Celebrate LevelUp / RankUp domain events |
| **Entry** | EventBus → `LevelUpViewModel` |
| **Exit** | CONTINUE dismisses overlay (route unchanged) |

**Visual structure:** Full-screen dim + cyan glow; staged LEVEL UP text / Rank Up static copy.

**File:** `ui/levelup/LevelUpHost.kt`

---

### 6.15 Streak recovery overlay

| | |
|--|--|
| **Purpose** | Guided recovery after `StreakBroken` |
| **Entry** | EventBus → `StreakRecoveryViewModel` |
| **Exit** | BEGIN AGAIN → dismiss + navigate to Quests |

**Visual structure:** 3 steps — stats → reflective question (`SystemMessages.FALL_QUESTION`) → motivational close.

**File:** `ui/streak/StreakRecoveryHost.kt`

---

## 7. Component-level design analysis

There is no shared component library. Below are **recurring recipes** and **file-local** helpers.

### Buttons

| Variant | Visual | Where |
|---------|--------|-------|
| Primary `Button` | Filled primary; often full-width 48.dp | CTAs (START, Complete, Save, CONTINUE) |
| `OutlinedButton` | Outline | Back, Undo, secondary, module Off |
| Icon buttons | TopAppBar / date strip | Dashboard, Fitness |

Interaction states: Material 3 defaults (enabled/disabled). Disabled used for invalid forms / locked actions.

### Inputs / forms

- `OutlinedTextField` dominant
- `ExposedDropdownMenuBox` in diet catalog
- Labels/placeholders are inline; no shared form field wrapper
- Validation: `EntryValidation` + snackbar and/or inline `colors.error` text

### Cards

Repeated pattern (local names differ):

```kotlin
Card(
  colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer),
  border = BorderStroke(1.dp, colors.outline),
)
```

Highlight variant: `surfaceContainerHigh` + `primary.copy(alpha = 0.35f–0.5f)` border.

Local helpers: `QuestCard`, `CareerCard`, `ModuleCard`, `HistoryCard`, `SettingsCard`, `SelfSectionCard`, `MoreRow`, `ModuleGoalCard`.

### Navigation

- `NavigationBar` / `NavigationRail` + `NavigationBarItem`
- `FilterChip` as in-screen tabs
- No breadcrumbs, no ModalBottomSheet

### Progress

- `LinearProgressIndicator` for XP, daily %, macros, boss, DSA
- Track color typically `surfaceContainerHighest`
- Fill: `GlowCyan`, `primary`, `SystemSuccess`, or `SystemWarning`

### Overlays / dialogs

- Full-screen custom overlays (level-up, streak)
- `AlertDialog` (Settings only)
- Global `SnackbarHost`

### Lists

- `LazyColumn` (Dashboard, Quests, Achievements)
- Nested `forEach` in scroll columns (Career, Modules, Analytics)

### Empty / loading / error

| State | Component pattern |
|-------|-------------------|
| Empty | Card or text block with SYSTEM-toned copy + optional CTA |
| Loading | **No** `CircularProgressIndicator` / skeletons in `ui/` |
| Error | Snackbar; inline error text; dialogs for destructive confirms |

### Icons / badges

- Material Icons (`Home`, `Star`, `FitnessCenter`, `Restaurant`, `MoreHoriz`, …)
- Achievement status text badges (UNLOCKED/LOCKED)
- No shared Avatar component

**Design system membership:** Theme tokens = yes. Composables = informal copy-paste system, not a package.

---

## 8. Interaction design

| Interaction | Observed behavior |
|-------------|-------------------|
| Hover | Not applicable (mobile-first); desktop Compose defaults only if run on desktop |
| Focus | Standard Compose/M3 focus for text fields |
| Selected | Chips / nav items: primary tint fill + primary label |
| Pressed | M3 ripple defaults |
| Disabled | Buttons/fields disabled when invalid or locked |
| Loading | Absent as dedicated UI |
| Success | Snackbars; level-up overlay; green status text |
| Error | Snackbars; red inline text; confirm dialogs |
| Scroll | Vertical primary; horizontal for chips/dates |
| Drag/drop | Not present |
| Gestures | System back; standard scroll |

These keep feedback lightweight: most actions resolve via snackbar or domain overlay rather than per-widget success chrome.

---

## 9. Motion & animation

| Location | Trigger | Timing / behavior | UX purpose |
|----------|---------|-------------------|------------|
| WelcomeSplash | Enter | ARISE fade/scale (~700/550/220ms); 起きろ delay 400ms + fade 600ms; glow pulse 1600ms reverse | Brand presence while loading |
| WelcomeSplash audio | Enter | `okiro_deep` at ~450ms | Reinforce ARISE moment |
| LevelUpHost | LevelUp event | Stages 0→6 with delays 200–500ms; `fadeIn`/`fadeOut` | Dramatic reveal of progress |
| RankUp path | RankUp event | Mostly immediate text + CONTINUE | Faster acknowledgment |
| StreakRecoveryHost | Step changes | Instant (no enter animation) | Functional wizard |
| NavHost | Route change | Default Compose Navigation (no custom transitions) | Neutral |
| Elsewhere | — | No shared micro-interaction library | — |

---

## 10. Forms & input UX

### Styling

- `OutlinedTextField` on dark surfaces
- Chip selectors for enums / days / modules
- Full-width primary submit buttons (~48.dp)

### Validation & feedback

| Mechanism | Where |
|-----------|-------|
| Disable primary button | Onboarding Next/Initialize |
| `EntryValidation` → snackbar | Fitness, Career, Modules, Settings |
| Inline `colors.error` | Onboarding split map; Settings split map |
| Confirm dialogs | Settings module disable; reset all |

### Submit / loading / success

- Submits launch coroutines; **no** in-button spinners observed
- Success = snackbar string and/or list refresh via StateFlow
- Onboarding success navigates away

### Form layout

- Vertical stacks inside cards
- Related fields grouped in the same card section
- Chip rows for multi-select / day maps

**Guidance quality (observed):** Validation messages exist for many numeric/text entries; onboarding gates Next; some long screens (Modules/Fitness) rely on user scrolling to find fields.

---

## 11. Feedback & system states

| State | Visual treatment | User message | User action | UX purpose |
|-------|------------------|--------------|-------------|------------|
| Loading (app) | Splash | ARISE / 起きろ | Wait | Cover bootstrap |
| Loading (screens) | None / stale defaults | — | — | Data arrives via flows |
| Empty missions | Card + ADD TASK | “NO MISSIONS TODAY” | Add task | Recover empty day |
| Empty workout/diet | SYSTEM copy + CTA | “YOUR SYSTEM IS READY” / “NO MEALS LOGGED” | Start logging | Prompt first action |
| Empty boss / quests | Card text | “No active boss” / “No … quests” | Switch tab / wait | Clarify absence |
| Success (mission) | Snackbar | `✓ MISSION COMPLETE +XP` | Continue | Reward loop |
| Success (generic) | Snackbar | Action-specific string | Continue | Confirm write |
| Error (quest/validation) | Snackbar / inline | Human-readable / validation | Fix & retry | Prevent bad data |
| Destructive | AlertDialog | Confirm disable/reset | Confirm/cancel | Prevent accidents |
| Level/rank up | Full-screen overlay | LEVEL UP / RANK UP copy | CONTINUE | Celebrate |
| Streak broken | Full-screen wizard | Stats + fall question + recovery | BEGIN AGAIN → Quests | Re-engage |
| Offline | Not specially handled in UI | — | — | App is offline-first local JSON |

**Toasts:** Not used; Snackbars only.  
**Skeletons:** Not present.

---

## 12. Gamification / progress UX

| Element | How visualized | Where |
|---------|----------------|-------|
| Level / XP bar | Number + `LinearProgressIndicator` (`GlowCyan` / primary) | Dashboard, Character, Analytics, rail |
| Rank | Text | Character, RankUp overlay |
| Daily completion % | Progress + `SystemSuccess` | Dashboard |
| Streak | Cyan-accent text; recovery overlay on break | Dashboard, Character, StreakRecovery |
| Quests / XP rewards | Per-quest XP; complete/undo | Quests, Dashboard |
| Boss progress | Warning-colored bar | Quests |
| Achievements | Locked dim / unlocked green | Achievements; trophy entry |
| Attributes | Code + value grid | Character |
| Improvement % | Level-up + Analytics before/now | LevelUpHost, Analytics |
| Career DSA/SD % | Progress text/bars | Dashboard, Career |
| Motivational copy | `SystemMessages` categories | Level-up, streak, mission complete |
| Next unlock | Teaser card | Dashboard |

**Engagement loop (observed):** See priority → complete mission/log → snackbar XP → eventual level-up overlay → streak pressure → recovery funnel back to Quests.

---

## 13. Accessibility

### Implemented accessibility (observed)

- Some `contentDescription`s: Dashboard trophy/settings; Fitness date arrows; nav tab icons; FAB “Quick actions”
- Material 3 components provide baseline semantics for buttons/text fields/dialogs
- Settings `AlertDialog`s use standard M3 dialog structure
- Touch targets for primary buttons often 48.dp height

### Potential accessibility improvements (not claiming gaps as bugs unless evidenced)

- Most secondary screens lack `contentDescription` / `semantics`
- Clickable text links (History) may not expose button role
- No `semantics { }` blocks found under `ui/`
- No reduced-motion handling for splash / level-up animations
- Color-only locked/unlocked distinction is paired with text labels (good), but contrast of muted locked cards was not measured here
- Timed level-up staging has no user-controlled speed
- More screen is non-scrolling — large font scale may clip (layout risk)

**Do not claim WCAG/compliance** — not evidenced by automated a11y tests in repo from this review.

---

## 14. Responsive design

| Aspect | Observed |
|--------|----------|
| Breakpoint | `screenWidthDp >= 840` → NavigationRail |
| Mobile | Bottom NavigationBar + optional FAB |
| Tablet/desktop width | Rail with SYSTEM header, Settings, LVL/XP |
| Per-screen adaptive grids | Not found |
| Typography scaling | System font scale via Compose; splash uses fixed sp |
| Navigation change | Bar ↔ rail only |
| Chip overflow | `horizontalScroll` |
| Touch | Primary interaction model |

Responsiveness is **shell-level**, not screen-level. Content remains single-column.

---

## 15. Design tokens & styling architecture

```
MainActivity
  └── SololevellingTheme          Theme.kt
        ├── DarkColorScheme         ← Color.kt tokens
        └── Typography              ← Type.kt Cascadia
              └── Screens use MaterialTheme + occasional raw tokens
```

### Where to change what

| Need | Go here |
|------|---------|
| Global colors | `ui/theme/Color.kt` + mapping in `Theme.kt` |
| Typography / font | `Type.kt` + `res/font/` |
| Spacing | No tokens — edit screen paddings locally |
| Button styling | M3 defaults / per-call `Modifier` on screens |
| Card styling | Copy existing Card recipe on the target screen (or extract later) |
| Global layout / nav | `SoloLevellingAppRoot.kt` |
| Responsive rail breakpoint | `SoloLevellingAppRoot.kt` (`840`) |
| Success/warning/glow | Raw vals in `Color.kt`; imported by screens |
| System copy | `domain/copy/SystemMessages.kt` |

**Not present:** CSS variables, Tailwind, CSS modules, styled-components, `Shape.kt`.

---

## 16. UX patterns & design patterns

| Pattern | Where | Why useful | How | Consistent? |
|---------|-------|------------|-----|-------------|
| Card-based IA | Almost all screens | Scannable sections | M3 Card + border | Yes |
| Module-gated chrome | Tabs + dashboard sections | Personalize IA | `EnabledModules` | Yes |
| Progressive disclosure | Onboarding steps; Career/Fitness chips; streak steps | Reduce overwhelm | Local step/tab state | Mostly |
| Persistent primary nav | Bottom bar / rail | Fast area switch | Scaffold | Yes |
| Secondary hub | More | Keep bar lean | Card list | Yes |
| Dashboard hub | Home | Daily OS glance | Priority engine cards | Yes |
| Wizard | Onboarding; streak recovery | Guided setup/recovery | Step index | Yes within those flows |
| Gamified progression | XP/level/achievements | Motivation | Domain events + UI | Yes |
| Empty-state guidance | Dashboard/Fitness/Quests | Prompt next action | Copy + CTA | Partial (wording varies) |
| Inline + snackbar validation | Forms | Catch bad input | `EntryValidation` | Mostly |
| Event-driven overlays | Level-up, streak | Cross-cutting feedback | EventBus hosts | Yes |
| FilterChip tabs | Quests, Career, Fitness, Settings | Lightweight in-page nav | Repeated chip colors | Visual yes; not shared composable |
| Confirmation for danger | Settings | Prevent wipe | AlertDialog | Localized to Settings |

**Bottom sheets:** Not used.

---

## 17. Consistency analysis

### Consistent patterns

- Dark SYSTEM palette via theme
- Cascadia everywhere
- Outlined flat cards
- FilterChip selected styling
- 16.dp screen padding on main app screens
- Snackbar-via-`onMessage` feedback
- ALL-CAPS + letter-spacing on several hub headers
- Module-aware navigation

### Inconsistent patterns

| Area | Inconsistency |
|------|---------------|
| Navigation chrome | Dashboard has TopAppBar; secondary screens do not |
| Section headers | Some ALL-CAPS letter-spaced; Career/History more plain |
| Card helpers | Many private `*Card` duplicates with slight padding differences (12 vs 16) |
| Semantic colors | Success/warning/glow bypass `colorScheme` |
| Empty copy | Mix of SYSTEM voice vs plain “No …” |
| Lists | LazyColumn vs verticalScroll `forEach` |
| Overlays | Level-up animated; streak steps instant |
| Career vs Modules | Overlapping DSA/career UI still in Modules |
| Loading | Splash only; elsewhere none |

### Recommended standardization (guidance only — not implemented)

1. Extract one `SystemCard` + `SystemFilterChip` recipe.
2. Add optional shared screen scaffold with title + back for secondary routes.
3. Map success/warning into the color scheme or a tiny semantic object.
4. Pick one empty-state template (title + body + primary CTA).
5. Remove or clearly deprecate duplicate career/DSA UI in Modules once Career is canonical.

---

## 18. UX strengths

| Strength | Why it works | Implementation | User problem solved |
|----------|--------------|----------------|---------------------|
| Priority card on Dashboard | Immediate next action | `PriorityEngine` + START CTA | Decision fatigue |
| Module-gated tabs | Nav matches enabled life areas | `buildMainTabs` | Irrelevant features |
| Brand moments (splash / level-up) | Emotional reinforcement | Animation + `SystemMessages` | Motivation / identity |
| Fast logging surfaces | Fitness/Diet forms + FAB | Shared FitnessScreen + quick actions | Log at gym/kitchen |
| Global snackbar | Predictable feedback | Root `SnackbarHost` | Confirm writes/errors |
| Streak recovery funnel | Turns failure into action | Overlay → Quests | Re-engagement after break |
| Event-driven level-up | True progression feedback | Domain events → host | Trust that XP mattered |
| Read-only Character | Clear “stats sheet” | No edit chrome | Understand progression without clutter |

---

## 19. UX/UI issues & improvements

Evidence-based; severity for product/UX risk.

### Critical

*None observed that block core flows in code review alone.*

### High

| Problem | Evidence | UX impact | Recommended improvement |
|---------|----------|-----------|-------------------------|
| Secondary screens lack in-app back | No TopAppBar/Up on Quests, Settings, etc. | Harder navigation for users who don’t use system back | Shared app bar with navigate-up |
| No loading indicators on data screens | No `CircularProgressIndicator` in `ui/` | Unclear if data is stale/empty vs loading | Lightweight loading on first emit |
| Doc/IA drift in older `ui-design.md` | Lists 6 tabs / AssistChips not in code | Devs may implement wrong IA | Prefer this document for current UI |

### Medium

| Problem | Evidence | UX impact | Recommended improvement |
|---------|----------|-----------|-------------------------|
| Duplicated card/chip recipes | Private helpers per file | Visual drift over time | Small shared composables |
| Modules still hosts career/DSA | `ModulesScreen` sections + Career screen | Confusing duplicate paths | Single career surface |
| More screen non-scroll | `Column` without scroll | Possible clip with large fonts | Make scrollable |
| Overlay stacking | Streak composed after LevelUp | If both pending, streak covers level-up | Coordinate pending overlays |
| Form-heavy Fitness/Modules density | Very long screens | Cognitive load | Clearer section anchors |

### Low

| Problem | Evidence | UX impact | Recommended improvement |
|---------|----------|-----------|-------------------------|
| Light theme stub | Incomplete `LightColorScheme` | Broken if toggled | Keep dark-only or finish light |
| Unused `SystemOnSuccess` / `OnWarning` | Defined, unused | Token noise | Use or remove |
| Accessibility gaps | Few contentDescriptions | TalkBack weaker on secondary | Add descriptions/roles |
| No nav transition design | Default NavHost | Less polished | Optional fade |

---

## 20. Developer design guide

### How to structure a new screen

1. Add route to `AppRoute` and `NavHost` in `SoloLevellingAppRoot`.
2. Decide primary vs secondary: primary must be wired through `buildMainTabs` / module flags; secondary usually linked from More or Dashboard.
3. Use `Column`/`LazyColumn` with **16.dp** padding and **12–14.dp** spacing.
4. Title: `headlineSmall` + Bold.
5. Content in outlined Cards.
6. Pass `onMessage` for snackbars; do not add a local SnackbarHost.
7. Gate module-specific UI with `EnabledModules` / flags already used nearby.

### Reuse these — do not reinvent

- `MaterialTheme.colorScheme` for surfaces/text/primary/error
- `GlowCyan`, `SystemSuccess`, `SystemWarning` for XP/success/boss semantics (matching neighbors)
- Existing Card + FilterChip recipes
- `EntryValidation` for form checks
- `SystemMessages` for SYSTEM-voice user strings when applicable

### Do not duplicate

- Another snackbar host
- A second theme
- Parallel nav graphs
- New bottom-sheet frameworks (none exist; stay with dialogs/overlays/cards)
- Duplicate career/DSA UIs outside Career unless explicitly required

### Colors

- Prefer `colors.primary`, `surfaceContainer`, `outline`, `onSurfaceVariant`, `error`
- Import raw success/warning/glow only when matching existing semantic usage

### Typography

- Cascadia comes from theme — don’t introduce a second family
- Page title `headlineSmall`; sections bold + optional `letterSpacing = 2.sp`; body `bodySmall`/`bodyMedium`

### Spacing

- Screen 16.dp; card 12–16.dp; primary buttons ~48.dp height

### Buttons

- Primary filled for the one main action
- Outlined for secondary/undo/back
- Destructive: `error` colors (see Settings reset)

### Cards

```kotlin
Card(
    colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer),
    border = BorderStroke(1.dp, colors.outline),
) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { … } }
```

Highlight: `surfaceContainerHigh` + `BorderStroke(1.dp, colors.primary.copy(alpha = 0.5f))`.

### Loading / error / empty

- Empty: short SYSTEM or plain copy + primary CTA when an action exists
- Error: `onMessage(...)` snackbar; inline error for field-level
- Loading: not standardized yet — if adding, keep subtle and local

### Responsive

- Don’t assume per-screen breakpoints; ensure chip rows can `horizontalScroll`
- Remember bottom bar hides on secondary routes

### Where to add tokens

- Colors → `Color.kt` (+ `Theme.kt` if mapping into scheme)
- Type → `Type.kt`
- Copy → `SystemMessages.kt`

---

## 21. Codebase reference map

| UI/UX area | Component / file | Purpose | Notes |
|------------|------------------|---------|-------|
| Global theme | `ui/theme/Theme.kt` | `SololevellingTheme` | Dark default; dynamic color off |
| Color tokens | `ui/theme/Color.kt` | Palette | Success/warning/glow extras |
| Typography | `ui/theme/Type.kt` | Cascadia M3 scale | |
| Fonts | `res/font/cascadia_code_*.ttf` | Typefaces | |
| App shell | `ui/SoloLevellingAppRoot.kt` | Nav, bar/rail, FAB, snackbar, overlays | 840dp rail |
| Routes | `ui/navigation/AppRoute.kt` | Route strings | |
| Tab gating | `ui/navigation/ModuleNavigation.kt` | `buildMainTabs` | |
| Splash | `ui/WelcomeSplash.kt` | Brand gate | 3.2s min in root |
| Onboarding | `ui/onboarding/OnboardingScreen.kt` | Wizard | Dynamic steps |
| Dashboard | `ui/dashboard/DashboardScreen.kt` | Home SYSTEM | TopAppBar |
| Quests | `ui/quests/QuestsScreen.kt` | Missions | |
| Fitness | `ui/fitness/FitnessScreen.kt` | Workout + Diet | Dual routes |
| Career | `ui/career/CareerScreen.kt` | Career hub | |
| Character | `ui/character/CharacterScreen.kt` | Self attributes | Read-only |
| Analytics | `ui/analytics/AnalyticsScreen.kt` | Progress review | |
| Achievements | `ui/achievements/AchievementsScreen.kt` | Unlocks | |
| History | `ui/history/HistoryScreen.kt` | Recent logs | |
| Modules / Life | `ui/modules/ModulesScreen.kt` | Focus/journal/etc. | |
| More | `ui/more/MoreScreen.kt` | Secondary hub | |
| Settings | `ui/settings/SettingsScreen.kt` | Config + dialogs | |
| Level-up | `ui/levelup/LevelUpHost.kt` | Overlay | EventBus |
| Streak recovery | `ui/streak/StreakRecoveryHost.kt` | Overlay wizard | → Quests |
| System copy | `domain/copy/SystemMessages.kt` | Motivational / mission strings | |
| Theme tests | `ui/theme/*Test.kt` | Token/font locks | |
| Entry | `MainActivity.kt` | Theme + root | Edge-to-edge |

---

## 22. Final design system summary

### Overall design language

Dark, monospace **SYSTEM** interface: navy surfaces, blue/cyan accents, flat outlined cards, RPG vocabulary over productivity logging.

### Core visual principles

1. Fixed dark brand palette (no Material You)  
2. Cascadia Code everywhere  
3. Flat depth (borders + surface tiers, not shadows)  
4. Restrained glow reserved for splash / level-up / XP  
5. ALL-CAPS system labels with letter-spacing on key hubs  

### Core UX principles

1. Dashboard answers “what next?” quickly  
2. Module flags shape navigation and content  
3. Fast logging over multi-step wizards (except onboarding/streak)  
4. EventBus-driven celebration and recovery  
5. Snackbars for operational feedback  

### Major reusable pieces (de facto)

Material 3 Card / FilterChip / Button / OutlinedTextField / LinearProgressIndicator / AlertDialog; theme tokens; file-local `*Card` helpers; global snackbar + overlays.

### Major interaction patterns

Primary tab nav + More hub; chip sub-tabs; full-width CTAs; confirm dialogs for danger; staged level-up; streak recovery wizard.

### Responsive strategy

Single breakpoint at 840dp for rail vs bottom bar; content stays single-column with horizontal chip scroll.

### Accessibility approach

Sparse contentDescriptions on key icons; reliance on M3 defaults; no dedicated a11y layer observed.

### Strongest parts

Priority-driven Dashboard, branded splash/level-up, module-aware IA, cohesive dark SYSTEM look, event-driven progression feedback.

### Biggest opportunities

Shared UI primitives, secondary-screen app bars, loading states, dedupe Career vs Modules, stronger a11y, keep docs aligned with code (this file).

---

### Quick reference

| Item | Convention |
|------|------------|
| Background | `#05070D` |
| Card | `#0D1320` + 1.dp outline |
| Primary | `#4DA3FF` |
| XP / glow | `#67D4FF` |
| Success | `#4ADE80` |
| Warning | `#F5C84C` |
| Error | `#E35D5D` |
| Font | Cascadia Code |
| Screen pad | 16.dp |
| Button height | ~48.dp |
| Chip selected | primary @ 15% fill |
| Primary tabs | Home · Career? · Workout? · Diet? · More |
| Secondary | via More / Dashboard |
| Feedback | Snackbar (`onMessage`) |
| Celebration | `LevelUpHost` |
| Streak loss | `StreakRecoveryHost` → Quests |
| Theme entry | `SololevellingTheme` in `MainActivity` |

---

*Generated from the Compose implementation under `app/src/main/java/com/example/solo_levelling/ui/`. Prefer this document over older UI notes when they conflict.*
