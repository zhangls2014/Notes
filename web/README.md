# Notes website

- `build.mjs` generates the published release endpoint, distribution files and HTML fallback. `build.test.mjs` covers its public output and input validation.
- `published-builds.json` is the append-only inventory of builds available for download.
- `open-source/` owns the four current pages, published at fixed paths under `/open-source/` for all app versions. The old published 1.1.2 notice URLs forward to the matching current page, including its query and fragment. New builds do not create notice archives or versioned URLs.
- `dist/` is disposable build output and is ignored.

The `:web` Gradle module owns the Compose website. Browser code lives in
`src/wasmJsMain/kotlin/`, and website assets live in `src/wasmJsMain/composeResources/`.
The release page fetches `/releases/published.json` on demand. The generator
derives this endpoint and the HTML fallback from the mobile app’s `releases.json`.
The dedicated notices page links to the current platform documents.

From the repository root, run:

```sh
node --test web/build.test.mjs
./gradlew --configure-on-demand -Pkotlin.js.yarn=false :web:buildSite
python3 -m http.server 8842 --bind 127.0.0.1 --directory web/dist
```

The Gradle build combines the Wasm distribution with the existing APK pages,
update feeds, notices and association files. Kotlin may warn about configuration
on demand. A standalone `node web/build.mjs` build produces the HTML fallback.
See [deployment](../docs/notes-web-deployment.md).

The bundled Noto Sans CJK SC font covers frequent Simplified Chinese characters
and current public copy. It was subset from the [upstream Regular font](https://github.com/notofonts/noto-cjk/blob/main/Sans/OTF/SimplifiedChinese/NotoSansCJKsc-Regular.otf)
with HarfBuzz. The [font license](src/wasmJsMain/resources/licenses/noto-sans-cjk-OFL.txt)
ships in the website distribution. Platform icons come from [Material Symbols Rounded](https://github.com/google/material-design-icons)
and [Simple Icons](https://github.com/simple-icons/simple-icons/blob/develop/icons/apple.svg).
