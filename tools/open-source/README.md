# Open-source notices tooling

This directory exports resolved application dependencies and refreshes the
reviewed inventory. Use English, lowercase hyphenated script names. Temporary
Gradle reports belong in `build/`; the inventory and reviewed upstream text live
in [web/open-source](../../web/open-source/).
Keep upstream license and NOTICE text verbatim. Never infer an unknown license.

The Android inventory uses the production global release runtime configuration; iOS
uses the device ARM64 compilation. Test and build plugin dependencies are excluded.

## Regenerate

Run from the repository root with the project's JDK 25. Never run two Gradle
processes against the same checkout concurrently.

```bash
./gradlew -I tools/open-source/export-dependencies.gradle exportNoticeDependencies \
  --no-configuration-cache --no-parallel
python3 tools/open-source/generate.py --refresh
python3 -m unittest discover -s tools/open-source -p 'test_*.py'
node --test web/build.test.mjs
./gradlew -Pkotlin.js.yarn=false :web:buildSite
```

Each platform configuration is resolved by a task in its owning project, then the
root task merges their JSON outputs. This preserves Gradle project isolation and
avoids cross-project configuration resolution during task execution.

Export uses the resolved component graph (including BOM/metadata modules), not an
APK size or native link audit. Android sources are `:androidApp`'s
`prdGlobalReleaseRuntimeClasspath`; iOS sources are `:composeApp`'s
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

The website build works from the checked-in inventory without re-exporting Gradle
dependencies or fetching upstream license metadata. It embeds validated list and
detail data into the shared notice entry documents. Kotlin/Wasm composables lay out and draw the notice pages on Canvas in browsers and app WebViews that support the Wasm bundle. The website build does not add dependencies to the mobile inventories.
Verify platform isolation, full text retention, and fixed detail links with the
web tests. See the [website guide](../../web/README.md) for preview instructions.
