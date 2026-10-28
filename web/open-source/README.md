# Open-source notice pages

This directory owns the Android and iOS static open-source notices. `android.html`
and `ios.html` must be independently usable, with inline styling and no external
scripts, fonts, analytics, or build dependencies. Filenames use lowercase English.
`dependencies.json` records the resolved platform inventories and source metadata.
`licenses/` retains upstream license texts used by the generator.

Display one card per exact Maven `groupId` (the first coordinate segment), with
each module and version listed inside that card. Do not merge parent/child package
names or discard module-specific source links, licenses or NOTICE references.
The page count represents package groups; the inventory retains every module.
List pages must not embed license or NOTICE bodies. Link buttons navigate to the
matching anchor in `android-licenses.html` or `ios-licenses.html`, which preserve
the complete upstream text and provide a return link to the component list.
License, project source and NOTICE links share the same button style in a wrapping
flex row. Keep each button's text on one line and move whole buttons to the next
line when space is insufficient. The component list's top navigation uses plain,
vertically centered links, including the link to full license texts.

Typography follows the application's baseline reading styles: 16px/24px body and
license text, 14px/20px auxiliary text and action buttons, and 16px/24px component
names at browser defaults. Relative units scale these values together. App launch
URLs carry the app-only `fontScale` (1, 1.5 or 2), never the system-multiplied scale.
An inline script reads it before rendering and preserves it across local list/detail
links. External links are unchanged. Standalone/invalid parameters use scale 1;
browser font preferences and zoom still apply. Long buttons may wrap internally
only when a single button would otherwise exceed the card width at large scales.
App URLs also carry `theme=light` or `theme=dark`, resolved from the application's
existing theme policy. This explicit theme overrides the browser's system theme;
absent/invalid theme parameters use `prefers-color-scheme`. Local navigation retains
all appearance parameters. App launches also supply six validated RGB hex colors:
`bg` (surface), `card` (surfaceContainerLow), `fg` (onSurface), `muted`
(onSurfaceVariant), `line` (outlineVariant), and `accent` (primary). Use the resolved
Material color scheme, including Android dynamic colors; do not recreate palettes
from a preference flag. Only six-digit RGB hex values are accepted and retained in
local links. Missing/invalid values keep the standalone light/dark fallback palettes.
These HTML files are the only checked-in webpage source. Bundled copies must remain byte-identical.
Git attributes preserve upstream text and source HTML line endings. Retain
upstream whitespace in license/NOTICE text instead of applying code formatting.

Regenerate both platforms whenever distributed dependencies change. Preserve license
text and attribution; update the generator instead of manually editing its HTML output.
Commit the four HTML files in this directory. Do not keep packaged copies under `src/`.
Gradle automatically syncs the Android pair into
`feature/about/build/generated/open-source/androidMain/composeResources/files/open-source/`
and the iOS pair into
`feature/about/build/generated/open-source/iosMain/composeResources/files/open-source/`.
These ignored outputs are Compose Resources inputs, with their Sync task dependencies
provided through `customDirectory`. Each app contains only its own platform's two pages.
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
