# Testing and Verification Guidelines
When making code or UI changes, always verify them efficiently to avoid repeated errors and save tokens/usage.
- After implementing fixes, double-check that the code modifications actually replaced the intended logic (especially when dealing with hardware back button overrides or navigation).
- When a UI element is supposed to be removed, ensure that BOTH its XML definition and all associated programmatic references (like click listeners or visibility toggles) are fully stripped out to avoid duplicates or crashes.
- Do not blindly assume edits were fully successful—if multiple changes are made, review the file or build the app locally to confirm everything behaves as expected without wasting the user's time on partial fixes.

# UI Failure Protocol
- If a UI fix attempt fails and the user points out that it is still broken after I thought I fixed it, I MUST run a UI test myself to verify the issue after my next fix attempt.
- Alternatively, I should explicitly ask the user if they want me to run a UI test after the second fix attempt.

# Project Documentation and Context
- This repository contains a highly detailed documentation file at `CLAUDE.md`.
- **CRITICAL**: Before making architectural changes, adding new features, or modifying database schemas, you MUST read `CLAUDE.md` to understand the project's existing conventions, MVVM structure, and database history.
- Do not guess the architecture or authentication flows; they are extensively documented there.

# Design & UX Patterns
- **Screen Transitions:** The app uses a bespoke 350ms crossfade (`screen_fade_in` / `screen_fade_out` via `windowAnimationStyle` in `themes.xml`) instead of standard Android horizontal slides. This isolates the vertical glass-card slide (`Entrance.play()`) as the primary motion on screen over the wood backgrounds. Note this exact implementation to easily port it to the sister app when ready.

# UI Performance Rules
- **RecyclerView Optimization:** Never use nested `ConstraintLayout`s inside `RecyclerView` item definitions (e.g. `item_assignment.xml`). Nested `ConstraintLayout`s cause multiple layout passes, which will severely lag simultaneous transition animations like `Entrance.play()`. Always use flat `LinearLayout`s or a single flat `ConstraintLayout` for list items.

# Feature Tracking Rules
- **CRITICAL**: Before suggesting features or bug fixes to the user, ALWAYS verify the current code to confirm the issue actually still exists. Do NOT suggest things based on memory or session summaries alone — the code is the source of truth.
- Maintain the completed features list below. When a feature or bug fix is shipped (committed/pushed), add it here immediately. When suggesting "next steps", cross-reference this list first and never re-suggest completed items.

## Completed Features & Fixes
- [x] Notification reliability: AlarmManager.setExactAndAllowWhileIdle used across all schedulers (AssignmentReminderScheduler, CustomEventScheduler, FocusTimerScheduler, ReviewReminderScheduler)
- [x] Boot receiver: BootReceiver.kt reschedules all alarms after device reboot
- [x] Assignment row redesign: rows 15% larger, 4-button 2x2 grid (done, checklist, edit, delete), thin divider lines, title repositioned
- [x] Tapping assignment row (not buttons) navigates to deck overview for that assignment
# Testing and Verification Guidelines
When making code or UI changes, always verify them efficiently to avoid repeated errors and save tokens/usage.
- After implementing fixes, double-check that the code modifications actually replaced the intended logic (especially when dealing with hardware back button overrides or navigation).
- When a UI element is supposed to be removed, ensure that BOTH its XML definition and all associated programmatic references (like click listeners or visibility toggles) are fully stripped out to avoid duplicates or crashes.
- Do not blindly assume edits were fully successful—if multiple changes are made, review the file or build the app locally to confirm everything behaves as expected without wasting the user's time on partial fixes.

# UI Failure Protocol
- If a UI fix attempt fails and the user points out that it is still broken after I thought I fixed it, I MUST run a UI test myself to verify the issue after my next fix attempt.
- Alternatively, I should explicitly ask the user if they want me to run a UI test after the second fix attempt.

# Project Documentation and Context
- This repository contains a highly detailed documentation file at `CLAUDE.md`.
- **CRITICAL**: Before making architectural changes, adding new features, or modifying database schemas, you MUST read `CLAUDE.md` to understand the project's existing conventions, MVVM structure, and database history.
- Do not guess the architecture or authentication flows; they are extensively documented there.

# Design & UX Patterns
- **Screen Transitions:** The app uses a bespoke 350ms crossfade (`screen_fade_in` / `screen_fade_out` via `windowAnimationStyle` in `themes.xml`) instead of standard Android horizontal slides. This isolates the vertical glass-card slide (`Entrance.play()`) as the primary motion on screen over the wood backgrounds. Note this exact implementation to easily port it to the sister app when ready.

# UI Performance Rules
- **RecyclerView Optimization:** Never use nested `ConstraintLayout`s inside `RecyclerView` item definitions (e.g. `item_assignment.xml`). Nested `ConstraintLayout`s cause multiple layout passes, which will severely lag simultaneous transition animations like `Entrance.play()`. Always use flat `LinearLayout`s or a single flat `ConstraintLayout` for list items.

# Feature Tracking Rules
- **CRITICAL**: Before suggesting features or bug fixes to the user, ALWAYS verify the current code to confirm the issue actually still exists. Do NOT suggest things based on memory or session summaries alone — the code is the source of truth.
- Maintain the completed features list below. When a feature or bug fix is shipped (committed/pushed), add it here immediately. When suggesting "next steps", cross-reference this list first and never re-suggest completed items.

## Completed Features & Fixes
- [x] Notification reliability: AlarmManager.setExactAndAllowWhileIdle used across all schedulers (AssignmentReminderScheduler, CustomEventScheduler, FocusTimerScheduler, ReviewReminderScheduler)
- [x] Boot receiver: BootReceiver.kt reschedules all alarms after device reboot
- [x] Assignment row redesign: rows 15% larger, 4-button 2x2 grid (done, checklist, edit, delete), thin divider lines, title repositioned
- [x] Tapping assignment row (not buttons) navigates to deck overview for that assignment
- [x] Checklist screen: slide-in animation, empty state with icon + text, working "+" button
- [x] Empty state animations: icon and text slide in on checklist screen
- [x] RecyclerView performance: item_assignment.xml buttonGrid converted from nested ConstraintLayout to flat LinearLayouts, fixing entrance animation lag
- [x] AssignmentsActivity performance: color/assignment swatches only rebuilt when data changes (not on every panel swap)
- [x] FlashcardDecksActivity performance: same swatch caching optimization applied
- [x] Unit Testing Architecture: Configured MockK, kotlinx-coroutines-test, and `MainCoroutineRule.kt`. Fixed flakiness in `HomeViewModel`, `ReviewDeckViewModel`, and `CalendarViewModel` tests by ensuring Room database background operations correctly bypass explicit `Dispatchers.IO` launching. All 68 tests now pass successfully.
- [x] Calendar UI Enhancements: Inline Editing of Custom Events, Consistent Icon Spacing, and Pull-to-Refresh.
- [x] Accessibility Improvements: View content-descriptions and 48dp minimum touch targets.
- [x] Polish "At a Glance" Statistics: Exclude completed items from the active count.
- [x] Home screen Tall Calendar Widget (2x3) with updating logic upon database changes.
- [x] Calendar Activity swipe gestures to change month.
