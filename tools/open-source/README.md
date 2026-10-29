# Open-source notices tooling

This directory contains dependency export and static notice generation tools only.
Use English, lowercase hyphenated script names. Generated dependency inventories and
HTML in `web/open-source/` are the only checked-in webpage source used for packaging.
The generator updates this directory only. Do not copy generated pages into `src/`.
Gradle Sync tasks prepare each platform's list and individual detail pages under
`feature/about/build/generated/open-source/<platform>Main/composeResources/files/open-source/`.
Compose Resources consumes these task outputs through `customDirectory`; build outputs
are ignored by Git. Temporary Gradle reports belong in `build/`.
Keep upstream license and NOTICE text verbatim. Never infer an unknown license.

The Android inventory uses the production full release runtime configuration; iOS
uses the device ARM64 compilation. Test and build plugin dependencies are excluded.

## Regenerate

Run from the repository root with the project's JDK 25. Never run two Gradle
processes against the same checkout concurrently.

```bash
./gradlew -I tools/open-source/export-dependencies.gradle exportNoticeDependencies \
  --no-configuration-cache --no-parallel
python3 tools/open-source/generate.py --refresh
python3 -m unittest discover -s tools/open-source -p 'test_*.py'
node tools/open-source/test-font-scale.mjs
```

Export uses the resolved component graph (including BOM/metadata modules), not an
APK size or native link audit. Android sources are `:androidApp`'s
`prdFullReleaseRuntimeClasspath`; iOS sources are `:composeApp`'s
`iosArm64CompileKlibraries`. The current debug-only LeakCanary dependency is
intentionally excluded. Revisit configuration selection if flavor dependencies diverge.

Refresh reads local Maven POMs, fetching missing POMs from Google Maven or Maven
Central and caching them under `build/open-source/poms/`. Maven parent licenses
are inherited when absent locally. Unknown licenses stop generation. Runtime
archives selected by Gradle contribute LICENSE/NOTICE text, including nested
`classes.jar`; sources, Javadoc and test archives are excluded by the selected
configurations. Export resolves and downloads external artifacts, excluding project
outputs before artifact variant selection. The generator rejects missing exported
files. BOM and metadata-only graph nodes legitimately have no runtime archive.

`web/open-source/assets.json` adds distributed resources and native libraries that
do not appear separately in the Maven graph. When Skiko changes, verify its pinned
Skia version in upstream `skiko/gradle.properties`, retrieve that release's iOS
ARM64 package, and update preserved native licenses. `licenses/sources.json` records
upstream text sources; the SQLite file preserves the official copyright page's
three paragraphs defining the public-domain dedication and deliverable boundary.

For deterministic offline regeneration from the checked-in inventory:

```bash
python3 tools/open-source/generate.py
```

`dependencies.json` deduplicates preserved notice text by SHA-256. Component
buttons link directly to `<platform>-license-<id>.html` or
`<platform>-notice-<hash>.html`, each containing exactly one original text without
intro card or webpage navigation. List intro cards contain no navigation links.
The generator also retains the old aggregate `-licenses.html` files pending
authorized cleanup; those files are neither linked nor packaged. Android and iOS
builds sync only their own list and individual details into Compose resources.

Each page inlines styling and a small script applying app language, font scale,
resolved theme and eight Material colors across local navigation. UI text supports
Simplified Chinese and English; app URL language takes priority over browser
language, unsupported languages fall back to English, and upstream texts remain
verbatim. The Node test discovers all generated HTML and executes isolated DOM
fixtures: 10 font-scale inputs × 4 themes × 3 origins × 4 palettes per page, plus
8 language selection and link-retention cases per page. At 20 current files this
is 9,600 appearance/navigation and 160 language cases. These tests do not verify
browser rendering or native font metrics. No Python or Node packages are required.

Ordinary builds copy HTML without invoking Python or refreshing dependencies. Gradle
tracks source HTML as inputs and reuses unchanged Sync outputs. Each Sync destination
is owned by its task; stale generated files are removed during synchronization.
When moving resources between source sets, validate packages without historical
shared-resource outputs. This migration check is separate from normal regeneration.

The inline CSS shares the app's Expressive shape/type hierarchy. Component action
links use primary-container colors, unchanged capsule corners in resting/pressed states and
state layers; reduced-motion preferences disable these transitions. Passive pointer/keyboard
listeners maintain press state without cancelling native scrolling or link navigation.
