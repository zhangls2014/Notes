# Pane-Only Hinge Avoidance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restrict hinge avoidance to the login supporting-pane scaffold and the email list-detail scene while restoring every single-pane screen, overlay, and navigation component to its standard implementation.

**Architecture:** The composition root continues to derive one `PaneScaffoldDirective` with `HingePolicy.AlwaysAvoid`. Only `SupportingPaneScaffold` and Navigation 3's `ListDetailSceneStrategy` consume that directive; all general-purpose hinge-safe containers and component switches are removed.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Material 3 Adaptive, Navigation 3, Kotlin test, Gradle.

---

## File map

- Keep and simplify `core/theme/.../layout/HingeGeometry.kt`: stable directive calculation only.
- Keep `core/theme/.../layout/WindowAdaptiveInfo.kt`: one root-provided directive; remove raw hinge locals.
- Keep `feature/login/.../LoginScreen.kt`: use `SupportingPaneScaffold` only.
- Keep `composeApp/.../AppNavHost.kt`: pass the directive only to list-detail scene strategy.
- Restore standard UI paths in `AppShell`, settings, search, email sheet, dialogs, preference controls, and tooltip.
- Delete `HingeSafeContent`, `HingeSafeDialog`, `HingeNavigationLayout`, and `PreferenceSelectionDialog` plus platform actuals.
- Update geometry/login tests and architecture documentation.

### Task 1: Establish the pane-only architecture guard

**Files:**
- Test: existing source tree (one-off structural regression command)

- [ ] **Step 1: Run the failing source-boundary check**

```bash
matches=$(rg -n \
  "HingeSafe|LocalWindowHinges|hasHinges|hasHorizontalHinge|safeRegions|largestSafeRegion" \
  composeApp/src core/preference/src core/theme/src feature/email/src feature/login/src feature/main/src feature/settings/src \
  --glob '*.kt' || true)
test -z "$matches" || { printf '%s\n' "$matches"; exit 1; }
```

Expected: FAIL and print the current single-pane hinge branches and generic safe-area implementations.

- [ ] **Step 2: Record the intended exceptions separately**

```bash
rg -n "LocalPaneScaffoldDirective" \
  composeApp/src/commonMain/kotlin/me/zhangls/entry/AppNavHost.kt \
  feature/login/src/commonMain/kotlin/me/zhangls/login/LoginScreen.kt
```

Expected: PASS with the two intended pane consumers.

### Task 2: Restore the standard application shell and navigation host

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/me/zhangls/entry/App.kt`
- Modify: `composeApp/src/commonMain/kotlin/me/zhangls/entry/AppNavHost.kt`
- Modify: `feature/main/src/commonMain/kotlin/me/zhangls/main/AppShell.kt`
- Delete: `feature/main/src/commonMain/kotlin/me/zhangls/main/HingeNavigationLayout.kt`

- [ ] **Step 1: Restore the NavDisplay tree without a generic hinge wrapper**

Remove the `HingeSafeContent` import, delete the wrapper line
`HingeSafeContent(horizontalOnly = true, enabled = backStack.tabSelection() != null) {`,
and delete its matching final brace. Do not change the existing `AppShell`, `NavDisplay`, decorators,
scene strategies, or entry provider. The directive flow remains:

```kotlin
val listDetailSceneStrategy = rememberListDetailSceneStrategy<NavKey>(
  shouldHandleSinglePaneLayout = true,
  directive = LocalPaneScaffoldDirective.current,
)
```

- [ ] **Step 2: Restore the standard navigation suite**

Replace the `hasHinges` branch in `AppShell` with one standard scaffold:

```kotlin
NavigationSuiteScaffold(
  navigationSuiteItems = {
    MainTab.entries.forEach { tab ->
      item(
        icon = { Icon(imageVector = tab.icon, contentDescription = stringResource(tab.label)) },
        label = { Text(stringResource(tab.label)) },
        selected = tab.destination == selected,
        onClick = { onSelectTab(tab.destination) },
      )
    }
  },
  layoutType = layoutType,
) {
  Box(Modifier.consumeWindowInsets(layoutType.navigationSuiteInsets())) { content() }
}
```

Call `NavigationSuiteScaffoldDefaults.navigationSuiteType(this)` so hinge posture does not alter the navigation component type.

- [ ] **Step 3: Remove raw hinge propagation and the custom navigation layout**

Delete `LocalWindowHinges` providers from `App.kt` and `ProvideWindowAdaptiveInfo`; delete `HingeNavigationLayout.kt`.

- [ ] **Step 4: Compile the affected host and shell**

Run:

```bash
./gradlew :composeApp:compileKotlinAndroid :feature:main:testAndroidHostTest
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/me/zhangls/entry/App.kt \
  composeApp/src/commonMain/kotlin/me/zhangls/entry/AppNavHost.kt \
  feature/main/src/commonMain/kotlin/me/zhangls/main/AppShell.kt \
  feature/main/src/commonMain/kotlin/me/zhangls/main/HingeNavigationLayout.kt
git commit -m "refactor(adaptive): limit hinge policy to pane scaffolds"
```

### Task 3: Restore all single-pane screens and overlays

**Files:**
- Modify: `feature/settings/src/commonMain/kotlin/me/zhangls/settings/SettingsScreen.kt`
- Modify: `feature/email/src/commonMain/kotlin/me/zhangls/email/search/EmailSearchBar.kt`
- Modify: `feature/email/src/commonMain/kotlin/me/zhangls/email/component/NewEmailSheet.kt`
- Modify: `core/theme/src/commonMain/kotlin/me/zhangls/theme/component/SimpleDialog.kt`
- Modify: `core/theme/src/commonMain/kotlin/me/zhangls/theme/component/TooltipIconButton.kt`
- Modify: `core/preference/src/commonMain/kotlin/me/zhangls/preference/ui/PreferenceRow.kt`
- Modify: `core/preference/src/commonMain/kotlin/me/zhangls/preference/ui/SelectIconButton.kt`
- Delete: `core/preference/src/commonMain/kotlin/me/zhangls/preference/ui/PreferenceSelectionDialog.kt`
- Delete: `core/theme/src/commonMain/kotlin/me/zhangls/theme/component/HingeSafeDialog.kt`
- Delete: `core/theme/src/androidMain/kotlin/me/zhangls/theme/component/HingeSafeDialog.android.kt`
- Delete: `core/theme/src/iosMain/kotlin/me/zhangls/theme/component/HingeSafeDialog.ios.kt`

- [ ] **Step 1: Remove the settings-page wrapper**

Render the existing `Scaffold` directly; do not wrap it in `HingeSafeContent`.

- [ ] **Step 2: Restore the Material search implementations**

Use `ExpandedFullScreenSearchBar` and `ExpandedDockedSearchBar` for expanded search exactly as selected by `SearchBarState`; remove `hasHinges`, `HingeSafeDialog`, hinge-only scrolling, and hinge-only state transitions.

- [ ] **Step 3: Restore standard modal surfaces**

Use the existing Compose `Dialog` in `SimpleDialog` and the existing `ModalBottomSheet` in `NewEmailSheet`, without any hinge branch or conditional vertical scrolling.

- [ ] **Step 4: Restore standard preference interactions**

Use `ListPreference` unconditionally in `PreferenceRow`, and `DropdownMenuPopup` unconditionally in `SelectIconButton`. Restore `remember { mutableStateOf(false) }` for popup state and delete `PreferenceSelectionDialog`.

- [ ] **Step 5: Restore the standard tooltip**

Render the existing `PlainTooltip` unconditionally; remove all `LocalWindowHinges` checks.

- [ ] **Step 6: Delete hinge-safe dialog implementations**

Delete the common expect and Android/iOS actual files because no overlay consumes them.

- [ ] **Step 7: Compile all affected UI modules**

Run:

```bash
./gradlew :core:theme:compileKotlinAndroid \
  :core:preference:compileKotlinAndroid \
  :feature:settings:compileKotlinAndroid \
  :feature:email:compileKotlinAndroid
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 8: Commit**

```bash
git add core/preference core/theme/src/commonMain/kotlin/me/zhangls/theme/component \
  core/theme/src/androidMain/kotlin/me/zhangls/theme/component \
  core/theme/src/iosMain/kotlin/me/zhangls/theme/component \
  feature/settings/src/commonMain/kotlin/me/zhangls/settings/SettingsScreen.kt \
  feature/email/src/commonMain/kotlin/me/zhangls/email
git commit -m "refactor(ui): keep single-pane components hinge-agnostic"
```

### Task 4: Keep hinge handling inside the login pane scaffold

**Files:**
- Modify: `feature/login/src/commonMain/kotlin/me/zhangls/login/LoginScreen.kt`
- Modify: `feature/login/src/commonTest/kotlin/me/zhangls/login/LoginPanePlanTest.kt`

- [ ] **Step 1: Restore the failing tabletop pane-plan assertions**

Restore tests that pass both `WindowAdaptiveInfo` and `PaneScaffoldDirective` to `loginPanePlan` and assert:

```kotlin
val plan = loginPanePlan(adaptiveInfo, directive)
assertEquals(1, plan.directive.maxHorizontalPartitions)
assertEquals(PaneAdaptedValue.Expanded, plan.value[SupportingPaneScaffoldRole.Main])
assertEquals(
  PaneAdaptedValue.Reflowed(SupportingPaneScaffoldRole.Main),
  plan.value[SupportingPaneScaffoldRole.Supporting],
)
```

- [ ] **Step 2: Run the focused test and verify RED**

Run:

```bash
./gradlew :feature:login:testAndroidHostTest --tests '*LoginPanePlanTest*tabletop*'
```

Expected: FAIL because the current plan ignores posture and the screen bypasses `SupportingPaneScaffold` for a horizontal hinge.

- [ ] **Step 3: Make `SupportingPaneScaffold` the only login layout path**

Restore `loginPanePlan(adaptiveInfo, directive)` so tabletop posture copies the directive with `maxHorizontalPartitions = 1` and applies `AdaptStrategy.Reflow(SupportingPaneScaffoldRole.Main)`. Render `SupportingPaneScaffold` unconditionally and remove `HingeSafeColumn`, `hasHorizontalHinge`, and the hinge-only `BrandPane` scrolling parameter.

- [ ] **Step 4: Verify login tests GREEN**

Run:

```bash
./gradlew :feature:login:testAndroidHostTest
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add feature/login/src/commonMain/kotlin/me/zhangls/login/LoginScreen.kt \
  feature/login/src/commonTest/kotlin/me/zhangls/login/LoginPanePlanTest.kt
git commit -m "refactor(login): avoid hinges through supporting panes"
```

### Task 5: Delete generic safe-region geometry and retain directive stability

**Files:**
- Modify: `core/theme/src/commonMain/kotlin/me/zhangls/theme/layout/HingeGeometry.kt`
- Delete: `core/theme/src/commonMain/kotlin/me/zhangls/theme/layout/HingeSafeContent.kt`
- Modify: `core/theme/src/commonTest/kotlin/me/zhangls/theme/layout/HingeGeometryTest.kt`

- [ ] **Step 1: Replace generic safe-region tests with directive-only tests**

Keep tests for:

```kotlin
assertEquals(
  calculateAppPaneScaffoldDirective(halfOpen),
  calculateAppPaneScaffoldDirective(flat),
)
assertEquals(2, calculateAppPaneScaffoldDirective(flat).excludedBounds.size)
```

Add a no-hinge assertion:

```kotlin
assertTrue(calculateAppPaneScaffoldDirective(info(emptyList())).excludedBounds.isEmpty())
```

Remove tests for `safeRegions`, `largestSafeRegion`, and `shouldUseHingeLayout`.

- [ ] **Step 2: Run the focused test and verify RED**

Run:

```bash
./gradlew :core:theme:testAndroidHostTest --tests '*HingeGeometryTest*'
```

Expected: FAIL to compile while deleted generic APIs still have production consumers, or fail the final structural guard until those APIs are removed.

- [ ] **Step 3: Simplify production geometry**

Keep only `forStableLayout()` and `calculateAppPaneScaffoldDirective()`. Delete `LocalWindowHinges`, hinge boolean extensions, `shouldUseHingeLayout`, `safeRegions`, `largestSafeRegion`, and `HingeSafeContent.kt`.

- [ ] **Step 4: Verify geometry tests and the structural guard GREEN**

Run:

```bash
./gradlew :core:theme:testAndroidHostTest
matches=$(rg -n \
  "HingeSafe|LocalWindowHinges|hasHinges|hasHorizontalHinge|safeRegions|largestSafeRegion" \
  composeApp/src core/preference/src core/theme/src feature/email/src feature/login/src feature/main/src feature/settings/src \
  --glob '*.kt' || true)
test -z "$matches" || { printf '%s\n' "$matches"; exit 1; }
```

Expected: BUILD SUCCESSFUL and no structural matches.

- [ ] **Step 5: Commit**

```bash
git add core/theme/src/commonMain/kotlin/me/zhangls/theme/layout \
  core/theme/src/commonTest/kotlin/me/zhangls/theme/layout/HingeGeometryTest.kt
git commit -m "refactor(theme): scope hinge geometry to pane directives"
```

### Task 6: Update documentation and perform full verification

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/architecture.md`
- Modify: `docs/audits/2026-09-23-foldable-hinge.md`
- Modify: `docs/superpowers/specs/2026-09-23-pane-only-hinge-avoidance-design.md`

- [ ] **Step 1: Document the final ownership rule**

State that only login supporting panes and email list-detail scenes consume hinge-aware directives; single-pane screens and overlays must not use hinge-safe wrappers or component switches. Mark the design status as implemented.

- [ ] **Step 2: Run relevant tests**

```bash
./gradlew :core:theme:testAndroidHostTest \
  :core:preference:testAndroidHostTest \
  :feature:login:testAndroidHostTest \
  :feature:main:testAndroidHostTest \
  :feature:settings:testAndroidHostTest \
  :feature:email:testAndroidHostTest
```

Expected: BUILD SUCCESSFUL; modules without tests may report NO-SOURCE.

- [ ] **Step 3: Run platform builds**

```bash
./gradlew :androidApp:assembleDebug
./gradlew :composeApp:compileKotlinIosSimulatorArm64
```

Expected: both commands BUILD SUCCESSFUL.

- [ ] **Step 4: Run static checks sequentially**

```bash
./gradlew :core:theme:detekt :core:preference:detekt :feature:login:detekt \
  :feature:main:detekt :feature:email:detekt :feature:settings:detekt
./gradlew :androidApp:lintDevFullDebug
```

Expected: both commands BUILD SUCCESSFUL.

- [ ] **Step 5: Check final source boundary and diff**

```bash
rg -n "LocalPaneScaffoldDirective" \
  composeApp/src/commonMain/kotlin/me/zhangls/entry/AppNavHost.kt \
  feature/login/src/commonMain/kotlin/me/zhangls/login/LoginScreen.kt
git diff --check
git status --short
```

Expected: exactly the intended directive consumers, no whitespace errors, and only planned files changed.

- [ ] **Step 6: Commit documentation**

```bash
git add AGENTS.md docs/architecture.md docs/audits/2026-09-23-foldable-hinge.md \
  docs/superpowers/specs/2026-09-23-pane-only-hinge-avoidance-design.md
git commit -m "docs: scope hinge avoidance to pane layouts"
```
