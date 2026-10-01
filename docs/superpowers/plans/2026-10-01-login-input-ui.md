# Login Input UI Implementation Plan

**Goal:** Improve field identity, spacing, validation and action feedback using rounded outlined fields.

**Architecture:** Change feature/login/LoginScreen.kt and localized resources; reuse OutlinedTextField/IconButton semantics and keep form layout and MVI.

**Tech Stack:** Compose Multiplatform Material 3 Expressive, Robolectric UI Test, Roborazzi.

- [x] Add loginInputActionsAreAccessible to AdaptiveUiTest: clear reviewer account, toggle Test1234 visibility, assert Account/Password labels remain. Run the test before implementation; expect missing Clear account.
- [x] Replace TextField with OutlinedTextField, floating labels and outlineVariant; use labeled IconButton actions; render aligned error supporting text only when needed; remove duplicate account top padding.
- [x] Run AdaptiveUiTest, verifying field actions, Tab order, IME and resizing. Generate candidates and inspect focus/error/dark/large-font states.
- [x] Run :androidApp:installDevFullDebug using the existing emulator debug certificate and :composeApp:compileKotlinIosSimulatorArm64. Keep emulator display settings unchanged.
- [x] Update testing docs, request independent review, check diff, and collect all work into a single final commit.

## Verification

- The new action test failed because Clear account had no accessible node; the outlined field and labeled IconButton implementation passed.
- Full AdaptiveUiTest passed: 527 parameterized cases, 79 executed, 448 skipped as inapplicable, zero failures/errors. Covers IME, Tab, resizing, clear/reveal, error text and disabled login.
- Android DevFullDebug installed successfully on emulator-5554 with the matching standard debug certificate, preserving data. iOS simulator Kotlin compilation passed. Production signing configuration is unchanged.
- Compact, dark, large-font, focused/revealed and invalid-password candidates inspected; official baselines unchanged.
- Independent review reported no blockers. Its error-state large-font reachability suggestion is included in the action test.
- Floating navigation is absent from this branch; its canceled trial remains solely on codex/floating-bottom-navigation. Emulator size/density show Physical values only, no overrides.
