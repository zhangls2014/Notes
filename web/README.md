# Notes website

- `build.mjs` generates the published release endpoint, distribution files and HTML fallback. `build.test.mjs` covers its public output and input validation.
- `published-builds.json` records the current published build for each active distribution channel.
- [`open-source/`](open-source/) contains the reviewed dependency inventories and upstream texts. The website and installed apps use the same fixed notice URLs and Compose components.
- `dist/` is disposable build output and is ignored.

The `:web` Gradle module owns the Compose website. Browser code lives in
`src/wasmJsMain/kotlin/`, and website assets live in `src/wasmJsMain/composeResources/`.
The release page fetches `/releases/published.json` on demand. The generator
derives this endpoint and the HTML fallback from the mobile app’s `releases.json`.
Compose uses a Material 3 top app bar when the viewport can fit the navigation
labels and a compact navigation rail on the left at smaller widths. The
navigation menu opens the corresponding notice list directly. The
fixed list and detail URLs use the same Kotlin/Wasm navigation, language, theme
and text size as release notes; app embeds hide website navigation with
`embed=1`. Compose owns their
layout, Canvas drawing, scrolling and text selection. The notice bootstrap
uses no page stylesheet. A small browser adapter handles link activation and
stores the list position in browser history.

Localized UI strings are in Compose Resources under `wasmJsMain`.
The build supplies the reviewed dependency and
license data in the notice launch documents. Reading those documents requires
Kotlin/Wasm browser support.

From the repository root, run:

```sh
node --test web/build.test.mjs
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

The bundled Noto Sans CJK SC font covers frequent Simplified Chinese characters
and current public copy. It was subset from the [upstream Regular font](https://github.com/notofonts/noto-cjk/blob/main/Sans/OTF/SimplifiedChinese/NotoSansCJKsc-Regular.otf)
with HarfBuzz. The [font license](src/wasmJsMain/resources/licenses/noto-sans-cjk-OFL.txt)
ships in the website distribution. Platform icons come from [Material Symbols Rounded](https://github.com/google/material-design-icons)
and [Simple Icons](https://github.com/simple-icons/simple-icons/blob/develop/icons/apple.svg).
