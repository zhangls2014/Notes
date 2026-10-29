# Open-source notices acceptance plan

Status (2026-10-28): implementation complete; remaining device acceptance is open.
The approved implementation uses bundled Android/iOS pages. Hosting and disabled
URL-based entry proposals have been superseded.

Current architecture, data flow and navigation rules are maintained in
[the architecture guide](../../architecture.md#网页打开能力).
Dependency inventory, preserved licenses and regeneration commands are maintained
in [the generator guide](../../../tools/open-source/README.md) and
[the page directory rules](../../../web/open-source/README.md).
Test commands and automated evidence are maintained in
[the testing guide](../../testing/README.md#网页打开能力回归).

## Remaining acceptance

1. Check both platforms offline: About → component list → license/NOTICE anchor →
   list → About → Settings. Verify retained reading position and external project
   links using the system in-app browser.
2. Check app font scales 1/1.5/2, explicit light/dark and system theme changes.
   On Android, toggle dynamic colors and check all four page types after reopening.
3. Check narrow/large windows, long-button wrapping, About scrolling from both
   blank margins, native webpage drag, and predictive return commit/cancellation.
4. Record actual device outcomes in
   [the central acceptance checklist](../../testing/pending-acceptance.md#系统应用内浏览器).
   Compilation, host tests and package identity do not replace visual acceptance.

The Android navigation issue was confirmed resolved by Xander. Later webpage
scrolling, wide-margin scrolling and full appearance support have automated
regressions, but their device checks remain open. Native pixel tests retain the
existing combined-run presentation instability documented in
[the adaptive testing guide](../../testing/adaptive-ui.md).

Keep this plan while acceptance remains open. File deletion requires Xander's
separate authorization under the repository rules.

## Expressive follow-up implementation plan (approved 2026-10-28)

**Goal:** Match the app's Material 3 Expressive hierarchy and tonal button press
feedback across the four existing pages, preserving content and navigation.

**Architecture:** Keep the inline HTML/CSS generator and Gradle Sync packaging.
Pass resolved primary-container colors through the existing host appearance model;
use passive pointer/keyboard press state and CSS focus/state-layer feedback without
intercepting scrolling or navigation.

**Tech stack:** Kotlin/Compose Material 3, Python generation, native HTML/CSS.

- [x] Extend appearance regressions for container/content colors and local-link retention.
- [x] Map resolved Material colors and regenerate all four pages with 28px cards,
  tonal 48px-minimum actions, stable capsule corners during press, and reduced motion.
- [x] Run generator/script, native resource/navigation, Android and iOS checks.
  Inspect rendered colors, press/release/cancel, wrapping and 1/1.5/2 font scales.
- [x] Update current architecture and central device acceptance evidence. Keep
  upstream text, group counts, top plain links and package isolation unchanged.

Implementation and available automatic/rendered checks are complete. Full app/device
acceptance remains in the central checklist; no formal screenshot baseline was changed.

Follow-up correction (2026-10-28): pressing keeps the resting corner unchanged;
only the state layer animates. This supersedes the earlier 12px morph proposal.

## Localization and adaptive detail follow-up (approved 2026-10-28)

Goal: Chinese/English notices and list/detail panes, including project sources.
Use the root Navigation 3 scene policy and existing native web views. Preserve
list scroll, native link semantics, upstream texts, shape, colors and font scale.
External sources use an isolated network-enabled view in wide detail panes; narrow
windows continue using the system in-app browser. No new dependencies.

- [x] Localize generator text and retain language in local links; run Python/Node regressions.
- [x] Add detail destination URL and root scene metadata; replace selected detail rather than accumulate entries.
- [x] Supply resolved app language and isolated Android/iOS source views.
- [x] Run affected host tests, Android build and iOS compilation; record device acceptance separately.

Validation (2026-10-29): Python 7; Node 1,920 appearance/navigation and 32
language cases passed. Android host tests: about 5 and composeApp targeted 22,
zero failures/skips. iOS Simulator tests: 43, zero failures/skips, including real
About Entry dispatch. DevFull Debug assembly and iOS compilation passed.
About detekt passed; composeApp detekt remains failed on 9 existing findings
outside this change. New real scene regression covers license/project details
through wide/narrow/wide sizes. No device visual acceptance or screenshot baseline
update was performed. Upstream pre bodies retain their original CRLF; diff
whitespace validation uses command-local cr-at-eol, without repository config changes.

## Selected details and full-width list (approved and implemented 2026-10-29)

- [x] Remove list intro navigation and detail intro cards.
- [x] Generate one file per license/NOTICE and load only the selected detail.
- [x] Keep the list full width until a detail exists; retain adaptive detail scenes.
- [x] Update resource routing/packaging, tests and current documentation.

Validation: Python 8, Node 9,600 appearance/navigation + 160 language cases;
Android about 6 + composeApp targeted 20 tests; iOS Simulator 43 tests passed.
Android DevFull Debug built/installed; APK contains exactly 9 Android notice
pages without the old aggregate. iOS compiled. About detekt passed; composeApp
retains 9 existing findings. Current emulator screenshots confirm full-width list
and a click opening Apache-2.0 directly in the adjacent pane without an intro.
No emulator display settings or formal screenshot baselines changed. Old aggregate
source files remain unlinked/unpackaged pending separate deletion authorization.

## Pane animation (approved 2026-10-29)

- [x] Keep a stable scene and list composition across opening and closing.
- [x] Animate list bounds and detail entry/exit; retain outgoing content until close settles.
- [x] Keep narrow navigation and replacement behavior; use the system motion duration scale.
- [x] Add frame-by-frame resize and content-lifetime regression coverage.
- [ ] Device visual acceptance, rapid close/reopen and disabled-system-animation interaction.

Current evidence and remaining device checks are recorded in
[the acceptance checklist](../../testing/pending-acceptance.md).
The emulator was disconnected during the first animation run; later device checks
are recorded in the central checklist. No display settings were changed.

Current status: implementation complete; remaining device acceptance and retained
process-file cleanup are tracked in the central checklist.
