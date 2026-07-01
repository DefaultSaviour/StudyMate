# Plan — 1.2: Exam Simulator & Trophy Room (StudyMate)

Source strategy doc: AntiGravity's `implementation_plan.md` ("StudyMate v1.2: The
Exam Simulator & Trophy Room Update"), adopted by Jamie as the core strategy for
this milestone, implemented by Claude. This file is the concrete engineering plan
grounded in the actual current codebase (read directly, not assumed).

## Vision (from AntiGravity's proposal)
1.1 perfected the daily grind (widgets, focus logging, checklists). 1.2 is about
**testing knowledge under pressure** and **rewarding the work already done**. Three
pillars: a **Multiple-Choice Exam Simulator**, a restructured **Deck screen** (so the
new buttons don't clutter it), and a **Tiered Trophy Room**, plus **Peer Deck Sharing**
as a lightweight bonus riding on the same restructure.

## Zero schema changes — confirmed against current code
This release needs **no new Room table and no migration** (DB stays at the current
version **14** — note: `CLAUDE.md` still says "version 12"; that doc is already stale
relative to the code, which has since gained `MIGRATION_12_13`/`MIGRATION_13_14` for
`Custom_Events`. Fixing that stale line is a small unrelated doc correction bundled
into this branch's docs phase, not part of 1.2 scope itself).

Verified against the actual entities/DAOs:
- **Trophy Room** — AntiGravity's own directive #1 says compute, don't store. All
  three named trophies are derivable from tables that already exist: card counts from
  `Flash_Cards` (`FlashCardDao.countAll`), focus-session counts from `Focus_Sessions`
  (needs one new *DAO query*, not a new table — see P5), and streak from
  `Review_Logs` (`ReviewLogDao.getReviewTimestamps`, same computation
  `StatisticsViewModel.computeStreak` already does).
- **Exam Simulator** — explicitly "does *not* alter SM-2 intervals" and the proposal
  never asks for exam history to be saved. Distractors are generated in-memory from
  cards already loaded for the deck (`CardRepo.getCards(deckId)`) — no new DAO query
  needed at all (simpler than AntiGravity's directive #2 implies; see P3).
- **Peer Deck Sharing** — directive #5 says "CSV string + `ACTION_SEND`, no server."
  Implemented as a **plain-text share** (`EXTRA_TEXT`, no file/`FileProvider`) so the
  recipient pastes it into their own StudyMate's existing **"Paste cards"** button —
  this reuses the 0.9F clipboard-import feature symmetrically and avoids adding a
  `FileProvider` + `res/xml/file_paths.xml` + manifest `<provider>` for a feature that
  doesn't need one. Flagged as a deliberate simplification of directive #5's literal
  wording ("CSV string" is still generated — it's just sent as text, not as a file).

## Corrections to the source proposal (verified against the real layout)
- **Settings icon is top-right, not top-left.** `activity_home.xml`'s
  `userSettingsBtn` is anchored `constraintEnd_toEndOf="@id/homeCard"` (top-right),
  confirmed by its own section comment `<!-- SETTINGS ICON — top-right -->`. AntiGravity's
  proposal says the trophy icon should sit "top-left... to mirror the Settings icon" —
  read as intending the two icons to be a *mirrored pair* on opposite corners. Building
  Trophy top-left (`constraintStart_toStartOf="@id/homeCard"`) achieves exactly that
  visual pairing, so no deviation — just noting the proposal's own inline description of
  Settings' position was inaccurate.
- **Deck screen already has exactly the 4 primary buttons this proposal wants to end
  up with** (`activity_deck_cards.xml`): Row 1 = Start review / Add card. Row 2 = Import
  CSV / Paste cards. The restructure is: keep Row 1 as-is but change its second slot to
  **Mock Exam**, and replace Row 2's two buttons with a single **Manage Deck** button
  that opens the bottom sheet containing Import CSV / Paste cards / **Export & Share**
  (new). Net result matches the proposal's target 4-button layout.

## Open decisions (flagging before building, per "don't make large calls without asking")
1. **Trophy roster.** AntiGravity's proposal marks all three trophies "Example -", i.e.
   illustrative. Building **exactly these three** (The Architect / cards authored, The
   Sprinter / focus sessions, The Unbroken / study streak) matches the spec precisely
   without inventing scope. The trophy list will be a small config array
   (`TrophyDefinition` list) so adding a 4th/5th trophy later is a few-line change, not
   a redesign — but the initial roster ships with just these three unless told otherwise.
2. **Trophy metrics are live, not lifetime.** "Cards authored" reads today's
   `Flash_Cards` count (deleting cards can lower your tier), "focus sessions" reads
   total `Focus_Sessions` rows ever logged (these aren't deleted by normal use, so this
   one is effectively lifetime in practice), "streak" is the existing live streak calc.
   This matches directive #1's "compute dynamically" instruction rather than adding a
   permanent counter — flagging so it's a known, intentional trade-off rather than a
   silent gap (this is the exact concern raised during the pitch comparison, and the
   directive resolves it by choosing "live" over "lifetime").
3. **New background art — CONFIRMED by Jamie: reuse an existing background for now.**
   The Trophy Room ships using **`bg_dashboard.jpg`** (same placeholder precedent 0.9E's
   onboarding carousel set) and the Exam screen reuses **`bg_flashcards.jpg`** (it's
   deck-scoped, same visual context as the deck/card screens). Jamie will supply a
   dedicated `bg_trophy.jpg` later. Trophies themselves render as **flat vector tile
   cards** (icon + tier ring + progress), not photorealistic 3D shelf renders — a
   deliberate, buildable scope interpretation of "beautifully rendered vector/image
   trophies." **Reminder to self for later:** when a real `bg_trophy.jpg` is supplied,
   downsize it to ~1600px before adding it to `res/drawable/` — oversized JPEGs caused
   the 1.1 main-thread jank bug fixed this session (see memory `perf-background-image-sizing`).
4. **Exam question cap.** Quizzing every card in an arbitrarily large deck could make
   for a very long "exam." Capping at **30 questions per session** (shuffled subset when
   the deck exceeds 30 cards) keeps it snappy; full-deck-sized decks under 30 are
   quizzed entirely. Flagging the number in case Jamie wants a different cap.
5. **Distractor fallback.** Distractors are 2 other cards' `back` text, filtered to
   exclude any that exactly match the correct answer's text (to avoid two "correct"
   looking options). If a deck has so much duplicate content that fewer than 2 valid
   distractors remain, the filter relaxes to "any other card" as a fallback — documented
   as a known edge case for degenerate decks, not specially handled beyond that.

## Phases

### P1 — Deck screen restructure (foundation for P3 + P4)
- `res/layout/activity_deck_cards.xml`: change `actionRow`'s second button from "Add
  card" to keep Add card where it is conceptually but reshuffle per the proposal's
  2×2: **Row 1** = `reviewBtn` ("Start review", unchanged) + new `mockExamBtn` ("Mock
  Exam", outlined, gated `isEnabled = cards.size >= 8`, same disabled-alpha pattern as
  `reviewBtn`'s empty-deck gating). **Row 2** = `addCardBtn` (unchanged, moved down) +
  new `manageDeckBtn` ("Manage deck", outlined) replacing the old `importRow`
  (`importCsvBtn` + `pasteCardsBtn` are removed from the visible layout — their
  functionality moves into the bottom sheet, same click handlers reused verbatim).
- `res/layout/bottom_sheet_manage_deck.xml` (new): three rows — "Import CSV", "Paste
  cards" (existing), "Export & share deck" (new) — styled as the existing mini-glass
  row pattern (`bg_subject_row` background, icon + label, 48dp touch target).
- `res/values/themes.xml`: new `Theme.StudyMate.BottomSheet` style, mirroring
  `Theme.StudyMate.AlertDialog`'s fix exactly (`colorSurface*` family → `navy_mid`,
  `elevationOverlayEnabled=false`) so the sheet doesn't regress into the same
  light-lavender-panel bug that `AlertDialog` hit before its 0.9D fix. Background
  drawable: reuse `@drawable/bg_dialog_dark` (already exists, rounded + gold border).
- `ui/DeckCardsActivity.kt`: replace `importCsvBtn`/`pasteCardsBtn` bindings/click
  wiring with a single `manageDeckBtn` that opens a `BottomSheetDialog(this,
  R.style.Theme_StudyMate_BottomSheet)` inflating the new layout; its three rows call
  the *existing* `csvPickerLauncher.launch(...)`, `importFromClipboard()`, and the new
  `exportDeck()` (P4) respectively, then dismiss the sheet. `mockExamBtn` launches
  `ExamActivity` (P3) the same way `reviewBtn` launches `ReviewDeckActivity`.
  `applySummary` gains the `mockExamBtn` gating line next to the existing `reviewBtn`
  gating.
- Accessibility: `manageDeckBtn`/`mockExamBtn` get `cd_` content descriptions; bottom
  sheet rows use `@dimen/min_touch_target`.
- No ViewModel changes in this phase — pure UI restructure, same underlying calls.

### P2 — CSV export utility (foundation for P3 is not needed, but for P4)
- `util/CsvCardExporter.kt` (new, pure Kotlin, mirrors `CsvCardParser`'s style):
  `fun toCsv(cards: List<FlashCard>): String` — header row `front,back`, one row per
  card, RFC-4180 quoting reused (quote a field if it contains the delimiter, a quote,
  or a newline; escape internal quotes as `""`), CRLF or `\n` row endings (match
  `CsvCardParser`'s tolerant reader — either works since it already ignores `\r`).
- Unit test `CsvCardExporterTest`: round-trip — `CsvCardParser.parse(CsvCardExporter.toCsv(cards))`
  recovers the same front/back pairs (order preserved); fields containing commas/quotes/
  newlines survive the round trip; empty list produces just the header (or empty string
  — decide during implementation, cover either way in the test).

### P3 — Mock Exam Simulator
- `data/entities` / DAO: **no changes** (uses `CardRepo.getCards(deckId)`, already exists).
- `util/ExamGenerator.kt` (new, pure Kotlin, unit-tested): given a deck's `List<FlashCard>`
  and a `maxQuestions` cap, returns a shuffled `List<ExamQuestion>` where each
  `ExamQuestion(correctCard: FlashCard, options: List<String>, correctIndex: Int)` has
  exactly 3 shuffled options (1 correct back text + 2 distractor back texts from other
  cards in the deck, excluding exact-text duplicates per the fallback rule above).
  Pure logic lives here specifically so it's unit-testable without Room/Android (mirrors
  `util/SpacedRepetition` / `util/FocusTimerEngine` precedent).
- `ui/viewmodels/ExamViewModel.kt` (new): resolves user via `SessionUserResolver`,
  loads the deck's cards via `CardRepo`, **defensively re-checks `cards.size >= 8`**
  (mirrors the "worker re-verifies at fire time" defensive pattern used elsewhere —
  e.g. `AssignmentReminderWorker`, `ReviewReminderWorker`) and emits an `Empty`/bounce
  state if not, builds the question list via `ExamGenerator`, and exposes a sealed
  `State` (`Loading` / `Question(index, total, question, selectedIndex, revealed)` /
  `Done(correctCount, total)`), an `answer(optionIndex)` function (locks the answer,
  reveals correct/incorrect, does **not** touch SM-2 or `Review_Logs` — no `CardRepo.reviewCard`
  call anywhere in this flow, matching "no SM-2 interference"), and `next()` /
  `restart()`.
- `ui/ExamActivity.kt` + `res/layout/activity_exam.xml` (new): wood-glass card (reuse
  `bg_flashcards.jpg`, per the open-decision above), progress text ("Question 3 of 12"),
  a question-text `TextView` (`maxLines=3`, `ellipsize=END` per the calendar-overflow
  convention already established for long text in a fixed layout), 3 stacked answer
  `MaterialButton`s bound each render, a `Next` button gated until an answer is picked.
  Each answer button gets `setOnLongClickListener` → a themed `MaterialAlertDialogBuilder`
  ("magnified" full-text popup, per directive #3) alongside its normal click-to-answer.
  On answer: colour the selected button green (`success_text`) if correct or
  `error_text` if wrong, and always colour the actually-correct button green so the
  right answer is visible either way — mirrors the Again/Wrong/Correct grade-button
  colour language already in the app. `Done` state shows score ("8 / 12 correct — 67%"),
  "Retry" (rebuild a fresh shuffled question set) and "Back to deck" buttons, matching
  `ReviewDeckActivity`'s `Done` state pattern. Entrance animation / orbs / OrientationLock
  / KeyboardInsets(not needed, no text entry) follow the standard screen template.
- `AndroidManifest.xml`: add `<activity android:name=".ui.ExamActivity" android:exported="false" />`.
- Accessibility: question/option rows get spoken content descriptions; correct/incorrect
  state changes are conveyed by more than colour alone (a "✓ Correct" / "✗ Wrong, correct
  answer: …" label appended to the text, not colour-only feedback — avoids a colour-blind
  accessibility gap the rest of the app doesn't currently have to deal with elsewhere).

### P4 — Peer deck sharing
- `ui/DeckCardsActivity.kt`: `exportDeck()` — `CardRepo.getCards(deckId)` →
  `CsvCardExporter.toCsv(...)` → `Intent(Intent.ACTION_SEND).apply { type = "text/plain";
  putExtra(EXTRA_TEXT, csv); putExtra(EXTRA_SUBJECT, "StudyMate deck: $deckName") }` →
  `startActivity(Intent.createChooser(intent, "Share deck"))`. Wired as the third row
  in the P1 bottom sheet. No new permission, no `FileProvider`.
- No ViewModel change needed (`CardRepo.getCards` already exists and is a simple
  suspend call — trigger it from a `lifecycleScope.launch` in the Activity, matching
  how `importFromClipboard()` is already Activity-side for a similar reason, or add a
  one-line VM passthrough if keeping all DB access out of the Activity is preferred —
  decide during implementation to match whichever precedent is closer; `CardRepo` is
  already only ever called from ViewModels elsewhere in the codebase, so the safer
  choice is a small `DeckCardsViewModel.exportDeckCsv(): String` suspend wrapper with a
  `LiveData<String?>` the Activity observes once to fire the share intent, keeping the
  "all DB access goes through ViewModels" house rule intact).

### P5 — Trophy Room
- `data/dao/FocusSessionDao.kt`: add `@Query("SELECT COUNT(*) FROM Focus_Sessions
  WHERE user_id = :userId") suspend fun countAll(userId: Int): Int` — the one new DAO
  *method* this release needs (no schema change, just a new query against the existing
  table).
- `util/TrophyProgress.kt` (new, pure Kotlin, unit-tested): `TrophyDefinition(id, name,
  description, iconRes, thresholds: IntArray[5])` for Bronze/Silver/Gold/Platinum/Diamond,
  and `fun tierFor(value: Int, thresholds: IntArray): Tier` (NONE..DIAMOND) +
  `fun progressToNext(value: Int, thresholds: IntArray): Pair<Int, Int>?` (current/next
  threshold, null if already at Diamond) for the progress readout. The three
  `TrophyDefinition`s ship with exactly the proposal's thresholds:
  - The Architect (cards authored): 8 / 16 / 32 / 64 / 128
  - The Sprinter (focus sessions): 1 / 5 / 25 / 50 / 100
  - The Unbroken (study streak): 3 / 7 / 14 / 30 / 100
- `ui/viewmodels/TrophyRoomViewModel.kt` (new): resolves user, pulls `cardDao().countAll(userId)`,
  `focusSessionDao().countAll(userId)`, and the existing streak computation (extract
  `StatisticsViewModel.computeStreak` to a shared pure helper — e.g. move it onto
  `util/StreakCalculator.kt` and have both ViewModels call it, rather than duplicating
  the algorithm — this is a small refactor of existing logic, not new behaviour),
  computes each trophy's tier via `TrophyProgress`, exposes a `List<TrophyUiState>`.
- `ui/TrophyRoomActivity.kt` + `res/layout/activity_trophy_room.xml` (new): wood-glass
  card (placeholder background per the open-decision above), title, then three
  programmatically-built trophy tiles (`StatisticsActivity`'s "rows built in code from
  a summary object" pattern, reused rather than standing up a RecyclerView + adapter for
  a fixed list of 3) — each tile: icon (tinted by tier colour, gold/silver/bronze/etc.,
  greyed `trophy_locked` colour if `Tier.NONE`), trophy name, current tier label, and a
  slim progress readout ("32 / 64 to Platinum" or "Diamond — maxed" at the top tier).
  Standard entrance animation / orbs / `OrientationLock`.
- `res/values/colors.xml`: add `trophy_bronze` (`#CD7F32`), `trophy_silver` (`#C7CBD1`),
  `trophy_gold_tier` (`#E8C468` — deliberately distinct from the app's brand `gold`
  `#C4A24A` so "you earned the Gold tier" doesn't visually read as just "the app's
  normal gold accent"), `trophy_platinum` (`#D9E5E8`), `trophy_diamond` (`#9FDBEB`),
  `trophy_locked` (`#4A5568`).
- `res/drawable/ic_trophy.xml` (new vector, 24dp Material-style, for the Home button
  and as a generic tile icon/fallback).
- `activity_home.xml` / `HomeActivity.kt`: new `trophyRoomBtn` — 52dp `IconButton`,
  same visual recipe as `userSettingsBtn` (`#59000000` fill, `#99C4A24A` stroke, 26dp
  corner radius), anchored `constraintBottom_toTopOf="@id/homeCard"` +
  `constraintStart_toStartOf="@id/homeCard"` (top-left, mirroring settings' top-right),
  icon `ic_trophy` tinted `@color/surface`. **`OrbField.scatter`'s avoid-list gains
  `trophyRoomBtn`** alongside `userSettingsBtn` — a real regression risk if missed
  (orbs could spawn behind the new icon). Click opens `TrophyRoomActivity`. Not part of
  the `Entrance.play` stagger list, matching how the settings icon isn't either (it
  floats in immediately, not staggered).
- `AndroidManifest.xml`: add `<activity android:name=".ui.TrophyRoomActivity" android:exported="false" />`.
- Accessibility: each trophy tile is one TalkBack node ("The Architect, Gold tier, 40 of
  64 to Platinum"), `accessibilityHeading` on the screen title, `ic_trophy` gets a `cd_`
  description on the Home button.

### P6 — Tests, docs, build verification
- **Unit tests** (JVM, no emulator — `./gradlew test`):
  - `CsvCardExporterTest` — round-trip with `CsvCardParser` (P2).
  - `ExamGeneratorTest` — exactly 3 options per question, exactly one correct index,
    no distractor duplicates the correct text when enough distinct cards exist, the
    degenerate-deck fallback still returns 3 options, question count respects the
    ≥8-card minimum and the 30-question cap, shuffling doesn't crash on exactly-8-card
    decks (the minimum boundary).
  - `TrophyProgressTest` — tier boundaries (exactly-at-threshold counts as that tier,
    one-below does not), `progressToNext` at each tier including `null` at Diamond,
    zero-value returns `Tier.NONE`.
  - `StreakCalculatorTest` (if extracted per P5) — carry over the existing behaviour
    `StatisticsViewModel.computeStreak` already has (today-not-yet-studied still counts
    yesterday's streak, gap breaks the streak, empty list → 0) as a regression test now
    that it's a standalone unit.
- **Instrumented tests** (`./gradlew connectedAndroidTest`, needs a device/emulator):
  - `FocusSessionDaoTest` — new `countAll` query.
  - Smoke-test `DeckCardsActivity`'s bottom sheet opens and its three actions still
    fire the right underlying calls (existing CSV/paste tests, if any, get re-pointed
    at the new entry points rather than the removed buttons).
- **Manual verification (Jamie, end of cycle, per standing "testing batched to the
  end" preference):**
  - Mock Exam: disabled under 8 cards, enabled at 8+, magnify long-press works, score
    screen is correct, retrying reshuffles, SM-2 due dates are untouched by an exam
    (check a card's due date before/after an exam session).
  - Manage Deck sheet: Import CSV / Paste cards behave exactly as before; Export &
    share produces a CSV a second device's "Paste cards" can consume; sheet is styled
    dark (no light-panel regression).
  - Trophy Room: tiers advance at the right thresholds (author 8 cards → Architect
    Bronze lights up), streak/focus trophies reflect Statistics' own numbers (cross-check
    against the Statistics screen so the two never disagree), Home's new icon doesn't
    collide with orbs or the settings icon, top-left placement looks intentional on
    phone + tablet + foldable (standing device matrix).
  - Full device matrix per CLAUDE.md's 0.8 section: phone (portrait-locked), `sw600dp`
    tablet/foldable (rotate-enabled, centred capped card) — the new screens are plain
    wood-glass cards inside the existing centring system, so this should be a quick
    confirmation rather than new work.
- **Build:** `gradlew.bat test`, `gradlew.bat assembleDebug`, `installDebug` smoke on
  the emulator matrix used earlier this session.
- **Docs:** `CLAUDE.md` —
  - New sections: "Mock Exam Simulator", "Peer deck sharing (CSV share)", "Trophy Room",
    following the existing section style (what/why/which files, not a changelog).
  - Update the Deck screen's existing bullet under "Screen checklist" to describe the
    new 2×2 button layout + Manage Deck sheet.
  - Update "Key Files & Packages" table with the new files (`ExamGenerator`,
    `CsvCardExporter`, `TrophyProgress`, the two new Activities/ViewModels).
  - Roadmap: mark "1.2" shipped, list Exam/Trophy/Sharing under a new bullet.
  - Bundle the unrelated **DB-version-is-actually-14-not-12** doc fix and the
    `Custom_Events` migration history gap (12→13, 13→14 aren't mentioned in the
    migration-history prose) into this same docs pass since it's touched anyway.

## Reuse checklist (don't re-implement)
- `SessionUserResolver`, `CardRepo`, `db.withTransaction` — not needed for exam (no
  writes) but standard for anything that is.
- `bg_subject_row`, `bg_text_pill_subtle`, `bg_dialog_dark`, `Theme.StudyMate.AlertDialog`
  fix pattern, `Entrance.play`, `OrbField.scatter`, `OrientationLock.apply`,
  `MaterialAlertDialogBuilder` + `Theme_StudyMate_AlertDialog` for the magnify popup,
  `@dimen/min_touch_target`, `cd_`-prefixed string convention, `error_text`/`success_text`
  colour tokens already in `colors.xml` for the exam's right/wrong feedback (no need to
  invent new red/green tokens).
- The Again/Wrong/Correct button-row's `maxLines=1` + zeroed insets pattern
  (`activity_review_deck.xml`) for the exam's 3 answer buttons if they end up in a
  weighted row instead of stacked — stacked is the current plan (more room for full
  answer text), but keep this fix in mind if that changes during layout.

## Workflow
- No new Gradle dependency (Material's `BottomSheetDialog` is already available via
  the existing `material` dependency). No new permission. **No DB migration.**
- Branch: `feature/1.2-exam-trophy-sharing` off `master`.
- Standard default applies (no override given this session): **Claude creates the
  branch first, implements + builds + verifies, then hands over commit + PR message
  text.** Jamie does commit/push/PR/merge/prune, per the standing workflow.
- Implementation order follows the phase numbering above (P1 unlocks P3/P4's entry
  points; P5 is independent and could be built in parallel if desired, but will be done
  sequentially for a single coherent PR).
