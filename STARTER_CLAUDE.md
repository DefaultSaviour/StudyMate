# CLAUDE.md (starter — Dark Academic / wood-glass Android app)

> **How to use this file:** copy it into a new project's root, rename it to
> `CLAUDE.md`, and fill in the `About <APP>` and `Architecture` sections for the new
> app. Everything below the "Design System" line is reusable as-is — it's the look,
> ethos, and conventions carried over from a previous app (StudyMate). Claude Code
> auto-loads `CLAUDE.md` into context at the start of every session, so once it's in
> the repo root the new session follows it without any extra step. (See the note at
> the very bottom about `/init`.)

## About <APP_NAME>

<One paragraph: what the app is and who it's for. Then a bullet list of the core
features. Keep it concrete.>

## Ethos / product principles (carried over — keep these)

- **Offline-first, privacy by design.** No backend, no account server, no ads, no
  tracking, no analytics SDKs. All user data lives on-device. If a feature seems to
  need a server, look for an on-device equivalent first (WorkManager over FCM, SAF
  file export over cloud sync, local Room over a remote DB).
- **No subscription / no dark patterns.** One-time or free; never nag.
- **Depth over breadth.** Prefer connecting existing features into one loop over
  adding isolated features. New capability should be opt-in and add *minimum clutter*.
- **No scrolling unless unavoidable.** Screens should fit on a small phone
  (reference: ~627dp usable card height). Only let a dedicated sub-region scroll, and
  only when content overflows.
- **Maintain professional visual quality.** Reuse the established idioms below rather
  than inventing new ones per screen.

## Architecture (fill in for the new app; this is the StudyMate baseline)

**MVVM + Repository pattern — no DI framework** (Hilt/Dagger). Repositories are
instantiated directly in ViewModels.

```
Activity  →  ViewModel (viewModelScope + Dispatchers.IO)
                 ↓
            Repository (all DB access; suspend fns)
                 ↓
            Room DAOs  →  AppDatabase (versioned, hand-written migrations)
```

- One Activity + one ViewModel per screen; RecyclerView adapters for lists.
- All DB access goes through repositories; DAO methods are `suspend`.
- Keep pure logic (schedulers, parsers, formatters, state machines) **Android-free**
  in a `util/` package so it can be unit-tested on the JVM.
- Room: any schema change = a new `Migration` object added to the `MIGRATIONS` array
  + a version bump. Migration tests required. FKs use `CASCADE`.

## Build & Test (Android)

```bash
./gradlew assembleDebug          # debug build  (gradlew.bat on Windows)
./gradlew installDebug           # install on device/emulator
./gradlew test                   # JVM unit tests
./gradlew connectedAndroidTest   # instrumented tests (needs a device)
./gradlew lint
./gradlew bundleRelease          # signed .aab for Play (needs keystore.properties)
```

Min SDK 30 · target/compile SDK 35. Release uses R8 + resource shrink — **always
smoke-test a release build on a device** (R8 only fully verifies at runtime).

---

# Design System — "Dark Academic / wood-glass" (replicate across every screen)

Dark Academic: navy, brass gold, warm cream over stained-wood photography. Glass
cards float over a full-bleed wood photo with ambient drifting orbs.

## Colour palette (`res/values/colors.xml`)

| Token | Hex | Usage |
|-------|-----|-------|
| `navy` | `#0F172A` | Page background, status/nav bar |
| `navy_mid` | `#1E293B` | Secondary dark surface (solid dialog panels) |
| `gold` | `#C4A24A` | Bright gold — focused input outlines, icon accents |
| `gold_dark` | `#8B6B1A` | `colorPrimary` |
| `gold_light` | `#D4BC7E` | Outlined button text/border, subtext labels |
| `surface` | `#FAF8F5` | Cream — card text/icons |
| danger | `#E8A48A` | Muted red — delete actions |

## Background treatment (first three children of every screen's root layout)

1. Root `android:background="@color/navy"`.
2. `<ImageView scaleType="centerCrop" src="@drawable/bg_<screen>">` constrained to all
   four parent edges — wood photo, **never stretched** (`centerCrop`, not `fill`).
3. `<View android:background="@drawable/bg_wood_overlay">` — a dark veil + a
   top-to-bottom navy gradient (~75% navy at the bottom so grain shows through glass).

Do not fade to 100% navy — the glass card needs texture behind it. Orbs are added
programmatically *after* these layers.

## Glass panel (MaterialCardView)

```xml
app:cardBackgroundColor="#59000000"   <!-- 35% black tint over the wood -->
app:cardForegroundColor="@android:color/transparent"
app:cardElevation="0dp"
app:strokeColor="#99C4A24A"           <!-- 60% gold border -->
app:strokeWidth="1dp"
app:shapeAppearanceOverlay="..."      <!-- top corners 28dp, bottom 0dp -->
```

- **Do NOT use `android:theme` overlays inside the card** — Material 3 TextInputLayout
  deadlocks the UI thread when theme attrs are missing. Set every colour explicitly on
  each component. (The one exception: a wrapper `FrameLayout` holding *only* a
  `DatePicker`/`TimePicker` may take a theme overlay.)

## Form elements inside the glass panel

- **TextInputLayout:** `app:boxStrokeColor` MUST point at a `ColorStateList`
  (`@color/box_stroke_gold` — gold_light unfocused, bright gold focused, faded
  disabled). A plain colour falls back to a near-black theme attr for unfocused — if a
  field ever looks black-outlined, this is why. Also `app:hintTextColor="@color/gold_light"`,
  `android:textColorHint="#99D4BC7E"`, edit text `android:textColor="@color/surface"`.
- **Primary button (gold fill, navy bold text):** `android:textColor="@color/navy"`,
  `android:backgroundTint="@color/gold"`, `android:textStyle="bold"`,
  `app:iconTint="@color/navy"`. Don't rely on the theme's `colorPrimary`.
- **Outlined / secondary button on glass:** `android:textColor="@color/gold_light"`,
  `android:backgroundTint="#59000000"`, `app:strokeColor="@color/gold_light"`,
  `app:strokeWidth="1dp"`, `app:cornerRadius="12dp"`, `app:rippleColor="#33D4BC7E"`.
- **Gated actions:** when a button needs prior fields filled, gate with
  `isEnabled` + `alpha` (0.45 disabled / 1f enabled), driven by a TextWatcher.
- **Weighted button rows** (3+ buttons sharing a row): zero the insets/padding/minWidth
  and `maxLines="1"` or labels wrap mid-word.

## RecyclerView rows = mini glass cards

Root `LinearLayout` with `@drawable/bg_subject_row` (12dp rounded, 20% black fill,
33% gold stroke), 8dp top margin. Bold cream title + small gold-light subtitle, plus
trailing icon buttons (gold pencil `ic_edit`, muted-red bin `ic_delete`). Adapter
takes `onEdit`/`onDelete` lambdas; delete always confirms via themed dialog.

## Themed confirm / warning dialogs

`MaterialAlertDialogBuilder(this, R.style.Theme_StudyMate_AlertDialog)` — **never**
plain `AlertDialog.Builder` (renders light over the dark UI). The themed panel is a
**solid `navy_mid` surface with `elevationOverlayEnabled=false`** (M3 paints the panel
from `colorSurface` + elevation tint, not `windowBackground`). Positive ("Delete")
button = muted red; negative = gold-light; title cream-bold; body gold-light.

## Ambient floating orbs (runtime, not XML)

A `util/OrbField.scatter(card, avoidViews)` helper measures the wood that *frames* the
card (top band + side gutters on tablets) and scatters a space-appropriate,
non-overlapping set of softly drifting icon orbs into it (jittered grid, keep-out
around top-corner buttons, global cap ~16). Don't hand-place orbs in XML — static orbs
break across device sizes. Orbs set `importantForAccessibility="no"`.

## Edge-to-edge insets (compileSdk 35 enables this automatically)

- **Top:** a `Guideline` at ~16% pushes the card below the status bar, leaving a wood
  band for orbs + a top-right action icon.
- **Bottom:** every scroll container applies a dynamic bottom padding =
  `navigationBars()` inset + a small base, via `ViewCompat.setOnApplyWindowInsetsListener`;
  set `clipToPadding="false"`. For multi-panel cards, pad *every* panel.
- **Keyboard:** `adjustResize` alone doesn't lift content under API 35 edge-to-edge —
  use a `KeyboardInsets.apply(activity)` helper listening for `ime()` insets. Also a
  `Keyboard.hide(activity)` helper called from a global `onActivityPaused` hook + on
  panel swaps so the IME never lingers onto the next screen.

## Large screens / tablets (sw600dp)

Phone-first, adapt by **centring not redesigning**: cards use `width=0dp` +
`app:layout_constraintWidth_max="@dimen/card_max_width"` (unbounded on phones, ~852dp
on `sw600dp`) so they cap + centre with wood gutters. Top-right action buttons anchor
to the *card* edge, not parent. Orientation via an `OrientationLock.apply(activity)`
helper reading `@bool/lock_portrait` (true phones / false sw600dp) — phones stay
portrait, tablets rotate.

## Panel-swap (single-activity multi-form)

Multiple panels in a `FrameLayout` inside the card; inactive = `INVISIBLE` (not GONE,
so it still measures). Staggered slide animation: per-element direction multiplier,
`sign` of +1 forward / −1 back so each element returns along the same axis inverted.
Reset `translationX` to 0 after each swap or the next dance pre-snaps. Prefer this over
launching a second Activity for a related sub-action (add/edit/picker).

## Embedded date/time pickers (not dialogs)

Inline `DatePicker`/`TimePicker` as panels in the swap chain (the dialog scrim looked
cheap). Wrap the picker in a `FrameLayout` with the date-time theme overlay + `weight=1`
so it compresses to fit, never scrolls. `TimePicker.setIs24HourView(true)` in code.

## Accessibility (keep the a11y lint checks at zero)

- Touch targets ≥ 48dp (grow the hit frame via `@dimen/min_touch_target`, keep
  `iconSize` small).
- Content descriptions in `strings.xml` with a `cd_` prefix (no hardcoded literals);
  set them in code for programmatically-built views.
- Composite a built row into ONE TalkBack node:
  `importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS`.
- Decorative views (orbs, glow rings, colour dots) set `importantForAccessibility="no"`.
- Section labels get `android:accessibilityHeading="true"`.

## Entrance animation

Glass card slides up from ~280dp off-screen (`PathInterpolator(0,0,0.2,1)`, ~700ms);
branding fades in sequentially; form elements alpha 0→1 + translationY 20→0, staggered
~90ms. Promote animated views to `LAYER_TYPE_HARDWARE` during the animation, back to
`NONE` on end.

## Input sanitisation (single-line fields)

XML: `inputType="text|textCapWords"`, `maxLines=1`, `maxLength`, `imeOptions=actionDone`.
ViewModel: a `sanitizeSingleLine()` that collapses `\r\n\t`/runs of whitespace to one
space and trims — call it instead of `.trim()` before validation.

---

# Workflow & conventions (carried over)

- **Git:** Claude creates the feature branch FIRST (off `master`), implements, builds,
  verifies, then hands over commit + PR messages. The human does commit/push/PR/merge/prune.
- **Branch/version scheme:** letter-suffixed pre-1.0 milestones (e.g. `feature/0.9X-...`),
  one PR each.
- **PR body style:** opens "Branched from master." + why; emoji section headers; a
  ✅ Verification section; ends plain-text "🤖 Generated with Claude Code".
- **Commit messages** end: `Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>`.
- Only commit/push when asked. Smoke-test release builds before shipping.

---

> ## A note on `/init`
> `/init` does **not** read this file and merge it — it scans the *codebase* and writes
> a fresh `CLAUDE.md` describing what it finds. So:
> - **To carry this over:** copy this file into the new repo's root as `CLAUDE.md`
>   *before* doing anything else. Claude auto-loads `CLAUDE.md` every session — you do
>   **not** need to run `/init` at all for it to take effect.
> - **If you do run `/init`** in a repo that already has this `CLAUDE.md`, tell it
>   explicitly: *"keep the existing Design System / ethos / workflow sections verbatim;
>   only add an accurate `About` + `Architecture` for this codebase."* Otherwise `/init`
>   may rewrite and flatten the carried-over content.
> - **Alternative (global):** put the reusable design/ethos/workflow content in
>   `~/.claude/CLAUDE.md` instead — that's loaded for *every* project on your machine,
>   so a new app inherits it automatically and the project `CLAUDE.md` only needs the
>   app-specific parts. Use this only if you want the style applied to everything.
