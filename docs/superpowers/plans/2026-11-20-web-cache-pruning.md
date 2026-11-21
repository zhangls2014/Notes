# Manifest-based web cache maintenance

Status: implementation and behavior verification complete; read-only review passed. Static analysis remains blocked by existing findings, including the already oversized download service. The maintainer authorized code submission and mainland 1.1.7/build 10 publication on 2026-11-21; publication and independent public verification are complete.

1. Record the invariant in architecture before code changes. About owns cache maintenance and resource labels; reuse common filesystem/download support without adding distributed dependencies.
2. Add failing regressions for successful manifest replacement, old completed files/checkpoints, failed/invalid manifests, partial download failures, cancellation of obsolete downloads, unrelated files and site isolation. Preserve the existing downloader's size/SHA-256 verification.
3. After a valid manifest, cancel obsolete background tasks and await acknowledgment/publication completion before deleting old fragments. Persist the verified manifest atomically only after both current resources verify, then remove completed obsolete files. Block manual-clear races with the existing asset mutex; preserve current resources/checkpoints and other cache families.
4. Read resource identity from the stored manifest for completed files and checkpoint rows; carry explicit identity for active transfers. Unknown Wasm/font files receive generic names. Keep dimensions, spacing, swipe and confirmation interactions unchanged.
5. Run About common tests on Android/iOS, About cache UI host regression, Android build and iOS compilation. Validate on the available emulator without changing display overrides. Record actual results and outstanding device acceptance centrally.

User authorization covers obsolete app-managed web resources/checkpoints under this policy. No repository files or arbitrary caches are to be deleted. The 1.1.7 changelog is finalized for 2026-11-21 and Android/iOS source versions move to 1.1.7/build 10; source version alignment does not imply App Store publication.

Follow-up requirement: cancel obsolete in-flight downloads rather than retain them. A valid manifest cancels old tasks in the current site, suppresses late delegate writes/retries and waits for cancellation/publication completion before deleting old fragments. Completed old files remain until current resources verify. [Apple documents asynchronous cancellation](https://developer.apple.com/documentation/foundation/urlsessiontask/cancel%28%29), so callback suppression and completion checks are required.

## Verification evidence

- The original cache test failed because an obsolete Wasm and its checkpoint remained after a manifest update. It passes with cleanup. Android About: 35 passed; iOS About: 36 passed; iOS download: 8 passed; composition-root cache UI: 12 passed. No failures or skips in these final behavior runs. Android Debug build and iOS compilation passed with incremental/cache reuse.
- Native cancellation testing uses registered then suspended ephemeral URLSession tasks with a bounded main-run-loop harness. It verifies cancellation acknowledgment, retained current/foreign-site tasks and suppression of a late cancellation callback retry. The initial runTest harness stalled without a main loop; the corrected fixture was also checked for registration before cancellation.
- The emulator automatically removed the old 8,648,883-byte Skiko file after upgrading. Current Skiko and MiSans and the persisted verified manifest remained, reducing managed resource bytes from 22,633,101 to 13,984,218. No display overrides, manual cache deletion or formal screenshot baseline updates were used.
- Static tasks were executed with rules unchanged: About 127, download 61, composeApp 434 findings remain. Modified/new lines were inspected; new style/condition warnings were corrected. Residual affected declarations are the existing resourceDownload three-return warning and the download class already above the 11-function limit (14 before, 15 with cancellation). No baseline/suppression or unrelated refactor was added.
- The above implementation-stage evidence used a local 1.1.6/build 9 Debug package. Physical iOS background-session restoration/cancellation and device visual acceptance remain in the central checklist.

## Authorized release

1. Revalidate the signed mainland 1.1.7/build 10 APK, dependency inventory and About/native/UI regression before committing source and finalized version metadata.
2. Prepare the digest-bound bundle and publish through the local release command. Verify the public APK, feed, history and website distribution data; preserve global/App Store pointers.
3. Synchronize the publication records and receipt into the same final local commit. No Git push, service restart, website code deployment, cache sweep or unrelated server configuration change is included.

Release preparation on 2026-11-21 passed mainland Release assembly/lintVital and explicitly rerun Android About 35 plus selected UI/navigation 81 tests. Native About 36, download 8 and iOS compilation tasks were UP-TO-DATE, reusing implementation-stage results. Refreshed actual dependency inventory produced no declaration changes; notice generator 2, Node 32 and release-tool Python 2 checks passed.

Publication on 2026-11-21 completed through the local command after the source commit. The public mainland APK is 8,020,521 bytes, SHA-256 `64ea2cd5990e5f30bb2ea1af677d05efe4845966ebe5b67692d51867172a5274`. Complete public download/hash verification, feed/site/history checks and repository pointer synchronization passed; other channels and previous history were preserved. The exact signed Release bundle upgraded the emulator from 1.1.6 to 1.1.7/build 10 without uninstall/data clearing and remained running. No website code deployment or authentication restart was needed.

Official-site browser regression: first run failed at the scroll-anchor assertion; its screenshot was preserved privately. The unchanged test passed on a reproducibility rerun. No test or production code was modified to obtain that result; the intermittent cause is unconfirmed and remains recorded for future reproduction. Source, final publication records and this receipt are consolidated into one local commit; no Git push is included.
