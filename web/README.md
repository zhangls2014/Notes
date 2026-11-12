# Notes website

- `build.mjs` generates the static site. `build.test.mjs` covers its public output and input validation.
- `archive-notices.py` copies or compares the notices in a signed distribution package.
- `published-builds.json` is the append-only inventory of builds available for download.
- `notice-archives/<channel>/<version>/<build>/` stores the exact two HTML files packaged with each published build. Use lowercase paths and retain old builds.
- `open-source/` owns the generator output for the current source tree. `dist/` is disposable build output and is ignored.

Run `node --test web/build.test.mjs` and `node web/build.mjs` from a fresh checkout, then serve `web/dist` locally. See [deployment](../docs/notes-web-deployment.md).
