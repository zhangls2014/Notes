# Local mobile releases

Run from the repository root with Node.js 24, OpenSSH (`ssh`, `scp`) and, for mainland APKs, Android SDK `apkanalyzer` / `apksigner` plus a compatible JDK. The server needs Python 3 on Linux, writable static website/download directories, and no new service. This tool does not build the application, restart authentication, configure Nginx, or modify CI/CD.

## Private configuration

Use `.local/release.json` (directory `0700`, file `0600`), or pass `--config /path/to/private/config.json`. Actual hosts, credentials and signer fingerprints must not enter Git. Configure authentication using existing SSH keys/agent and a previously verified `known_hosts` entry. Batch mode and strict host verification are mandatory; the tool never accepts a new host key automatically.

Example only; replace paths and identity values with reviewed local values:

```json
{
  "siteOrigin": "https://notes.example.com",
  "apkOrigin": "https://downloads.example.com",
  "sshHost": "release@example-host",
  "identityFile": "/path/to/existing/deployment-key",
  "knownHostsFile": "/path/to/existing/verified-known-hosts",
  "siteRoot": "/var/www/notes-site",
  "downloadsRoot": "/var/www/notes-downloads",
  "stagingRoot": "/var/lib/notes-releases",
  "packageName": "me.example.notes",
  "signerSha256": "0000000000000000000000000000000000000000000000000000000000000000",
  "apkanalyzer": "/path/to/android-sdk/cmdline-tools/latest/bin/apkanalyzer",
  "apksigner": "/path/to/android-sdk/build-tools/37.0.0/apksigner",
  "javaHome": "/path/to/jdk"
}
```

The APK package and signer are the trusted identity of the existing installation, not values copied from the candidate being uploaded. Colon-separated SHA-256 fingerprints are accepted. `javaHome` is optional when the current process already uses a suitable JDK. Global and App Store announcements do not need APK tools or signer configuration.

`identityFile` and `knownHostsFile` optionally select existing private SSH material for both SSH and SCP without changing system SSH configuration. When omitted, existing OpenSSH configuration/agent settings apply. Neither option creates keys or accepts unverified host keys.

`stagingRoot` must be outside both public roots, owned by the release user and mode `0700`. Roots must use absolute paths without whitespace or shell metacharacters. The release user must already have write access; no `sudo` is used. Nginx must serve the configured download path and allow public traversal of its parents. This setup and the initial website deployment require separate operational authorization.

## Prepare offline

Finalize the release date in the app's existing `releases.json`, build and test the app, and refresh open-source notices when distributed dependencies change. Preparation never changes the version, release date, or active channel pointers.

```bash
node tools/release/release.mjs prepare \
  --channel android-prd-mainland \
  --apk /path/to/Notes-release.apk
```

The APK supplies the version/build. Optional `--version` and `--build` must match it. Debuggable APKs, unexpected package/channel/signer, unsigned APKs and releases without a finalized date are rejected. Both the source and bundle copy are inspected. The output contains `release.json` and, for mainland, `artifact.apk`; it prints the bundle location, entry and SHA-256 of `release.json`. The manifest binds the APK size/hash, release content and public origins.

By default bundles go to `.local/releases/<unique-id>/`; `--output /path/to/new-directory` selects an explicit new directory. Existing directories are never overwritten. Failed preparation may leave a partial directory; use a new output directory for another attempt.

Other channels announce availability without uploading an APK:

```bash
node tools/release/release.mjs prepare \
  --channel android-prd-global --version 1.2.0 --build 10

node tools/release/release.mjs prepare \
  --channel ios-app-store --version 1.2.0 --build 10 \
  --store-url https://apps.apple.com/app/id123456789
```

App Store publication still happens through Apple. The tool checks that the supplied page responds successfully; this does not establish release availability in every region. Complete that acceptance before announcing it.

## Publish an explicitly approved bundle

Review the prepared version, channel, changelog and target. Authorize that exact bundle digest, then run:

```bash
node tools/release/release.mjs publish \
  --bundle /path/to/prepared-release \
  --confirm <release-json-sha256>
```

There is no interactive prompt or credential input. A job runner can invoke exactly the same command after an external approval step. `--confirm` is an execution guard binding the request to a reviewed bundle, not permission for an AI to bypass the repository's production authorization rule.

The command:

1. Revalidates the bundle and, for mainland, its APK identity with the configured trusted signer. Requires public `/releases/site.json`, proving the new data endpoint has been deployed.
2. Creates a unique private upload directory over SSH and copies only the named bundle files with SCP.
3. Checks remote hashes and installs a new immutable APK path. An existing different APK is never overwritten. Downloads and hashes the public APK before proceeding. Complete public APK verification allows up to 15 minutes, including response and body transfer; a slow download still has to match the complete size and SHA-256 before any feed switches.
4. Takes a nonblocking publication lock shared across channels, reloads current server data, rejects repeat/lower builds or lower versions, and merges history and other channels. Records the pending digest in private `publication-state.json`. An interrupted publication can resume only with that exact bundle; another release must wait. Existing historical notes must match; reconcile deliberate corrections separately before publishing.
5. Atomically replaces each JSON file: history first, website builds second, the selected channel's client feed last. There is no multi-file transaction: an interruption can expose newer history/site data before the client feed changes. No automatic rollback is attempted.
6. Checks public history, website builds and the exact client feed. Synchronizes `web/published-builds.json` with all live channels only after verification. Review and commit this record with finalized release notes before the next full website build.

Keep `/releases/*` and `/updates/v1/*` revalidatable (`Cache-Control: no-cache`), as described in [deployment](../../docs/notes-web-deployment.md#官网缓存策略). Website data refreshes on page load; pages already open are not polled. Fetch failures retain the HTML's embedded fallback data.

## Failures and recovery

- A failed transfer/staging/hash check does not switch public version data. A successfully staged APK can remain publicly accessible at its immutable path without being announced.
- Lock contention fails immediately. Retry after the other publisher finishes, checking the resulting live version first.
- A connection error during commit leaves the result uncertain. Inspect live JSON and private `publication-state.json` before retrying. If its status is `pending`, rerun the same publish command with the original bundle and digest: the command finishes the remaining writes even if the website already points to that build. It never rolls back, overwrites APKs, or deletes files.
- Once the progress status is `complete`, repeat publication is rejected even for the same bundle. A failed public check after commit can mean the release is already active. If publication succeeded but local synchronization did not, review the live `/releases/site.json` and reconcile `web/published-builds.json` manually.
- Private uploads, partial bundles and hidden `.upload-*` hard links are retained for inspection. They consume disk space; review and remove them only with maintenance authorization. Keep historical public APKs.
- Before a full website deployment, reconcile repository channel records against live data. Deploying an old checkout can still restore old pointers; the mobile release command does not control independent website deployments.

## Verification

```bash
node --test tools/release/*.test.mjs web/build.test.mjs
PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s tools/release -p 'test_*.py'
```

Temporary-directory tests exercise staging, locks, digest failures, channel preservation and build monotonicity without SSH or production writes. Website build/browser commands are in [web/README.md](../../web/README.md). Actual SSH permissions, server filesystem behavior, production cache headers and physical-device installation remain deployment acceptance checks.
