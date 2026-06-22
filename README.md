# StudyMate

An offline-first Android study companion for students. Plan your assignments,
memorise with spaced-repetition flashcards, stay focused with a built-in Pomodoro
timer, and see your progress — all on-device, no account server, no ads, no tracking.

> **Privacy by design:** StudyMate has no backend. Every account, assignment, deck,
> and statistic lives only on your device. Nothing is sent anywhere.

## Features

- **Assignments** — name, colour, icon and due date, with a per-assignment
  **checklist** and a completion state. Local reminders (7 days / 1 day / day-of) via
  WorkManager — no server, no push account.
- **Flashcards** — build decks of question/answer cards and review them with the
  **SM-2 spaced-repetition** algorithm (Again / Wrong / Correct). Import cards from
  CSV/TSV or paste straight from Quizlet/Anki.
- **Focus timer** — a Pomodoro study timer that can target an assignment, tick its
  checklist off live, and log your focused minutes.
- **Statistics** — a live dashboard: cards due/reviewed, study streak, mature cards,
  assignment completion, and focused time today / this week.
- **Calendar** — see assignments by date and jump straight to their decks.
- **Accounts** — multiple local users (unique username), with optional one-device
  biometric / screen-lock quick sign-in and auto sign-in.
- **Backup** — export/import your whole study tree to an on-device JSON file (no
  cloud involved).
- **Onboarding** — new accounts get a seeded sample deck and a short intro carousel,
  so the app is never empty on day one.

## Tech stack

- **Language:** Kotlin
- **Architecture:** MVVM + Repository pattern (no DI framework — repositories are
  instantiated directly in ViewModels)
- **Persistence:** Room (SQLite), schema version 12, with hand-written migrations
- **Background work:** WorkManager (local notifications; no Firebase/FCM)
- **Auth:** AndroidX BiometricPrompt + EncryptedSharedPreferences (Tink)
- **UI:** Android Views + Material 3, ConstraintLayout, a custom "wood-glass" design
  system; phone-first, adapts to tablets/foldables (`sw600dp`)
- **Min SDK 30 · target/compile SDK 35**

## Building & running

Requires Android Studio (or the Android SDK + JDK 11). From the project root
(`gradlew.bat` on Windows):

```bash
# Debug build
./gradlew assembleDebug

# Install on a connected device / emulator
./gradlew installDebug

# Unit tests (JVM, no device needed)
./gradlew test

# Instrumented tests (needs a connected device/emulator)
./gradlew connectedAndroidTest

# Lint
./gradlew lint
```

For release builds and Play Store packaging (signing, R8, App Bundle), see
[RELEASE.md](RELEASE.md).

## Project layout

```
app/src/main/java/uws/ac/uk/studymate/
├── data/            Room database, entities, DAOs, relations, repositories
├── ui/              Activities + RecyclerView adapters (one screen per Activity)
│   └── viewmodels/  One ViewModel per screen
├── notifications/   WorkManager schedulers + workers (reminders, focus timer)
└── util/            Pure, unit-tested helpers (SM-2, backup serializer, CSV parser,
                     focus-timer engine, date/time, session, etc.)
```

The pure logic (SM-2 scheduler, backup format, CSV parser, focus-timer state machine,
duration formatting) is deliberately Android-free so it can be unit-tested on the JVM.

## Documentation

- [CLAUDE.md](CLAUDE.md) — full architecture, design system, conventions, and the
  roadmap toward 1.0.
- [RELEASE.md](RELEASE.md) — build, signing, and Play Store release checklist.
- [PRIVACY_POLICY.md](PRIVACY_POLICY.md) — user-facing privacy terms.

## Status

In closed testing toward a 1.0 Play Store release. The app is feature-complete for
launch; see the roadmap in [CLAUDE.md](CLAUDE.md) for what's planned beyond it
(e.g. a home-screen widget).
