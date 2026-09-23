# Login Form Centering Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the login page support only horizontal side-by-side panes, while centering the form body when height permits and preserving its dimensions and spacing with scrolling when constrained.

**Architecture:** Keep `SupportingPaneScaffold`, but calculate its value only from horizontal partition capacity with `AdaptStrategy.Hide`; horizontal hinges never trigger vertical reflow. A small measured layout keeps the settings row at the top and centers the form body when it fits; the existing scroll container handles overflow.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Material 3 Adaptive, `commonTest`.

---

### Task 1: Specify form placement

**Files:** `feature/login/src/commonTest/kotlin/me/zhangls/login/LoginFormLayoutTest.kt`, `feature/login/src/commonMain/kotlin/me/zhangls/login/LoginFormLayout.kt`

- [x] Add tests for a body centered in a tall viewport and placed below settings in a short viewport.
- [x] Implement a pure placement calculation that expands scroll content only when required.

### Task 2: Apply the placement without changing pane behavior

**Files:** `feature/login/src/commonMain/kotlin/me/zhangls/login/LoginScreen.kt`, `feature/login/src/commonTest/kotlin/me/zhangls/login/LoginPanePlanTest.kt`

- [x] Measure and position the unchanged settings row and form body inside the existing scroll container.
- [x] Write failing tests proving a horizontal hinge preserves width-based side-by-side or single-pane behavior.
- [x] Remove the tabletop `Reflow` branch and make the pane calculation use horizontal capacity only.

### Task 3: Record the horizontal-only pane rule

**Files:** `docs/architecture.md`, `docs/superpowers/specs/2026-09-24-login-form-centering-design.md`

- [x] Document that login supports left/right panes only and that horizontal hinges do not trigger top/bottom reflow.

### Task 4: Verify

**Files:** `docs/architecture.md`

- [x] Run the login tests, Android build, Android lint, and `git diff --check`.
