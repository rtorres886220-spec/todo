# Anchor: Behind-The-Scenes Cognitive Architecture & Deep Reasoning Engine

An intelligent, non-invasive systems architecture that equips Anchor with advanced Gemini reasoning, autonomous background optimization, and local reflective habit learning—operating entirely behind the scenes with **zero changes to the existing user interface**.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> The following explicit user constraints strictly govern this implementation:
> - **Zero UI Modifications**: The user interface, layouts, screens, tabs, cards, and theme remain 100% visually untouched. No new visual banners, buttons, or screens will be introduced.
> - **Intelligent Task Mutability ("Know what it is doing")**: The AI automatically evaluates whether a task is **non-negotiable** (fixed external commitments such as flights, meetings, doctor visits, or hard deadlines) versus **negotiable** (flexible internal tasks like drafting, errands, or research). Non-negotiable tasks are strictly locked; only flexible tasks are intelligently scheduled around them.
> - **Strategic Guidance Delivered Through Micro-Steps**: The AI injects its strategic advice, tactical sequencing, and friction-reducing starter moves directly into the task's existing micro-steps breakdown (`aiBreakdownSteps`), seamlessly viewable inside the existing task details sheet.
> - **Local Reflective Habit Memory**: A dedicated local Room telemetry layer logs completion velocity, snooze friction, and estimation variance without external network latency, allowing the AI to learn personal user rhythms and self-correct estimates.
> - **Quota-Guarded Zero-Waste Pipeline**: Smart debounce (1500ms), differential state change detection, and high-density prompt compaction ensure maximum Gemini reasoning depth with minimal API quota consumption.

---

## 1. Overview & Core Concept

### What It Does
An autonomous, background-only cognitive layer that acts as a silent chief of staff. When tasks are added, completed, or postponed, the background engine evaluates real-world constraints, detects friction, logs behavioral telemetry, and enriches tasks with tactical micro-steps and calibrated durations—leaving the visual design intact.

### Key Capabilities
1. **Semantic Mutability Detection**: Understands context to recognize immutable commitments (e.g., "Doctor at 2 PM", "Interview with Jane") vs. malleable goals ("Write blog post", "Clean garage").
2. **Behavioral Self-Calibration**: Tracks actual completion time versus initial estimates, learning the user's specific ratio (e.g., 1.35x on deep work) to silently self-correct duration forecasts.
3. **Tactical Micro-Step Generation**: Breaks down complex or repeatedly snoozed anchors into hyper-specific, friction-free action steps injected directly into the existing sub-task list.
4. **Token-Guarded Efficiency**: Compresses contextual state into high-density prompts and skips API calls when local heuristics suffice or task state hasn't semantically changed.

---

## 2. User Experience & Visual Design

### Strict UI Invariance
- **No visual layout changes**: All existing Composables (`TodayView`, `InboxView`, `UpcomingView`, `NotesView`, `DoneView`, `AnchorCard`, `BentoGridCard`, `CaptureBar`, `QuietNavigationBar`) retain their exact visual styling, structure, and hierarchy.
- **Natural Surfacing**:
  - Strategic guidance and breakdown steps appear organically within the existing `TaskDetailSheet` checklist.
  - Due times and estimated minutes adjust gracefully in their existing metadata slots on task cards.
  - The existing Morning Brief and Evening Review dialogs reflect deeper, habit-aware insights without altering their layout.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Semantic Mutability Tagging (Non-Negotiable vs. Flexible)**
  - *Chosen Approach*: During AI capture parsing and background evaluation, the AI assigns an internal `isLockedSchedule` flag based on temporal specificity and external dependency indicators.
  - *Why*: Prevents the AI from shifting critical hard-calendar events, while giving it full strategic latitude to optimize flexible workloads.
  - *Alternatives Considered*: Manual lock toggles would require UI changes; automated semantic understanding honors the zero-UI-change mandate.

- **Decision 2: Micro-Step Injection as the Strategic Delivery Vehicle**
  - *Chosen Approach*: Inject strategic reasoning directly as the first high-leverage sub-step in `item.aiBreakdownSteps` (e.g., `["★ LEVERAGE: Review Section 3 first (unlocks team review)", "Draft executive summary", "Verify numbers"]`).
  - *Why*: Utilizes existing UI components with zero visual redesign, giving immediate actionable value where the user actually executes.

- **Decision 3: Local SQLite Habit Telemetry vs. In-Memory Tracking**
  - *Chosen Approach*: Store historical execution records (task ID, time of day, estimated vs. actual minutes, snooze count) in a local Room table (`HabitMetricEntity`).
  - *Why*: Allows the AI to remember habits and self-correct across app launches, with zero cloud dependency and zero API quota usage.

- **Decision 4: High-Density Pipelined Prompt Compaction**
  - *Chosen Approach*: Format active tasks into compact piped strings (`ID|Title|Prio|Due|Snoozes|EstMins|Locked`) rather than full JSON in prompts.
  - *Why*: Cuts input token size by over 70%, drastically speeding up response times and preventing quota exhaustion.

---

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│               BEHIND-THE-SCENES COGNITIVE DATA FLOW                   │
│                                                                        │
│   Existing UI (Unchanged)                                              │
│   [TodayView / InboxView / TaskDetailSheet / MorningBrief]             │
│                         │                                              │
│                         ▼ User Action (Capture / Complete / Snooze)    │
│   ┌────────────────────────────────────────────────────────────────┐   │
│   │ AnchorViewModel                                                │   │
│   │ - Emits state change event                                     │   │
│   │ - Debounces background processing (1500ms)                     │   │
│   └──────────────────────────────┬─────────────────────────────────┘   │
│                                  │                                     │
│                                  ▼                                     │
│   ┌────────────────────────────────────────────────────────────────┐   │
│   │ StrategicOrchestrator (Background Engine)                      │   │
│   │ 1. Logs HabitMetric (timestamp, action, duration variance)     │   │
│   │ 2. Calculates User Velocity & Estimation Multiplier            │   │
│   │ 3. Classifies Task Mutability (Locked vs. Flexible)            │   │
│   │ 4. Computes Diff: Does state require Deep Gemini Reasoning?    │   │
│   └──────────────┬─────────────────────────────────┬───────────────┘   │
│                  │ If Semantics Changed            │ If No Deep Call   │
│                  ▼                                 ▼ Needed            │
│   ┌───────────────────────────────┐  ┌─────────────────────────────┐   │
│   │ GeminiService (Tier 2 AI)     │  │ Local Heuristic (Tier 1)    │   │
│   │ - High-Reasoning Flash Model  │  │ - Instantaneous rule sort   │   │
│   │ - Compact Pipelined Context   │  │ - Local estimation adjust   │   │
│   │ - Strategic Micro-Step Engine │  │ - Zero API Quota Consumed   │   │
│   └──────────────┬────────────────┘  └─────────────┬───────────────┘   │
│                  │                                 │                   │
│                  └────────────────┬────────────────┘                   │
│                                   ▼                                    │
│   ┌────────────────────────────────────────────────────────────────┐   │
│   │ Room Database (AnchorDatabase)                                 │   │
│   │ - Updates AnchorItem (calibrated estMinutes, aiBreakdownSteps) │   │
│   │ - Updates HabitMetrics table                                   │   │
│   └────────────────────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────────────────────┘
```

### Data Layer Specifications

1. **`HabitMetricEntity` (Room Table: `habit_metrics`)**:
   - `id`: Long (Primary Key, autogenerate)
   - `itemId`: Long (Associated anchor item)
   - `actionType`: String (`"complete"`, `"snooze"`, `"create"`)
   - `timestamp`: Long
   - `hourOfDay`: Int (0-23)
   - `dayOfWeek`: Int (1-7)
   - `estimatedMinutes`: Int
   - `actualMinutes`: Int?
   - `snoozeCountAtAction`: Int

2. **`AnchorItem` Schema Extension**:
   - `isLockedSchedule`: Boolean = `false` (flag indicating immutable hard commitments)
   - Existing fields (`aiBreakdownSteps`, `estimatedMinutes`, `priority`, `dueDate`, `dueTime`) updated in place.

3. **Behavioral Profile Engine (`HabitProfiler`)**:
   - Computes rolling estimation bias: `actual_minutes / estimated_minutes`.
   - Identifies peak performance hours (hours with highest completion velocity).
   - Identifies friction tags (tags with highest snooze rates).

---

## 5. Verification & Test Plan

1. **Automated Unit & Robolectric Tests**:
   - `TaskMutabilityTest`: Verifies that appointment/flight/hard-deadline tasks are correctly tagged as locked while general tasks remain flexible.
   - `HabitLearningTest`: Verifies that repeated completion delays adjust the rolling estimation multiplier and auto-correct task durations.
   - `MicroStepInjectionTest`: Verifies that strategic high-leverage guidance is prepended into `aiBreakdownSteps` properly formatted for the existing UI checklist.
   - `QuotaThrottlingTest`: Verifies that rapid bursts of task mutations trigger only one debounced Gemini evaluation.
2. **Build & Regression Verification**:
   - Run `compile_applet` to confirm clean compilation.
   - Run `gradle :app:testDebugUnitTest` to guarantee all existing and new unit tests pass.
   - Ensure zero visual or layout regressions across all existing Compose screens.
