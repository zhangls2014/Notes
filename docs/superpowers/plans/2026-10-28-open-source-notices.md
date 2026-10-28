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
