# Open-source notice pages

This directory owns the Android and iOS static open-source notices. `android.html`
and `ios.html` must be independently usable, with inline styling and no external
scripts, fonts, analytics, or build dependencies. Filenames use lowercase English.
`dependencies.json` records the resolved platform inventories and source metadata.
`licenses/` retains upstream license texts used by the generator.

Display one card per exact Maven `groupId` (the first coordinate segment), with
each module and version listed inside that card. Do not merge parent/child package
names or discard module-specific source links, licenses or NOTICE references.
The inventory retains every module. List pages must not embed license or NOTICE
bodies. Buttons link to `<platform>-license-<id>.html` or
`<platform>-notice-<hash>.html`; each detail contains exactly one preserved text,
without intro card or webpage navigation. The list intro contains no navigation
links. The old aggregate `android-licenses.html` and `ios-licenses.html` files are
retained pending authorized cleanup, but are neither linked nor packaged.
License, project source and NOTICE links share the same button style in a wrapping
flex row. Keep each button's text on one line and move whole buttons to the next
line when space is insufficient.

The pages follow the app's Material 3 Expressive design. Group cards and the
introductory container use the app's 28px extra-large corner. Component action links share
a Filled Tonal treatment: the same capsule corner in resting and pressed states,
and a state layer on hover/press. Pressing must not morph the corner shape. Passive pointer/keyboard events maintain the pressed
attribute and clear it on release, cancel, leave, blur or key release; only the
primary left button and Enter activate it. Space preserves native link scrolling. Action targets are at least
48px high. Retain native link semantics, scrolling and cancellation; do not add
pointer handlers that prevent default gestures. Focus is visible and reduced-motion
preferences disable decorative transitions. Body and action type scales stay unchanged.

Typography follows the application's baseline reading styles: 16px/24px body and
license text, 14px/20px auxiliary text and action buttons, and 16px/24px component
names at browser defaults. Relative units scale these values together. App launch
URLs carry the effective `fontScale` read inside the app theme, including both
system font settings and the application multiplier. Any finite positive scale is
accepted, including scales below 1 or above 2.
An inline script reads it before rendering and preserves it across local list/detail
links. External links are unchanged. Standalone/invalid parameters use scale 1;
browser font preferences and zoom still apply. Long buttons may wrap internally
only when a single button would otherwise exceed the card width at large scales.
App URLs also carry `theme=light` or `theme=dark`, resolved from the application's
existing theme policy. This explicit theme overrides the browser's system theme;
absent/invalid theme parameters use `prefers-color-scheme`. Local navigation retains
all appearance parameters. App launches supply validated RGB hex colors:
`bg` (surface), `card` (surfaceContainerLow), `fg` (onSurface), `muted`
(onSurfaceVariant), `line` (outlineVariant), `accent` (primary), `container`
(primaryContainer), and `on-container` (onPrimaryContainer). Use the resolved
Material color scheme, including Android dynamic colors; do not recreate palettes
from a preference flag. Only six-digit RGB hex values are accepted and retained in
local links. Missing/invalid values keep the standalone light/dark fallback palettes.
These HTML files are the only checked-in webpage source. Bundled copies must remain byte-identical.
Git attributes preserve upstream text and source HTML line endings. Retain
upstream whitespace in license/NOTICE text instead of applying code formatting.

Regenerate both platforms whenever distributed dependencies change. Preserve license
text and attribution; update the generator instead of manually editing its HTML output.
Commit generated HTML files in this directory. Do not keep packaged copies under `src/`.
Gradle automatically syncs the Android list and individual detail files into
`feature/about/build/generated/open-source/androidMain/composeResources/files/open-source/`
and the corresponding iOS files into
`feature/about/build/generated/open-source/iosMain/composeResources/files/open-source/`.
These ignored outputs are Compose Resources inputs, with their Sync task dependencies
provided through `customDirectory`. Each app contains only its own platform's list and individual details.
No hosting is required.

## Preview and package

Generate using [the tooling instructions](../../tools/open-source/README.md), then
preview with `python3 -m http.server 8842 --bind 127.0.0.1 --directory web/open-source`.
Open `/android.html` and `/ios.html`. Keep each platform's list and detail files together in its
packaged resource directory so relative links work.
Every page inlines its own styling and requires no external assets.

The application build includes these resources through Compose Resources. The About
entry works offline; external project sources still require a network connection.
Each installed app retains the declarations shipped with that version.

UI text supports Simplified Chinese and English. App launch language overrides
browser language; standalone unsupported languages fall back to English. Local
links retain language, while upstream license/NOTICE bodies remain verbatim.
Wide app windows retain the component list beside license/NOTICE or external
project content using the root Navigation 3 scene policy. External project views
are separate from bundled notice views. Narrow project links use the system browser.

The list fills the viewport until a detail is opened, then root scenes adapt.

List and detail pages omit the app-authored footer disclaimer; upstream license
and NOTICE text remains complete and unchanged.

NOTICE detail bodies omit the app-authored heading; upstream text is unchanged.
Native license detail bars use the localized generic “License” title. Bundled
web views have no loading indicator; project source views retain theirs.

Lists and details use equal top/bottom outer padding: 24px normally and 16px at
400px or less. First/last content margins cannot add outer whitespace. Detail
bars use a right-side close action when the list is visible, otherwise a back
action; closing details restores the full-width list and its reading position.
