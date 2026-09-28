# 搜索框首次测量宽度实施计划

**Goal:** 搜索框沿用组件自然测量宽度，长查询不改变锚点或停靠展开尺寸，不引入宽度常量。

**Architecture:** 在 EmailSearchBar 的锚点输入槽内测量一次空查询基准，再在同一测量周期约束真实输入框。宽度仅保存在当前组合实例，密度、字号、布局方向或提示文本变化时失效；父级变窄时临时收缩，恢复空间后恢复基准。展开输入框填满 Material 提供的空间，保留全屏和停靠原有布局及状态恢复。

**Tech Stack:** Kotlin Multiplatform、Compose Material 3、SubcomposeLayout、Robolectric Compose UI Test。

- [x] 在 `composeApp/src/androidHostTest/kotlin/me/zhangls/entry/adaptive/AdaptiveUiTest.kt` 增加长查询、长查询恢复与窗口收缩/恢复测试；断言初始宽度而非硬编码库默认宽度。
- [x] 执行 `./gradlew :composeApp:testAndroidHostTest --tests '*AdaptiveUiTest*Search*'`，确认新增稳定性断言能检出原问题。
- [x] 在 `feature/email/src/commonMain/kotlin/me/zhangls/email/search/` 增加首次测量容器；将 EmailSearchBar 的锚点槽与展开槽分开。基准禁用输入、焦点与业务交互且不放置；真实文本、焦点、搜索状态不被测量修改。
- [x] 重跑搜索测试，再运行完整 AdaptiveUiTest 与 `:composeApp:compileKotlinIosSimulatorArm64`；Gradle 串行运行。
- [x] 更新 `docs/testing/adaptive-ui.md` 记录测量契约和验证结果，检查差异并保留用户已有的 architecture.md 暂存与未暂存修改。
