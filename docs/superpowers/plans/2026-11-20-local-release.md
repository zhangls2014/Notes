# Local application publication

Status: implementation and local verification complete; read-only review passed. Website bootstrap and mainland 1.1.6/build 9 were separately authorized and published. This process record remains until outstanding acceptance is closed and cleanup is authorized.

## Delivered scope

- Offline release preparation validates finalized changelogs and APK package, channel, version, signer, size and SHA-256.
- Explicit digest authorization starts SSH staging, complete public APK verification and serialized static-data publication. Existing APKs and unrelated channels are preserved; interrupted writes can resume only with the original bundle.
- Website distribution data loads at runtime. Successful publication synchronizes repository channel records for subsequent website builds.
- Private target configuration uses existing deployment keys and verified host files without changing system SSH configuration. The maintainer separately authorized the nonrecursive APK channel-directory ownership adjustment.
- Android/iOS source versions are 1.1.6/build 9. Mainland is published; global remains 1.1.4/build 7 and App Store publication was not performed.

## Acceptance and maintenance

Current commands and recovery rules live in [the release guide](../../../tools/release/README.md); architecture belongs to [the architecture guide](../../architecture.md#官网发布渠道与认证域). Verification evidence and remaining physical-device download/installer and iOS association acceptance live in [the central checklist](../../testing/pending-acceptance.md#本地应用发布命令2026-11-20).

The first complete APK verification exceeded its former 120-second deadline on a measured slow connection. The deadline is now 15 minutes; complete size and SHA-256 validation remain mandatory before feed activation. The original bundle passed verification and publication after the correction. Detailed private deployment receipts and backups remain in ignored `.local/` storage.

No process-file deletion is authorized. After remaining acceptance is closed, preserve any new decisions in the current guides before requesting cleanup of this record.
