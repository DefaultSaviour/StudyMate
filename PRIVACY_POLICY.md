# StudyMate — Privacy Policy

_Last updated: 14 June 2026_

StudyMate is a study-companion app for students. This policy explains what data
the app handles. **Short version: everything stays on your device. StudyMate has
no servers, no analytics, no ads, and sends none of your data to the developer or
any third party.**

## Who we are
StudyMate is developed by Jamie Coleman. For privacy questions, contact
**defaultsaviour@gmail.com**.

## What data StudyMate stores (all on-device)
- **Account:** a username and a password. The password is never stored in plain
  text — only a salted PBKDF2 hash is kept, on your device.
- **Your study content:** subjects, assignments (and their due dates / completion),
  flashcard decks and cards, and your review history.
- **Preferences:** notification, auto sign-in, and quick sign-in settings.
- **Optional quick sign-in:** if you turn on fingerprint / face / screen-lock
  sign-in, your credentials are kept in an encrypted store protected by a
  device-bound Android Keystore key. This never leaves your device and is excluded
  from backups.

All of the above is stored locally in the app's private storage. None of it is
transmitted to us or to any third party.

## What we collect about you
**Nothing.** StudyMate has no backend, performs no network requests for your data,
and contains no analytics, tracking, advertising, or third-party data-collection
SDKs.

## Notifications
Assignment and review reminders are scheduled and shown **locally on your device**
(via Android WorkManager). There is no push server and no message is sent off the
device.

## Backups
If you have **Google Backup** enabled in your Android settings, Android may back up
the app's data to your own Google account (Android Auto Backup). The encrypted
quick-sign-in credential store is explicitly **excluded** from backup. This backup
is a feature of your device and Google account — it is governed by
[Google's Privacy Policy](https://policies.google.com/privacy), and the developer
never receives it. You can disable it in your device's backup settings.

## Permissions
- **Notifications (POST_NOTIFICATIONS):** to show your assignment and review
  reminders. Optional — the app works without it.
- **Biometric / device credential:** only used, with your consent, to unlock the
  optional quick sign-in.

## Deleting your data
- **In-app:** Settings → Delete account permanently removes your account and all
  associated subjects, assignments, decks, flashcards and history.
- **Uninstalling** the app removes all of its local data from your device.

## Children
StudyMate is intended for students and is not directed at children under 13. We do
not knowingly collect any personal information from anyone.

## Changes to this policy
If this policy changes, the "Last updated" date above will change and the new
version will be published at the same location.
