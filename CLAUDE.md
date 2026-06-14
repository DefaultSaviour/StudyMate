# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## About StudyMate

An Android study-companion app for students. Core features:
- **Subjects** — create named subject groups
- **Assignments** — track due dates and completion per subject
- **Flashcards** — create decks of question/answer cards per subject; review with **SM-2 spaced repetition** (Again / Wrong / Correct)
- **Statistics** — study dashboard: cards due/reviewed, study streak, mature cards, and assignment completion (all computed live)
- **Calendar** — view assignments by date
- **Sign-in** — multi-user accounts (unique username), with optional one-device biometric / screen-lock quick sign-in (see "Authentication & Sign-in")
- **Reminders** — per-assignment local notifications (T-7d, T-1d, day-of) via WorkManager (see "Notifications")

## Build & Test Commands

All commands run from the project root. On Windows use `gradlew.bat` instead of `./gradlew`.

```bash
# Build
./gradlew assembleDebug

# Unit tests (JVM, no emulator)
./gradlew test

# Single unit test class
./gradlew test --tests "uws.ac.uk.studymate.ExampleUnitTest"

# Instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Single instrumented test class
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=uws.ac.uk.studymate.data.dao.UserDaoTest

# Install debug build
./gradlew installDebug

# Lint
./gradlew lint
```

## Architecture

**MVVM + Repository pattern** — no DI framework (Hilt/Dagger); repositories are instantiated directly in ViewModels.

```
LoginActivity / RegisterActivity → LoginViewModel / RegisterViewModel
                                          ↓
HomeActivity / AssignmentsActivity / ...  → HomeViewModel / AssignmentsViewModel / ...
                                          ↓
                              Repositories (UserRepo, SubjectRepo, ...)
                                          ↓
                                  Room DAOs → StudyMateDatabase (v9)
```

**Session flow:** `LoginActivity` writes a session via `SessionManager` (SharedPreferences). Every ViewModel that needs the current user calls `SessionUserResolver.resolveUser()` to load the `User` entity — do not read SharedPreferences directly in ViewModels.

> `RegisterActivity` no longer exists — sign-up was folded into `LoginActivity`'s panel-swap router (sign-in / signup-chooser / signup-password). See "Authentication & Sign-in".

## Key Files & Packages

| Path | Purpose |
|------|---------|
| `data/StudyMateDatabase.kt` | Room singleton, v9, exposes all DAOs; migrations `MIGRATION_4_5` … `MIGRATION_8_9` and the `MIGRATIONS` array live here |
| `util/SpacedRepetition.kt` | Pure-Kotlin SM-2 scheduler (ease/interval/repetitions → next due date); maps the 4 review buttons to SM-2 quality. Unit-tested |
| `data/entities/` | Room `@Entity` classes — one file per table |
| `data/relations/` | `@Relation` data classes for one-to-many queries (e.g. `SubjectWithAssignments`) |
| `data/repositories/` | All DB access goes through repos; methods are `suspend` functions |
| `ui/viewmodels/` | One ViewModel per screen; use `viewModelScope` + `Dispatchers.IO` for repo calls |
| `StudyMateApplication.kt` | `Application` subclass — creates the `assignment_reminders` and `review_reminders` notification channels on startup; registered via `android:name` in the manifest |
| `util/SessionManager.kt` | SharedPreferences session read/write; also stores `authMode` and `lastUserId` for the cold-launch fast path |
| `util/SessionUserResolver.kt` | Validates session ID and returns the `User` — single source of truth for "who is logged in" |
| `util/BiometricLoginManager.kt` | BiometricPrompt wrapper + `EncryptedSharedPreferences` credential store for the one-device biometric account; `isEnabledForUser` / `isOwnedByAnotherUser` answer quick-sign-in ownership per account |
| `util/BiometricOwnership.kt` | Pure (Android-free) per-account ownership rules for the single quick-sign-in slot; unit-tested in `BiometricOwnershipTest` |
| `util/AssignmentDateTimeUtils.kt` | Shared date/time parsing & formatting — use this rather than duplicating logic |
| `util/PasswordUtils.kt` | PBKDF2 hashing; used only during registration and login |
| `util/KeyboardInsets.kt` | Adds IME-height bottom padding so text fields aren't hidden by the keyboard (edge-to-edge fix — see "Keyboard / IME insets") |
| `ui/PulseRingView.kt` | Custom `View` that paints a soft, slowly breathing gold halo around a rounded-rect ring; overlaid on the dashboard "Review due decks" button (see "Review due decks") |
| `notifications/AssignmentReminderScheduler.kt` | Schedules / cancels per-assignment reminder work (see "Notifications") |
| `notifications/AssignmentReminderWorker.kt` | `CoroutineWorker` that re-verifies state at fire time and posts the notification |

## Database Conventions

- Database version is **9**. Any schema change requires a new `Migration` object, adding it to the `MIGRATIONS` array, and a bump to the version constant in `StudyMateDatabase`.
- Migration history: `4→5`, `5→6` (earlier schema), `6→7` (wipe `User`), `7→8` (multi-user / one-bio model — drops the email unique index, adds the `auth_mode` column defaulting to `password`, adds a unique index on `name`; wipes `User` first to avoid name collisions), `8→9` (spaced repetition — adds SM-2 columns `ease_factor`/`interval_days`/`repetitions`/`due_at`/`last_reviewed_at` to `Flash_Cards`, `completed_at` to `Assignments`, and creates the `Review_Logs` table; additive only, existing rows preserved).
- The `User` table's user-facing identifier is **`name`** (unique). `email` is now an internal placeholder, not user-visible. `auth_mode` is `password` or `biometric_only`.
- Foreign keys use `CASCADE` delete — deleting a `User` removes all their subjects, assignments, flashcards, etc.
- DAOs use `LOWER()` for case-insensitive lookups.
- Multi-table queries use `@Transaction` on the DAO method.

## Testing Conventions

Instrumented tests live in `app/src/androidTest/`. All DAO and Repository tests extend `RoomDbTestBase` which provides:
- An in-memory Room database (no migrations applied)
- Helper insert methods: `insertUser()`, `insertSubject()`, `insertAssignment()`, etc.

Migration tests (`StudyMateDatabaseMigrationTest`) use `MigrationTestHelper` with the real on-disk database and must cover every new migration.

## Authentication & Sign-in

**Model: multi-user, one-bio.** Any number of accounts can exist on a device, each with a unique `name`. Each account is either a **password** account or a **biometric_only** account (`auth_mode`). At most **one** account on the device may own the biometric / screen-lock quick sign-in slot.

- **`LoginActivity` is a panel-swap router**, not a navigation graph. Three panels live in one card: `signInPanel`, `signupChoosePanel`, `signupPasswordPanel`. There is no `RegisterActivity` (deleted). The visible panel is chosen from DB state on launch.
- **`BiometricLoginManager`** wraps `androidx.biometric.BiometricPrompt` with `BIOMETRIC_STRONG | DEVICE_CREDENTIAL` (fingerprint, face, or PIN/pattern/password). It stores the owning account's credentials in `EncryptedSharedPreferences` (`androidx.security:security-crypto`) keyed by `user_id`. Only one credential set exists at a time — that enforces the one-bio rule.
- **Cold-launch fast path:** `SessionManager.lastUserId` + `authMode` let `LoginActivity` show a minimal panel for the returning user — just the biometric prompt (biometric_only) or just the password field (password). The full sign-in screen only appears after an explicit **Sign out**.
- **Biometric prompt timing:** auto-launch the prompt at the *start* of the splash fade-out (`splashDone` / `pendingBiometricAutoLaunch`), never over the splash itself.
- **One-bio enforcement on signup:** when the bio slot is already taken, the "Use fingerprint or screen lock" signup option is hidden and the password path is promoted to primary. The biometric toggle in `UserSettings` likewise refuses to enable if another account owns the slot.
- **Per-account toggle state (important):** the biometric store is a single device-global slot (one `enabled` flag + one `storedUserId`). The `UserSettings` "Quick sign-in" toggle must therefore be evaluated **per account**, never from the global `isEnabled()` — use `BiometricLoginManager.isEnabledForUser(uid)` for the checked state and `isOwnedByAnotherUser(uid)` to disable + grey the row for non-owners ("Another account on this device already uses quick sign-in"). The pure rules live in `util/BiometricOwnership` and are unit-tested (`BiometricOwnershipTest`). Reading the global flag was a real bug: a second account showed the toggle as ON for a slot a different account owned.
- **Sign out** clears the session and `lastUserId` but must **not** call `clearCredentials()` — that would orphan a biometric_only account and make it unrecoverable.
- Username uniqueness is validated **early** (on the chooser, before the password panel), not after the user has typed a password.
- The "or" divider on the sign-in panel is shown only when **both** the Sign In button and the biometric button are visible (`R.id.orDivider` in `LoginActivity`).

## Notifications

Per-assignment local reminders via **WorkManager** (`androidx.work:work-runtime-ktx`). No server, no FCM.

- **Channel:** `assignment_reminders`, created in `StudyMateApplication.onCreate()`.
- **`AssignmentReminderScheduler`** (object) schedules three one-shot `WorkRequest`s per assignment — **T-7 days, T-1 day, day-of (08:00)** — with stable unique names (`uniqueNameFor(assignmentId, type)`, `ExistingWorkPolicy.REPLACE`) and tags (`assignment_$id`, `user_$id`) for cancellation. Past fire times are skipped. Hooked from `AssignmentsViewModel` add/update/delete and from a full reschedule on the user's assignments.
- **`AssignmentReminderWorker`** (`CoroutineWorker`) **re-verifies state at fire time** before posting: assignment still exists, user still exists, `pushNotificationsEnabled`, and `POST_NOTIFICATIONS` granted. Body includes the username so multi-user devices stay unambiguous. Tap opens `LoginActivity` with `EXTRA_NOTIFICATION_USERNAME`.
- **Per-user toggle** lives in `UserSettings` (`pushNotificationsEnabled` on `User`). The worker checks it at fire time, so toggling off stops delivery even for already-scheduled work.
- **Runtime permission:** `POST_NOTIFICATIONS` (API 33+) is requested from `UserSettingsActivity` when the toggle is switched on. The toggle reflecting "on" is necessary but not sufficient — OS-level permission and channel importance also gate delivery.

### Flashcard review reminders (SM-2 due dates)
Separate from assignment reminders. When the SM-2 schedule says cards are next due, the user gets a single reminder to review.

- **Channel:** `review_reminders` (`StudyMateApplication.CHANNEL_REVIEW_REMINDERS`), created in `StudyMateApplication.onCreate()` alongside `assignment_reminders`.
- **`ReviewReminderScheduler`** (object) schedules **one** `OneTimeWorkRequest` **per user** (not per deck) via `enqueueUniqueWork(uniqueNameFor(userId), REPLACE, …)` — unique name `review_reminder_user_<id>`, tag `review_user_<id>`. It fires at the user's *earliest* future due date (`FlashCardDao.getNextDueDate(userId, today)` = `MIN(due_at) WHERE due_at > today`) at **09:00**. No-op if `pushNotificationsEnabled != true` or there's no future due date. Because the job is keyed per user and rescheduled (REPLACE) on the global next-due date, **5 due decks still produce only 1 notification.**
- **`ReviewReminderWorker`** (`CoroutineWorker`) re-verifies at fire time: user exists, `pushNotificationsEnabled`, and `FlashCardDao.countDue(userId, today) > 0` (user-wide across all decks). Posts one notification ("you have N cards ready to review"), `notifId = 900_000 + userId`. Tap opens `LoginActivity`.
- **Rescheduled** from `ReviewDeckViewModel` after a review session (queue start + session done) — grading a card changes its due date, so the next-due reminder is recomputed each time.
- **Deck due indicator (UI, not a notification):** the Flashcards **list** row shows only a short `"N due"` badge (`FlashcardDecksViewModel.dueBadgeFor`) to avoid truncation; the fuller wording — `"6 cards due now"` / `"Next review tomorrow"` / `"Next review in 3 days"` / `"Next review in 2 weeks"` — lives **inside the deck screen** (`DeckCardsViewModel.dueTextFor`, shown on a second line of the deck subtitle). Both derive the gap from the *actual* earliest future `due_at`, so a card due Wednesday reads "in 3 days", never a false "tomorrow".

### Review due decks (dashboard quick-review)
A single dashboard button reviews every deck that has cards due, back-to-back, without returning to the dashboard between decks.

- **Gating:** `HomeViewModel` computes `dueDeckIds` / `dueDeckNames` (decks with ≥1 due card, ordered by subject name then deck name — same order as the Flashcards list) plus `dueCardCount`. `reviewDueBtn` is enabled only when `dueDeckIds` is non-empty; disabled it reads "No decks due" at 0.45 alpha, enabled it reads "Review N cards now".
- **Breathing glow:** `ui/PulseRingView` is a custom `View` overlaid on the button inside a `FrameLayout`. It paints a soft warm-gold halo around the whole ring (a blurred rounded-rect stroke) that slowly **breathes** — swelling thicker + brighter then receding — on an `AccelerateDecelerateInterpolator`, matching the floating orbs' gentle motion (no travelling/comet; that earlier version read as cheap). It is **non-clickable**, so taps fall through to the button beneath. The halo blooms slightly outside the border, so the scroll content `LinearLayout` + the `FrameLayout` keep `clipChildren=false` (and the scroll view keeps `clipToPadding=false`). `HomeActivity` calls `startAnimating()` / `stopAnimating()` from the summary observer (only while enabled); the view also stops itself on detach. **Corner radius (12dp) is hand-synced** with the button's `app:cornerRadius`.
- **Chaining:** the button launches `ReviewDeckActivity` with `deck_queue_ids` (IntArray) + `deck_queue_names` (String[]). `ReviewDeckViewModel.loadChain(...)` walks the decks in order; when one deck's queue empties it rolls the completed count into `chainTotal` and immediately `advanceToNextDeck()` (a deck that turns out to have nothing due is skipped silently). The final `Done` state reports the whole-session total. `ReviewDeckActivity.render()` re-sets the title from `state.deckName` each emission so the header follows the current deck. Single-deck entry from the deck screen (`load(deckId, deckName)`) is unchanged.

## UI & Theming

- **Theme:** `Theme.StudyMate` (defined in `res/values/themes.xml`), extends Material 3.
- **Min SDK 30**, target/compile SDK 35.
- Layouts use `ConstraintLayout`; Material 3 components (`MaterialButton`, `TextInputLayout`, etc.) are preferred.
- **Design direction:** Dark Academic — navy, brass gold, warm cream over stained-wood photography.

---

## Design System (established on Login screen — replicate across all screens)

### Colour palette (`res/values/colors.xml`)
| Token | Hex | Usage |
|-------|-----|-------|
| `navy` | `#0F172A` | Page background, status/nav bar |
| `navy_mid` | `#1E293B` | Secondary dark surface |
| `gold` | `#C4A24A` | Bright gold — focused input outlines, icon accents |
| `gold_dark` | `#8B6B1A` | `colorPrimary` — filled button background |
| `gold_light` | `#D4BC7E` | Outlined button text/border, subtext labels |
| `surface` | `#FAF8F5` | Cream — card input text, icons inside glass panel |

**Reusable resources:**
- `@color/box_stroke_gold` (`res/color/box_stroke_gold.xml`) — ColorStateList that keeps TextInputLayout outlines gold in every state. Always use this for `app:boxStrokeColor`.
- `@drawable/bg_subject_row` — 12dp rounded rect, 20 % black fill, 33 % gold stroke. Use for any RecyclerView item that should look like a "mini glass card" inside the main glass card.
- `@drawable/bg_color_dot` — small oval that takes a runtime tint via `GradientDrawable.setColor(int)`. Use for subject swatches.
- `@drawable/ic_edit`, `ic_delete`, `ic_add`, `ic_arrow_back` — Material-style 24dp vector icons added in the redesign. Reuse rather than redrawing.
- `@drawable/bg_icon_badge` — 12dp rounded rect with a runtime-tinted fill, used as the icon background on assignment rows. Tint with the parent subject's colour via `(badge.background as GradientDrawable).setColor(int)`.
- `util/AssignmentIcons.options` — single source of truth for the 30 selectable assignment icon keys. Tint icons in the picker with the currently selected subject's colour (`setColorFilter(subjectColorInt)`) to reinforce the subject identity. Grid layout: 5 tiles per row, 30 icons → 6 rows. Tile = 54dp `FrameLayout` with a 1dp gold stroke. **Cell margins must be symmetric (`marginStart = marginEnd = gap/2`)** and the container needs `paddingStart/End = 6dp` — otherwise the first column's left stroke gets clipped by the row edge.
- `R.style.Theme_StudyMate_DateTimePicker` (themes.xml) — apply via `android:theme="..."` on a `FrameLayout` wrapping an inline `DatePicker` / `TimePicker` so the calendar and clock render with 35% black surface, gold accents, gold-light text. (Originally written for `DatePickerDialog` / `TimePickerDialog` but we now embed the pickers as panels — see "Embedded date / time pickers" below.) Backed by `@drawable/bg_dialog_dark` (rounded 35% black + gold border).
- `R.style.Theme_StudyMate_AlertDialog` (themes.xml) — pass as the second arg to `MaterialAlertDialogBuilder(this, R.style.Theme_StudyMate_AlertDialog)` for any confirm / warning dialog. Cream bold title, gold-light body, **muted red `#E8A48A`** positive ("Delete") button, gold-light negative button. Uses `bg_dialog_dark` so the dialog matches the date / time pickers visually.

### Gated form actions
When a follow-up action (icon picker, save) requires *prior* fields to be filled, gate the button with `isEnabled = …` and fade with `alpha = 0.45f` when disabled, `1f` when enabled. Wire a `TextWatcher` on text inputs and call the gating check from every field-changed handler so the button unlocks the moment the last required field is set.

Chain multiple gates from one recompute function. On Assignments:
- "Choose icon" unlocks when title + subject + due-date are set.
- "Save assignment" only unlocks when **all four** (title + subject + due-date + icon) are set.
- Both checks live in the same `updateAddIconEnabled()` / `updateEditIconEnabled()` call so any field change recomputes both buttons in one pass. Call this function from the date picker confirm, icon picker confirm, subject swatch tap, and the title text watcher.

### Background treatment
- **Layer-list** (`bg_login_combined.xml` pattern):
  1. Solid navy base
  2. Wood photo bitmap (`gravity="fill"`) — user supplies per-screen JPEG as `bg_<screen>.jpg`
  3. `#1A000000` dark veil for contrast
  4. Top-to-bottom gradient: `#000F172A` → `#BF0F172A` (75 % navy at bottom so wood grain shows through the glass panel)
- Do **not** fade to 100 % navy — the glass card needs texture behind it.

### Glass panel (MaterialCardView)
```xml
app:cardBackgroundColor="#59000000"   <!-- 35 % black tint — current spec, gives the wood real depth -->
app:cardForegroundColor="@android:color/transparent"
app:cardElevation="0dp"
app:strokeColor="#99C4A24A"           <!-- 60 % gold border -->
app:strokeWidth="1dp"
app:shapeAppearanceOverlay="@style/ShapeAppearance.StudyMate.LoginCard"
```
- Top corners 28 dp rounded, bottom corners 0 dp (card anchors to bottom of screen).
- **Do NOT use `android:theme` overlays inside the card** — Material 3 TextInputLayout deadlocks the UI thread when required theme attributes are missing. Set all colours explicitly on each component instead.
- **History:** the original spec used `#2BFFFFFF` (17 % white frost). It was switched to `#59000000` (35 % black) on home + login because the white tint washed out the wood grain — black at 35 % keeps the grain visible while still giving form fields enough contrast.

### Form elements inside the glass panel
```xml
<!-- TextInputLayout — gold outline on every state -->
app:boxStrokeColor="@color/box_stroke_gold"
app:hintTextColor="@color/gold_light"
android:textColorHint="#99D4BC7E"
app:startIconTint="@color/gold_light"
app:endIconTint="@color/gold_light"   <!-- password toggle -->

<!-- TextInputEditText -->
android:textColor="@color/surface"

<!-- Outlined button (secondary action) -->
android:textColor="@color/gold_light"
app:strokeColor="@color/gold_light"
app:strokeWidth="1.5dp"
```
- **Critical:** `boxStrokeColor` only reads the focused state from a plain colour and falls back to the theme attribute (`?colorOnSurfaceVariant`, which is near-black here) for unfocused/disabled. You MUST point it at a `ColorStateList` selector. Use `@color/box_stroke_gold` (defined in `res/color/box_stroke_gold.xml`) — it returns `gold_light` for the unfocused state, bright `gold` for focused, and a faded gold for disabled. If a TextInputLayout ever looks black-outlined, this is the cause.
- **Primary action button (Sign In, Create Account, Save subject, etc.) — gold fill, navy bold text:**
  ```xml
  android:textStyle="bold"
  android:textColor="@color/navy"
  android:backgroundTint="@color/gold"
  ```
  If the button has an icon (`app:icon`), set `app:iconTint="@color/navy"` to match. **Do not** rely on the theme's default `colorPrimary` for primary buttons — the theme uses the darker `gold_dark` which doesn't pop enough against the 35 % black card. Override explicitly on each primary button.
- **Outlined nav / secondary button on the glass card** (the 5 home dashboard buttons, future similar lists):
  ```xml
  android:textColor="@color/gold_light"
  android:gravity="center"
  android:backgroundTint="#59000000"   <!-- 35 % black, matches the card -->
  app:strokeColor="@color/gold_light"
  app:strokeWidth="1dp"
  app:cornerRadius="12dp"
  app:rippleColor="#33D4BC7E"
  ```

### RecyclerView rows on the glass card
Each row is a mini glass card with cream/gold text and inline edit + delete icon buttons. Inflated layout (`item_subject.xml`, `item_assignment.xml`) uses:
- Root `LinearLayout`, `background="@drawable/bg_subject_row"` (12dp rounded, 20% black fill, 33% gold stroke), `padding="10-12dp"`, `layout_marginTop="8dp"`.
- Left content area (colour dot or icon badge) followed by a weighted text column (bold cream title + small gold-light subtitle).
- Two trailing `MaterialButton style="@style/Widget.Material3.Button.IconButton"` for edit (gold pencil `ic_edit`) and delete (muted red bin `ic_delete` tinted `#E8A48A`). Sizes 38–40dp, `iconSize="18-20dp"`, `backgroundTint="#00000000"`, `insetTop/Bottom="0dp"`.
- Adapter passes `onEdit` and `onDelete` lambdas through the constructor. Delete should always go through `MaterialAlertDialogBuilder` (see "Themed confirm / warning dialogs").

### Horizontal swatch / chip rows
For pickers that span more chips than fit on a screen width (subjects, future tag rows), wrap in `HorizontalScrollView` with:
```xml
android:scrollbars="horizontal"
android:scrollbarThumbHorizontal="@color/gold_light"
android:scrollbarSize="3dp"
android:scrollbarAlwaysDrawHorizontalTrack="false"
android:requiresFadingEdge="horizontal"
android:fadingEdgeLength="32dp"
android:overScrollMode="never"
```
The thin gold scrollbar + 32dp fading edge tell the user there's more content off-screen — much better than a silent overflow. Use for any list that could grow past ~5 chips wide.

### Text legibility over wood texture
- **Tagline / page title pill** — `@drawable/bg_text_pill` (`#55000000`, 33 % black, 20 dp corners).
- **In-card subtext labels** ("Sign in to continue" etc.) — `@drawable/bg_text_pill_subtle` (`#2E000000`, 18 % black, 20 dp corners). Same padding: 14 / 5 dp.
- App name uses `android:shadowColor="#99000000"` radius 6 for legibility over the wood.

### Ambient floating orbs (background decoration)
- 6 × `ImageView` with `@drawable/bg_orb` background (`#14C4A24A`, 8 % gold tint) and a subject icon at ~34–42 % alpha. (Login still uses 8 because there is more empty space above the card.)
- Animated with `ObjectAnimator` on `translationY`, `INFINITE/REVERSE`, `AccelerateDecelerateInterpolator`. Each orb gets a unique amplitude (8–17 dp), duration (3100–5200 ms) and start delay so they never sync up.
- Orb sizes range 42–56 dp; scatter them strictly **in the wood band above the glass card** in two rows (~40 dp and ~90 dp from top). Do not let orbs sit inside the card — they look cluttered against form content.
- Declare the orbs **after** the card in XML so they paint on top of the gold stroke if any overlap.

### Cycling icon (branding header)
- Single `ImageView` (`logoIcon`), no background circle.
- Rotates on the **X axis** (`rotationX`) for a vertical flip: 0 → 90° (AccelerateInterpolator 1.5×, 500 ms) → swap drawable → −90 → 0° (DecelerateInterpolator 1.5×, 500 ms). `cameraDistance = 14000 × density`.
- Cycles every 5 500 ms through `AssignmentIcons.options`.

### Panel-swap animation (single-activity multi-form pattern)
- Two or more panels live in a `FrameLayout` inside the glass card. `ScrollView` for form panels, plain `LinearLayout` for list panels that already include a `RecyclerView`.
- Inactive panel: `visibility = INVISIBLE` (not GONE — must stay in layout for measurement, but must be invisible so it doesn't intercept touches).
- Each element in the list carries a direction multiplier (`-1f` = exits/enters left, `+1f` = right). Exit and entry directions alternate per element.
- Timing: stagger 72 ms, exit 420 ms (AccelerateInterpolator 1.3×), entry starts only after all exits finish `(n−1)×stagger + exitDur`, entry 440 ms (DecelerateInterpolator 1.3×).
- Multiply the per-element direction by a `sign` value: `+1` for "going forward" (from the home panel into a sub-panel), `−1` for "going back". This way each row enters and exits through the same axis, just inverted on the return trip.
- For nested sub-panels (e.g. ADD → ICON, ADD → DATE → TIME in Assignments), track which panel the user came from (`iconPickerOrigin`, `duePickerOrigin`). Cancel and system back return to that origin. Treat any "deeper" move as forward; anything that unwinds is backward. The Assignments flow is the reference: LIST → ADD/EDIT → ICON; ADD/EDIT → DATE → TIME → ADD/EDIT (forward chain), with system back unwinding TIME → DATE → form → list.
- Always call `message.visibility = View.GONE` on the outgoing panel's error TextView before animating so stale error text doesn't bleed through.
- After the swap, **reset each outgoing element's `translationX` to `0f`** once it's hidden. Otherwise the next swap pre-snaps relative to a stale offset and the dance breaks.

### Embedded date / time pickers (no dialogs)
Use a `DatePicker` and `TimePicker` as inline panels, not as `DatePickerDialog` / `TimePickerDialog`. Reasons: the dialog scrim showed the form underneath through it, which looked cheap; and the dialog buttons clashed with the panel-swap flow.

Layout pattern (one panel per widget — date and time are separate panels in the swap chain):
```xml
<LinearLayout
    android:id="@+id/datePanel"
    android:orientation="vertical"
    android:paddingTop="12dp"
    android:visibility="invisible" ...>

    <FrameLayout
        android:id="@+id/dateContent"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:theme="@style/Theme.StudyMate.DateTimePicker">

        <DatePicker
            android:id="@+id/datePanelDatePicker"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:calendarViewShown="true"
            android:datePickerMode="calendar"
            android:spinnersShown="false" />

    </FrameLayout>

    <!-- Next / Cancel buttons here -->
</LinearLayout>
```

Critical points:
- **No `NestedScrollView`** wrapping the picker — it makes the calendar/clock vertically scrollable, which feels cheap. The `FrameLayout` with `weight="1"` and the `DatePicker` with `match_parent` height lets the widget compress to fit, never scroll.
- **No title or subtext pill above the picker** — every dp of vertical space matters to fit on the A14 5G. The buttons under the picker are the only chrome.
- **`android:theme="@style/Theme.StudyMate.DateTimePicker"` on the inner `FrameLayout`** — this is the *exception* to "no theme overlays inside the glass card." The TextInputLayout deadlock is specific to that component; `DatePicker` and `TimePicker` accept theme overlays just fine, so apply it to a wrapper view that only contains the picker.
- **TimePicker — always `setIs24HourView(true)` in code.** That collapses the AM/PM toggle and the separate hour/minute clock into a single 24-hour clock face. Don't rely on locale defaults.
- Wire the picker buttons through the panel-swap helper (`swapToPanel(...)`) — never call `.show()`. The picker is just a panel like ADD or EDIT.

### Themed confirm / warning dialogs
For one-shot confirmation popups (delete, sign-out warning, etc.) use `MaterialAlertDialogBuilder` with the alert dialog theme:
```kotlin
import com.google.android.material.dialog.MaterialAlertDialogBuilder

MaterialAlertDialogBuilder(this, R.style.Theme_StudyMate_AlertDialog)
    .setTitle("Delete subject")
    .setMessage("This will delete \"$name\" and its $n assignment(s). Continue?")
    .setPositiveButton("Delete") { _, _ -> vm.delete(...) }
    .setNegativeButton("Cancel", null)
    .show()
```
- **Never use plain `android.app.AlertDialog.Builder`** for new code — the result is unthemed and shows a stock light dialog over the dark UI.
- The positive button styles in muted red `#E8A48A` automatically (cautionary action). Negative button is gold-light. Title is cream-bold, body is gold-light.

### Top-right action icon (outside the card)
Pattern used for the user-settings icon on the Home screen — any global per-screen action button that shouldn't live inside the form sits here.
```xml
<com.google.android.material.button.MaterialButton
    style="@style/Widget.Material3.Button.IconButton"
    android:layout_width="52dp"
    android:layout_height="52dp"
    android:insetTop="0dp"
    android:insetBottom="0dp"
    app:icon="@drawable/ic_person"
    app:iconTint="@color/surface"
    app:iconSize="28dp"
    app:iconGravity="textStart"
    app:iconPadding="0dp"
    android:backgroundTint="#59000000"   <!-- matches glass card 35 % black -->
    app:strokeColor="#99C4A24A"
    app:strokeWidth="1dp"
    app:cornerRadius="26dp"               <!-- half of width = full pill -->
    app:rippleColor="#33D4BC7E"
    app:layout_constraintBottom_toTopOf="@id/<cardId>"
    app:layout_constraintEnd_toEndOf="parent"
    android:layout_marginBottom="12dp"
    android:layout_marginEnd="16dp" />
```
- Anchor with `constraintBottom_toTopOf="@id/<cardId>"` so the button sits in the wood band 12 dp above the card edge — **not** inside the card.

---

## Device & window-insets handling (API 35 edge-to-edge)

We target compile SDK 35, which automatically enables edge-to-edge for the app. This affects every screen.

### What edge-to-edge actually does
- The Activity's root `ConstraintLayout` fills the **entire** screen — including the area behind the status bar at the top and the navigation bar at the bottom.
- Anything anchored to `parent` bottom is drawn **behind** the system nav bar. The nav bar is opaque, so your content is hidden under it.

### Required pattern
1. **Top of screen** — use a `Guideline` with `app:layout_constraintGuide_percent="0.16"` (or similar) to push the glass card's top below the status bar and leave a wood band for orbs and the top-right action icon.
2. **Bottom of screen** — every scrollable content container (e.g. `NestedScrollView`) must apply a dynamic bottom padding equal to the nav bar inset:
   ```kotlin
   ViewCompat.setOnApplyWindowInsetsListener(scrollView) { view, insets ->
       val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
       val base = (16 * resources.displayMetrics.density).toInt()
       view.setPadding(0, 0, 0, navBar + base)
       insets
   }
   ```
   - Also set `android:clipToPadding="false"` on the scroll container so the padding does not clip the bottom item.
3. **Card itself** uses `match_parent` height inside its constraint slot; only the *content inside* needs the inset padding. The card stroke / background extending behind the nav bar is fine and intentional — it keeps the rounded look against any nav-bar colour.
4. **Multi-panel cards** — when the card hosts several swappable panels, apply the navBar padding to *every* panel inside the inset listener, not just the visible one. Use a small base (e.g. 4dp), since the visible card area is already tight on the A14 5G:
   ```kotlin
   val base = (4 * resources.displayMetrics.density).toInt()
   listPanel.setPadding(listPanel.paddingLeft, listPanel.paddingTop, listPanel.paddingRight, navBar + base)
   addPanel.setPadding(0, 0, 0, navBar + base)
   editPanel.setPadding(0, 0, 0, navBar + base)
   datePanel.setPadding(datePanel.paddingLeft, datePanel.paddingTop, datePanel.paddingRight, navBar + base)
   // ... one line per panel
   ```

### Keyboard / IME insets
`windowSoftInputMode="adjustResize"` alone does **not** lift content above the keyboard under API 35 edge-to-edge — the window doesn't auto-resize for the IME. Use **`KeyboardInsets.apply(activity)`** (call in `onCreate` after `setContentView`) on every activity with text fields. It listens for `WindowInsetsCompat.Type.ime()` and pads the root (`android.R.id.content`) by the IME height so the focused field stays visible. Already wired into Login, Assignments, UserSettings, Subjects, FlashcardDecks, and DeckCards.

### Sizing budget on Samsung A14 5G (our reference device)
This is the worst-case "small phone" we test against. Other devices are larger and easier.

| Measurement | Value |
|---|---|
| Physical resolution | 1080 × 2408 px |
| Override density | **480 dpi** (3× — Samsung Display Size = Default) |
| Logical dp viewport | **360 × 803 dp** |
| Status bar | ~21 dp |
| Navigation bar (3-button) | **48 dp** |
| 6 % top guideline | ~48 dp |
| 16 % top guideline | ~128 dp |

With `headerGuide` at 16 % and a 48 dp nav bar, the visible card area on the A14 5G is **~627 dp tall**. Design content to fit comfortably inside that with everything visible without scrolling on the home/dashboard-style screens.

### Emulator vs phone density mismatch
Default AVD images run at ~420 dpi while the A14 5G runs at 480 dpi — same pixel resolution, different dp viewport, layouts look very different. To force the emulator to match the A14 5G:

```bash
adb -s emulator-5554 shell wm density 480
adb -s emulator-5554 shell wm size 1080x2408
adb -s emulator-5554 shell cmd overlay enable-exclusive com.android.internal.systemui.navbar.threebutton
```
Changing density mid-run can ANR the system UI — reboot the emulator if anything goes weird.

### Debugging tip
For "is this layout actually fitting?" questions, drop a one-off Toast in `onCreate` that prints measured heights:
```kotlin
window.decorView.post {
    val d = resources.displayMetrics.density
    fun dp(px: Int) = (px / d + .5f).toInt()
    Toast.makeText(this, "card=${dp(card.height)}dp scroll=${dp(sv.height)}dp", Toast.LENGTH_LONG).show()
}
```
Remove the block before committing — never ship the Toast.

---

### Performance — hardware layers on animated views
Promoting animated views to `LAYER_TYPE_HARDWARE` rasterises them once and lets the GPU just translate the bitmap each frame. Big difference for the wood-glass screens because the card has a rounded shape + 1dp stroke + 35% black fill — every frame of software-rendered slide-up redraws all of that.

- **Card slide-up:** set `LAYER_TYPE_HARDWARE` right before `.animate()`, drop back to `LAYER_TYPE_NONE` via `.withEndAction { card.setLayerType(View.LAYER_TYPE_NONE, null) }`.
- **Orbs:** set `LAYER_TYPE_HARDWARE` once at orb-animator start. They animate INFINITE/REVERSE so they never stop — leaving the layer on permanently is correct.
- **Known stutter, deferred:** even with hardware layers, debug builds on the A14 5G still show some entrance-animation jank on first-launch / first-navigation of a screen. Release builds with R8 optimisation help. Other future options if it bothers us: `singleTop` launch mode on HomeActivity, skip the entrance animation when the activity is *resuming* (not cold-starting), or refactor to Fragments with shared-element transitions.

### Always-6-row calendar grid
The month grid renders 6 weighted rows × 7 weighted columns regardless of how many weeks the visible month spans. Yes the 6th row is empty for some months — that's the point. Reasons:
- A 31-day month starting on Sunday (e.g. March 2026) genuinely needs 6 rows, so any "trim to needed" rule has to handle both 5 and 6.
- Always 6 = predictable cell height. Days are the same size every month. No visual "jump" when nav-ing between months.
- Cell height: `LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f)` on each row inside the weighted grid container — they auto-divide whatever the card height is on this device.

### Buttons in a weighted row (no text wrapping)
`MaterialButton` ships with ~16dp content padding per side **plus** a 6dp left/right
inset and a non-zero `minWidth`. In a `layout_weight`-split row of 3+ buttons that
leaves very little room for the label, so a word like "Correct" wraps to two lines
("Correc" / "t"). For any weighted button row, zero those out and cap to one line:
```xml
android:maxLines="1"
android:insetLeft="0dp"
android:insetRight="0dp"
android:paddingStart="0dp"
android:paddingEnd="0dp"
android:minWidth="0dp"
```
Reference: the Again / Wrong / Correct grade buttons in `activity_review_deck.xml`.

### Input sanitisation (single-line text fields)
Two-layer defence so a user can't sneak newlines / tabs / long pastes into single-line fields:

**XML layer** — on every `TextInputEditText` for names/titles:
```xml
android:inputType="text|textCapWords"   <!-- include the explicit `text` base, not just the flag -->
android:maxLines="1"
android:maxLength="60"                  <!-- 100 for assignment titles -->
android:imeOptions="actionDone"
```

**ViewModel layer** — `sanitizeSingleLine(raw)` helper:
```kotlin
private fun sanitizeSingleLine(raw: String): String {
    return raw.replace(Regex("[\\r\\n\\t]+"), " ")
        .replace(Regex("\\s{2,}"), " ")
        .trim()
}
```
Call this instead of `.trim()` on incoming text — collapses any pasted multi-line content into a single space-separated line before validation. Used in `SubjectsViewModel` and `AssignmentsViewModel`.

### Calendar day-list overflow guard
The day-detail panel's assignment rows are built programmatically — title / subject / time TextViews **must** set `maxLines = 1` + `ellipsize = TextUtils.TruncateAt.END`. Without it, a long title wraps to multiple lines and breaks the no-scroll layout budget.

### Entrance animation (screen open)
- Glass card slides up from 280 dp off-screen: `PathInterpolator(0,0,0.2,1)`, 700 ms, 60 ms delay.
- Branding elements (logo, name, tagline) fade in sequentially, 80 ms apart.
- Form elements animate in with alpha 0→1 + translationY 20→0 dp, staggered 90 ms apart, starting at 600 ms.

---

## UI Redesign TODO

The login screen is the established reference. Every other screen should be brought up to the same standard. Work through these in order — each screen follows the same template.

### Template for each screen
1. Add a wood background JPEG as `bg_<screen>.jpg` (Jamie supplies the photo).
2. Create `bg_<screen>_combined.xml` layer-list (navy base → wood bitmap → dark veil → gradient).
3. Set the activity/fragment root background to the new drawable.
4. Wrap the screen's main content in a `MaterialCardView` glass panel (copy card attributes from login).
5. Re-style all form / list elements inside the card using the explicit colour attributes above.
6. Add 6–8 ambient floating orbs in the branding / header area — use subject-relevant icons.
7. Implement panel-swap animation if the screen currently navigates to a second screen for a related action (e.g. "Add subject" sitting inside the Subjects list screen).
8. Add an entrance animation (card slide-up + element fade-in stagger).
9. Keep all ViewModels and data layer **untouched** — only `res/layout/`, `res/drawable/`, `res/values/`, and the Activity/Fragment's visual setup code change.

### Screen checklist
- [x] **Home / Dashboard** — main landing after login; wood bg, glass card with NEXT DUE + 5 nav buttons (Assignments, Flashcards, Calendar, Statistics, **Review due decks**), 6 orbs in wood band above, settings icon outside the card top-right. **Subjects is no longer a dashboard button** — it lives inside the Assignments screen (mirrors how Flashcards owns its decks under one dashboard entry). The slot Subjects freed is now occupied by the Review-due-decks button (see "Review due decks" below).
- [x] **Subjects list** — RecyclerView of glass-row items (colour dot + name + assignment count + edit/delete icons), single gold "Create subject" primary button, inline add + edit panel-swap, back-arrow icon outside card top-right. **Reached from the Assignments screen's "Subjects" button** (not the dashboard); `SubjectsActivity.openHome()` is `finish()`, so back returns to Assignments.
- [ ] **Subject detail** — wood bg, glass header card with subject name/icon, assignment list below
- [x] **Assignments list** — RecyclerView of upcoming assignments (subject-coloured icon badge + title + subject + due), inline add + edit + icon-picker panel-swap, "Choose icon" gated behind title+subject+due completion, icon tiles tinted with the selected subject's colour. Hosts a secondary **"Subjects" button** (opens `SubjectsActivity`) and **gates "Create assignment"** (disabled + 0.45 alpha) until at least one subject exists; the empty state then prompts the user to add a subject first.
- [x] **Assignment detail / edit** — folded into the Assignments screen as the edit panel (no separate activity); old `AddAssignmentActivity` and `AddAssignmentViewModel` deleted
- [x] **Flashcard decks list** — RecyclerView of glass-row decks (subject dot + deck name + "Subject • N cards • N due" subtitle + edit/delete), gold "Create new deck" primary button, inline add + edit panel-swap, tapping a deck opens `DeckCardsActivity`, back-arrow icon outside card top-right. The row shows only a short "N due" badge; the "Next review …" wording lives inside the deck screen (see Notifications → deck due indicator)
- [x] **Flashcard study / flip view** — `ReviewDeckActivity` (backed by `ReviewDeckViewModel`) runs an **SM-2 review session**: walks the deck's *due* cards one at a time, "Show answer" flips to the back, then **three grade buttons — Again / Wrong / Correct**. **Correct** schedules the card further out (SM-2 Good) and it leaves the session; **Again** re-shows the card immediately (front of the session queue); **Wrong** re-shows it later (back of the queue). Both Again and Wrong count as a lapse for the SM-2 schedule (reset, due tomorrow). The session runs on an `ArrayDeque`, so a missed card keeps coming back until graded Correct. Each grade writes a `Review_Logs` row via `CardRepo.reviewCard`. "All caught up" state when nothing is due, with a "Review all cards anyway" fallback. (Replaced the old Prev/Next browse flow.)
- [x] **Deck detail + manage cards** — consolidated 7 old activities (`DeckOptions`, `AlterDeck`, `AddCard`, `EditCard`, `ModifyCards`, `RemoveCards`, `ReviewDeck`) into 3: `DeckDetailActivity` (Review + Manage Cards), `DeckCardsActivity` (RecyclerView of cards with inline add/edit panel-swap), `ReviewDeckActivity` (restyled). Delete-deck stays on the main Flashcards list only.
- [x] **Statistics** — **restored** as a real screen (`StatisticsActivity` + `StatisticsViewModel`), reached from a Home "Statistics" button. Computes everything **live** (no `User_Stats` writes): flashcard cards due/reviewed today + this week, study streak (consecutive days with ≥1 review, from `Review_Logs`), mature cards (interval ≥ 21), assignment completed/pending/due-this-week, and per-subject completed/total. Rows are built programmatically from `StatsSummary` to keep the layout small. The small "AT A GLANCE" panel in `UserSettingsActivity` still exists as a quick glance.
  - **No "overdue" concept.** An assignment counts as **complete** when `completedAt != null` **OR its due date has passed** (`StatisticsViewModel.isComplete`). A passed deadline is treated as done, not overdue — there is no overdue row/state anywhere. "Completed this week" covers both manual completion (within 7d of `completedAt`) and auto-completion (due date within the last 7d).
- [x] **Calendar** — wood bg, glass card with month grid + day-detail panel-swap; always 6-row grid (consistent height), today gets a gold ring, past days at 50% alpha, days with assignments show up to 3 subject-coloured dots; tap → swaps to read-only day list (max 9 rows, "+N more" footer), no edit/delete (jump to Assignments)
- [x] **Settings / Profile** — wood-glass card with three sections (ACCOUNT, AT A GLANCE, PREFERENCES) using mini glass-card rows; muted-red outlined "Sign out" button with themed confirm dialog; absorbed the small library/assignment counts that used to be on the Statistics screen

### Shared components to build (once, reuse everywhere)
- [ ] `WoodGlassActivity` base class or utility — handles background setup, entrance animation, and orb float so each screen doesn't duplicate the boilerplate
- [ ] `GlassCardRecyclerViewAdapter` pattern — RecyclerView items styled as mini glass cards (semi-transparent row backgrounds, cream text, gold accent)
- [ ] Consistent empty-state illustration — wood-tinted icon + gold subtext for "no items yet" states
- [ ] Transition animations between screens — shared-element or slide transitions that feel consistent with the panel-swap style
