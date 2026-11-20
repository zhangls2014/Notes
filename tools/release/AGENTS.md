# Local release tooling

- `release.mjs` owns local preparation, APK inspection and SSH orchestration; `remote.py` owns remote staging and publication. Use only standard libraries and existing SDK/SSH tools.
- Tests live beside the tools as `*.test.mjs` and `test_remote.py`. Run `node --test tools/release/*.test.mjs web/build.test.mjs` and `PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s tools/release -p 'test_*.py'`.
- Generated bundles belong in ignored `.local/releases/`; never store APKs, connection details or credentials in Git. Configuration examples use reserved example domains.
- Preparation is offline. Publishing requires an explicit bundle digest and target configuration. Never invoke production publication as a development check.
- Keep installed client feed paths and fields compatible. Preserve old APKs and unrelated channels. Temporary uploads stay outside public roots; no automatic deletion or rollback.
