# Notes website

- `build.mjs` generates the static site. `build.test.mjs` covers its public output and input validation.
- `published-builds.json` is the append-only inventory of builds available for download.
- `open-source/` owns the four current pages, published at fixed paths under `/open-source/` for all app versions. The old published 1.1.2 notice URLs forward to the matching current page, including its query and fragment. New builds do not create notice archives or versioned URLs.
- `dist/` is disposable build output and is ignored.

Run `node --test web/build.test.mjs` and `node web/build.mjs` from a fresh checkout, then serve `web/dist` locally. See [deployment](../docs/notes-web-deployment.md).
