# Notes website

- `build.mjs` generates the published release endpoint, distribution files and HTML launch documents. `build.test.mjs` covers its public output and input validation.
- `published-builds.json` records the current published build for each active distribution channel.
- [`open-source/`](open-source/) contains the reviewed dependency inventories and upstream texts. The website and installed apps use the same fixed notice URLs and Compose components.
- `dist/` is disposable build output and is ignored.

The `:web` Gradle module owns the Compose website. Browser code lives in
`src/wasmJsMain/kotlin/`, and website assets live in `src/wasmJsMain/composeResources/`.
The HTML documents start Compose Wasm with an empty host. The release page
fetches `/releases/published.json` on demand, which the generator derives from the
mobile app’s `releases.json`.
The website fetches current distribution records from `/releases/site.json` on
startup, using HTML-embedded data as an offline/error fallback. The local
[release tool](../tools/release/README.md) updates these records and client feeds
without rebuilding the website. Existing open pages refresh on the next load.
Compose uses a Material 3 top app bar when the viewport can fit the navigation
labels and a compact navigation rail on the left at smaller widths. The
navigation menu opens the corresponding notice list directly. The
fixed list and detail URLs use the same Kotlin/Wasm navigation, language, theme
and text size as release notes; app embeds hide website navigation with
`embed=1`. Compose owns their
layout, Canvas drawing, scrolling and text selection. The notice bootstrap
uses no page stylesheet. An inline head script applies the URL theme and background
before Wasm starts, falling back to the saved website theme and system preference.
The Compose root reports its first drawn content through `data-page-frame`;
embedded apps wait for this signal before revealing their WebView.
A small browser adapter handles link activation and stores the list position in browser history.

Localized UI strings are in Compose Resources under `wasmJsMain`.
The build supplies the reviewed dependency and
license data in the notice launch documents. Reading those documents requires
Kotlin/Wasm browser support.

From the repository root, run:

```sh
node --test web/build.test.mjs web/device-viewport.test.mjs
./gradlew --configure-on-demand -Pkotlin.js.yarn=false :web:buildSite
python3 web/preview.py --port 8842
```

The Gradle build combines the Wasm website, update feeds and association files.
The downloads page links directly to the APK or App Store. Use a fresh output
directory to avoid serving obsolete files from an earlier build.
The notice launch documents contain an empty Compose host,
platform data, and a reference to the shared Wasm bootstrap. The `404.html`
document also starts Compose; open an unknown path in the preview to check the
custom error page and its HTTP 404 response.
Kotlin may warn about configuration on demand.
See [deployment](../docs/notes-web-deployment.md).

The Compose website uses the unmodified MiSans Regular OTF from the [official
MiSans download](https://hyperos.mi.com/font/details/sc). The Settings menu
credits Xiaomi MiSans and links to the bundled [font license](src/wasmJsMain/resources/licenses/misans-license.pdf).
Platform icons come from [Material Symbols Rounded](https://github.com/google/material-design-icons)
and [Simple Icons](https://github.com/simple-icons/simple-icons/blob/develop/icons/apple.svg).

Release content uses continuous reading in narrow content windows and a version list
with detail on larger windows. Mobile platforms are fixed to the host platform or
browser device platform, including iPadOS. The site navigation and settings keep
their existing layout. The standalone `device-viewport.js` is published at a
content-versioned URL before Wasm; other pages may use it without Compose. Wire
format, coordinate conversion and live updates are defined in
[Device Viewport Protocol](../docs/web-device-viewport.md).

Browser policy tests run with `:web:wasmJsBrowserTest` (Chrome Headless; set
`CHROME_BIN` when needed). They do not replace native WebView or device acceptance.

After `buildSite`, start the local preview above and run
`CHROME_BIN=<chrome-executable> node --test web/release-browser.test.mjs`.
`NOTES_PREVIEW_URL` overrides the default local preview URL. This browser check
records candidates in `web/build/release-candidates/` and verifies reading anchors,
browser back, mobile platforms and live fold changes against the built website.
