# Releasing StudyMate to the Play Store

This is the build/signing/checklist reference for shipping a release. The Gradle
config is already wired — you just need to create a keystore and a
`keystore.properties` file (both gitignored).

## 1. Create an upload keystore (once)

From the repo root, generate a keystore. Keep the file and passwords safe — if you
lose them you can't update the app (unless enrolled in Play App Signing, which is
recommended; see step 5).

```bash
keytool -genkeypair -v -keystore studymate-release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 -alias studymate
```

`keytool` ships with the JDK (it's in the same `bin` folder as `java`).

## 2. Create keystore.properties

Copy the example and fill in your real values:

```bash
cp keystore.properties.example keystore.properties
```

```properties
storeFile=../studymate-release.jks   # path relative to the app/ module
storePassword=...
keyAlias=studymate
keyPassword=...
```

`keystore.properties`, `*.jks`, and `*.keystore` are gitignored — **never commit
them.** When the file is absent, `assembleRelease` still builds (unsigned), so CI
and fresh clones keep working.

## 3. Build

```bash
# App Bundle (.aab) — this is what you upload to Play
./gradlew bundleRelease       # -> app/build/outputs/bundle/release/app-release.aab

# Or a signed APK (e.g. for sideloading / testing)
./gradlew assembleRelease     # -> app/build/outputs/apk/release/app-release.apk
```

Release builds run **R8** (code shrink + obfuscation) and **resource shrinking**.
Keep rules live in [`app/proguard-rules.pro`](app/proguard-rules.pro) (Room
entities, ViewModels, WorkManager workers, Tink). After upload, keep the generated
`app/build/outputs/mapping/release/mapping.txt` so Play can de-obfuscate crash
reports (Play accepts it on upload).

## 4. Privacy policy

Host [`PRIVACY_POLICY.md`](PRIVACY_POLICY.md) at a public URL (e.g. GitHub Pages)
and paste that URL into the Play Console listing. A privacy policy URL is required.

## 5. Play Console setup

- **Play App Signing:** enroll (recommended). You upload with the key above; Google
  manages the final app-signing key, so a lost upload key can be reset.
- **App content / Data Safety form** — StudyMate is fully on-device, so:
  - **Data collected:** None. (No data is sent off the device to the developer or
    a third party.)
  - **Data shared:** None.
  - **Account creation:** username + password, stored on-device only.
  - **Data deletion:** Yes — in-app *Settings → Delete account*, and uninstalling
    removes all local data. (Mention there's no off-device account to delete.)
  - Note Android Auto Backup writes to the user's own Google account, not to the
    developer.
- **Content rating** questionnaire (study/education app, no objectionable content).
- **Target audience:** not directed at children under 13.
- **Store listing assets:** app icon (done), feature graphic, phone screenshots.

## 6. Closed testing (required before production)

Personal/individual developer accounts created after **13 Nov 2023** can't publish
to production until they've run a **closed test** that meets Google's bar:

- **≥ 20 testers** opted in to a **closed** testing track (internal testing does
  **not** count toward this).
- Kept **continuously for ≥ 14 days** — the count must stay at/above 20 for the
  whole window, so recruit a buffer (~25) since testers drift.
- After the window, apply for **production access**; Google then reviews the account.
- **Org accounts are exempt** (different identity verification, needs a D-U-N-S
  number) — for a solo project the personal-account + closed-test path is simplest.

**Recruiting 20 testers without a personal network:** use **tester-exchange
communities** — other indie devs who join your closed test in return for you joining
theirs. Free, and they're real people on real devices (which is the whole point):

- Reddit: r/AndroidAppTesting, r/alphaandbetausers, r/TestMyApp
- Discord: "Android closed testing" / "20 testers" exchange servers
- Google Groups dedicated to the 20-tester / 14-day requirement

**Do NOT** buy testers from "guaranteed approval" services or spin up your own
accounts — those are manufactured installs (clustered device fingerprints, idle
accounts, VPNs) that Google's anti-abuse systems flag, and getting flagged can
terminate the $25 account *and* the app, with no refund. No third party can
actually guarantee production approval; that's Google's call.

## 7. Pre-upload checklist

- [ ] `keystore.properties` present; `bundleRelease` produces a signed `.aab`
- [ ] `versionCode` / `versionName` bumped in [`app/build.gradle.kts`](app/build.gradle.kts) for each upload
- [ ] Smoke-test the **release** build on a device (R8 can only be fully trusted at runtime)
- [ ] Privacy policy URL live and pasted into the listing
- [ ] Data Safety form completed (see above)
- [ ] `mapping.txt` retained / uploaded for crash de-obfuscation
