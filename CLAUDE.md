# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## About StudyMate

An Android study-companion app for students. Core features:
- **Assignments** — the single top-level study item (e.g. "Maths - Calculus"). Each has a name, a colour, an icon, a **required** due date, and a completion state. (As of **v11** the old separate *Subject* concept is folded in — there is no Subjects table or screen any more; an assignment carries its own colour and owns its decks directly.)
- **Flashcards** — create decks of question/answer cards under an assignment; review with **SM-2 spaced repetition** (Again / Wrong / Correct)
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

# Release build (R8 + resource shrink). Signed only if keystore.properties exists.
./gradlew bundleRelease     # .aab for Play
./gradlew assembleRelease   # signed/unsigned .apk
```

## Release build & signing

Release uses **R8** (`isMinifyEnabled` + `isShrinkResources`). Keep rules live in `app/proguard-rules.pro` (Room entities, ViewModels, WorkManager workers, Tink for `EncryptedSharedPreferences`) — **R8 only fully verifies at runtime, so always smoke-test a release build on a device** before shipping. Signing is read from a gitignored `keystore.properties` at the repo root (template: `keystore.properties.example`); when absent the release build is left unsigned rather than failing. **Android Auto Backup is configured** (`res/xml/backup_rules.xml` + `data_extraction_rules.xml`): the Room DB is backed up; **excluded** are the encrypted biometric store (its Keystore key is device-bound and won't restore) and the session prefs (`studymate_session.xml`, holds `lastUserId` — backing it up would let auto sign-in enter the account password-free on a restored device). Full launch steps + Data Safety answers are in `RELEASE.md`; user-facing privacy terms in `PRIVACY_POLICY.md`.

## Architecture

**MVVM + Repository pattern** — no DI framework (Hilt/Dagger); repositories are instantiated directly in ViewModels.

```
LoginActivity / RegisterActivity → LoginViewModel / RegisterViewModel
                                          ↓
HomeActivity / AssignmentsActivity / ...  → HomeViewModel / AssignmentsViewModel / ...
                                          ↓
                              Repositories (UserRepo, AssignmentRepo, ...)
                                          ↓
                                  Room DAOs → StudyMateDatabase (v11)
```

**Session flow:** `LoginActivity` writes a session via `SessionManager` (SharedPreferences). Every ViewModel that needs the current user calls `SessionUserResolver.resolveUser()` to load the `User` entity — do not read SharedPreferences directly in ViewModels.

> `RegisterActivity` no longer exists — sign-up was folded into `LoginActivity`'s panel-swap router (sign-in / signup-chooser / signup-password). See "Authentication & Sign-in".

## Key Files & Packages

| Path | Purpose |
|------|---------|
| `data/StudyMateDatabase.kt` | Room singleton, v11, exposes all DAOs; migrations `MIGRATION_4_5` … `MIGRATION_10_11` and the `MIGRATIONS` array live here |
| `data/entities/Assignments.kt` | The merged top-level entity (`Assignment`): `title`, `color`, `dueDate`, `icon`, `completedAt`. FK → `User` only. Decks FK to it. (Was two tables — `Subject` was merged in at v11.) |
| `util/SpacedRepetition.kt` | Pure-Kotlin SM-2 scheduler (ease/interval/repetitions → next due date); maps the 4 review buttons to SM-2 quality. Unit-tested |
| `data/entities/` | Room `@Entity` classes — one file per table |
| `data/relations/` | `@Relation` data classes for one-to-many queries (e.g. `DeckWithCards`) |
| `data/repositories/` | All DB access goes through repos; methods are `suspend` functions |
| `ui/viewmodels/` | One ViewModel per screen; use `viewModelScope` + `Dispatchers.IO` for repo calls |
| `StudyMateApplication.kt` | `Application` subclass — creates the `assignment_reminders` and `review_reminders` notification channels on startup; registered via `android:name` in the manifest |
| `util/SessionManager.kt` | SharedPreferences session read/write; also stores `authMode` and `lastUserId` for the cold-launch fast path |
| `util/SessionUserResolver.kt` | Validates session ID and returns the `User` — single source of truth for "who is logged in" |
| `util/BiometricLoginManager.kt` | BiometricPrompt wrapper + `EncryptedSharedPreferences` credential store for the one-device biometric account; `isEnabledForUser` / `isOwnedByAnotherUser` answer quick-sign-in ownership per account |
| `util/BiometricOwnership.kt` | Pure (Android-free) per-account ownership rules for the single quick-sign-in slot; unit-tested in `BiometricOwnershipTest` |
| `util/AssignmentDateTimeUtils.kt` | Shared date/time parsing & formatting — use this rather than duplicating logic |
| `util/PasswordUtils.kt` | PBKDF2 hashing; used only during registration and login |
| `util/BackupSerializer.kt` | Pure (Android-free) converter for the data-backup JSON format (`toJson`/`fromJson` over plain DTOs); uses built-in `org.json`, throws `InvalidBackupException` on bad files. Unit-tested in `BackupSerializerTest`. See "Data backup (export / import)" |
| `data/repositories/BackupRepo.kt` | Reads a user's whole study tree into backup DTOs (`export`) and restores one under a user (`import`, transactional, merge-by-name); re-stamps all FKs since the format carries no ids |
| `util/CsvCardParser.kt` | Pure (Android-free) CSV/TSV → front/back parser for flashcard import (0.9F). Comma/tab auto-detect, RFC-4180-ish quoting, header-row skip, first-two-columns, `MAX_CARDS` cap. Unit-tested in `CsvCardParserTest`. See "Flashcard CSV import" |
| `data/repositories/SampleContentSeeder.kt` | First-run content (0.9E): `seed(userId)` inserts the "Getting Started" assignment + "How StudyMate works" tutorial deck (6 due cards) under a new account, in one transaction (same insert pattern as `BackupRepo`). Called from `RegisterViewModel` for **every** newly created account. See "First-run onboarding" |
| `ui/OnboardingActivity.kt` | First-run wood-glass **4-page swipe carousel** (0.9E); shown by `LoginActivity` on `registrationSuccess`. 3 intro slides + a CTA slide whose primary button ("Try the sample deck") launches a real review of the seeded deck (`ReviewDeckActivity` with `EXTRA_FROM_ONBOARDING`, which returns to Home when done); Skip (any page) goes straight to Home. Backed by `OnboardingViewModel` (resolves the seeded deck). See "First-run onboarding" |
| `util/KeyboardInsets.kt` | Adds IME-height bottom padding so text fields aren't hidden by the keyboard (edge-to-edge fix — see "Keyboard / IME insets") |
| `util/Keyboard.kt` | `Keyboard.hide(activity)` — dismisses the soft keyboard + clears focus. Called from a global `onActivityPaused` hook and on panel-swaps / swatch taps so the IME never lingers over the next screen (see "Keyboard / IME insets") |
| `util/OrbField.kt` | Runtime generator for the ambient floating orbs — measures the wood that frames a glass card (top band + side gutters on tablets) and scatters a space-appropriate, non-overlapping set into it (jittered grid, keep-out around the top-right button, anchored so they follow the card under the keyboard, total capped at 16). Replaced all per-screen static orbs except Login. See "Ambient floating orbs" |
| `util/OrientationLock.kt` | `OrientationLock.apply(activity)` — reads `@bool/lock_portrait` and locks phones to portrait while letting `sw600dp` (tablets/foldables) rotate. Called in every Activity's `onCreate` (replaced the static manifest `screenOrientation` locks). See "Large-screen / tablet layout" |
| `ui/PulseRingView.kt` | Custom `View` that paints a soft, slowly breathing gold halo around a rounded-rect ring. Overlaid on the dashboard "Review due decks" button and on each next-required field of the three "create" panels (see "Review due decks" and "Progressive-glow guidance") |
| `notifications/AssignmentReminderScheduler.kt` | Schedules / cancels per-assignment reminder work (see "Notifications") |
| `notifications/AssignmentReminderWorker.kt` | `CoroutineWorker` that re-verifies state at fire time and posts the notification |
| `util/FocusTimerEngine.kt` | Pure (Android-free) Pomodoro state machine (0.9G): `Phase`/`Config`/`TimerState`, `advance`/`phaseDurationSeconds`/`initial`. The rules only — the VM owns the clock. Unit-tested in `FocusTimerEngineTest`. See "Focus / Pomodoro timer" |
| `notifications/FocusTimerScheduler.kt` / `FocusTimerWorker.kt` | One unique WorkManager one-shot that posts the focus-timer phase-complete notification (0.9G). Background fallback only — the screen chimes/buzzes itself at the exact boundary. See "Focus / Pomodoro timer" |
| `ui/FocusTimerActivity.kt` + `ui/viewmodels/FocusTimerViewModel.kt` | Wood-glass focus-timer screen + its VM (timestamp-based countdown, survives rotation / leave-return via SharedPreferences). Reached from the Home "Focus timer" button. See "Focus / Pomodoro timer" |

## Database Conventions

- Database version is **11**. Any schema change requires a new `Migration` object, adding it to the `MIGRATIONS` array, and a bump to the version constant in `StudyMateDatabase`.
- Migration history: `4→5`, `5→6` (earlier schema), `6→7` (wipe `User`), `7→8` (multi-user / one-bio model — drops the email unique index, adds the `auth_mode` column defaulting to `password`, adds a unique index on `name`; wipes `User` first to avoid name collisions), `8→9` (spaced repetition — adds SM-2 columns `ease_factor`/`interval_days`/`repetitions`/`due_at`/`last_reviewed_at` to `Flash_Cards`, `completed_at` to `Assignments`, and creates the `Review_Logs` table; additive only, existing rows preserved), `9→10` (auto-login — adds `auto_login_enabled` to `User`, `NOT NULL DEFAULT 1` so existing/new accounts default to on; additive only), `10→11` (**merge Subject into Assignment** — **DESTRUCTIVE for the study tree**: drops `Subjects` + `Subject_Progress`, rebuilds `Assignments` with its own `color` and no `subject_id`, rebuilds `Flashcard_Decks` keyed by `assignment_id`; clears `Flash_Cards`/`Review_Logs`. `User`/settings/stats are kept. No row-by-row mapping was sensible — a subject had no due date, and decks could belong to a subject spanning several assignments — so the tree is wiped and rebuilt, agreed with the user pre-1.0).
- The `User` table's user-facing identifier is **`name`** (unique). `email` is now an internal placeholder, not user-visible. `auth_mode` is `password` or `biometric_only`.
- Foreign keys use `CASCADE` delete — deleting a `User` removes all their assignments, decks, cards, etc.; deleting an `Assignment` removes its decks (and their cards).
- DAOs use `LOWER()` for case-insensitive lookups.
- Multi-table queries use `@Transaction` on the DAO method.

## Testing Conventions

Instrumented tests live in `app/src/androidTest/`. All DAO and Repository tests extend `RoomDbTestBase` which provides:
- An in-memory Room database (no migrations applied)
- Helper insert methods: `insertUser()`, `insertAssignment()` (carries `color`, no parent), `insertDeck(userId, assignmentId)`, etc. (`insertSubject`/`insertProgress` were removed at v11.)

Migration tests (`StudyMateDatabaseMigrationTest`) use `MigrationTestHelper` with the real on-disk database and must cover every new migration.

## Authentication & Sign-in

**Model: multi-user, one-bio.** Any number of accounts can exist on a device, each with a unique `name`. Each account is either a **password** account or a **biometric_only** account (`auth_mode`). At most **one** account on the device may own the biometric / screen-lock quick sign-in slot.

- **`LoginActivity` is a panel-swap router**, not a navigation graph. Three panels live in one card: `signInPanel`, `signupChoosePanel`, `signupPasswordPanel`. There is no `RegisterActivity` (deleted). The visible panel is chosen from DB state on launch.
- **`BiometricLoginManager`** wraps `androidx.biometric.BiometricPrompt` with `BIOMETRIC_STRONG | DEVICE_CREDENTIAL` (fingerprint, face, or PIN/pattern/password). It stores the owning account's credentials in `EncryptedSharedPreferences` (`androidx.security:security-crypto`) keyed by `user_id`. Only one credential set exists at a time — that enforces the one-bio rule.
- **Cold-launch fast path:** `SessionManager.lastUserId` + `authMode` let `LoginActivity` show a minimal panel for the returning user — just the biometric prompt (biometric_only) or just the password field (password). The full sign-in screen only appears after an explicit **Sign out**.
- **Auto sign-in (`User.auto_login_enabled`, on by default):** a per-account column. On cold launch, if the remembered `lastUser` is a **password** account with auto-login on, `LoginActivity.decidePanelAsync` writes the session (`sessionManager.login`/`setLastUserId`) and opens Home **without** the password — deferred to `onSplashFinished` (`pendingAutoLaunchHome`) so the splash still plays. It is **password-only**: biometric_only accounts always keep their prompt (bypassing it would defeat that mode), and a tapped notification still routes to its own user first. Toggled in `UserSettings` ("Auto sign-in" row) via `updateAutoLogin`; the row is **hidden for biometric_only accounts** (`UserSettingsSummary.isPasswordAccount`). Sign out clears `lastUserId`, so the next launch shows full sign-in regardless of the flag — that's the way to switch accounts.
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
- **Runtime permission:** `POST_NOTIFICATIONS` (API 33+) is requested from `UserSettingsActivity` when the toggle is switched on, **and from `HomeActivity` when the user taps "Yes" on the first-run reminders prompt** (0.9D). The toggle reflecting "on" is necessary but not sufficient — OS-level permission and channel importance also gate delivery.
- **First-run prompt (0.9D):** `HomeActivity.showPushNotificationsChoice` is a **themed** `MaterialAlertDialogBuilder` (was a plain unthemed `AlertDialog.Builder`). On **Yes** it saves `pushNotificationsEnabled = true` *and* fires the real system permission dialog (`requestSystemNotificationPermissionIfNeeded`, API 33+) — the in-app prompt is a pre-prompt for the OS one, not a replacement. On **No** it just saves the preference and never shows the OS dialog. Shown once per user (gated by `HomeViewModel.userNeedingPushChoice` when `pushNotificationsEnabled == null`).

### Flashcard review reminders (SM-2 due dates)
Separate from assignment reminders. When the SM-2 schedule says cards are next due, the user gets a single reminder to review.

- **Channel:** `review_reminders` (`StudyMateApplication.CHANNEL_REVIEW_REMINDERS`), created in `StudyMateApplication.onCreate()` alongside `assignment_reminders`.
- **`ReviewReminderScheduler`** (object) schedules **one** `OneTimeWorkRequest` **per user** (not per deck) via `enqueueUniqueWork(uniqueNameFor(userId), REPLACE, …)` — unique name `review_reminder_user_<id>`, tag `review_user_<id>`. It fires at the user's *earliest* future due date (`FlashCardDao.getNextDueDateActive(userId, today, nowIso)`) at **09:00**. No-op if `pushNotificationsEnabled != true` or there's no future due date. Because the job is keyed per user and rescheduled (REPLACE) on the global next-due date, **5 due decks still produce only 1 notification.**
- **`ReviewReminderWorker`** (`CoroutineWorker`) re-verifies at fire time: user exists, `pushNotificationsEnabled`, and `FlashCardDao.countDueActive(userId, today, nowIso) > 0` (user-wide across all decks). Posts one notification ("you have N cards ready to review"), `notifId = 900_000 + userId`. Tap opens `LoginActivity`.
- **Completed assignments are excluded from review (0.9C):** once an assignment is finished — `completed_at` set **or** its due date passed — its decks' cards drop out of every *automatic* review surface. The `*Active` `FlashCardDao` queries (`getDueCardsActive` / `countDueActive` / `getNextDueDateActive`) JOIN `Flash_Cards → Flashcard_Decks → Assignments` and keep only `a.completed_at IS NULL AND (a.due_date IS NULL OR a.due_date >= :nowIso)` (ISO strings sort lexicographically, so the comparisons are correct date/time checks). These back the review reminders **and** the dashboard "Review due decks" (`HomeViewModel`). Manual per-deck "Start review" is left working for finished decks. Covered by `FlashCardDaoInstrumentedTest` (CARDAO4–8).
- **Rescheduled** from `ReviewDeckViewModel` after a review session (queue start + session done) — grading a card changes its due date, so the next-due reminder is recomputed each time.
- **Deck due indicator (UI, not a notification):** the Flashcards **list** row shows only a short `"N due"` badge (`FlashcardDecksViewModel.dueBadgeFor`) to avoid truncation; the fuller wording — `"6 cards due now"` / `"Next review tomorrow"` / `"Next review in 3 days"` / `"Next review in 2 weeks"` — lives **inside the deck screen** (`DeckCardsViewModel.dueTextFor`, shown on a second line of the deck subtitle). Both derive the gap from the *actual* earliest future `due_at`, so a card due Wednesday reads "in 3 days", never a false "tomorrow".

### Review due decks (dashboard quick-review)
A single dashboard button reviews every deck that has cards due, back-to-back, without returning to the dashboard between decks.

- **Gating:** `HomeViewModel` computes `dueDeckIds` / `dueDeckNames` (decks with ≥1 due card, ordered by assignment name then deck name — same order as the Flashcards list) plus `dueCardCount`, all via `getDueCardsActive` so **decks under a finished/past-due assignment are excluded** (0.9C). `reviewDueBtn` is enabled only when `dueDeckIds` is non-empty; disabled it reads "No decks due" at 0.45 alpha, enabled it reads "Review N cards now".
- **Breathing glow:** `ui/PulseRingView` is a custom `View` overlaid on the button inside a `FrameLayout`. It paints a soft warm-gold halo around the whole ring (a blurred rounded-rect stroke) that slowly **breathes** — swelling thicker + brighter then receding — on an `AccelerateDecelerateInterpolator`, matching the floating orbs' gentle motion (no travelling/comet; that earlier version read as cheap). It is **non-clickable**, so taps fall through to the button beneath. The halo blooms slightly outside the border, so the scroll content `LinearLayout` + the `FrameLayout` keep `clipChildren=false` (and the scroll view keeps `clipToPadding=false`). `HomeActivity` calls `startAnimating()` / `stopAnimating()` from the summary observer (only while enabled); the view also stops itself on detach. **Corner radius (12dp) is hand-synced** with the button's `app:cornerRadius`.
- **Chaining:** the button launches `ReviewDeckActivity` with `deck_queue_ids` (IntArray) + `deck_queue_names` (String[]). `ReviewDeckViewModel.loadChain(...)` walks the decks in order; when one deck's queue empties it rolls the completed count into `chainTotal` and immediately `advanceToNextDeck()` (a deck that turns out to have nothing due is skipped silently). The final `Done` state reports the whole-session total. `ReviewDeckActivity.render()` re-sets the title from `state.deckName` each emission so the header follows the current deck. Single-deck entry from the deck screen (`load(deckId, deckName)`) is unchanged.

## Data backup (export / import)

Manual, on-device JSON backup + restore of a user's whole study tree. **No
backend, no network** — the app writes a file and reads one back; the OS handles
where it lives. Surfaced in **Settings → DATA** ("Export my data" / "Import data").

- **Format (`util/BackupSerializer.kt`):** a **flat nested** tree —
  `assignments[] → { color, dueDate, icon, completedAt, decks[] → cards[] }` —
  carrying **no database ids** (PKs are `autoGenerate` and device-local, so
  meaningless elsewhere). Relationships are implicit in the nesting. Root has
  `format:"studymate-backup"`, `version` (current **2**), `exportedAt`. Card
  scheduling state (ease/interval/reps/dueAt/lastReviewedAt) and assignment
  `completedAt`/`color` **are** kept (this is the user's own data for migration).
  `fromJson` rejects wrong format / newer version / **older incompatible version**
  (v1 had nested subjects above assignments — no clean map into the flat model, so
  v1 import was deliberately dropped) / malformed JSON with `InvalidBackupException`;
  tolerates missing optional fields and skips nameless assignments / empty cards.
  Pure Kotlin over `org.json` (built into Android — **no new runtime dependency**);
  unit-tested in `BackupSerializerTest`.
  > `org.json` is a *stub* on the JVM test classpath, so `testImplementation("org.json:json:…")`
  > is added (test-only) so the serializer can be unit-tested.
- **DB I/O (`data/repositories/BackupRepo.kt`):** `export(userId)` walks
  `assignmentDao.getAssignments` → `deckDao.getDecksForAssignment` →
  `cardDao.getCards`. `import(userId, data)` runs in a single `db.withTransaction`
  (a bad file is a no-op): **assignments merge by name** (case-insensitive
  `getByName`, reuse else create), **decks/cards always create** under the resolved
  assignment, **all FKs re-stamped** with the current user + freshly generated
  parent ids. Additive — never deletes existing data. Returns an `ImportSummary`
  (counts) for the toast.
- **UI (`UserSettingsActivity` + `UserSettingsViewModel`):** two Storage Access
  Framework launchers — `CreateDocument("application/json")` (export, default name
  `studymate_backup_<date>.json`) and `OpenDocument()` (import, accepts
  json/text/`*/*` since providers mislabel JSON). **No storage permission needed**
  on API 30+. The VM does file I/O via `contentResolver.openOutputStream` /
  `openInputStream` on `Dispatchers.IO`, resolves the user via `SessionUserResolver`,
  and emits a `DataOpResult` (Export/Import success or Error) the Activity toasts.
- **Out of scope (v1, deferred):** per-deck *Share*, merge UI, and any
  networked/class-code distribution. These are subsets of the same insert plumbing if
  ever wanted. **Not migrated:** `Review_Logs` (so study streaks reset on a restored
  device) and derived stats. (CSV card import shipped in 0.9F — see below.)

## Flashcard CSV import (0.9F)

Lets a user bring cards in from a file they authored elsewhere (Quizlet / Anki / a
spreadsheet) instead of hand-typing them. **Distinct from the JSON account backup
above:** that's a same-app round-trip of the whole tree; this is the interchange path
for *external* content, and appends cards to one deck.

- **Entry point:** per-deck — two buttons sharing a row on the `DeckCardsActivity`
  list panel: **"Import CSV"** (file) and **"Paste cards"** (clipboard). The open deck
  is the target, so there's no assignment/deck picker. (To make a brand-new deck from a
  file: create the deck, open it, import.)
- **Paste path (the everyday Quizlet route):** "Paste cards" reads the clipboard
  (`ClipboardManager`) and runs the text through the **same `CsvCardParser`** — so
  Quizlet's "Copy text" export (Tab between term/definition) imports with **no file at
  all**: Quizlet → Copy text → StudyMate deck → Paste cards. `DeckCardsViewModel.importFromText`
  shares the parse/insert/toast logic with `importCsv` (`importParsedText`). The file
  path stays for Anki/spreadsheet users who already have a `.csv`/`.txt`.
- **Picker:** a Storage Access Framework `ActivityResultContracts.OpenDocument()`
  launcher (same pattern as the Settings backup import), MIME-filtered to
  `text/csv`, `text/comma-separated-values`, `text/tab-separated-values`, `text/plain`,
  `*/*` (providers mislabel CSV — the JSON importer widened to `*/*` for the same reason).
  Read-only; nothing is persisted.
- **Parser (`util/CsvCardParser.kt`):** pure Kotlin, **no new dependency** (`org.json`
  is not involved). Auto-detects comma vs tab (Anki/Quizlet often tab) from the first
  line; RFC-4180-ish quoting (quoted fields may hold the delimiter, newlines, and `""`
  escaped quotes); skips a header row (`front/back`, `question/answer`,
  `term/definition`); takes the **first two columns** as front/back and ignores extras
  (tags etc.); strips a leading BOM; runs each field through `TextSanitizer.multiLine`;
  skips blank lines silently and counts rows with a missing front/back as `skipped`;
  caps at `MAX_CARDS = 2000`. Returns `Result(cards, skipped)` — never throws (an
  unparseable file yields no cards). Unit-tested in `CsvCardParserTest`.
- **Insert:** `DeckCardsViewModel.importCsv(resolver, uri)` reads the file on
  `Dispatchers.IO`, parses, maps to `FlashCard`s (default SM-2 state, `dueAt = null` →
  due now) and calls `CardRepo.addCards(...)` (one `db.withTransaction` bulk insert,
  mirrors `BackupRepo`/`SampleContentSeeder`). Reports via the existing `_message`
  toast ("Imported N cards" / "… · skipped M bad rows" / "No cards found in that file"
  / "Couldn't read that file"), then reloads the list. **Additive only** — no dedupe,
  no overwrite.
- **Deferred:** semicolon-delimited (some locales) and CSV *export*; a
  create-deck-from-file shortcut; per-row preview/edit.

## Focus / Pomodoro timer (0.9G)

An in-app study-session timer — the focused-work half of studying, between captures
(assignments) and recall (reviews). Fully **offline / no account / no DB** (DB stays
v11; settings live in a small SharedPreferences). Lives entirely in the wood-glass
design system (a normal Activity — no widget/RemoteViews constraints). **No new Gradle
dependency** (WorkManager + coroutines already present); one new normal permission
(`VIBRATE`).

- **Entry point:** a **"Focus timer"** outlined button on the Home dashboard
  (`activity_home.xml`, styled like `statisticsBtn`, in the entrance-stagger list),
  wired in `HomeActivity` to launch `FocusTimerActivity`. (The dashboard's six
  study-area buttons are spaced at 10dp with the card guideline at 13% to fit without
  scrolling on shorter phones.)
- **Session model:** alternates **FOCUS ⇄ BREAK** for `rounds` rounds, then **DONE**
  (`FOCUS r1 → BREAK r1 → … → FOCUS rN → BREAK rN → DONE`). Config is **three always-on
  editable boxes** under the labels *study time / break time / rounds*
  (defaults 25 / 5 / 4) — tap a box to edit just that value in a themed single-field
  `MaterialAlertDialogBuilder` (no presets/Custom button); the boxes grey out while a
  session is **in progress (running OR paused)** — editable only at the idle start or
  once finished.
  Last-used config persists. Controls: **Start/Pause** (→ Resume / Start again),
  **End round / End break** (skip the current phase) and **End** (stops the whole
  session, back to idle — `vm.reset()`); both End buttons are disabled until the session
  has started. A
  `PulseRingView` breathes around the big `MM:SS` countdown while running. (No separate
  long-break — deferred.)
- **`util/FocusTimerEngine`** — pure, Android-free rules only (`advance` /
  `phaseDurationSeconds` / `initial`); unit-tested (`FocusTimerEngineTest`). Mirrors
  `util/SpacedRepetition`.
- **`FocusTimerViewModel`** — owns the wall clock. **Timestamp-based:** a ~250 ms
  `viewModelScope` tick recomputes remaining from a monotonic `elapsedRealtime`
  phase-end and persists the remaining to SharedPreferences (`focus_timer`) on each
  whole-second change. **Runs in background, stops on close:** while the process is
  alive (backgrounded / screen off) the same VM keeps ticking and the WorkManager
  notification still fires; a config change (rotation) keeps the VM so it's unaffected.
  But on a **cold start** (the app was closed / the process killed) `restore()` comes
  back **paused** at the last persisted remaining — it never catches up — and cancels
  any queued phase-end notification. On each boundary it posts a `phaseEvent` (the
  Activity buzzes via `Vibrator`) and reschedules the background notification.
- **Phase-complete alert:** new **`focus_timer`** notification channel
  (`StudyMateApplication`, IMPORTANCE_HIGH). The notification is fired by a **single
  unique WorkManager one-shot** (`FocusTimerScheduler` + `FocusTimerWorker`, REPLACE on
  each (re)schedule, `cancel` on pause/reset/skip) — this is the **minimised-app
  fallback** (timing is best-effort; Doze can defer it). While the screen is alive the
  in-screen vibration is the primary cue. Independent of the study-reminder
  `pushNotificationsEnabled` toggle (user-initiated), but still gated by OS
  `POST_NOTIFICATIONS`. Tapping the notification reopens `FocusTimerActivity` (needs no
  session/DB).
- **Deferred:** a precise always-running **foreground-service** timer (a user-timer FGS
  on API 34+ has no natural `foregroundServiceType` — `specialUse` draws Play scrutiny,
  not worth it for v1); a long break after N rounds; linking a session to a deck/
  assignment; logging focus minutes into Statistics.

## First-run onboarding (0.9E)

A brand-new account no longer lands on an empty Home. On account creation the app
**seeds a sample deck** and then shows a **4-page swipe carousel** — three short
intro slides followed by a CTA slide offering a guided first action: actually
reviewing that deck. So the app is explained *and* the core flip-and-grade loop is
learned by doing in the real UI. **No schema change** (DB stays v11); this is purely
a VM hook + new Activities/VMs.

- **Trigger = account creation, not "first launch".** The welcome screen is launched
  only from `LoginActivity`'s `registerVm.registrationSuccess` observer (→ `openOnboarding()`).
  Signing into an existing account and every cold-launch fast-path still go straight
  to `openHome()`. There is **no persisted "seen" flag** — `registrationSuccess` fires
  once per account, so no guard is needed. (If the process is killed mid-onboarding the
  account already exists + session is set, so relaunch just resumes at Home.)
- **Sample deck (`data/repositories/SampleContentSeeder.kt`).** `seed(userId)` runs in
  one `db.withTransaction` (same insert pattern as `BackupRepo`) and inserts a
  "Getting Started" assignment (`icon = "english"`, a blue `#5B8DEF`) with one
  "How StudyMate works" deck of **6 tutorial cards**. Cards are left at `dueAt = null`
  (= due now) so the dashboard's "Review due decks" button lights up immediately as a
  live demo. The assignment's **due date is deliberately +1 hour** (not days): the
  short window means it can never fire assignment reminders (T-7d/T-1d/day-of all land
  in the past) alongside the user's real notifications. **As of 0.9I, finishing
  onboarding marks the sample assignment complete** (`OnboardingViewModel.completeSampleAssignment`,
  called from `OnboardingActivity` on **both** exits — Skip and the "Try the sample deck"
  CTA — on a detached scope so the write survives the navigation away), so it reads as
  **done** in the Assignments list (dimmed, deletable) and drops out of the review
  surfaces (a finished/past-due assignment is excluded from auto-review — 0.9C). The +1h
  due date is now just a **safety net** for a process killed mid-onboarding (it
  auto-completes once the hour passes). Seeded for **every new account** (it's the user's
  own data, deletable from the Flashcards screen) — called from **both** signup paths in
  `RegisterViewModel` (`createPasswordUser` / `createBiometricUser`) before posting
  success. Covered by `SampleContentSeederInstrumentedTest`.
- **Carousel (`ui/OnboardingActivity.kt` + `OnboardingViewModel.kt` +
  `res/layout/activity_onboarding.xml`).** Standard wood-glass screen (3-layer bg using
  `bg_dashboard` as a placeholder until a dedicated `bg_onboarding.jpg` is supplied,
  capped/centred card, `OrientationLock`, `OrbField.scatter`, entrance slide-up). **4
  pages** in a `FrameLayout`; navigation is a `GestureDetector` **fling** (swipe) **plus**
  the primary button, with 4 dot indicators (`onboarding_dot`, tinted gold/faded). Pages
  0–2 are intro slides (primary = "Next"); page 3 is the CTA (primary = "Try the sample
  deck"). The VM resolves the seeded deck by name (`getByName` → `getDecksForAssignment`).
  "Try the sample deck" launches `ReviewDeckActivity` with `EXTRA_FROM_ONBOARDING`; **Skip
  (any page)** → Home. **No ViewPager2 dependency** — kept the dep surface flat,
  consistent with the panel-swap idiom used elsewhere.
- **Guided review return path.** `ReviewDeckActivity` reads `EXTRA_FROM_ONBOARDING`: when
  set, the top back arrow and a repurposed **"Go to dashboard"** button on the `Done`
  state route to Home (`NEW_TASK | CLEAR_TASK`) instead of `finish()`-ing back to the
  (already finished) welcome screen. Normal deck/queue review entry is unchanged.

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
- `@drawable/bg_glow_field` — 12dp rounded rect, faint dark fill (`#14000000`), subtle gold stroke (`#55C4A24A`). Gives an otherwise-borderless selector (subject swatches, colour row) a field outline that the `PulseRingView` glow can trace. See "Progressive-glow guidance".
- `@drawable/bg_color_dot` — small oval that takes a runtime tint via `GradientDrawable.setColor(int)`. Use for subject swatches.
- `@drawable/ic_edit`, `ic_delete`, `ic_add`, `ic_arrow_back` — Material-style 24dp vector icons added in the redesign. Reuse rather than redrawing.
- `@drawable/bg_icon_badge` — 12dp rounded rect with a runtime-tinted fill, used as the icon background on assignment rows. Tint with the parent subject's colour via `(badge.background as GradientDrawable).setColor(int)`.
- `util/AssignmentIcons.options` — single source of truth for the 30 selectable assignment icon keys. Tint icons in the picker with the currently selected subject's colour (`setColorFilter(subjectColorInt)`) to reinforce the subject identity. Grid layout: 5 tiles per row, 30 icons → 6 rows. Tile = 54dp `FrameLayout` with a 1dp gold stroke. **Cell margins must be symmetric (`marginStart = marginEnd = gap/2`)** and the container needs `paddingStart/End = 6dp` — otherwise the first column's left stroke gets clipped by the row edge.
- `R.style.Theme_StudyMate_DateTimePicker` (themes.xml) — apply via `android:theme="..."` on a `FrameLayout` wrapping an inline `DatePicker` / `TimePicker` so the calendar and clock render with 35% black surface, gold accents, gold-light text. (Originally written for `DatePickerDialog` / `TimePickerDialog` but we now embed the pickers as panels — see "Embedded date / time pickers" below.) Backed by `@drawable/bg_dialog_dark` (rounded 35% black + gold border).
- `R.style.Theme_StudyMate_AlertDialog` (themes.xml) — pass as the second arg to `MaterialAlertDialogBuilder(this, R.style.Theme_StudyMate_AlertDialog)` for any confirm / warning dialog. Cream bold title, gold-light body, **muted red `#E8A48A`** positive ("Delete") button, gold-light negative button. **The panel is a solid dark `navy_mid` surface with `elevationOverlayEnabled=false` (0.9D fix):** the M3 `MaterialAlertDialog` paints its panel from `colorSurface` + an elevation tint, *not* `android:windowBackground`, so the old translucent `#59000000` surface plus the gold elevation overlay rendered as a light lavender panel. A solid surface with the overlay off keeps every alert dialog properly dark. (A plain `android.app.AlertDialog.Builder` will still look light — always use this themed `MaterialAlertDialogBuilder`.)

### Gated form actions
When a follow-up action (icon picker, save) requires *prior* fields to be filled, gate the button with `isEnabled = …` and fade with `alpha = 0.45f` when disabled, `1f` when enabled. Wire a `TextWatcher` on text inputs and call the gating check from every field-changed handler so the button unlocks the moment the last required field is set.

Chain multiple gates from one recompute function. On Assignments (order is
**name → colour → icon → due date → save**, 0.9C):
- "Choose icon" unlocks when title + colour are set.
- "Pick due date" unlocks when title + colour + icon are set.
- "Save assignment" only unlocks when **all four** (title + colour + icon + due-date) are set.
- Both checks live in the same `updateAddProgress()` / `updateEditIconEnabled()` call so any field change recomputes the gating in one pass. Call this function from the date picker confirm, icon picker confirm, colour swatch tap, and the title text watcher.

### Progressive-glow guidance (the three "create" panels)
The **New assignment** and **New deck** panels walk the user through their required fields one at a time, with the next step lit by the breathing gold glow.

- **Order:** assignment = name → colour → icon → due date → save (0.9C — icon moved ahead of the date); deck = name → assignment → save. (The assignment form's "colour" step reuses the swatch-row layout that was the old subject picker — its view ids are still `addSubjectRow`/`editSubjectRow` etc. The "ICON" step now has its own `addIconLabel`/`editIconLabel` pill to match the COLOUR/DUE DATE labels.)
- **Colour picker = two fixed rows, never a scroll (0.9C):** the assignment colour swatches build into a *vertical* `addSubjectRow`/`editSubjectRow` container as rows of three (`buildColorSwatches` → `chunked(3)`), replacing the old `HorizontalScrollView`. `highlightSelectedColor` therefore iterates rows-then-items. (The deck screen's *assignment* picker still uses a horizontal scroll — it's a variable-length list.)
- **Gating:** every control past the current step is disabled (`isEnabled=false` for buttons, a `…StepUnlocked` flag + `alpha=0.45` for the swatch/colour rows whose taps are ignored until unlocked). Only the name field and **Cancel** are always available. One `updateAddProgress()` per screen recomputes the whole chain; it's called from the name `TextWatcher`, every picker confirm/tap, `openAddPanel`, and on every swap **to** the ADD panel (and `stopAllAddGlows()` when leaving it, so the glow only animates while that panel shows).
- **Glow:** each step's field is wrapped in a `FrameLayout` (with `clipChildren=false`) holding the field plus a [`PulseRingView`](#) overlay; `setActiveGlow(view)` shows + animates exactly the next-required one and stops/hides the rest. The glow blooms slightly outside the field, so the ScrollView + its inner `LinearLayout` also keep `clipChildren/clipToPadding=false`.
- **Glow sizing (0.9C fix):** `PulseRingView.onMeasure` reports 0 for any non-`EXACTLY` spec. A plain `match_parent` `View` under a `wrap_content` parent otherwise expands to fill the *whole* available space (`getDefaultSize` returns the spec size for `AT_MOST`), which made the ring balloon to the entire panel. Reporting 0 lets the `FrameLayout` size to the real field, which then re-measures the overlay to that exact size. So glow `FrameLayout`s can stay `wrap_content` — they no longer need a hand-set height (the dashboard's "Review due decks" one keeps its explicit `56dp`, which also works).
- **Corner alignment:** the glow's ring is a fixed 12dp radius, so every glow target is given 12dp corners — `app:cornerRadius="12dp"` on the buttons, `setBoxCornerRadii(12dp…)` in code on the title `TextInputLayout`, and `@drawable/bg_glow_field` (12dp outlined box) wrapping the previously-borderless selectors (assignment subject swatches, deck subject swatches, subject colour row).
- **No pre-selection:** because each step must visibly "become next", the selectors no longer pre-pick a default — the deck's subject and the subject's colour both start empty and must be chosen (matching how the assignment panel already worked).

### Background treatment
**Every wood-glass screen uses the same three layers, in this order, as the first
children of the root `ConstraintLayout`** (the old per-screen `bg_*_combined.xml`
layer-lists with a `gravity="fill"` bitmap were **deleted in 0.8.5** — `fill`
stretches the grain, which smears badly once the wood becomes the dominant element
on a wide tablet):
  1. Root `android:background="@color/navy"` — solid navy base.
  2. `<ImageView android:id="@+id/bgImage" scaleType="centerCrop" src="@drawable/bg_<screen>">` constrained to all four parent edges — wood photo, **to scale (never stretched) at any width**. User supplies per-screen JPEG as `bg_<screen>.jpg`.
  3. `<View android:background="@drawable/bg_wood_overlay">` constrained to all four edges — the `#1A000000` dark veil + `#000F172A → #BF0F172A` top-to-bottom gradient (75 % navy at bottom so wood grain shows through the glass panel).
- Do **not** fade to 100 % navy — the glass card needs texture behind it.
- The orbs (`OrbField`) are added programmatically **after** these, so they sit above the wood.

### Large-screen / tablet layout (sw600dp)
Phone-first design that adapts to big screens by **centring**, not redesigning
(chosen with Jamie over a two-pane split). On `sw600dp` (tablets / unfolded
foldables) the glass card becomes a capped, centred column on a full-bleed wood
background with orbs framing it; phones are untouched.
- **Card width:** every card sets `app:layout_constraintWidth_max="@dimen/card_max_width"` while keeping `width=0dp` + start/end → parent (so it centres). The dimen is effectively unbounded on phones (`values/` = 2000dp) and **852dp** on `values-sw600dp/` — that's the unfolded foldable's own width, so the foldable still fills edge-to-edge while a wider tablet caps at the same width and shows wood gutters.
- **Top-right action button** anchors `constraintEnd_toEndOf="@id/<card>"` (not parent) so it tracks the centred card's corner instead of floating in the screen corner.
- **Orientation:** `OrientationLock.apply(this)` in each `onCreate` reads `@bool/lock_portrait` (`true` in `values/`, `false` in `values-sw600dp/`) — phones stay portrait, tablets rotate freely. The static manifest `screenOrientation="portrait"` locks were removed.
- **Orbs** fill the side gutters automatically — see "Ambient floating orbs".
- A future pass could add true multi-pane layouts that *use* the width (list + detail) rather than centring a phone-width column.

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

### Ambient floating orbs (`util/OrbField`)
Orbs are **generated at runtime by `util/OrbField.scatter(card, avoid)`** — do NOT
hand-place orbs in XML any more (the old per-screen `<ImageView>` orbs + `floatOrb`
helpers were deleted). Static orbs were tuned to one device and broke everywhere
else: on short screens the wood band shrinks and the percentage-positioned card
rises, so fixed orbs slid *under* the card or collapsed into a single line.

`OrbField` measures the wood that FRAMES the card once at layout and drops in a
space-appropriate scatter:
- **Call site:** each wood-glass Activity calls `OrbField.scatter(findViewById(R.id.<card>), listOf(findViewById(R.id.<topRightBtn>)))` in `onCreate` (where the old `floatOrb` loop was). The card id gives the region geometry; the button(s) are keep-out rects so no orb hides behind the back/settings button.
- **Regions = wood minus the card (0.8.5).** Always the **top band** (status-bar inset → card top); plus the **left/right gutters** beside the card when it's narrower than the screen (tablets/foldables — the card is capped + centred, see "Large-screen / tablet layout"). On a phone the card fills the width so the side strips compute to zero width and you get exactly the original top-band-only scatter — **no form-factor branching**, it falls out of the geometry. Regions are the wood *minus* the card rect, so an orb can never sit under the glass (structural, not a runtime check).
- **Deferred placement:** runs inside `card.doOnLayout { card.post { … } }` — placing views *during* the layout pass made `requestLayout` get dropped (orbs never measured **and** the constraint solve corrupted, which once blanked the settings button). `post` runs it after layout.
- **Anchoring:** top-band orbs are `bottomToTop = card.id` + `startToStart = parent` so they **ride up with the card when the keyboard pushes it up** (off-screen is fine); side-gutter orbs are anchored to the parent (static — the keyboard docks at the bottom and the card already handles that inset). Added on **top** (`addView` at end) so the full-screen `centerCrop` wood `ImageView` behind every screen can't hide them.
- **Jittered-grid scatter (not random):** each region is divided into rows × cols; orbs are jittered one-per-cell so they land in **different rows** (real 2D scatter, never a horizontal line). Per-region count is **deterministic** (~60% of cells), then the whole frame is scaled down to a **global cap of 16** (`BUDGET`) so a wide tablet frame stays ambient rather than swarmed. On a phone the single region's target is already < 16, so the scale is 1 and the result is identical to the original. Orb size is **uniform** across regions — scaled to the top-band height (`(bandH-8)/2`, capped) so **two rows always fit** even on a short band. Distinct shuffled icon per orb. Float: `translationY` up-only, `REVERSE/INFINITE`, duration 3900–9000 ms, with `currentPlayTime` jumped to a random phase so they don't swell in unison.
- **Exception — Login keeps its 8 static XML orbs + own `floatOrb`** (its card sits at 37%, so the band is large and the static orbs already clear it).

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
`windowSoftInputMode="adjustResize"` alone does **not** lift content above the keyboard under API 35 edge-to-edge — the window doesn't auto-resize for the IME. Use **`KeyboardInsets.apply(activity)`** (call in `onCreate` after `setContentView`) on every activity with text fields. It listens for `WindowInsetsCompat.Type.ime()` and pads the root (`android.R.id.content`) by the IME height so the focused field stays visible. Already wired into Login, Assignments, UserSettings, FlashcardDecks, and DeckCards.

**Dismissing the keyboard (0.9C — `util/Keyboard`).** The IME also wouldn't close on its own when focus left a field or the screen navigated away, so it lingered over the next page. `Keyboard.hide(activity)` (hides the IME + clears focus) is called from: a **global `Application.ActivityLifecycleCallbacks.onActivityPaused`** hook (`StudyMateApplication`) so it never bleeds onto the next activity; the **panel-swap** in Assignments / FlashcardDecks / DeckCards (leaving a text panel); and the **colour / assignment swatch taps** (the usual next action after typing a name).

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

### Accessibility (0.9H)
Conventions to keep the app usable with TalkBack and large fonts (and to keep the
Play pre-launch report clean). The accessibility lint checks (`ContentDescription`,
`TouchTargetSizeCheck`, `KeyboardInaccessibleWidget`, `ClickableViewAccessibility`)
are **at zero** — keep them there.
- **Touch targets ≥ 48dp.** Icon buttons use `@dimen/min_touch_target` (48dp) for
  the tappable frame while keeping a small `app:iconSize` — the icon stays visually
  small, only the hit area grows. Applied to the assignment/deck/card row buttons and
  the calendar month-nav arrows. (Top-bar back/settings buttons were already 52dp.)
- **Content descriptions live in `strings.xml`** with a `cd_` prefix (no hardcoded
  literals — hardcoded `contentDescription` trips `HardcodedText`). Dynamic ones take
  placeholders (`cd_mark_done` = "Mark %1$s as done", `cd_open_deck`, `cd_calendar_*`).
- **Programmatically-built views must set descriptions in code.** Calendar day cells
  (`CalendarActivity.createDayCell`) and day-detail rows (`buildAssignmentRow`) build a
  spoken description ("Today, 15 June, 2 assignments due" / "<title>, due <time>, opens
  flashcard decks") and hide their children with
  `importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS` so the
  whole cell/row reads as **one** TalkBack node. Same pattern for the Statistics rows
  (`StatisticsActivity.addStatRow`: "Reviewed today, 6"). RecyclerView rows set the
  state-dependent / titled descriptions in the adapter (`AssignmentListAdapter` flips
  the done button between `cd_mark_done`/`cd_mark_not_done`; `DeckListAdapter` sets the
  row summary).
- **Mark decoration non-important.** Orbs (`util/OrbField`), `PulseRingView` glows (set
  in its `init`), the login `logoIcon`, and colour dots / tinted row icons all set
  `importantForAccessibility="no"` so TalkBack skips them.
- **Section labels are headings.** `android:accessibilityHeading="true"` on the
  Settings (ACCOUNT/PREFERENCES/AT A GLANCE), Statistics (FLASHCARDS/ASSIGNMENTS) and
  focus-timer phase labels, and on the decks "COMPLETED ASSIGNMENTS" header (set in
  `DeckListAdapter`) — lets TalkBack jump between sections. (Safe at minSdk 30.)
- **Deferred:** full RTL / localisation (the ~140 remaining `HardcodedText` warnings on
  `android:text` are that, out of scope here); a high-contrast theme; switch-access /
  custom row actions.

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
Call this instead of `.trim()` on incoming text — collapses any pasted multi-line content into a single space-separated line before validation. Used in `AssignmentsViewModel` and `FlashcardDecksViewModel`.

### Calendar day-list overflow guard
The day-detail panel's assignment rows are built programmatically — title / time TextViews **must** set `maxLines = 1` + `ellipsize = TextUtils.TruncateAt.END`. Without it, a long title wraps to multiple lines and breaks the no-scroll layout budget.

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
- [x] **Home / Dashboard** — main landing after login; wood bg, glass card with NEXT DUE + 5 nav buttons (Assignments, Flashcards, Calendar, Statistics, **Review due decks**), 6 orbs in wood band above, settings icon outside the card top-right.
- [x] **~~Subjects list~~ — REMOVED (v11).** `SubjectsActivity`/`SubjectsViewModel`/`SubjectListAdapter` + `activity_subjects.xml`/`item_subject.xml` deleted. Subject was merged into Assignment; there is no separate subjects screen. The "Subjects" button on the Assignments screen is gone.
- [x] **Assignments list** — the single top-level hub. RecyclerView of upcoming assignments (the row icon is now the assignment's icon **tinted in its colour**, no badge fill/outline — 0.9C; name + due), inline add + edit + icon-picker panel-swap. The add/edit form gathers **name → colour → icon → due date → save** (0.9C order; the old subject picker is now a **colour picker**, ported from Subjects). "Choose icon" gates behind name+colour; "Pick due date" behind name+colour+icon; "Save" behind all four. "Create assignment" is always available (no parent needed).
- [x] **Assignment detail / edit** — folded into the Assignments screen as the edit panel (no separate activity); old `AddAssignmentActivity` and `AddAssignmentViewModel` deleted
- [x] **Flashcard decks list** — RecyclerView of glass-row decks (assignment colour dot + deck name + "Assignment • N cards • N due" subtitle + edit/delete), gold "Create new deck" primary button, inline add + edit panel-swap (the deck's parent picker lists the user's **assignments**, each shown as that assignment's **icon tinted in its colour** — 0.9C, was a plain dot), tapping a deck opens `DeckCardsActivity`, back-arrow icon outside card top-right. The row shows only a short "N due" badge; the "Next review …" wording lives inside the deck screen (see Notifications → deck due indicator).
  - **Completed-assignment decks (0.9C):** decks whose assignment is finished (`completedAt != null` **or** due date passed) sort below the active ones under a multi-view-type header row (`item_deck_header.xml`, `DeckRow.Header`/`DeckRow.Deck` in `DeckListAdapter`). The "COMPLETED ASSIGNMENTS" header shows **only when that group is non-empty**; those rows are dimmed and drop their "N due" badge. The list is built in `FlashcardDecksActivity.applySummary` (`DeckListItem.isCompleted` from the VM).
  - **Scoped entry (0.9C):** opening this screen with intent extras `scoped_assignment_id` / `scoped_assignment_name` (from a calendar day row) filters the list to that one assignment via `loadScreen(filterAssignmentId)`, retitles the header, and pre-picks that assignment when creating a deck.
- [x] **Flashcard study / flip view** — `ReviewDeckActivity` (backed by `ReviewDeckViewModel`) runs an **SM-2 review session**: walks the deck's *due* cards one at a time, "Show answer" flips to the back, then **three grade buttons — Again / Wrong / Correct**. **Correct** schedules the card further out (SM-2 Good) and it leaves the session. **Wrong** also **leaves the session** (you've graded it; it won't reappear this session) but is a lapse for the schedule — reset, due again tomorrow. **Again** is the same lapse scheduling as Wrong but re-shows the card immediately (front of the queue) for another go right now. (0.9C changed Wrong from "re-show later this session" to "leave now"; only Again re-queues.) Both Wrong and Correct count toward the session's reviewed total; the session runs on an `ArrayDeque`. Each grade writes a `Review_Logs` row via `CardRepo.reviewCard`. "All caught up" state when nothing is due, with a "Review all cards anyway" fallback. (Replaced the old Prev/Next browse flow.)
  - **Completed/finished decks are read-only practice (0.9I).** If the deck's assignment is complete (marked done **or** past-due — `AssignmentDateTimeUtils.isComplete`), `ReviewDeckViewModel` caches `currentDeckCompleted` when the queue loads and **skips `CardRepo.reviewCard` entirely** on every grade — no SM-2 reschedule, no `Review_Logs` row. The session still flips/advances so the deck stays usable for revision, but grading "Wrong" can't pull a finished deck's cards back into rotation. (Deck → assignment is resolved via the new `FlashcardDeckDao.getDeck(deckId)` → `AssignmentDao.getById`.) This complements 0.9C, which already excluded finished decks from the *automatic* review surfaces.
- [x] **Deck detail + manage cards** — consolidated 7 old activities (`DeckOptions`, `AlterDeck`, `AddCard`, `EditCard`, `ModifyCards`, `RemoveCards`, `ReviewDeck`) into 3: `DeckDetailActivity` (Review + Manage Cards), `DeckCardsActivity` (RecyclerView of cards with inline add/edit panel-swap), `ReviewDeckActivity` (restyled). Delete-deck stays on the main Flashcards list only.
- [x] **Statistics** — **restored** as a real screen (`StatisticsActivity` + `StatisticsViewModel`), reached from a Home "Statistics" button. Computes everything **live** (no `User_Stats` writes): flashcard cards due/reviewed today + this week, study streak (consecutive days with ≥1 review, from `Review_Logs`), mature cards (interval ≥ 21), and assignment completed/pending/due-this-week. (The per-subject breakdown was **dropped at v11** — there are no subjects to group by any more.) Rows are built programmatically from `StatsSummary` to keep the layout small. The small "AT A GLANCE" panel in `UserSettingsActivity` still exists as a quick glance.
  - **No "overdue" concept (centralised in 0.9I).** An assignment counts as **complete** when `completedAt != null` **OR its due date has passed**. This single rule lives in **`AssignmentDateTimeUtils.isComplete(completedAt, dueDate, now)`** and is reused by `StatisticsViewModel`, `AssignmentsViewModel`, `HomeViewModel`, and `ReviewDeckViewModel` — do not re-inline it. A passed deadline is treated as **done**, not overdue: there is no overdue row/state/wording anywhere. Consequences enforced in 0.9I: the **Assignments list shows past-due items as completed** (dimmed + struck-through, still **deletable**) rather than hiding them (`AssignmentsViewModel.buildAssignmentItems` no longer filters out `dueAt < now`); the **dashboard** "next due" only ever shows an *upcoming, not-yet-complete* assignment and never says "Overdue" (`HomeViewModel.findNextDueAssignment` / `buildCountdownText`). "Completed this week" covers both manual completion (within 7d of `completedAt`) and auto-completion (due date within the last 7d).
- [x] **Calendar** — wood bg, glass card with month grid + day-detail panel-swap; always 6-row grid (consistent height), today gets a gold ring, past days at 50% alpha, days with assignments show up to 3 assignment-coloured dots; tap → swaps to the day list (max 9 rows, "+N more" footer). Day rows show the assignment's icon **tinted in its colour** (0.9C — no badge fill/outline) and are **tappable → open that assignment's decks** (`FlashcardDecksActivity` in scoped mode). No edit/delete (jump to Assignments for that).
- [x] **Settings / Profile** — wood-glass card with three sections (ACCOUNT, AT A GLANCE, PREFERENCES) using mini glass-card rows; muted-red outlined "Sign out" button with themed confirm dialog; absorbed the small library/assignment counts that used to be on the Statistics screen

### Shared components to build (once, reuse everywhere)
- [ ] `WoodGlassActivity` base class or utility — handles background setup, entrance animation, and orb float so each screen doesn't duplicate the boilerplate
- [ ] `GlassCardRecyclerViewAdapter` pattern — RecyclerView items styled as mini glass cards (semi-transparent row backgrounds, cream text, gold accent)
- [ ] Consistent empty-state illustration — wood-tinted icon + gold subtext for "no items yet" states
- [ ] Transition animations between screens — shared-element or slide transitions that feel consistent with the panel-swap style

---

## Roadmap (toward 1.0)

Milestone naming: the build is currently targeting **0.8** (device/compatibility
hardening) even though `versionName` in `app/build.gradle.kts` still reads `1.0` —
**align `versionCode`/`versionName` with the real milestone before any upload.**

### 0.8 — device & compatibility testing (current focus)
The app has only been validated on the **Samsung A14 5G** (one size, one density,
one API level). Known gaps to close before widening the audience:
- **Orientation — DONE, resource-driven (0.8.5).** No longer a static manifest
  lock. `OrientationLock.apply(this)` (called in every Activity's `onCreate`) reads
  `@bool/lock_portrait` — `true` on phones (locks portrait), `false` on `sw600dp`
  (tablets/foldables rotate freely). Done this way because `screenOrientation` can't
  be qualified by a resource bucket. Note: apps targeting **SDK 36+** have
  orientation locks *ignored* on large screens — we target 35, so it's honored.
- **Large screens / tablets — DONE: centred column on full-bleed wood (0.8.5).**
  See "Large-screen / tablet layout (sw600dp)" in the design system. The old
  portrait-letterbox fallback is gone; tablets now show the glass card capped +
  centred with wood (and ambient orbs) framing it.
- **Orbs verified across a device matrix** (Small Phone/Nexus 5 → Pixel 8/9 →
  foldable + the A14) — see `util/OrbField`, which replaced the per-device static
  orbs precisely because they didn't survive this matrix.
- **API range:** `minSdk 30` → `compileSdk/targetSdk 35`, only tested on API 34.
  Smoke-test the edges (API 30 edge-to-edge + pre-33 notification behaviour, and a
  current API).
- **Density/size matrix:** only 480dpi tested. Cover small/large + ldpi…xxxhdpi.

### 0.9 — polish & growth features
- **Export / import — JSON account backup shipped in 0.9** (Settings → DATA; see
  "Data backup (export / import)"). Full study tree to/from a user-picked file via
  SAF, no backend. (CSV deck import shipped in 0.9F — see below.)
- **First-run onboarding — shipped in 0.9E.** New accounts get a pre-loaded sample
  deck + a 3-page intro carousel, so the app is never empty on day one (see "First-run
  onboarding"). **Still pending:** the "consistent empty-state illustration" for screens
  the user later empties out themselves (deletes all decks/assignments).
- **Focus / Pomodoro timer — shipped in 0.9G** (Home → "Focus timer"; see "Focus /
  Pomodoro timer"). In-app study-session timer, fully offline.
- **Accessibility pass — shipped in 0.9H.** 48dp touch targets, content descriptions
  (incl. programmatically-built calendar/stats views), decorative-view exclusions, and
  heading semantics for TalkBack; the accessibility lint checks are at zero. See
  "Accessibility (0.9H)" in the design system. Deferred: full localisation / RTL, a
  high-contrast theme.
- **Home-screen widget** — "N cards due / next assignment" for the daily-return loop.
- **Flashcard import — CSV/TSV shipped in 0.9F** (per-deck "Import cards from CSV";
  Quizlet/Anki/spreadsheet exports — see "Flashcard CSV import"). CSV *export* /
  per-deck Share was considered and **dropped** (low expected use, decided with Jamie).
- **Tablet / large-screen layouts — basic support shipped in 0.8.5** (centred
  column on full-bleed wood). A future pass could go further: true multi-pane
  layouts that *use* the extra width (e.g. list + detail) rather than centring a
  phone-width column.

### 1.0 — store launch
- Generate the upload keystore + `keystore.properties` (see `RELEASE.md`).
- Host `PRIVACY_POLICY.md`, complete the Play Data Safety form, content rating.
- Store listing assets (feature graphic, screenshots).
