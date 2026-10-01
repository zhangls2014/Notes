# Material 3 Expressive 整体风格迁移可行性

- 日期：2026-10-01
- 状态：可行性调查与迁移实现完成，双端编译和宿主检查通过；设备动效验收与正式视觉基线批准尚未完成。
- 范围：Android / iOS 共享 Compose UI、主题、组件与验收方式。
- 方法：检查版本目录、项目源码、实际版本的本地缓存 sources.jar、现有截图基线及官方发布说明。缓存源码证明该版本提供相关 API，不等同于本次构建已解析或运行成功。

## 结论

可以在现有依赖和模块架构内迁移，暂未发现必须升级 Compose 或重构导航、数据层的前置条件。项目已经部分使用 Expressive 组件；主要工作是统一主题、字体与形状策略，以及逐页调整组件与视觉层级。整体工作量中等，风险主要在 Alpha API、页面尺寸变化和双端交互回归。

如果要求仅使用稳定版本且不接受实验性组件，需重新评估目标组件范围；当前项目 Material 3 本身已经使用 Alpha 版本。

## 依赖和 API 证据

`gradle/kmp.versions.toml` 当前配置：

| 项目 | 版本 |
| --- | --- |
| Compose Multiplatform | 1.12.0 |
| JetBrains Material 3 | 1.12.0-alpha03 |
| JetBrains Material 3 Adaptive | 1.3.0-rc01 |
| Kotlin | 2.4.20 |
| Compose Preference | 2.2.0 |

JetBrains 的 [1.12.0 发布说明](https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.12.0) 将该 Material 3 版本对应到 AndroidX Material 3 1.5.0-alpha22。Compose 和 Material 3 分别维护版本，不能因为 Compose 是稳定版便把 Material 3 的 Alpha 能力视为稳定。

检查 `material3:1.12.0-alpha03` 缓存中的共享源码，确认存在：

- `MaterialExpressiveTheme`、`expressiveLightColorScheme()` 和 `MotionScheme.expressive()`。
- 带 `ButtonShapes` 参数的按钮重载，支持按压形状变化。
- Expressive 加载指示器、浮动工具栏及形状能力。
- 强调文字样式相关能力；但主题入口仍以 `Typography()` 为默认字体，并带有替换为 Expressive 默认字体的 TODO。

`MaterialTheme.kt` 中 `MaterialExpressiveTheme` 第 259–284 行明确使用默认 Expressive 动效、`Shapes()` 和普通 `Typography()`；它位于 `commonMain`。本地缓存也包含 iOS ARM64、iOS 模拟器 ARM64 与 Android 对应版本的源码包。因此共享 UI 实现具备基础；具体双端行为仍需迁移后编译、运行验收。

注意：[Android 当前 API 文档](https://developer.android.com/reference/kotlin/androidx/compose/material3/MaterialExpressiveTheme.composable) 展示的版本可能晚于项目依赖。实施时以项目依赖源码为准，不能直接复制最新文档中的全部组件或参数。

## 当前基础和迁移范围

| 范围 | 当前实现 | 迁移建议 / 工作量 |
| --- | --- | --- |
| 全局主题 | `core:theme/Theme.kt` 集中提供 `MaterialTheme`，保留深浅色、动态色与字号配置 | 改为 `MaterialExpressiveTheme`，明确字体、形状和动效策略；小 |
| 导航外壳 | `AppShell.kt` 已使用 ShortNavigationBar / WideNavigationRail 形态 | 复用现有策略，审阅主题改变后的视觉与占位；小 |
| 邮件浮动工具栏 | `EmailActionBar.kt` 已用 `HorizontalFloatingToolbar` | 统一色彩、形状与交互；小 |
| 加载反馈 | `core:theme` 已有 `ContainedLoadingIndicator`，邮件 FAB 仍有圆形进度指示器 | 按具体语义区分不确定加载和进度反馈；小 |
| 登录表单 | 普通 Button / TextField，品牌容器有固定 24dp 圆角 | 调整主次动作、字体层级、主题形状引用和按钮形变；中 |
| 邮件列表、详情、搜索、写邮件弹层 | 大量复用 Material 3，但存在页面特定布局和显式样式 | 逐页统一容器层级、选中态、按钮与间距，保留信息密度；中 |
| 设置下拉菜单 | `SelectIconButton.kt` 已用 DropdownMenuPopup / DropdownMenuGroup / MenuDefaults | 复用并检查双端锚点、焦点与返回行为；小 |
| 设置列表 | `PreferenceRow.kt` 包装 Compose Preference 2.2.0 | 能继承主题色和部分字体，但分组容器、圆角、密度不会自动变为 Expressive；中 |
| 自定义动画 | `EmailHeader.kt` 箭头使用 300ms tween，列表使用 animateItem | 按动画语义接入主题 MotionScheme，逐项评估；中 |

设置页现有截图显示独立平铺行。第三方 Preference 缓存源码读取 `MaterialTheme`，同时有自身 padding 等布局策略，因此不能假设更换根主题会自动形成分组卡片样式。如需重做设置行，优先在 `core:preference` 包装层实现；保持元数据、渲染契约、当前值与 Feature Intent 的现有边界。

## 只换主题的实际效果和限制

主题入口切换可以集中提供 Expressive 动效与主题参数，并让消费这些参数的控件继承默认值。现有 `MaterialTheme.colorScheme` 等读取方式可继续使用，无需在每个 Feature 替换。

但以下工作仍需显式完成：

1. 保留当前 `colorScheme` 会保留当前品牌配色；如需 Expressive 默认浅色或新的品牌色策略，需要主动修改颜色定义。动态色不要求关闭，深色模式也不应直接采用默认浅色。
2. 当前版本不会自动提供完整 Expressive 字体方案，需要在 `core:theme` 定义字体层级，并验证中文、英文和大字号表现。
3. 现有普通 `Button(onClick = ...)` 使用单个 `Shape`，不会因为更换主题自动变为 `ButtonShapes` 的按压形变重载。关键操作需要显式采用新重载及相应尺寸、内边距。
4. 自定义固定圆角、间距、动画以及第三方控件布局，需要逐项处理。
5. 系统 Toast、系统键盘和其他原生系统 UI 不受共享 Compose 主题控制。

官方的 [Androidify 实践](https://android-developers.googleblog.com/2025/05/androidify-building-delightful-ui-with-compose.html) 可作为主题与动效使用参考；具体 API 仍以项目版本为准。

## 架构约束

- 主题与通用视觉能力优先放在 `core:theme`，设置渲染放在 `core:preference`，Feature 保留页面装配。
- 保留组合根窗口事实、统一 ContentWidth、现有 Navigation 3 SceneStrategy 和 HingePolicy.AlwaysAvoid。
- 登录保留左右分栏与横向铰链处理规则。单栏页面、导航、搜索和浮层不得为视觉迁移新增铰链避让包装。
- 不迁移 MVI、Repository、Room 或 Koin；本次目标没有要求这些层变化。
- 实验性 API 按具体使用位置 opt-in，避免无差别向所有源集扩散。
- 如后续发现需要调整依赖，只在版本目录修改，并同时验证 Android 与 iOS。

## 建议实施顺序

1. 明确视觉方向：保留品牌色和现有信息密度，统一文字层级、容器和主次操作；Expressive 并不要求把所有内容都放大。
2. 在 `core:theme` 建立主题入口、字体、形状和动效方案，保留动态色、深浅色与字号设置。
3. 优先改登录页和设置页，生成紧凑 / 宽屏、深浅色与大字号候选图，形成可审阅的风格样本。
4. 推广到邮件列表、收藏、详情、搜索、写邮件弹层及共享控件。
5. 完成双端验证、文档更新和候选截图审阅后更新已批准基线。合入 master 时按仓库要求整理为一个最终提交并 fast-forward。

## 验收和已有风险

现有 `docs/testing/adaptive-ui.md` 记录了完整的多尺寸、折叠、大字号和深色场景，也记录了部分邮件截图空白、正式基线与最新低高度导航策略不同的问题。后续行为测试通过的记录，不等同于全套视觉 verify 已恢复稳定。迁移时需先区分既有问题与新增差异，不能自动覆盖基线或放宽比较阈值来获得通过。

迁移后建议顺序执行，禁止同目录并发 Gradle 构建：

```bash
./gradlew :androidApp:assembleDebug
./gradlew :composeApp:compileKotlinIosSimulatorArm64
./gradlew :composeApp:testAndroidHostTest --tests '*AdaptiveUiTest*'
./gradlew :composeApp:testAndroidHostTest --tests '*AdaptiveUiTest*' \
  -Proborazzi.test.record=true -Pnotes.screenshots.candidates=true
./gradlew :androidApp:lintDevFullDebug
```

候选图必须人工审阅。宿主测试默认关闭动画，不能验收 Expressive 弹簧动效、按压形变和真实键盘体验；Android 与 iOS 运行时需补测焦点、滚动、搜索展开/收起、弹层、系统返回/侧滑、大字号和触控可达性。Android 12 以下与 iOS 的内置颜色回退路径也需覆盖。

可行性调查阶段未运行 Gradle、未启动模拟器，也未修改应用实现；当时结论仅限源码与依赖层面。用户随后授权迁移，实施结果如下。

## 迁移实施结果

已统一根主题、强调字体、形状与动效，迁移登录、设置分组、邮件卡片/收藏按钮、回复/保存按钮、加载反馈和共享确认弹窗。保留品牌配色、动态色、深浅色、普通页面字号设置，以及导航、数据、分栏、窗口和 IME 边界；依赖版本没有变化。

Android 整包、iOS 模拟器 Kotlin 编译、Android lint 均通过。宿主检查共实际执行 78 项，0 失败；另有 434 项按场景条件跳过。生成 190 张候选图并审阅代表页面，未覆盖正式基线。独立代码审查未发现阻止迁移的问题。

限制：未在真实 Android/iOS 设备运行验收动效；既有全套正式截图 verify 不稳定，未据此声称视觉基线通过。Android 独立弹窗窗口可能覆盖父主题的应用内字号配置，属于原有行为，本轮没有修复；长对话框正文仍无滚动，当前生产调用为短确认文案。详细记录见 [自适应 UI 回归](../testing/adaptive-ui.md)。
