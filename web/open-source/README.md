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
bodies. Buttons link to the fixed <platform>-licenses.html page with a
license or NOTICE fragment ID. Its inert templates preserve all upstream texts,
and the script renders only the selected declaration. Unknown IDs show an error.
Generate exactly two HTML files per platform. The generator removes obsolete
per-declaration pages; no content-hash filenames are generated.
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
These HTML files are the only checked-in webpage source. Website copies must remain byte-identical.
Git attributes preserve upstream text and source HTML line endings. Retain
upstream whitespace in license/NOTICE text instead of applying code formatting.

Regenerate both platforms whenever distributed dependencies change. Preserve license
text and attribution; update the generator instead of manually editing its HTML output.
Commit generated HTML files in this directory. The website build copies all four pages
to `web/dist/open-source/`. Keep list and detail files together so relative links work.
The app opens its fixed platform list online. All app versions share the current
notices for their platform. The [release workflow](../../docs/notes-web-deployment.md#发布新构建)
does not create version or build archives.

## Preview

Generate using [the tooling instructions](../../tools/open-source/README.md), then
run `node web/build.mjs` and serve `web/dist`. Open `/open-source/android.html` or
`/open-source/ios.html`.

UI text supports Simplified Chinese and English. App launch language overrides
browser language; standalone unsupported languages fall back to English. Local
links retain language, while upstream license/NOTICE bodies remain verbatim.
List and detail pages omit the app-authored footer disclaimer; upstream license
and NOTICE text remains complete and unchanged.

NOTICE detail bodies omit the app-authored heading; upstream text is unchanged.

Lists and details use equal top/bottom outer padding: 24px normally and 16px at
400px or less. First/last content margins cannot add outer whitespace.
