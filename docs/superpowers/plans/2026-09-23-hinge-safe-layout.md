# Hinge-safe layout implementation plan

> Execute in the existing isolated worktree. Approved specification: docs/audits/2026-09-23-foldable-hinge.md. Use subagent-driven-development for the independent overlay integration, with review; run Gradle tasks serially.

**Goal:** Keep pane geometry stable across posture-only changes and avoid every reported hinge across pages and overlays.

**Architecture:** The root retains the single WindowAdaptiveInfo reading. core:theme owns pure safe-region geometry, stable directives and reusable bounded layouts/dialogs. Nav3 continues owning list/detail navigation. Layout decisions never enter feature contracts or MVI state.

**Tech Stack:** Kotlin Multiplatform, Compose, Material3 Adaptive 1.3.0-rc01, commonTest on Android host plus Android/iOS compilation.

- [x] Add regression tests for half-open/flat vertical hinges, horizontal hinges, two unsorted hinges, zero-width hinges, clipping and local origins. Observe failures before implementing. Add kotlin.test to core:theme commonTest.
- [x] Implement `safeRegions(bounds: Rect, hinges: List<HingeInfo>): List<Rect>` and deterministic largest-region selection; honor both orientations, clip to viewport, ignore out-of-window hinges, preserve zero-thickness splits. Rectangles are in window pixels. Normalize rectangles before passing to library pane scaffolds.
- [x] Implement stable pane directives using `HingePolicy.AlwaysAvoid` and geometry-derived horizontal folding facts, without modifying the raw posture source. Existing remember function delegates to the pure policy.
- [x] Implement `HingeSafeContent(modifier, horizontalOnly = false, content)` in core:theme: measure outer bounds in window coordinates, restrict children to the largest safe region, clip, and render no unsafe first frame while geometry is unavailable. Implement `HingeSafeColumn` for login's physical horizontal bands. Preserve composition identity during posture-only changes.
- [x] Implement `HingeSafeDialog(onDismissRequest, content)` using a full-window dialog and safe region, safeDrawing/IME insets, bounded width and scrolling at consumer level. Expose `LocalWindowAdaptiveInfo.current.hasHinges` for choosing fallback overlays.
- [x] Integrate horizontal safety around AppShell/NavDisplay. Keep scene directive and host navigation geometry stable. Wrap settings' whole page for vertical safety. Login uses physical horizontal bands and existing pane scaffold otherwise; remove reliance on Reflow.
- [x] Integrate safe overlays for NewEmailSheet, EmailSearchBar, SimpleDialog, PreferenceRow and SelectIconButton. Keep normal-device native controls and preserve state; foldable overlays use the shared safe dialog. A subagent owns these files exclusively.
- [x] Update tests for new navigation/login policies. Run `./gradlew :core:theme:testAndroidHostTest :feature:login:testAndroidHostTest :feature:main:testAndroidHostTest` (actual project enables host tests despite older AGENTS prose).
- [x] Run `./gradlew :androidApp:assembleDebug :composeApp:compileKotlinIosSimulatorArm64`, then focused lint if available; review diff, geometry and state-preservation behavior. Exercise available emulator for folding/rotation if device access is available. Record unavailable physical three-fold validation explicitly.
- [x] Update audit and AGENTS architecture notes to reflect final implementation and verified results. Leave changes reviewable in the worktree; no publish or merge requested.
