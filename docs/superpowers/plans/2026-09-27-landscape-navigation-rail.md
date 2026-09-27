# 横屏导航侧栏实施计划

用户已批准按窗口尺寸恢复横屏侧栏体验。

**Goal:** 宽度至少 600dp、高度不足 480dp 的窗口使用折叠侧栏。
**Architecture:** 保留根窗口事实与 AppShell 的导航策略入口；不改变导航状态、窗格或铰链策略。低高度覆盖优先于超大宽度展开规则。
**Tech Stack:** Kotlin Multiplatform、Compose Material 3、Navigation 3、Robolectric。

- [x] 更新 feature/main 的 NavigationSuitePolicyTest：800×400、600×400、1600×479 使用折叠侧栏；599×400 保持底栏；1600×480 保持展开侧栏。先运行 `./gradlew :feature:main:testAndroidHostTest` 确认新增用例失败。
- [x] 在 AppShell.navigationSuiteType 中优先判断宽度达到 WIDTH_DP_MEDIUM_LOWER_BOUND 且高度未达到 HEIGHT_DP_MEDIUM_LOWER_BOUND，返回 WideNavigationRailCollapsed；其他情况保留现有逻辑。
- [x] 更新 AdaptiveUiTest 安全区几何断言，增加低高度大字体导航可见与切换验证，生成候选截图，不覆盖批准基线。
- [x] 更新 docs/architecture.md 与 docs/testing/adaptive-ui.md；保留用户现有暂存内容。
- [x] 串行运行策略测试、自适应宿主测试、Android 构建及 iOS 编译，审阅截图和最终 diff。

## 验证结果

- 策略测试先失败 3 项（期望折叠侧栏、实际底栏），实现后 13 项全部通过。
- 自适应 UI 测试 47 项通过，157 项按场景跳过；生成候选截图，批准基线保持不变。
- 900×400dp 的左右刘海安全区均通过；侧栏占 96dp，列表仍为 360dp。
- Android assembleDebug 与 iOS compileKotlinIosSimulatorArm64 通过。
- 已查看普通横屏邮件详情与双倍字体导航截图。部分首页候选图仍存在既有空白列表问题，详见 docs/testing/adaptive-ui.md。
- 保留 docs/architecture.md 的用户暂存改动；本需求整理为一个提交，不包含无关的导航动画文档改动。
- 最终标签完整可见性回归：3 项通过、14 项按配置跳过；Android lintDevFullDebug 通过。
