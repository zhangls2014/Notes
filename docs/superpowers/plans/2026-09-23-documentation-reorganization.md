# Documentation Reorganization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reorganize the repository documentation so current architecture, operating procedures, and historical audits have distinct, accurate sources of truth.

**Architecture:** Keep `AGENTS.md` as a compact AI entry point, `README.md` as the project overview, and `docs/architecture.md` as the authoritative current architecture. Move dated investigations under `docs/audits/`, extract emulator procedures to `docs/testing/`, and add `docs/README.md` as the document index.

**Tech Stack:** Markdown, Kotlin Multiplatform, Compose Multiplatform, Navigation 3, Koin, Gradle.

---

### Task 1: Establish the documentation layout

**Files:**
- Create: `docs/README.md`
- Move: `docs/adaptive-layout-audit.md` to `docs/audits/2026-09-21-adaptive-layout.md`
- Move: `docs/android-17-multiform-factor-audit.md` to `docs/audits/2026-09-22-android-17-multiform-factor.md`

- [ ] **Step 1: Create the target directories and move the two dated audit files**

Use filesystem moves so Git can recognize the original audit contents as renames.

- [ ] **Step 2: Add the documentation index**

List the current architecture, emulator guide, dated audits, and documentation design records. State which documents are authoritative and which are historical.

- [ ] **Step 3: Verify the new paths**

Run: `find docs -maxdepth 3 -type f -print | sort`

Expected: current documents are at the root/testing paths and dated reports are under `docs/audits/`.

### Task 2: Replace the generic KMP document with the repository architecture

**Files:**
- Delete: `docs/android-kmp-architecture-modules.md`
- Create: `docs/architecture.md`

- [ ] **Step 1: Write the current module map**

Use the exact modules from `settings.gradle.kts`: platform/app roots, `core:model`, `core:database`, `core:data`, `core:theme`, `core:preference`, `core:network`, `core:framework`, and the four Feature API/implementation pairs.

- [ ] **Step 2: Document dependency and ownership rules**

Cover Core layering, Room isolation through `implementation`, Feature API/implementation boundaries, composition-root ownership, and Gradle `api` versus `implementation` rules.

- [ ] **Step 3: Document current runtime architecture**

Describe Koin assembly, MVI opt-in state persistence, Navigation 3 destination/effect contracts, adaptive-window facts, list-detail scene ownership, preference metadata ownership, and resource conventions using actual project names.

- [ ] **Step 4: Add change checklists**

Provide compact checklists for adding a Feature, moving a shared type, changing Room schema, and changing adaptive layout.

- [ ] **Step 5: Scan for generic-template terminology**

Run: `rg -n ':app|Hilt|EntryProviderInstaller|com\.example|LoginRoute' docs/architecture.md`

Expected: no matches.

### Task 3: Extract the Android emulator procedure and annotate audits

**Files:**
- Create: `docs/testing/android-emulator.md`
- Modify: `docs/audits/2026-09-21-adaptive-layout.md`
- Modify: `docs/audits/2026-09-22-android-17-multiform-factor.md`

- [ ] **Step 1: Write the emulator guide**

Include device selection, authoritative app-window measurement, resolution/density/rotation commands, fold posture notes, and a cleanup block using `trap`. Require reset verification with no `Override` line.

- [ ] **Step 2: Add historical-status banners to both audits**

State that the reports capture dated investigations and link to `../architecture.md` for current rules.

- [ ] **Step 3: Record resolved and retained Android 17 findings**

Mark login hinge handling as resolved by `SupportingPaneScaffold` and its common tests. Clarify that `savedKey = null` remains an intentional safety default rather than an unfinished blanket migration.

- [ ] **Step 4: Link the reusable procedure**

Point audit reproduction sections to `../testing/android-emulator.md` while retaining original evidence.

### Task 4: Compress the AI entry point

**Files:**
- Modify: `AGENTS.md`

- [ ] **Step 1: Keep executable project commands**

Retain Android build/install, iOS compile/test, lint, Fused Library, and OOM retry commands.

- [ ] **Step 2: Condense global hard rules**

Keep dependency direction, Room boundary, Feature API rules, MVI persistence safety, adaptive-window ownership, resource placeholders, schema migration, Gradle serialization, and emulator reset requirements.

- [ ] **Step 3: Replace implementation narratives with links**

Link to `docs/architecture.md`, `docs/testing/android-emulator.md`, and `docs/README.md`. Remove historical examples and component inventories already covered elsewhere.

- [ ] **Step 4: Check size and required rules**

Run: `wc -l AGENTS.md` and `rg -n 'wm size reset|implementation|savedKey|%1\$s|并发' AGENTS.md`

Expected: roughly 80–130 lines and every required rule is present.

### Task 5: Refresh the project README

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Correct module and Feature descriptions**

Reflect `composeApp`, all current Core modules, the Feature API/implementation pairs, and `feature:email` ownership of email screens.

- [ ] **Step 2: Correct versions and commands**

Use the version catalog values for AGP, Kotlin, JDK target, compile/target/min SDK, and use the valid Android assemble/install and iOS compile commands.

- [ ] **Step 3: Correct architecture and state descriptions**

Describe Koin, Navigation 3, Room/data isolation, adaptive layout, and opt-in `SavedStateHandle` persistence accurately.

- [ ] **Step 4: Add documentation links**

Link readers to `docs/README.md` and the authoritative architecture document rather than duplicating all details.

### Task 6: Validate the documentation set

**Files:**
- Verify: `AGENTS.md`
- Verify: `README.md`
- Verify: `docs/**/*.md`

- [ ] **Step 1: Validate relative Markdown links**

Run a local script that extracts relative `.md` links and checks that each target exists.

Expected: zero missing targets.

- [ ] **Step 2: Search for stale names and claims**

Run targeted `rg` searches for the removed audit paths, compileSdk 36, `:app`, Hilt, and unresolved login hinge claims outside historical context.

Expected: no misleading current-document matches.

- [ ] **Step 3: Check patch formatting and scope**

Run: `git diff --check`

Expected: exit code 0.

Run: `git status --short`

Expected: only documentation files in scope are changed, apart from the already committed design and plan records.

- [ ] **Step 4: Review the final diff against the design**

Confirm every document has one responsibility, the current architecture has one authoritative source, and the pre-existing emulator reset rule remains enforced.
