# Anchor: Full Codebase Audit & Defect Resolution Plan

A comprehensive engineering audit of the Anchor Android codebase was performed. This document details the audit findings, architecture, root causes, and the concrete implementation plan to repair all non-working components, broken user flows, and edge cases.

---

### User Review & Critical Decisions

> [!IMPORTANT] 
> The following user preferences were confirmed in Phase 1 clarification and govern the implementation:
> - **Today Time Horizon Filtering**: The Day / Week / Month filter pills and circular day chips will dynamically filter tasks by time horizon and selected date, rather than switching to disjoint screens.
> - **Reminder & Notification Scheduling**: Full Android `AlarmManager` scheduling will be wired for task due times, morning brief, and evening review alarms.
> - **Note Viewer & Editor**: Notes will open in a dedicated, distraction-free Note reader and editor sheet tailored to text and ideas rather than task metrics.

---

## 1. Overview & Core Concept

**Anchor** is a zero-friction capture to-do and notes Android application with a soft-dimensional dark luxury aesthetic, powered by local Room persistence and Gemini AI intelligence.

### Audit Summary & Defect Inventory
During the full audit, 10 specific functional and UI bugs/deficits were identified:

1. **Non-functional Time Horizon Filter (TodayView)**: The `Day` / `Week` / `Month` pills change visual selection state but do not filter the displayed tasks at all.
2. **Dead-End Circular Day Chips**: The horizontal date chips at the top of Today view vibrate on click but perform no date filtering or selection.
3. **Inappropriate Note Detail Presentation**: Tapping a Note opens `TaskDetailSheet`, which presents task-only UI (start/end time, duration, and a "Complete Anchor" button that marks notes as completed tasks).
4. **Mocked Micro-Steps Progression**: `TaskDetailSheet` renders hardcoded dummy circular rings (`listOf(1.0f, 0.75f, 0.4f, 0.1f)`) rather than loading and checking off the task's actual `item.aiBreakdownSteps`.
5. **Ephemeral Settings (No Persistence)**: Custom morning/evening brief times, quiet hours, and escalation settings in `SettingsSheet` only exist in memory and reset to defaults upon app relaunch.
6. **Missing System Alarm Scheduling**: Notifications exist only as manual test triggers; background alarms are never scheduled via Android's `AlarmManager` for due tasks or daily brief nudges.
7. **Voice Capture Failure Handling**: Clicking the microphone orb crashes or fails silently on emulators/devices lacking speech recognition packages without fallback feedback.
8. **Missing System Back Navigation (`BackHandler`)**: Pressing the Android back button from Inbox, Upcoming, Notes, or Done immediately exits the application rather than returning to Today, and does not dismiss open modal sheets.
9. **Inverted Swipe Actions on TodayView**: Swiping an item in Today view executes `onMoveToToday()`, which is redundant, and overdue task calculations miss tasks due earlier today whose scheduled time has passed.
10. **Gemini Model API Fallback Order**: The model candidate list uses invalid endpoint names before falling back to local heuristic parsing.

---

## 2. User Experience & Visual Design

### Unified Soft-Dimensional Visual Identity
All fixes will strictly maintain Anchor's luxury dark aesthetic:
- **Background Atmosphere**: `#08080A` with `#101014` radial lift and 3% subtle stippled film-grain overlay.
- **Accents & Tokens**: Champagne gold (`#E6C98A`), muted warm off-white (`#F2EFEA`), and urgency coral (`#FF7A5C`).
- **Typography**: Geometric display headers in *Plus Jakarta Sans* (300 weight), precision metadata in *Geist Mono* (11px, 0.14em tracking).
- **Surface Elevation**: 28px rounded corners, 1px top highlight border (`rgba(255,255,255,0.07)`), and soft layered drop shadows.

### Repaired Screen Flows

```
┌─────────────────────────────────────────────────────────────┐
│                          ANCHOR                             │
│                                                             │
│   ┌─────────────────────────────────────────────────────┐   │
│   │ [Today]  (18) (19) (20) (21)  <-- Active Day Filter  │   │
│   │ ┌─────────────────────────────────────────────────┐ │   │
│   │ │ OCTOBER 29 • 02:30 PM      NEXT ANCHOR          │ │   │
│   │ │ Quarterly architecture review & sync            │ │   │
│   │ └─────────────────────────────────────────────────┘ │   │
│   └─────────────────────────────────────────────────────┘   │
│                                                             │
│   Up next:  [Day] [Week] [Month]   • BRIEF  • REVIEW        │
│   ┌───────────────────────┐   ┌─────────────────────────┐   │
│   │ 14:30                 │   │ 16:00                   │   │
│   │ Bento Card (Sage)     │   │ Bento Card (Apricot)    │   │
│   └───────────────────────┘   └─────────────────────────┘   │
│                                                             │
│   [ Capture task, thought, or memo...        (MIC) (SEND) ] │
│   [ INBOX (3) | TODAY (5) | UPCOMING | NOTES | DONE       ] │
└─────────────────────────────────────────────────────────────┘
```

1. **Today View with Active Date & Horizon Filtering**:
   - Selecting **Day**: Shows today's tasks or the specific tapped day chip date (with a clear badge to return to Today).
   - Selecting **Week**: Aggregates tasks across the next 7 days grouped by calendar date.
   - Selecting **Month**: Expands the view to all planned tasks within 30 days.
2. **Dedicated Note Viewer & Editor Sheet (`NoteDetailSheet`)**:
   - Tapping any card in the **Notes** tab opens a sleek full-screen editor.
   - Clean title editing, multi-line markdown/body note editing, tag editor chips, "Copy to Clipboard" with haptic confirmation, and "Delete Note".
   - Completely removes task-specific widgets like "Sprint Mode" and "Complete Anchor".
3. **Interactive Micro-Steps Checklist in `TaskDetailSheet`**:
   - Parses the task's stored micro-steps (`item.aiBreakdownSteps`) into actual checklist items.
   - Tapping each step toggles its done state with haptic feedback and animates the progress indicator.
   - Includes an inline field to add quick extra sub-steps.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Persistent SharedPreferences Settings vs. New Database Table**
  - *Chosen Approach*: Implement an `AnchorPreferences` manager using Android `SharedPreferences`.
  - *Rationale*: App settings (notification times, quiet hours, inactivity nudge intervals) are lightweight key-value preferences. Storing them in `SharedPreferences` ensures instant read on startup without Room database migrations or asynchronous query overhead.
- **Decision 2: AlarmManager vs. WorkManager for Task Reminders**
  - *Chosen Approach*: Use `AlarmManager` with `PendingIntent` and `BroadcastReceiver` for scheduled task due times and morning/evening briefs.
  - *Rationale*: WorkManager is designed for deferrable background jobs; exact task reminders and scheduled morning briefs require timely alarm precision.
- **Decision 3: Today Swipe Actions**
  - *Chosen Approach*: In `TodayView`, swipe right triggers Complete (`onComplete`), and swipe left triggers Snooze (`onSnooze`). In `InboxView`, swipe right moves to Today.
  - *Rationale*: A task already in Today doesn't need to be moved to Today. Marking complete on swipe right is the standard, natural gesture.
- **Decision 4: Time-Aware Overdue Calculation**
  - *Chosen Approach*: Check both `dueDate < today` OR (`dueDate == today` AND `dueTime < currentTime`).
  - *Rationale*: A task scheduled for 09:00 AM today is overdue when viewed at 05:00 PM today.

---

## 4. Technical Architecture & Data Strategy

### System Component Diagram

```
┌───────────────────────────────────────────────────────────────────────────┐
│                               MainActivity                                │
│   ├─ enableEdgeToEdge()                                                   │
│   ├─ BackHandler Navigation & Sheet Dismissal                             │
│   └─ Incoming Intent / Voice Capture Dispatcher                           │
└─────────────────────────────────────┬─────────────────────────────────────┘
                                      │
                                      ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                              AnchorViewModel                              │
│   ├─ uiState (activeTab, filters, selectedDate, dialog/sheet states)      │
│   ├─ AnchorPreferences (morningTime, eveningTime, quietHours, deadApp)    │
│   ├─ AlarmScheduler (schedules task reminders & daily brief alarms)       │
│   └─ AnchorRepository ─── Room Database (AnchorDao / AnchorItem)          │
└──────────────┬─────────────────────────────────────────────┬──────────────┘
               │                                             │
               ▼                                             ▼
┌───────────────────────────────┐             ┌─────────────────────────────┐
│          Views Layer          │             │      Sheet & Modal Layer    │
│  ├─ TodayView (Day/Wk/Mo)     │             │  ├─ TaskDetailSheet (Steps) │
│  ├─ InboxView (Unsorted)      │             │  ├─ NoteDetailSheet (Reader)│
│  ├─ UpcomingView (Timeline)   │             │  ├─ EditItemSheet (Picker)  │
│  ├─ NotesView (Bento Grid)    │             │  ├─ MorningBriefDialog      │
│  └─ DoneView (Strikethrough)  │             │  ├─ EveningReviewDialog     │
│                               │             │  ├─ SnoozeCoachDialog       │
│                               │             │  └─ SettingsSheet (Persist) │
└───────────────────────────────┘             └─────────────────────────────┘
```

### Detailed Fix Execution Steps

1. **Data & Persistence Layer**:
   - Create `app/src/main/java/com/example/data/AnchorPreferences.kt` to serialize and load all user preferences (`morningTime`, `eveningTime`, `quietHours`, `escalationIntensity`, `deadAppHours`).
   - Update `AnchorDao.kt` with query helpers or repository helpers to properly flag time-aware overdue items for today.
   - Update `AnchorRepository.kt` to persist and manage subtask check states in `aiBreakdownSteps` (e.g. `[x] step title`).

2. **Alarm & Notification Scheduling**:
   - Create `app/src/main/java/com/example/notification/AnchorAlarmScheduler.kt` with methods:
     - `scheduleTaskReminder(item: AnchorItem)`
     - `cancelTaskReminder(taskId: Long)`
     - `scheduleDailyBriefs(morningTime: String, eveningTime: String)`
   - Update `AnchorNotificationReceiver.kt` to receive `ACTION_TASK_REMINDER`, `ACTION_MORNING_BRIEF`, and `ACTION_EVENING_REVIEW`.
   - Update `BootReceiver.kt` to reschedule alarms on device boot.

3. **Sheets & Interactive Components**:
   - Create `app/src/main/java/com/example/ui/components/NoteDetailSheet.kt` for viewing, editing, copying, and deleting notes without task UI baggage.
   - Upgrade `TaskDetailSheet.kt` to parse `aiBreakdownSteps`, render interactive checklist rows with completion strikes and a progress bar, and provide an "Add subtask" input.
   - Upgrade `SettingsSheet.kt` to load from and save to `AnchorPreferences`, and invoke `AnchorAlarmScheduler` to reschedule morning/evening alarms upon saving.
   - Update `EditItemSheet.kt` with intuitive priority selectors (High/Med/Low pills) and quick date buttons.

4. **Screen & View Improvements**:
   - **`TodayView.kt`**: Implement actual filtering for `Day`, `Week`, and `Month`. Wire circular day chips to filter tasks for that selected date with a reset option. Change card swipe to complete (right) and snooze (left).
   - **`MainScreen.kt`**: Add `BackHandler` so pressing Back closes any open sheet/dialog first, and if on a secondary tab, navigates back to `AnchorTab.TODAY`. Route tapped notes to `NoteDetailSheet` instead of `TaskDetailSheet`.
   - **`CaptureBar.kt`**: Add package manager check for speech recognition with clean toast fallback if unavailable, and ensure voice capture is reliable.

5. **AI Service Polish**:
   - In `GeminiService.kt`, update model candidates to `gemini-2.5-flash`, `gemini-2.0-flash`, `gemini-1.5-flash` with clean JSON parsing and fallback.

6. **Verification & Testing**:
   - Run `compile_applet` to confirm clean compilation across all modules.
   - Verify all test tags (`settings_button`, `tab_today`, `tab_inbox`, `tab_notes`, `today_hero_card`, `note_detail_sheet`, etc.) remain in place.
