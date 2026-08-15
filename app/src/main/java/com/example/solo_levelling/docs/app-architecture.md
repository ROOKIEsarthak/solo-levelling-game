# SYSTEM — Personal Leveling & Life Operating System

> A Solo Leveling–inspired personal tracking system designed to turn real-world actions into measurable progression.
>
> **Document type:** Product Requirements + Game Design + Technical Specification  
> **Version:** 1.0  
> **Primary goal:** Build a fully functional personal tracking app.

---

# 1. Vision

The app should make the user's real life behave like an RPG.

Instead of:

> "I should study more."

the system should produce:

> **QUEST:** Solve 2 DSA problems  
> **REWARD:** +40 XP, +INT, +DISC  
> **RESULT:** 2/2 completed → Quest Cleared

The system should answer, every day:

1. What should I do today?
2. Did I actually do it?
3. How much XP did I earn?
4. Which attributes improved?
5. What major objective am I progressing toward?
6. Am I becoming better over weeks and months?

The system should be designed around **execution**, not vanity metrics.

---

# 2. Core Philosophy

## 2.1 Real life is the game

The app does not create fake progress.

Only real-world actions generate XP.

Examples:

- Solving DSA → INT XP
- Completing a workout → STR XP
- Hitting a step target → END XP
- Following nutrition targets → VIT XP
- Deep work → FOC XP
- Completing planned work despite low motivation → DISC XP
- Reviewing mistakes → WIS XP

---

## 2.2 Never optimize for the game instead of life

The app must not encourage:

- pointless task creation
- repeatedly completing trivial tasks for XP
- unhealthy punishment
- obsessive tracking
- manipulating metrics

XP should reward **meaningful behavior**.

---

## 2.3 Lifetime progress is permanent

Missing a day does not delete historical XP.

A missed day may affect:

- current streak
- weekly score
- active quests

But never:

- lifetime XP
- historical achievements
- previous levels

---

# 3. Player Identity

The user is the **Player**.

The system is the **System**.

The user has:

- Level
- Rank
- XP
- Attributes
- Skills
- Quests
- Boss Quests
- Achievements
- Streaks
- Seasons
- Statistics

Example:

```text
┌─────────────────────────────────────────┐
│              PLAYER PROFILE             │
├─────────────────────────────────────────┤
│ Sarthak                                  │
│ LEVEL 18                                 │
│ RANK: C                                  │
│                                          │
│ XP  ███████████████░░░  72%              │
│ 18,420 / 25,400 XP                       │
│                                          │
│ STR   42     END   38                    │
│ INT   61     VIT   47                    │
│ DISC  55     FOC   49                    │
│ WIS   31                                  │
└─────────────────────────────────────────┘
```

---

# 4. Core Attributes

The first version should use seven attributes.

| Code | Attribute | Meaning |
|---|---|---|
| STR | Strength | Physical strength and training |
| END | Endurance | Steps, cardio and physical stamina |
| INT | Intelligence | DSA, algorithms, system design, technical learning |
| VIT | Vitality | Nutrition, recovery and health-supporting habits |
| DISC | Discipline | Doing what was planned |
| FOC | Focus | Deep, distraction-free work |
| WIS | Wisdom | Reflection, review and learning from mistakes |

## Attribute rule

Attributes should be earned primarily through actions.

Do not expose:

```text
Edit INT = 80
```

in the normal UI.

Instead:

```text
Solve DSA Quest
       ↓
+40 XP
       ↓
+INT progression
```

---

# 5. Attribute Progression

Each quest can contribute to one or more attributes.

Example:

```json
{
  "quest": "Solve 2 DSA problems",
  "xp": 40,
  "attributeRewards": {
    "INT": 30,
    "DISC": 10
  }
}
```

A workout could be:

```json
{
  "quest": "Complete Push Workout",
  "xp": 50,
  "attributeRewards": {
    "STR": 35,
    "DISC": 10,
    "VIT": 5
  }
}
```

---

# 6. XP System

## Quest tiers

| Quest Type | Typical XP |
|---|---:|
| Micro | 5–15 |
| Standard | 20–50 |
| Hard | 60–100 |
| Boss | 150–500 |
| Weekly Bonus | 100 |
| Major Milestone | 500–1,000 |

The values should be configurable.

---

# 7. XP Formula

Use a nonlinear level curve.

Recommended starting formula:

```text
XP_FOR_NEXT_LEVEL = floor(100 × level^1.35)
```

The exact formula should live in a configuration table/service rather than being hardcoded into the frontend.

Example:

```text
Level 1 → easy
Level 10 → moderate
Level 25 → substantial
Level 50 → requires sustained progression
```

---

# 8. XP Ledger

This is one of the most important architectural decisions.

Do **not** store only:

```text
user.totalXp = 18420
```

Every XP change must create a ledger event.

Example:

```text
QUEST_COMPLETED
questId: 381
amount: +40
timestamp: ...
```

If completion is reversed:

```text
QUEST_COMPLETION_REVERSED
questId: 381
amount: -40
timestamp: ...
```

This provides:

- auditability
- debugging
- rollback
- analytics
- fraud prevention
- reliable historical reconstruction

---

# 9. Ranks

Recommended rank progression:

| Rank | Levels |
|---|---|
| E | 1–5 |
| D | 6–10 |
| C | 11–20 |
| B | 21–35 |
| A | 36–50 |
| S | 51–75 |
| SS | 76–100 |
| MONARCH | 100+ |

Ranks should be configurable.

---

# 10. Quest System

Quests are the primary interaction mechanism.

## Quest categories

### Daily Quest

Resets every day.

Examples:

- Complete DSA session
- Reach step target
- Complete workout
- Hit nutrition target
- Complete deep-work block

### Weekly Quest

Must be completed during the week.

Examples:

- Solve 10 DSA problems
- Complete 5 workouts
- Complete 3 system-design sessions

### Milestone Quest

One-time measurable achievement.

Example:

```text
Solve 100 DSA problems
```

### Boss Quest

A major goal composed of multiple smaller objectives.

Example:

```text
BOSS:
Become Interview Ready

Progress:
██████████░░░░░░ 62%

Objectives:
✓ Arrays
✓ Strings
✓ Two Pointers
✓ Binary Search
✓ Trees
□ Graphs
□ Dynamic Programming
□ Mock Interviews
```

### Recovery Quest

Used after a missed planned action.

The goal is to restore momentum, not punish the player.

---

# 11. Daily Quest Engine

The system should generate quests based on the user's configured schedule.

Example:

```text
MONDAY
Career:
  DSA
  Backend

Fitness:
  Rest

Personal:
  Focus
  Journal
```

```text
TUESDAY
Career:
  DSA

Fitness:
  Push Workout
  Steps
  Nutrition

Personal:
  Focus
```

The user should not have to manually create these every morning.

---

# 12. Example Daily System Screen

```text
╔══════════════════════════════════════╗
║              SYSTEM                  ║
║          LEVEL 18 — RANK C           ║
╠══════════════════════════════════════╣
║ DAILY QUESTS                         ║
║                                      ║
║ [ ] Solve 2 DSA Problems       +40   ║
║ [ ] System Design — 45 min     +30   ║
║ [ ] Complete Workout            +50   ║
║ [ ] 12,000 Steps                +25   ║
║ [ ] Nutrition Target            +25   ║
║ [ ] Deep Work — 60 min          +30   ║
║ [ ] Daily Reflection            +10   ║
║                                      ║
║ AVAILABLE XP: 210                    ║
╚══════════════════════════════════════╝
```

---

# 13. Quest Completion

When a quest is completed:

```text
User
 ↓
Complete Quest
 ↓
Validate completion
 ↓
Database transaction
 ↓
Create QuestCompletion
 ↓
Create XP Ledger Event
 ↓
Update XP projection
 ↓
Update attributes
 ↓
Update streak
 ↓
Evaluate achievements
 ↓
Update boss progress
 ↓
Return System Event
```

Example response:

```json
{
  "questCompleted": true,
  "xpAwarded": 40,
  "levelUp": false,
  "rankChanged": false,
  "attributeChanges": {
    "INT": 30,
    "DISC": 10
  },
  "achievementsUnlocked": []
}
```

---

# 14. Idempotency

Quest completion must be idempotent.

The following must NOT award XP twice:

```http
POST /quests/381/complete
POST /quests/381/complete
```

The backend should detect that the quest instance is already completed.

Use a unique constraint such as:

```text
UNIQUE(quest_instance_id)
```

or an equivalent idempotency mechanism.

---

# 15. Streak System

Streaks are secondary metrics.

Example:

```text
Current Streak: 12 days
Best Streak: 38 days
```

A missed day:

```text
Current streak → 0
Lifetime XP → unchanged
Achievements → unchanged
Historical data → unchanged
```

---

# 16. Recovery Mechanic

A single missed day should not create a psychological "everything is ruined" state.

Example:

```text
SYSTEM NOTICE

Daily Quest Missed.

Recovery Quest Available:

Complete today's primary quest.

Reward:
+15 Recovery XP
```

Recovery should be limited so that it does not become a loophole.

---

# 17. Anti-Gaming Rules

The system should prevent:

### XP farming

A user should not be able to create:

```text
Drink Water → +100 XP
Drink Water → +100 XP
Drink Water → +100 XP
```

Use:

- daily caps
- quest cooldowns
- predefined reward ranges
- diminishing returns

### Duplicate completion

Prevent repeated completion of the same quest instance.

### Manual XP manipulation

Normal users cannot edit XP.

### Retroactive cheating

Historical completions should have controlled editing.

Every correction should generate an audit event.

---

# 18. Career System

Career should have its own progression tree.

```text
CAREER
│
├── DSA
│   ├── Arrays
│   ├── Strings
│   ├── Two Pointers
│   ├── Sliding Window
│   ├── Binary Search
│   ├── Stack / Queue
│   ├── Trees
│   ├── Graphs
│   └── Dynamic Programming
│
├── Backend
│   ├── HTTP
│   ├── APIs
│   ├── Databases
│   ├── Indexing
│   ├── Transactions
│   ├── Caching
│   ├── Queues
│   ├── Distributed Systems
│   └── Reliability
│
├── System Design
│   ├── Requirements
│   ├── Capacity
│   ├── APIs
│   ├── Data Modeling
│   ├── Caching
│   ├── Scaling
│   ├── Failure Modes
│   └── Trade-offs
│
└── Interview
    ├── Problem Solving
    ├── Communication
    ├── Behavioral
    ├── Mock Interviews
    └── Applications
```

Each node can have:

```text
LOCKED
   ↓
STARTED
   ↓
LEARNING
   ↓
PRACTICED
   ↓
MASTERED
```

---

# 19. DSA Tracking

A DSA problem should be a first-class object.

Recommended fields:

```text
Problem
├── title
├── platform
├── externalId
├── difficulty
├── topic
├── subTopic
├── status
├── attempts
├── solvedAt
├── timeSpent
├── confidence
├── notes
└── reviewDates
```

Statuses:

```text
NOT_STARTED
ATTEMPTED
SOLVED
NEEDS_REVIEW
MASTERED
```

The system can award different XP:

```text
Attempt → +5
Solved → +20
Solved without hints → +25
Mastered after review → +15
```

Avoid rewarding repeated trivial attempts indefinitely.

---

# 20. Fitness System

Fitness should track:

```text
Workout
Steps
Calories
Protein
Body Weight
Exercise Performance
Recovery
```

## Workout

```text
Workout
├── date
├── workoutType
├── duration
├── exercises
│   ├── exercise
│   ├── sets
│   ├── reps
│   ├── weight
│   └── RIR
└── completed
```

## Exercise progression

Track:

```text
Previous:
Bench Press — 70kg × 8

Current:
Bench Press — 72.5kg × 8

Result:
Progress Detected
+STR progression
```

Do not award XP simply because body weight decreases.

Use trends rather than single measurements.

---

# 21. Nutrition Tracking

Recommended fields:

```text
date
calories
protein
carbohydrates
fat
waterOptional
```

Example:

```text
Daily Target
Calories: 1800
Protein: configured target

Actual
Calories: 1765
Protein: 124g

Quest:
Nutrition Target → COMPLETE
```

The target must be user-configurable.

---

# 22. Personal Routine System

The system can track:

- Wake/sleep schedule
- Deep work
- Journaling
- Reading
- Meditation
- Screen-time goals
- Other user-defined habits

These should be optional modules.

Do not force every possible habit into the core system.

---

# 23. Focus System

Deep work should be tracked separately from generic time.

Example:

```text
FOCUS QUEST

Target:
60 minutes

Session:
25 min
10 min break
35 min

Total:
60 min

Reward:
+30 FOC XP
```

Possible future integration:

```text
Pomodoro
Browser extension
Desktop timer
Calendar
```

---

# 24. Boss Quest System

Bosses represent outcomes rather than activities.

Example:

```text
╔══════════════════════════════════════╗
║ BOSS QUEST                           ║
║ SDE2 READY                           ║
╠══════════════════════════════════════╣
║ Progress: 61%                        ║
║                                      ║
║ DSA                 ████████░░        ║
║ System Design       ██████░░░░        ║
║ Backend              ███████░░░        ║
║ Projects             █████░░░░░        ║
║ Mock Interviews      ███░░░░░░░        ║
║ Applications         ██░░░░░░░░        ║
╚══════════════════════════════════════╝
```

Bosses can contain child quests.

Completion of child quests automatically updates boss progress.

---

# 25. Achievement System

Examples:

```text
FIRST QUEST
Complete your first quest.

7 DAY HUNTER
Maintain a qualifying 7-day streak.

30 DAY HUNTER
Maintain a qualifying 30-day streak.

PROBLEM SLAYER
Solve 50 DSA problems.

SYSTEM ARCHITECT
Complete the system-design curriculum.

IRON WILL
Complete a planned workout after a difficult day.

PERFECT WEEK
Reach the configured weekly completion threshold.

BOSS SLAYER
Complete a major boss quest.

S RANK
Reach Level 51.
```

Achievement criteria should be data-driven rather than hardcoded.

---

# 26. Skill System

Attributes describe the character.

Skills describe capabilities.

Example:

```text
INT
│
├── DSA
│   ├── Arrays — Level 4
│   ├── Binary Search — Level 3
│   ├── Trees — Level 2
│   └── Graphs — Level 1
│
└── System Design
    ├── API Design — Level 3
    ├── Caching — Level 2
    └── Distributed Systems — Level 1
```

A skill can have its own XP.

---

# 27. Skill XP

Example:

```json
{
  "skill": "Binary Search",
  "xp": 420,
  "level": 3,
  "nextLevelXp": 600
}
```

Actions can contribute:

```text
Study concept → +10
Solve problem → +20
Solve difficult problem → +30
Explain concept → +20
Review mistake → +10
```

---

# 28. Weekly System Review

Every week the system should generate:

```text
WEEKLY SYSTEM REPORT

XP Earned: 1,240

Quest Completion:
87%

Current Level:
18

Attribute Changes:
INT  +4
STR  +2
DISC +5
FOC  +3

Career:
DSA: 8 problems
System Design: 3 sessions
Backend: 2 sessions

Fitness:
Workouts: 5/5
Steps: 6/7 days
Nutrition: 6/7 days

Boss:
SDE2 Ready → 64%

Strongest Area:
Discipline

Weakest Area:
System Design

System Recommendation:
Increase System Design quests next week.
```

---

# 29. Adaptive Difficulty

Later versions can dynamically adjust quest difficulty.

Example:

```text
Previous 4 weeks:

DSA completion = 95%

System response:

Current DSA quest:
2 problems

New recommended quest:
3 problems
```

But difficulty must increase gradually.

Never automatically create unrealistic workloads.

---

# 30. Neglected Attribute Detection

The system should detect imbalance.

Example:

```text
ATTRIBUTE ANALYSIS

INT     ████████████
DISC    ██████████
STR     █████████
FOC     ███████
VIT     ██████
END     █████
WIS     ███

SYSTEM NOTICE:

WIS has received significantly less progression
than other attributes over the last 30 days.

Suggested Quest:
Weekly Reflection — 20 minutes
```

The user should be able to dismiss recommendations.

---

# 31. Seasons

Long-term progression can be divided into seasons.

Example:

```text
SEASON 01
THE SDE2 ARC

Duration:
12 weeks

Primary Boss:
SDE2 READY

Secondary Goals:
Fitness
Consistency
System Design
```

At the end of a season:

```text
Season XP:
14,820

Boss Completion:
78%

Achievements:
7

Final Rank:
A-
```

Lifetime progress remains intact.

---

# 32. Dashboard

The main dashboard should contain:

1. Character card
2. Level/rank
3. XP progress
4. Attribute cards
5. Today's quests
6. Active boss
7. Current streak
8. Weekly completion
9. Recent achievements
10. Quick-add buttons
11. Weekly trend

The dashboard should be usable in under 30 seconds.

---

# 33. Application Screens

## 33.1 Dashboard

Command center.

## 33.2 Character

- Level
- XP
- Rank
- Attributes
- Attribute history

## 33.3 Quests

- Today
- Upcoming
- Weekly
- Milestones
- Bosses

## 33.4 Career

- DSA
- Backend
- System Design
- Interview preparation

## 33.5 Fitness

- Workouts
- Steps
- Nutrition
- Weight
- Exercise progression

## 33.6 Skills

Skill tree and progression.

## 33.7 Achievements

Unlocked + locked.

## 33.8 Analytics

Weekly/monthly/yearly trends.

## 33.9 Journal

Short reflection.

## 33.10 Settings

- Goals
- Quest schedules
- XP rules
- Notifications
- Integrations
- Data export

---

# 34. Data Model

Recommended relational schema.

## users

```sql
users
-----
id
email
timezone
created_at
updated_at
```

## profiles

```sql
profiles
--------
user_id
level
total_xp
rank
created_at
updated_at
```

## stats

```sql
stats
-----
id
user_id
type
current_value
lifetime_xp
created_at
updated_at
```

## quests

```sql
quests
------
id
user_id
type
title
description
base_xp
schedule
active
created_at
updated_at
```

## quest_instances

```sql
quest_instances
---------------
id
quest_id
scheduled_date
status
completed_at
created_at
updated_at
```

## xp_ledger

```sql
xp_ledger
---------
id
user_id
source_type
source_id
amount
metadata
created_at
```

## bosses

```sql
bosses
------
id
user_id
title
description
target_value
current_value
xp_reward
deadline
status
created_at
updated_at
```

## boss_quests

```sql
boss_quests
-----------
boss_id
quest_id
weight
```

## achievements

```sql
achievements
------------
id
key
name
description
criteria
reward_xp
```

## user_achievements

```sql
user_achievements
-----------------
user_id
achievement_id
unlocked_at
```

## skills

```sql
skills
------
id
user_id
domain
name
xp
level
created_at
updated_at
```

## workouts

```sql
workouts
--------
id
user_id
date
type
duration
completed
notes
```

## workout_exercises

```sql
workout_exercises
-----------------
id
workout_id
exercise
set_number
weight
reps
rir
```

## nutrition_logs

```sql
nutrition_logs
--------------
id
user_id
date
calories
protein
carbs
fat
```

## metric_logs

```sql
metric_logs
-----------
id
user_id
metric_type
value
recorded_at
```

## journal_entries

```sql
journal_entries
---------------
id
user_id
date
content
created_at
updated_at
```

---

# 35. Important Database Constraints

Use constraints aggressively.

Examples:

```sql
UNIQUE(user_id, quest_id, scheduled_date)
```

for daily quest instances.

```sql
CHECK(amount <> 0)
```

for XP ledger events.

Use foreign keys for:

```text
user
quest
quest_instance
boss
achievement
skill
workout
```

Use indexes on:

```text
user_id
scheduled_date
completed_at
created_at
source_type
source_id
```

---

# 36. Backend Architecture

Recommended stack:

```text
Frontend
React + TypeScript

        ↓

API
Node.js + TypeScript

        ↓

Application Services
├── Quest Service
├── XP Service
├── Level Service
├── Stat Service
├── Achievement Service
├── Boss Service
├── Skill Service
├── Fitness Service
└── Analytics Service

        ↓

PostgreSQL / MySQL

        ↓

Redis
(optional cache / queues)

        ↓

Background Worker
```

---

# 37. Backend Modules

Recommended structure:

```text
src/
├── modules/
│   ├── auth/
│   ├── users/
│   ├── quests/
│   ├── xp/
│   ├── levels/
│   ├── stats/
│   ├── achievements/
│   ├── bosses/
│   ├── skills/
│   ├── career/
│   ├── fitness/
│   ├── nutrition/
│   ├── journal/
│   └── analytics/
│
├── jobs/
│   ├── dailyQuestGeneration
│   ├── weeklyReport
│   └── notifications
│
├── shared/
│   ├── database/
│   ├── events/
│   ├── errors/
│   └── validation/
│
└── app.ts
```

---

# 38. API Design

## Authentication

```http
POST /auth/register
POST /auth/login
POST /auth/logout
GET  /me
```

## Dashboard

```http
GET /dashboard
```

## Quests

```http
GET    /quests/today
GET    /quests
POST   /quests
PATCH  /quests/:id
DELETE /quests/:id

POST   /quests/:id/complete
POST   /quests/:id/uncomplete
```

## Character

```http
GET /character
GET /character/stats
GET /character/xp-history
```

## Bosses

```http
GET   /bosses
POST  /bosses
GET   /bosses/:id
PATCH /bosses/:id
```

## Skills

```http
GET  /skills
POST /skills
GET  /skills/:id
POST /skills/:id/progress
```

## Achievements

```http
GET /achievements
GET /achievements/unlocked
```

## Fitness

```http
GET  /fitness/workouts
POST /fitness/workouts
POST /fitness/weight
POST /fitness/nutrition
```

## Analytics

```http
GET /analytics/day
GET /analytics/week
GET /analytics/month
GET /analytics/year
```

---

# 39. Quest Completion Transaction

This operation should be atomic.

Pseudo-flow:

```text
BEGIN TRANSACTION

1. Load quest instance
2. Verify ownership
3. Verify status != COMPLETED
4. Mark COMPLETED
5. Insert XP ledger event
6. Update XP projection
7. Update attributes
8. Update streak
9. Update boss progress
10. Evaluate achievements

COMMIT
```

If any critical step fails:

```text
ROLLBACK
```

This prevents states such as:

```text
Quest = incomplete
XP = awarded
```

---

# 40. Domain Events

Use events internally.

Examples:

```text
QUEST_COMPLETED
XP_AWARDED
LEVEL_UP
RANK_UP
ACHIEVEMENT_UNLOCKED
BOSS_PROGRESS_UPDATED
BOSS_COMPLETED
SKILL_LEVEL_UP
STREAK_UPDATED
```

Example:

```json
{
  "event": "QUEST_COMPLETED",
  "userId": "123",
  "questId": "381",
  "timestamp": "..."
}
```

Event-driven architecture can initially remain inside the monolith.

Do not introduce microservices just for the sake of complexity.

---

# 41. Notification Engine

Useful notifications:

```text
DAILY_QUESTS_READY

BOSS_DEADLINE_APPROACHING

LEVEL_UP

RANK_UP

ACHIEVEMENT_UNLOCKED

WEEKLY_REVIEW_READY

RECOVERY_QUEST_AVAILABLE
```

Avoid:

```text
"You haven't completed your quest in 17 minutes!!!"
```

The app should not become intrusive.

---

# 42. Analytics

Track:

## Execution

```text
Quest completion %
Quest abandonment %
Streak
Recovery usage
```

## Career

```text
DSA problems solved
Study hours
Topics mastered
System design sessions
Backend learning
```

## Fitness

```text
Workout adherence
Step adherence
Nutrition adherence
Weight trend
Strength progression
```

## Character

```text
XP/day
XP/week
XP/month
Attribute progression
Level progression
```

---

# 43. Personal Score

Optional secondary metric:

```text
Weekly Score =
Quest Completion × 40%
Deep Work × 20%
Career Progress × 20%
Fitness Adherence × 10%
Reflection × 10%
```

Keep this separate from XP.

XP answers:

> "How much did I progress?"

Weekly Score answers:

> "How well did I execute this week?"

---

# 44. Avoiding Bad Gamification

Do NOT:

- punish missed workouts with negative health XP
- encourage extreme calorie restriction
- reward sleep deprivation
- reward excessive work
- make every minute measurable
- create endless notifications
- make the user fear losing progress
- turn life into a constant dashboard

The system should increase agency, not anxiety.

---

# 45. MVP

Build this first:

```text
✓ Authentication
✓ User profile
✓ Character
✓ XP
✓ Levels
✓ Ranks
✓ Seven attributes
✓ Daily quests
✓ Weekly quests
✓ Quest completion
✓ XP ledger
✓ Streaks
✓ Achievements
✓ Dashboard
✓ Basic analytics
```

Do NOT build integrations first.

---

# 46. V2

Add:

```text
□ Skill tree
□ Boss quests
□ Career module
□ DSA tracker
□ Fitness tracker
□ Nutrition tracker
□ Weekly review
□ Notifications
□ Advanced analytics
```

---

# 47. V3

Add:

```text
□ Calendar integration
□ Wearable integration
□ Mobile/PWA
□ Browser extension
□ Pomodoro
□ Automatic workout import
□ Automatic step import
□ External DSA platform integration
```

---

# 48. V4 — Adaptive System

Once sufficient historical data exists:

```text
□ Adaptive quest difficulty
□ Personalized recommendations
□ Neglected-stat detection
□ Automatic boss generation
□ AI weekly planning
□ Automatic review summaries
□ Predictive progress estimation
```

The AI layer should recommend actions, not silently change important goals.

---

# 49. Example Full Day

```text
08:30
SYSTEM generates daily quests.

10:00
Career Quest:
Solve 2 DSA problems
→ COMPLETE
→ +40 XP
→ +INT
→ +DISC

13:00
Deep Work:
60 minutes
→ COMPLETE
→ +30 XP
→ +FOC

18:00
System Design:
45 minutes
→ COMPLETE
→ +30 XP
→ +INT

20:30
Workout:
Push
→ COMPLETE
→ +50 XP
→ +STR
→ +DISC

22:00
Steps:
12,000
→ COMPLETE
→ +25 XP
→ +END

23:00
Daily Reflection:
→ COMPLETE
→ +10 XP
→ +WIS

DAY TOTAL:
185 XP
```

The system then evaluates:

```text
Level Progress
Attribute Progress
Streak
Boss Progress
Achievements
Weekly Score
```

---

# 50. Example Level-Up

```text
╔════════════════════════════════════╗
║                                    ║
║          LEVEL UP                  ║
║                                    ║
║             18 → 19                ║
║                                    ║
║        +1 ATTRIBUTE POINT           ║
║                                    ║
║        SYSTEM UNLOCKED:            ║
║        HARD QUESTS                  ║
║                                    ║
╚════════════════════════════════════╝
```

Attribute points can optionally exist as a secondary mechanic, but the preferred default is still behavior-derived attributes.

---

# 51. System Notifications

Use RPG-style notifications sparingly.

Example:

```text
[ SYSTEM ]

Quest Completed.

"Binary Search — 2 Problems"

Rewards:
+40 XP
+30 INT
+10 DISC
```

Level:

```text
[ SYSTEM ]

LEVEL UP.

18 → 19
```

Boss:

```text
[ SYSTEM ]

BOSS QUEST UPDATED.

SDE2 READY
Progress: 62% → 67%
```

---

# 52. Admin / Debug System

Create a protected admin/debug interface.

Possible actions:

```text
Grant XP
Reverse XP
Create quest
Rebuild XP projection
Recalculate level
Recalculate achievements
Rebuild statistics
Inspect event history
```

Every admin operation must generate an audit log.

---

# 53. Data Integrity

The system should support rebuilding derived data.

For example:

```text
xp_ledger
    ↓
calculate total XP
    ↓
calculate level
    ↓
calculate rank
```

This means `profiles.total_xp` is a projection, not the ultimate source of truth.

The same principle can apply to:

- streaks
- achievements
- boss progress
- analytics

---

# 54. Testing Strategy

## Unit tests

Test:

```text
XP calculation
Level calculation
Rank calculation
Stat rewards
Streak logic
Quest recurrence
Achievement criteria
Boss progress
```

## Integration tests

Test:

```text
Quest completion transaction
Duplicate completion
Quest reversal
Level-up flow
Achievement unlock
Boss completion
```

## End-to-end tests

Example:

```text
Register user
→ configure quests
→ complete quest
→ receive XP
→ level up
→ achievement unlocked
→ dashboard reflects state
```

---

# 55. Security

Implement:

```text
Authentication
Authorization
Input validation
Rate limiting
CSRF protection where applicable
Secure cookies/session handling
Database constraints
Audit logging
Encrypted secrets
```

Never trust frontend-calculated:

```text
XP
level
rank
achievement
quest completion
```

The backend is authoritative.

---

# 56. Performance

The dashboard should ideally require:

```text
1 API request
```

or a small number of efficient requests.

Create a dashboard projection/view if necessary.

Avoid:

```text
GET quests
GET stats
GET xp
GET streak
GET boss
GET achievements
GET analytics
```

for every page load if these can be efficiently aggregated.

---

# 57. Recommended Frontend State

Separate:

```text
Server state
```

from:

```text
UI state
```

Server state:

```text
React Query / TanStack Query
```

UI state:

```text
Context / Zustand / local state
```

Do not put the entire backend database into one global frontend store.

---

# 58. UX Principle

The user should spend more time **doing quests** than managing quests.

Good:

```text
Open app
↓
See today's quests
↓
Do work
↓
Tap complete
↓
System handles everything
```

Bad:

```text
Create task
Assign category
Choose XP
Choose attribute
Set recurrence
Set difficulty
Add tags
Add notes
Create reminder
...
```

Defaults should do most of the work.

---

# 59. First-Time User Experience

Onboarding:

```text
STEP 1
Choose primary goals

Career
Fitness
Personal
Learning
Money
Other
```

```text
STEP 2
Choose weekly priorities
```

```text
STEP 3
Configure schedule
```

```text
STEP 4
System generates initial quest plan
```

```text
STEP 5
SYSTEM INITIALIZED
```

---

# 60. Initial Character Creation

Do not ask the user to manually assign arbitrary stats.

Instead:

```text
Starting Level = 1
Starting XP = 0
Starting Attributes = baseline
```

The character grows through behavior.

Optional onboarding information can personalize quest recommendations without directly inflating stats.

---

# 61. Goal Hierarchy

Every major goal should follow:

```text
VISION
  ↓
GOAL
  ↓
BOSS
  ↓
MILESTONE
  ↓
QUEST
  ↓
ACTION
```

Example:

```text
VISION:
Become a high-level backend engineer

GOAL:
Get SDE2 role

BOSS:
SDE2 READY

MILESTONES:
DSA
System Design
Backend
Projects
Interview

QUESTS:
Solve 2 problems
Study caching
Design URL shortener
Build Redis feature
Mock interview
```

This hierarchy is one of the most important parts of the system.

---

# 62. Dependency System

Quests can depend on other quests.

Example:

```text
Learn Binary Search
      ↓
Solve Binary Search Easy
      ↓
Solve Binary Search Medium
      ↓
Master Binary Search
      ↓
Unlock Advanced Binary Search Quest
```

Dependency statuses:

```text
LOCKED
AVAILABLE
IN_PROGRESS
COMPLETED
```

---

# 63. Quest Templates

Instead of creating every recurring quest manually, create templates.

Example:

```json
{
  "template": "DSA_SESSION",
  "title": "DSA Practice",
  "defaultXp": 40,
  "attributeWeights": {
    "INT": 0.75,
    "DISC": 0.25
  }
}
```

A schedule turns the template into actual quest instances.

---

# 64. Quest Engine Rules

A quest should contain:

```text
id
title
description
type
difficulty
baseXp
attributeRewards
frequency
schedule
verificationType
target
unit
dependencies
deadline
active
```

Example:

```json
{
  "title": "12,000 Steps",
  "type": "DAILY",
  "difficulty": "STANDARD",
  "baseXp": 25,
  "verificationType": "METRIC_THRESHOLD",
  "target": 12000,
  "unit": "steps"
}
```

---

# 65. Verification Types

Support multiple completion methods:

```text
MANUAL
TIMER
COUNT
METRIC_THRESHOLD
INTEGRATION
AUTOMATIC
```

Examples:

```text
Manual:
Write journal

Timer:
Study for 45 minutes

Count:
Solve 2 problems

Metric:
12,000 steps

Integration:
Imported workout

Automatic:
Weekly goal completed
```

---

# 66. Future Integrations

Potential integrations:

```text
Calendar
Apple Health
Google Fit / Health Connect
Garmin
Strava
GitHub
LeetCode
Notion
Todoist
Browser extension
```

The core system must work without any integration.

---

# 67. Personal Operating System Rules

The system should have configurable rules.

Example:

```json
{
  "dailyXpCap": 300,
  "weeklyRecoveryLimit": 1,
  "streakGraceDays": 0,
  "questCompletionUndoMinutes": 30,
  "notificationsEnabled": true
}
```

Do not hardcode these into business logic.

---

# 68. Configuration Architecture

Create:

```text
system_config
user_config
quest_config
reward_config
```

Example:

```text
SYSTEM CONFIG
XP formulas
Rank thresholds
Default quest templates

USER CONFIG
Daily step target
Workout schedule
Study schedule
Notification preferences

REWARD CONFIG
XP values
Attribute weights
Achievement rewards
```

---

# 69. Observability

Track backend metrics:

```text
quest_completion_count
quest_completion_failure_count
xp_events_created
xp_reversal_count
level_up_count
achievement_unlock_count
api_latency
error_rate
```

Structured logs should include:

```text
requestId
userId
event
timestamp
```

Never log sensitive user data unnecessarily.

---

# 70. Definition of Done — MVP

V1 is complete when:

```text
✓ User can register/login
✓ User can configure goals
✓ System generates daily quests
✓ User can complete quests
✓ XP is awarded exactly once
✓ XP ledger is auditable
✓ Level updates correctly
✓ Rank updates correctly
✓ Attributes update correctly
✓ Streaks work
✓ Achievements work
✓ Dashboard shows current state
✓ Historical XP can be viewed
✓ Weekly analytics work
✓ Data can be exported
✓ Automated tests cover core domain logic
```

---

# 71. Recommended Build Order

## Phase 1 — Foundation

```text
Authentication
Database
User profile
Configuration
```

## Phase 2 — Core Game Engine

```text
Quest engine
Quest instances
XP ledger
Level engine
Rank engine
Attribute engine
```

## Phase 3 — Progression

```text
Streaks
Achievements
Boss quests
Skills
Dependencies
```

## Phase 4 — Personal Modules

```text
Career
DSA
Fitness
Nutrition
Focus
Journal
```

## Phase 5 — Analytics

```text
Dashboard
Weekly review
Trends
Reports
```

## Phase 6 — Automation

```text
Background jobs
Notifications
Recurring quest generation
Weekly reports
```

## Phase 7 — Intelligence

```text
Adaptive difficulty
Recommendations
AI planning
Neglected-stat detection
```

---

# 72. Final Product Architecture

The complete system should eventually look like:

```text
                         ┌─────────────────┐
                         │     PLAYER      │
                         └────────┬────────┘
                                  │
                                  ▼
                         ┌─────────────────┐
                         │    DASHBOARD    │
                         └────────┬────────┘
                                  │
             ┌────────────────────┼────────────────────┐
             ▼                    ▼                    ▼
        ┌─────────┐         ┌──────────┐        ┌──────────┐
        │  QUESTS │         │ CHARACTER│        │  BOSSES  │
        └────┬────┘         └────┬─────┘        └────┬─────┘
             │                   │                   │
             ▼                   ▼                   ▼
        ┌─────────┐         ┌──────────┐        ┌──────────┐
        │ ACTIONS │────────►│ XP ENGINE│◄────────│ MILESTONE│
        └─────────┘         └────┬─────┘        └──────────┘
                                 │
                ┌────────────────┼────────────────┐
                ▼                ▼                ▼
             LEVELS           STATS          ACHIEVEMENTS
                │                │                │
                └────────────────┼────────────────┘
                                 ▼
                           ┌────────────┐
                           │  ANALYTICS │
                           └────────────┘

        ┌──────────┐  ┌──────────┐  ┌──────────┐
        │ CAREER   │  │ FITNESS  │  │ PERSONAL │
        └────┬─────┘  └────┬─────┘  └────┬─────┘
             └─────────────┼─────────────┘
                           ▼
                     REAL WORLD DATA
```

---

# 73. The Core Rule

If only one principle survives from this specification, it should be this:

> **The System should make the important things in real life visible, measurable, and rewarding — without becoming another thing that gets in the way of doing them.**

The app is not the achievement.

**The user's real-world progression is the achievement.**
