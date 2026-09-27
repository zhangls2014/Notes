# 自适应 UI 与架构审计

日期：2026-09-25。状态：2026-09-27 已实施文档修正、自适应宿主测试和导航姿态稳定性修复；登录页已按用户最新决定取消横向铰链避让，160 张视觉基线已获用户批准并保存。

## 基线批准

2026-09-27 用户明确要求提交当前基线，已将审阅的 160 张候选图原样保存为正式基线。下文候选图未批准的说明属于批准前的验证记录。

## 2026-09-27 实施补充

- 清理 `docs/architecture.md` 中已废弃的全局安全区域、安全弹层要求。
- `composeApp/androidHostTest` 新增真实 Feature/导航的宿主测试：16 组配置 × 登录/邮件流程，覆盖 9 组宽高、桌面、大字号、深色、竖向与横向铰链的半开/全开。测试使用内存仓库，不访问生产数据库或网络。
- 修复实际发现的导航问题：相同窗口几何下 tabletop 标志曾使底栏切换为 Rail，移动内容窗格。导航现在仅依据窗口尺寸，物理铰链仍由实际窗格布局处理；新增纯策略与实际详情位置回归断言。
- 候选截图仅生成到 `composeApp/build/adaptive-candidates/`；未批准或写入视觉基线。命令和验证边界见 [自适应 UI 回归](../testing/adaptive-ui.md)。
- **最新设计决定**：用户审阅截图后决定保留左右分栏，但不在窗格内避开横向铰链。已移除上一轮的局部高度限制、裁剪容器和专用区间测试，恢复品牌与表单使用完整可用高度。
- 保留横向铰链半开/全开时的位置稳定性和登录按钮可达性断言。横向折痕可能穿过内容是本次明确接受的布局行为，不再列为待修复项；竖向铰链的原有处理不变。

### 最新布局决定的复验

取消局部避让后，16 个登录 UI 场景、13 个登录单元测试及 iOS 编译通过；登录候选图已重新生成。

### 首轮验证结果

- Android `:androidApp:assembleDebug` 与 iOS `:composeApp:compileKotlinIosSimulatorArm64` 成功。整包构建使用仓库约定的 8GB Gradle/Kotlin 堆配置。
- `:composeApp:testAndroidHostTest`：41 项通过，其中自适应流程 32 项；`core:theme` Android 宿主 4 项、`feature:login` Android 宿主 13 项、`feature:main` iOS 模拟器 11 项通过。
- 手机 400×500dp 与 book-half 的 4 项流程重复截图严格比对通过；这验证可重复性，不代表候选图已经人工批准。
- 共生成 160 张候选图，构建目录中的 `index.html` 可浏览。源码基线目录仅保留 `.gitkeep`。
- 最新 Debug APK 已安装到 emulator-5554，打开写邮件弹层检查。Gboard 显示浮动工具栏，未取得完整软键盘覆盖场景，因此 IME 遮挡与按钮可达性仍未完成验证。未输入或保存邮件内容。
- 临时输入法设置 `show_ime_with_hard_keyboard` 已恢复为原值 0；分辨率与密度未修改，检查结果为 1080×2400 / 420，均无 Override。

下文保留 2026-09-25 的审计基线，不能用当时的“基本符合”结论替代本轮新增的视觉证据。

## 范围与方法

检查 `composeApp`、`feature/main`、`feature/login`、`feature/email`、`feature/settings`、`core/theme` 和 `core/preference` 的 Compose UI 与导航装配，并对照本仓库 `AGENTS.md`、`docs/architecture.md` 和 `adaptive` 技能。静态检索覆盖全部 Kotlin 源码；未获得设备截图，视觉结论仅限代码证据。

## 结论

核心自适应架构基本符合项目约束：窗口尺寸和姿态在组合根统一读取；导航套件按尺寸与姿态切换；邮件首页和收藏页通过 Navigation 3 `ListDetailSceneStrategy` 形成列表—详情；登录表单能在窄/矮窗口滚动并在宽窗口分栏；正文、设置行和表单有集中定义的可读宽度上限。当前不能据此认定所有形态的实际视觉效果均已验证。

## 待处理项

### 1. 缺少主要页面的跨形态截图回归（中）

`feature/main/AppShell.kt` 有四档宽度的 `@Preview`，但全仓未发现 `@PreviewTest`、截图测试任务或基准图。登录、邮件列表/详情、搜索、设置和写邮件弹层均没有手机、折叠屏、平板、桌面窗口的可复查截图。现有 `LoginPanePlanTest`、`NavigationSuitePolicyTest` 与 `HingeGeometryTest` 验证决策逻辑，不能检出文字放大、键盘弹出、实际窗格约束或控件遮挡。建议优先为登录、列表—详情、设置和搜索建立多形态视觉基线，再补弹层及大字号场景。引入截图测试时保持基准图由人工审阅，不在审计中自动更新。

### 2. 架构文档遗留旧方案（中）

`docs/architecture.md` 自适应章节第 173、175、177 行仍要求导航与内容处于“物理安全区域”、由宿主限制横向折叠安全带、弹层使用共享安全容器。这与该文档第 165 行、当前 `AGENTS.md` 的“仅实际分栏消费铰链”硬约束，以及当前 `AppShell` / 弹层实现矛盾。`docs/audits/2026-09-23-foldable-hinge.md` 已标记早期全局安全区域方案为历史方案。建议删除或改写这三个遗留条目，避免以后按过期设计修改 UI。

### 3. 大屏输入方式与尺寸的交互验证不足（低）

现有实现主要依据窗口尺寸和铰链几何；全仓未见针对鼠标/触控板悬停、键盘快捷键或焦点遍历的专门处理与测试。现有标准 Compose 控件提供基础输入行为，因此不能仅凭缺少 `MediaQuery` 判为功能错误。建议在桌面窗口实测搜索、列表多选、弹层和登录输入的焦点顺序与指针操作后，再决定是否需要设备能力分支。

### 4. 非首页顶栏的滚动行为可评估（低）

首页搜索栏通过 `enterAlwaysSearchBarScrollBehavior` 与 `nestedScroll` 随列表滚动；设置和邮件详情使用固定顶栏。技能建议滚动时收起顶栏，但固定标题不必然是不适配。建议在小高度窗口和大字号下验证实际可用空间，再决定是否为这些页面增加滚动行为。

## 符合项与不适用项

- `App.kt` 使用 `rememberWindowAdaptiveInfo()` 和 `rememberPaneScaffoldDirective()` 各一次，并通过 CompositionLocal 下发；Feature 无独立窗口读取或第二套断点。
- `AppShell.kt` 使用 `NavigationSuiteScaffold`，紧凑窗口底栏、宽窗口 Rail；导航外壳位于 `NavDisplay` 外，详情打开时仍可达。
- `AppNavHost.kt` 配置 `ListDetailSceneStrategy`，列表和详情的 metadata 及空态完整；详情返回按钮依据实际场景窗格状态显示。
- 登录页的 `SupportingPaneScaffold` 是一个页面内的品牌与表单排布，不是两个导航目的地之间的 supporting pane。技能中“多窗格用 SceneStrategy”的规则针对相关屏幕导航，不应机械套在这里；当前实现还符合仓库明确约束。
- 邮件列表、搜索结果、邮件线程和设置项采用 `LazyColumn` 有顺序与语义依据。技能建议的自适应网格不适合把邮件和设置行拆成多列；列表—详情和可读宽度上限已承担宽屏布局职责。
- 搜索在紧凑宽度使用全屏形态，在更宽窗口使用 docked 形态。登录使用表单宽度上限、纵向滚动和 IME 留白；设置与邮件正文使用集中定义的内容宽度。
- Compose `Grid` / `FlexBox` 尚属实验性建议，本次审计没有发现必须引入它们的固定网格内容；无需为符合技能而升级依赖或改写列表。

## 验证边界

本次未修改产品代码，也未更新截图基线。以下命令执行成功：

```bash
./gradlew :feature:login:iosSimulatorArm64Test :feature:main:iosSimulatorArm64Test :core:theme:iosSimulatorArm64Test :composeApp:compileKotlinIosSimulatorArm64
./gradlew :androidApp:assembleDebug --quiet
```

设备侧验证未执行：当前环境 `adb` 无法启动服务（smartsocket 监听被系统拒绝），故没有更改模拟器显示设置。以上测试不代替真机/模拟器的跨形态视觉检查。

## 当前验证限制

2026-09-27 提交基线前，全量 verify 未通过：部分邮件列表/收藏场景的实际截图出现空白列表，而批准的基线含完整列表。登录场景通过。调整 Compose 动画时长与逐帧等待均未消除问题，实验性调整已撤回；根因仍需进一步定位。160 张正式基线保留用户审阅的原图，没有以失败截图覆盖，也没有放宽比较阈值。现阶段不能把整套截图任务作为稳定的合入门禁。
