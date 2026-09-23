# Hinge-Isolated Reimplementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restore the pre-`30b2ab1c` visual baseline on windows without hinges, then reintroduce hinge avoidance only when the platform reports visible hinge geometry.

**Architecture:** Revert the mixed adaptive commit first. Rebuild hinge geometry in `core:theme`, expose it from the composition root, and keep every existing Material component as the no-hinge branch. Feature-specific safe layouts are selected only from `WindowAdaptiveInfo.windowPosture.hingeList`; state is created outside those visual branches.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Material 3 Adaptive, Navigation 3, kotlin.test, Gradle, Android emulator/ADB.

---

## File map

- `core/theme/.../layout/HingeGeometry.kt`: pure clipping, normalization and stable safe-region selection.
- `core/theme/.../layout/HingeSafeContent.kt`: hinge-only bounded layout; direct content passthrough when there are no relevant hinges.
- `core/theme/.../layout/WindowAdaptiveInfo.kt`: composition locals and `AlwaysAvoid` pane directive.
- `core/theme/.../component/HingeSafeDialog*.kt`: hinge-only full-window dialog; callers retain their original dialog when there is no hinge.
- `composeApp/.../App.kt`: the only runtime window-fact provider.
- `composeApp/.../AppNavHost.kt`: hinge-only host constraint around the existing Nav3 assembly.
- `feature/main/.../AppShell.kt` and `HingeNavigationLayout.kt`: original navigation scaffold for ordinary windows, safe shell only for hinge windows.
- `feature/login/.../LoginScreen.kt`: original layout without hinges, safe physical regions with hinges.
- `feature/email/.../EmailSearchBar.kt` and `NewEmailSheet.kt`: original Material surfaces without hinges, safe dialogs with hinges; shared state outside branches.
- `feature/settings/.../SettingsScreen.kt`, `core/preference/...`: original preferences without hinges, safe content/dialogs with hinges.
- `core/theme`, `feature/login`, and `feature/main` tests: geometry and branch-policy regressions.
- `docs/architecture.md` and `docs/audits/2026-09-23-foldable-hinge.md`: implementation contract and evidence.

### Task 1: Restore the pre-adaptive visual baseline

**Files:** all files changed by commit `30b2ab1c`; preserve the approved spec and this plan.

- [ ] **Step 1: Confirm the isolated worktree is clean**

Run: `git status --short`

Expected: only this uncommitted plan file is listed.

- [ ] **Step 2: Commit the implementation plan**

Run:

```bash
git add docs/superpowers/plans/2026-09-23-hinge-isolated-reimplementation.md
git commit -m "docs: plan isolated hinge reimplementation"
```

Expected: one documentation-only commit.

- [ ] **Step 3: Revert the mixed hinge commit**

Run: `git revert --no-edit 30b2ab1c`

Expected: a new revert commit; the design and plan commits remain present.

- [ ] **Step 4: Verify the restored baseline compiles and its existing focused tests pass**

Run:

```bash
./gradlew :core:theme:testAndroidHostTest :feature:login:testAndroidHostTest :feature:main:testAndroidHostTest
```

Expected: `BUILD SUCCESSFUL`.

### Task 2: Reintroduce pure hinge geometry with RED/GREEN evidence

**Files:**
- Create: `core/theme/src/commonMain/kotlin/me/zhangls/theme/layout/HingeGeometry.kt`
- Create: `core/theme/src/commonTest/kotlin/me/zhangls/theme/layout/HingeGeometryTest.kt`
- Modify: `core/theme/build.gradle.kts`

- [ ] **Step 1: Write failing tests for empty, clipped, unordered, and posture-stable hinge inputs**

Add tests that call the not-yet-existing `safeRegions()` and `largestSafeRegion()` with `Rect` values. Assert:

```kotlin
assertEquals(listOf(window), safeRegions(window, emptyList()))
assertEquals(
  listOf(Rect(0f, 0f, 490f, 800f), Rect(510f, 0f, 1000f, 800f)),
  safeRegions(window, listOf(verticalHinge)),
)
assertEquals(
  safeRegions(window, halfOpenedHinges),
  safeRegions(window, flatHinges),
)
assertEquals(Rect(0f, 0f, 490f, 800f), largestSafeRegion(equalAreaRegions))
```

Use hinge test objects whose bounds are identical and whose posture flags differ, proving geometry—not folding state—controls layout.

- [ ] **Step 2: Run the test and verify RED**

Run: `./gradlew :core:theme:testAndroidHostTest --tests '*HingeGeometryTest*'`

Expected: compilation failure because `safeRegions` and `largestSafeRegion` do not exist.

- [ ] **Step 3: Implement the minimal geometry functions**

Implement `safeRegions(windowBounds, hinges)` so it intersects visible hinge bounds with the window, normalizes ordering, splits every current region on each intersecting hinge, removes zero-area output, and sorts by top then left. Implement:

```kotlin
fun largestSafeRegion(regions: List<Rect>): Rect? =
  regions.maxByOrNull { it.width * it.height }
```

Do not inspect `isFlat`, `isSeparating`, device name, or navigation placement.

- [ ] **Step 4: Run the focused test and verify GREEN**

Run: `./gradlew :core:theme:testAndroidHostTest --tests '*HingeGeometryTest*'`

Expected: `BUILD SUCCESSFUL` and all `HingeGeometryTest` cases pass.

- [ ] **Step 5: Commit geometry**

```bash
git add core/theme/build.gradle.kts core/theme/src/commonMain/kotlin/me/zhangls/theme/layout/HingeGeometry.kt core/theme/src/commonTest/kotlin/me/zhangls/theme/layout/HingeGeometryTest.kt
git commit -m "feat(theme): add posture-stable hinge geometry"
```

### Task 3: Provide hinge facts and transparent safe layout primitives

**Files:**
- Modify: `core/theme/src/commonMain/kotlin/me/zhangls/theme/layout/WindowAdaptiveInfo.kt`
- Create: `core/theme/src/commonMain/kotlin/me/zhangls/theme/layout/HingeSafeContent.kt`
- Modify: `composeApp/src/commonMain/kotlin/me/zhangls/entry/App.kt`
- Test: `core/theme/src/commonTest/kotlin/me/zhangls/theme/layout/HingeGeometryTest.kt`

- [ ] **Step 1: Add a failing policy test for relevance filtering**

Define tests for a new pure function:

```kotlin
assertFalse(shouldUseHingeLayout(emptyList(), horizontalOnly = false))
assertFalse(shouldUseHingeLayout(listOf(verticalHinge), horizontalOnly = true))
assertTrue(shouldUseHingeLayout(listOf(horizontalHinge), horizontalOnly = true))
assertTrue(shouldUseHingeLayout(listOf(verticalHinge), horizontalOnly = false))
```

- [ ] **Step 2: Run the test and verify RED**

Run: `./gradlew :core:theme:testAndroidHostTest --tests '*HingeGeometryTest*'`

Expected: compilation failure because `shouldUseHingeLayout` does not exist.

- [ ] **Step 3: Implement locals, directive, and a true no-op branch**

Add `LocalWindowHinges`, supply `windowPosture.hingeList` from `App.kt` and preview/test providers, and calculate the app directive with `HingePolicy.AlwaysAvoid`.

Implement `HingeSafeContent` with this required branch shape:

```kotlin
val hinges = LocalWindowHinges.current.filter { !horizontalOnly || !it.isVertical }
if (!enabled || hinges.isEmpty()) {
  content()
  return
}
HingeBoundedLayout(modifier, hinges, content)
```

The ordinary branch must not add `Box`, `fillMaxSize`, `clipToBounds`, constraints, or insets. Keep window-coordinate conversion inside the hinge-only layout.

- [ ] **Step 4: Run RED/GREEN target and focused consumers**

Run:

```bash
./gradlew :core:theme:testAndroidHostTest :androidApp:assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit the root contract**

```bash
git add core/theme/src/commonMain/kotlin/me/zhangls/theme/layout composeApp/src/commonMain/kotlin/me/zhangls/entry/App.kt
git commit -m "feat(theme): isolate hinge-safe layout from ordinary windows"
```

### Task 4: Rebuild login and navigation without changing ordinary components

**Files:**
- Modify: `feature/login/src/commonMain/kotlin/me/zhangls/login/LoginScreen.kt`
- Modify: `feature/login/src/commonTest/kotlin/me/zhangls/login/LoginPanePlanTest.kt`
- Modify: `feature/main/src/commonMain/kotlin/me/zhangls/main/AppShell.kt`
- Create: `feature/main/src/commonMain/kotlin/me/zhangls/main/HingeNavigationLayout.kt`
- Modify: `feature/main/src/commonTest/kotlin/me/zhangls/main/NavigationSuitePolicyTest.kt`
- Modify: `composeApp/src/commonMain/kotlin/me/zhangls/entry/AppNavHost.kt`

- [ ] **Step 1: Add failing branch-policy tests**

Assert that no hinges retain the pre-revert navigation type and login plan, while identical hinge bounds with half-open and flat posture produce identical results. Add explicit cases for horizontal and vertical hinges.

- [ ] **Step 2: Run the tests and verify RED**

Run:

```bash
./gradlew :feature:login:testAndroidHostTest :feature:main:testAndroidHostTest
```

Expected: failures for missing stable hinge policies.

- [ ] **Step 3: Add hinge-only branches**

Keep the existing login composable unchanged under `hingeList.isEmpty()`. Use safe physical regions only in the hinge branch. Keep `NavigationSuiteScaffold` byte-for-byte equivalent to the restored baseline in `AppShell`'s no-hinge branch; use `HingeNavigationLayout` only when `hasHinges` is true. Wrap the Nav3 host with `HingeSafeContent(horizontalOnly = true)`; its Task 3 no-op branch guarantees no node is added on ordinary windows.

- [ ] **Step 4: Run focused tests and verify GREEN**

Run:

```bash
./gradlew :feature:login:testAndroidHostTest :feature:main:testAndroidHostTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit login and shell isolation**

```bash
git add composeApp/src/commonMain/kotlin/me/zhangls/entry/AppNavHost.kt feature/login feature/main
git commit -m "feat(adaptive): isolate hinge login and navigation layouts"
```

### Task 5: Rebuild safe dialogs while preserving original Material surfaces

**Files:**
- Create: `core/theme/src/commonMain/kotlin/me/zhangls/theme/component/HingeSafeDialog.kt`
- Create: `core/theme/src/androidMain/kotlin/me/zhangls/theme/component/HingeSafeDialog.android.kt`
- Create: `core/theme/src/iosMain/kotlin/me/zhangls/theme/component/HingeSafeDialog.ios.kt`
- Modify: `core/theme/src/commonMain/kotlin/me/zhangls/theme/component/SimpleDialog.kt`
- Modify: `core/theme/src/commonMain/kotlin/me/zhangls/theme/component/TooltipIconButton.kt`
- Modify: `feature/email/src/commonMain/kotlin/me/zhangls/email/search/EmailSearchBar.kt`
- Modify: `feature/email/src/commonMain/kotlin/me/zhangls/email/component/NewEmailSheet.kt`
- Modify: `core/preference/src/commonMain/kotlin/me/zhangls/preference/ui/PreferenceRow.kt`
- Create: `core/preference/src/commonMain/kotlin/me/zhangls/preference/ui/PreferenceSelectionDialog.kt`
- Modify: `core/preference/src/commonMain/kotlin/me/zhangls/preference/ui/SelectIconButton.kt`

- [ ] **Step 1: Add failing pure branch tests for surface selection**

Introduce small internal policies returning enum values such as `OriginalMaterialSurface` and `HingeSafeSurface`. Test empty hinges, vertical hinges, and horizontal hinges. The empty case must always select `OriginalMaterialSurface`.

- [ ] **Step 2: Run the owning module tests and verify RED**

Run:

```bash
./gradlew :core:theme:testAndroidHostTest :core:preference:testAndroidHostTest :feature:email:testAndroidHostTest
```

Expected: compilation failures for the missing policies.

- [ ] **Step 3: Implement the original and hinge branches**

Preserve these exact ordinary branches:

- `SimpleDialog` uses its restored platform `Dialog`/Material content.
- Search uses restored `ExpandedFullScreenSearchBar` or `ExpandedDockedSearchBar`.
- New mail uses restored `ModalBottomSheet`.
- `ListPreference` and `DropdownMenuPopup` remain the ordinary preference surfaces.
- Tooltip popup remains enabled.

Only when `LocalWindowAdaptiveInfo.current.hasHinges` is true may callers construct `HingeSafeDialog`, `PreferenceSelectionDialog`, or the safe search/new-mail surfaces. Create text, search bar, scroll, draft, selection, and open/closed state before the visual branch.

- [ ] **Step 4: Run module tests and compile Android/iOS source sets**

Run:

```bash
./gradlew :core:theme:testAndroidHostTest :core:preference:testAndroidHostTest :feature:email:testAndroidHostTest :composeApp:compileKotlinIosSimulatorArm64
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit safe surfaces**

```bash
git add core/theme core/preference feature/email
git commit -m "feat(adaptive): isolate hinge-safe transient surfaces"
```

### Task 6: Rebuild settings hinge avoidance without wrapping ordinary settings

**Files:**
- Modify: `feature/settings/src/commonMain/kotlin/me/zhangls/settings/SettingsScreen.kt`
- Test: add a focused policy test under `feature/settings/src/commonTest/kotlin/me/zhangls/settings/SettingsLayoutPolicyTest.kt`

- [ ] **Step 1: Write the failing settings path test**

Assert:

```kotlin
assertEquals(SettingsLayout.Original, settingsLayoutFor(emptyList()))
assertEquals(SettingsLayout.HingeSafe, settingsLayoutFor(listOf(verticalHinge)))
assertEquals(SettingsLayout.HingeSafe, settingsLayoutFor(listOf(horizontalHinge)))
```

- [ ] **Step 2: Run the test and verify RED**

Run: `./gradlew :feature:settings:testAndroidHostTest --tests '*SettingsLayoutPolicyTest*'`

Expected: compilation failure because the policy does not exist.

- [ ] **Step 3: Implement two explicit screen branches**

Extract the restored `Scaffold` + `AdaptiveContent` + `LazyColumn` tree into a private composable used directly for `SettingsLayout.Original`. For `SettingsLayout.HingeSafe`, place that same screen content inside `HingeSafeContent`. Do not wrap the ordinary branch.

- [ ] **Step 4: Run settings and dependency tests**

Run:

```bash
./gradlew :feature:settings:testAndroidHostTest :core:preference:testAndroidHostTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit settings isolation**

```bash
git add feature/settings
git commit -m "feat(settings): apply hinge safety only to folded windows"
```

### Task 7: Update architecture evidence and run full verification

**Files:**
- Modify: `docs/architecture.md`
- Recreate: `docs/audits/2026-09-23-foldable-hinge.md`
- Modify if required: `docs/README.md`, `AGENTS.md`

- [ ] **Step 1: Update documentation to match the isolated implementation**

Document the invariant: empty hinge lists keep the original component tree and insets; hinge-specific layouts are selected only from window facts. Record automated and emulator evidence without calling simulator coverage real-device coverage.

- [ ] **Step 2: Run formatting/diff checks**

Run:

```bash
git diff --check
./gradlew :core:theme:detekt :core:preference:detekt :feature:login:detekt :feature:main:detekt :feature:email:detekt :feature:settings:detekt
```

Expected: no whitespace errors and `BUILD SUCCESSFUL`. This repository does not expose a `spotlessCheck` task.

- [ ] **Step 3: Run all required builds serially**

Run each command only after the previous command finishes:

```bash
./gradlew :core:theme:testAndroidHostTest :core:preference:testAndroidHostTest :feature:login:testAndroidHostTest :feature:main:testAndroidHostTest :feature:settings:testAndroidHostTest :feature:email:testAndroidHostTest
./gradlew :androidApp:assembleDebug
./gradlew :composeApp:compileKotlinIosSimulatorArm64
./gradlew :androidApp:lintDevFullDebug
```

Expected: every command exits 0.

- [ ] **Step 4: Perform ordinary-device visual regression checks**

Install/run the debug APK on an ordinary phone profile. Capture and compare login, home search collapsed/expanded, favorites, settings, preference selection, logout confirmation, and new-mail surfaces against the restored baseline. Confirm no extra padding, clipping, component replacement, or navigation-shape change.

- [ ] **Step 5: Perform foldable checks and restore emulator overrides**

On the foldable profile, check vertical and horizontal hinges, half-open/flat transitions, search, settings dialogs, new mail, and IME. If `wm size` or `wm density` is changed, use a shell `trap` and finish with:

```bash
adb -s <serial> shell wm size reset
adb -s <serial> shell wm density reset
adb -s <serial> shell wm size
adb -s <serial> shell wm density
```

Expected: the final queries contain no `Override`.

- [ ] **Step 6: Commit docs and verification record**

```bash
git add docs AGENTS.md
git commit -m "docs: record isolated hinge verification"
```

- [ ] **Step 7: Review final scope**

Run:

```bash
git status --short
git diff 741aafbe...HEAD --stat
git log --oneline 741aafbe..HEAD
```

Expected: clean status; changes are limited to the approved hinge reimplementation, tests, and documentation.
