# Home email pane implementation plan

**Goal:** Apply the approved notices pane behavior to Home emails only.

**Architecture:** Root Navigation 3 scenes own list/detail policy and share the
existing bounds and entry/exit animations. Email renders a localized close action
when Home detail is beside its list. Favorites retains its pane policy; its later approved close action dismisses all
Favorites detail history, while Back still pops one entry.

- [x] Add real-metadata Home motion coverage: full-width list, opening/closing
  interpolation, detail lifetime, replacement and vertical hinge behavior.
- [x] Generalize the root notices scene and bounds implementation for Home; keep
  each page's current safe-area behavior and narrow-screen navigation.
- [x] Route Home detail close through retained-content animation; replace Home
  detail on selection and ensure delayed pop targets its original stack.
- [x] Add the Home-wide close action and retain narrow back; later add Favorites close.
- [x] Run navigation, UI and native Entry regressions, Android install, iOS compile
  and static checks. Check Home open/close on the connected emulator.

Keep component styling, content widths, outer navigation and page padding intact.
No screenshot baseline updates or emulator display changes are part of this task.

Validation: 26 targeted Android host tests, 3 applicable adaptive tests and 41
native simulator tests passed. 31 adaptive cases were inapplicable and skipped.
Android installed; emulator confirmed full list, split detail and close, with
scrolled list title bounds unchanged. Static checks retain 9 composeApp and 3
email findings outside the change. Remaining device acceptance is tracked in
[the central checklist](../../testing/pending-acceptance.md).

Half-open correction (2026-10-29): when a non-flat hinge is reported in a
multi-pane window, keep the split even without a selected detail. Only the detail
content animates; its outer measurable stays present. Full-open windows retain
on-demand expansion. Tests cover both hinge orientations and active-detail
geometry across half/open postures; current 90-degree emulator state was verified
without changing device configuration. See the acceptance checklist for limits.

Current status: implementation complete, including restored Home placeholder and
Favorites close. Remaining device acceptance and process cleanup are tracked in
the central checklist.
