# Material 3 Expressive Implementation Plan

> 在当前已隔离 worktree 内顺序实施，执行方式为 inline；Gradle 检查不并发。

**Goal:** 在共享 UI 上统一 Expressive 主题、组件反馈和视觉层级，保留业务及自适应行为。

**Architecture:** core:theme 管理 MaterialExpressiveTheme、Typography、Shapes 与共享操作组件。core:preference 接收行分组边界并渲染形状，feature:settings 决定顺序及分组。邮件和登录只修改视觉组件，导航和数据边界保持现状。

**Tech Stack:** Kotlin 2.4.20、Compose 1.12.0、Material 3 1.12.0-alpha03、Robolectric / Roborazzi。

## 1. 主题契约与主题迁移

- [x] 添加 `composeApp/src/androidHostTest/kotlin/me/zhangls/entry/theme/ExpressiveThemeTest.kt`，验证配置切换保留动态色和字号、深浅色，以及操作标签与正文的字重层级。
- [x] 运行 `./gradlew :composeApp:testAndroidHostTest --tests '*ExpressiveThemeTest*'`，确认新文字层级断言在原主题失败。
- [x] 新建 `core/theme/.../Typography.kt`、`Shapes.kt`，主题入口使用 `MaterialExpressiveTheme(colorScheme = colorScheme, typography = AppTypography, shapes = AppShapes)`。标题/标签在默认 Typography 上复制字重，不改变正文尺寸。形状统一到 small 12 / medium 16 / large 20 / extraLarge 28dp。

## 2. 共享组件、登录与设置

- [x] `TooltipIconButton.kt` 显式传 `shapes = IconButtonDefaults.shapes()`，保留 tooltip 和无障碍文案。
- [x] `SimpleDialog.kt` 保留两个公开重载，主体改用 `AlertDialog`，确认使用形变 Button，取消使用 TextButton；保持显式回调关闭语义。
- [x] `LoginScreen.kt` 用 `ButtonDefaults.shapes()` 和主题文字样式替换固定圆角、14/16sp 与等宽字体；不改窗口、IME 和表单位置计算。
- [x] `PreferenceRow.kt` 增加 `isGroupStart / isGroupEnd` 参数，以主题 extraLarge 外角、extraSmall 内角和 surfaceContainerLow 渲染容器；底层 Preference 库保持 implementation。
- [x] `SettingsScreen.kt` 决定连续设置行分组与独立 Action 行，保留 LazyColumn、现有 padding 和顺序；用横向 16dp 留白及 2dp 行间隔。

## 3. 邮件组件

- [x] `EmailListItem.kt / EmailDetail.kt` 使用主题 large 圆角、surfaceContainerLow / secondaryContainer / primaryContainer，标题与正文分别采用 titleMedium / bodyMedium 或 bodyLarge。
- [x] `EmailHeader.kt` 采用 tonal 图标按钮表达收藏状态，头像翻转使用 `MaterialTheme.motionScheme.defaultSpatialSpec()`。
- [x] `EmailDetail.kt` 回复使用 `FilledTonalButton(shapes = ButtonDefaults.shapes())`。
- [x] `NewEmailSheet.kt` 保存使用形变 Button，输入框采用主题 large 圆角；保留最小行数和 IME inset。
- [x] `EmailFab.kt` 不确定发送使用 LoadingIndicator，保留阻止重复打开逻辑。

## 4. 验证与文档

- [x] 顺序执行主题测试、Android assembleDebug、iOS compileKotlinIosSimulatorArm64、自适应宿主测试并生成候选图、Android lintDevFullDebug。
- [x] 检查候选图中紧凑、宽屏、深色与大字号页面；不自动更新正式基线。
- [x] 更新 docs/architecture.md、docs/testing/adaptive-ui.md 和可行性审计状态，记录真实结果与限制。
- [x] 运行 git diff --check，检查最终变更；本次不合入 master，不覆盖用户批准的截图。

## 完成记录

最终双端编译、78 项实际执行的 UI/主题测试、Android lint 与 diff 检查通过。190 张候选图留在构建目录，正式基线未更新。独立审查未发现阻止迁移的问题。Android 弹窗字号与长文本滚动的既有边界记录在 testing/adaptive-ui.md；未进行设备动效验收。迁移实现、测试与过程文档整理为一个最终提交，主线采用 fast-forward 合入。
