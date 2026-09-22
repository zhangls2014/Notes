# 多形态屏幕适配审查与目标架构（Notes / KMP）

> 审查日期：2026-09-21。范围：全仓（`composeApp` / `core` / `feature` / `androidApp` / `iosApp` / 构建脚本）。
> 结论类型：**只做审查与设计，未改业务代码**。所有库行为断言均对照本地 Gradle 缓存中的源码核实，
> 出处写在每条的「证据」里。

---

## 0. 摘要

应用当前**没有**"窗口形态"这一概念的统一来源。它被四处各自读取、三次各自推导，并且最上游那个
读取点用的是**已废弃且功能受限的 API**，导致最宽的档位（Large / ExtraLarge）在本应用里根本不存在。

按严重度：

| # | 问题 | 性质 |
|---|---|---|
| **D1** | `AppShell` 用已废弃的 `currentWindowAdaptiveInfo()`（V1 档位），**1200dp 分支是死代码**，全应用永远拿不到 Large / XL 尺寸类 | 缺陷（可静态证明） |
| **D2** | 用 `minWidthDp > minHeightDp` 判断"横屏" —— 比较的是**分档下限**而非窗口尺寸 | 缺陷（语义错用） |
| **D3** | 列表-详情 directive 的来源**两端不同**（iOS 手算 V1、Android 走 navigator 内部的 V2）→ 同尺寸两端布局不同 | 缺陷（平台分叉） |
| **D4** | 手写 `NavigationSuiteType` 映射，背离库自带策略；丢折叠姿态、`NavigationDrawer` 归错类 | 结构 |
| **D5** | 搜索栏形态由 `NavigationPlacement` 反推窗口宽度 —— 老的 `isBottomNavigationBar` 三用途之一原样保留 | 结构 |
| **D6** | 旋转后搜索栏状态失同步 → 手写同步补丁（`initial` 标志 + `snapTo`） | 结构 |
| **D7** | 收藏页在大屏**不分栏**，首页分栏 —— 同一个用户动作两种表现 | 结构 |
| **D8** | 无内容宽度上限（设置页 / 详情正文）→ 1200dp+ 行宽不可读 | 缺陷（体验） |
| **D9** | insets 四套机制并存互不知情；`toContentPadding` 硬编码 LTR | 结构（含 RTL 缺陷） |
| **D10** | 全仓没有一处 IME inset 处理 → 键盘遮挡输入框 | 缺陷 |
| **D11** | 无 `configChanges`（本身可接受），但状态补偿散落各处 | 观察 |
| **D12** | 没有可覆盖 / 可测试的窗口信息入口 | 结构 |
| **D13** | `WindowSizeClass` 从**未声明**的传递依赖 `androidx.window:window-core` 取用 | 构建卫生 |

---

## 1. 现状：窗口信息是怎么流动的

```
                       ┌─ LocalWindowInfo.containerSize × LocalDensity
                       │
  ① AppShell.kt:70 ────┴─ currentWindowAdaptiveInfo()   ← 废弃 V1，档位只有 {0,600,840}
       │                    │
       │                    ├─ isWidthAtLeastBreakpoint(1200)  ← minWidthDp ≤ 840，恒 false
       │                    ├─ isWidthAtLeastBreakpoint(840)   ← 实为「宽度 ≥ 840」
       │                    ├─ minWidthDp > minHeightDp        ← 比较两个分档下限
       │                    └─ NavigationSuiteScaffold(layoutType = 手写结果)
       │                              │
       │                              └─ 压缩为 NavigationPlacement {Bottom | Side}
       │                                     │
       │                                     ├─ PaddingValues.toContentPadding()  ← 手写归零一侧
       │                                     ├─ EmailSearchBar:82  形态 = ?        ← 反推窗口宽度
       │                                     └─ EmailList / EmailDetail / Settings 内边距
       │
  ② PaneScaffold.ios.kt:19 ── currentWindowAdaptiveInfo()  ← 同一废弃 V1，手算 directive
  ③ HomeScreen.kt:35 ──────── rememberListDetailPaneScaffoldNavigator()
                               └─ 库内部默认值 = calculatePaneScaffoldDirective(currentWindowAdaptiveInfoV2())  ← V2
```

同一帧内三次读取、两条不同的档位口径（V1 / V2）、一次压缩（3 值 → 2 值）、一次反向推断（方位 → 宽度）。
问题不是某一处写错，而是**没有一个"窗口形态"的归属**。

---

## 2. 缺陷明细

### D1 · 用了废弃的 V1 API，最宽的两档在本应用里不存在（可静态证明）

**现状**：`feature/main/src/commonMain/kotlin/me/zhangls/main/AppShell.kt:70`

```kotlin
val adaptiveInfo = currentWindowAdaptiveInfo()
val windowSizeClass = adaptiveInfo.windowSizeClass
val customLayoutType = remember(windowSizeClass) {
  with(windowSizeClass) {
    if (isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_LARGE_LOWER_BOUND)) {   // 1200
      NavigationSuiteType.WideNavigationRailExpanded
    } else if (isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)) {  // 840
      NavigationSuiteType.WideNavigationRailCollapsed
    } ...
```

**证据链**（库源码，本地缓存）：

1. `androidx.compose.material3.adaptive`（jetbrains `1.3.0-rc01`）`commonMain/WindowAdaptiveInfo.kt:53-74`：
   `currentWindowAdaptiveInfo(supportLargeAndXLargeWidth: Boolean = false)` 标了
   `@Deprecated("Please use V2 version of this function to support L and XL width size classes.")`；
   兄弟函数 `currentWindowAdaptiveInfoV2()` = `currentWindowAdaptiveInfo(supportLargeAndXLargeWidth = true)`。
2. 同 jar `commonMain/WindowSizeClassHelper.kt:90 / 96 / 162-170`：
   `DpWidthSizeClasses.Default = setOf(0.dp, 600.dp, 840.dp)`；`DefaultV2 = Default + {1200.dp, 1600.dp}`。
   `computeFromDpSize(windowSize) { ... supportedWidthSizeClasses.filter { windowSize.width >= it }.maxOf { it.value } }`
   —— 也就是说 `WindowSizeClass.minWidthDp` 取的是**不超过实际宽度的最大档位下限**。
3. `androidx.window:window-core:1.5.0` `WindowSizeClass.kt:98-100`：
   `isWidthAtLeastBreakpoint(bp) = minWidthDp >= bp`。

**推论**：V1 下 `minWidthDp ∈ {0, 600, 840}`，故 `isWidthAtLeastBreakpoint(1200)` 恒为 `false`。
—— `WideNavigationRailExpanded` **永远不可达**。同时 `windowSizeClass` 本身也永远表达不出 L / XL，
所有下游消费（包括 D3 的 directive）都被压在"Expanded"这一档里。

**影响**：1600dp 级的桌面/大屏窗口上，导航套件仍是折叠的 Rail；`PaneScaffoldDirective` 进不了
`else` 分支（`DefaultPreferredWidthXL = 412.dp` 永不生效，见 D3）。

**修法**：`currentWindowAdaptiveInfoV2()`。若还想保留"≥1200dp 用展开 Rail"这个比库默认更激进的策略，
在 V2 之上自定义即可（V2 下 `minWidthDp` 可以是 1200 / 1600，分支恢复可达）。

---

### D2 · `minWidthDp > minHeightDp` 不是"横屏"

**现状**：`AppShell.kt:80`

```kotlin
} else if (isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)) {
  if (minWidthDp > minHeightDp) NavigationSuiteType.WideNavigationRailCollapsed
  else NavigationSuiteType.ShortNavigationBarMedium
}
```

`minWidthDp` / `minHeightDp` 是**分档下限**（见 D1 证据 2），不是窗口宽高。V1 下这两个值被量化到
`{0,600,840} × {0,480,900}`，于是这个"横竖判断"实际比较的是"600 与 480/900"。

**反例**（都能落到实际窗口上）：

| 实际窗口 | minWidthDp | minHeightDp | 本应用结果 | 库推荐 `navigationSuiteType()` |
|---|---|---|---|---|
| 620 × 940 dp（小尺寸竖屏平板） | 600 | 900 | `ShortNavigationBarMedium`（底栏） | `WideNavigationRailCollapsed` |
| 800 × 400 dp（矮窗口 / 分屏） | 600 | 0 | `WideNavigationRailCollapsed`（Rail） | `ShortNavigationBarMedium`（底栏） |
| 1600 × 1200 dp | 840 | 900 | `WideNavigationRailCollapsed` | `WideNavigationRailCollapsed` |

**修法**：不要从尺寸类反推朝向。库自带策略已经按
「宽 Compact → 底栏；tabletop 或高 Compact → 中号底栏；否则 Rail」处理，见 D4。

---

### D3 · 列表-详情 directive：两端来源不同 → 同尺寸两端布局不同

**现状**

- `feature/email/src/iosMain/.../home/PaneScaffold.ios.kt:19`
  `val directive = calculatePaneScaffoldDirective(currentWindowAdaptiveInfo())` ← **V1**
- `feature/email/src/androidMain/.../home/PaneScaffold.android.kt:17` `NavigableListDetailPaneScaffold(navigator = scaffoldNavigator, …)`，
  而 navigator 来自 `HomeScreen.kt:35` 的 `rememberListDetailPaneScaffoldNavigator<Long>()`，其**默认** directive 是
  `calculatePaneScaffoldDirective(currentWindowAdaptiveInfoV2())` ← **V2**

**证据**：`adaptive-navigation 1.3.0-rc01` `commonMain/…/ThreePaneScaffoldNavigator.kt:202-204`。

**后果**（`adaptive-layout 1.3.0-rc01` `PaneScaffoldDirective.kt:52-84`）：

```kotlin
when (windowAdaptiveInfo.windowSizeClass.minWidth) {
  Compact  -> maxHorizontalPartitions = 1; defaultPanePreferredWidth = 360.dp
  Medium   -> maxHorizontalPartitions = 1; defaultPanePreferredWidth = 360.dp
  Expanded -> maxHorizontalPartitions = 2; defaultPanePreferredWidth = 360.dp   // 24.dp 分栏间隙
  else     -> maxHorizontalPartitions = 3; defaultPanePreferredWidth = 412.dp   // ≤ 只有 V2 才到得了
}
```

- iOS（V1）永远进不了 `else` → **最多 2 栏、栏宽 360dp**；
- Android（V2）在 ≥1200dp 窗口进 `else` → **3 栏、栏宽 412dp**。

同时 `windowPosture`（折叠/帐篷姿态、铰链避让）在 iOS 侧恒为默认值
（`adaptive/nonAndroidMain/WindowAdaptiveInfo.nonAndroid.kt:31-35` `calculatePosture() = DefaultPosture`），
Android 侧由平台提供 —— 也就是说**同一个 expect/actual 想表达的"平台差异"，实质是"版本差异 + 姿态可得性差异"**。

**修法**：两端都不要手算 directive；让 Nav3 的 `ListDetailSceneStrategy`（内部即用 V2）接管，
expect/actual 整组删除。见 §3。

---

### D4 · 手写 `NavigationSuiteType` 映射，背离库自带策略

库在 `material3-adaptive-navigation-suite 1.12.0-alpha03` `NavigationSuiteScaffold.kt:1223-1236` 提供了：

```kotlin
fun navigationSuiteType(adaptiveInfo: WindowAdaptiveInfo): NavigationSuiteType =
  with(adaptiveInfo) {
    if (windowSizeClass.minWidth == Compact) ShortNavigationBarCompact
    else if (windowPosture.isTabletop || windowSizeClass.minHeight == Compact) ShortNavigationBarMedium
    else WideNavigationRailCollapsed
  }
```

本应用的手写版与之的差异：

1. **丢姿态**：不看 `windowPosture.isTabletop`（折叠设备半开时库会给中号底栏，本应用会给 Rail）。
2. **分档口径不同**：手写版用 6 档（含 `WideNavigationRailExpanded`），库版 3 档；但手写版的 6 档因 D1 实际只剩 4 档生效。
3. **`toNavigationPlacement()` 归类不全**（`AppShell.kt:119-130`）：`bottomNavigationTypes` 只列了
   `ShortNavigationBarCompact / ShortNavigationBarMedium / NavigationBar`，`else → Side`。
   于是 `NavigationDrawer`（模态抽屉，**不占位**）也被归成 `Side`，会让内容错误地丢掉一侧内边距。

**修法**：默认走 `NavigationSuiteScaffoldDefaults.navigationSuiteType(adaptiveInfo)`；
"≥1200dp 展开 Rail"如果要保留，作为显式的产品决策写在**一个**地方并配注释，不要伪装成通用逻辑。

---

### D5 · 搜索栏形态由导航方位反推窗口宽度

**现状**：`feature/email/src/commonMain/.../search/EmailSearchBar.kt:81-82`

```kotlin
// 紧凑窗口（导航套件在底部）用全屏搜索栏，宽窗口用 docked
val useFullScreenSearchBar = LocalNavigationPlacement.current == NavigationPlacement.Bottom
```

注释已经点明了错位：注释说的是"紧凑窗口"，代码读的是"导航套件在底部"。这两件事**现在相关**（因为
`NavigationPlacement` 就是从窗口尺寸类派生的），但**不是同一个问题**：
`ExpandedDockedSearchBar` vs `ExpandedFullScreenSearchBar` 问的是"有没有横向空间摊开一个 docked 结果面板"，
属于窗口尺寸类的直接消费方，不该绕道宿主布局。

这正是历史上 `isBottomNavigationBar: Boolean` 被当成三种用途里的第三种（"搜索栏形态，其实是屏幕尺寸"）
—— 参数换成了 `NavigationPlacement`，**语义错位原地保留**。

**修法**：读窗口尺寸类（`LocalWindowSizeClass` / `LocalWindowAdaptiveInfo`）：
`useFullScreenSearchBar = windowSizeClass.minWidth == Compact`。

---

### D6 · 旋转导致搜索栏状态失同步 → 手写同步补丁

**现状**：`EmailSearchBar.kt:153-182`

```kotlin
/**
 * 为了适配不同屏幕，采用了 ExpandedFullScreenSearchBar 和 ExpandedDockedSearchBar 来渲染搜索框。
 * 在屏幕旋转时，它们的状态时不同步的，所以这里需要手动同步一下状态
 */
LaunchedEffect(searchBarState.currentValue) {
  if (initial) { ... snapTo(1F)/snapTo(0F) ...; initial = false; return@LaunchedEffect }
  ...
}
```

根因是 D5：**旋转会切换渲染用的可组合项**，两个可组合项各有自己的内部状态；`rememberTextFieldState` /
`rememberSearchBarState` 又只是 `remember`（非 saveable），于是必须靠一段手写补丁在首帧把值对齐。

补丁本身还带着脆弱性：`initial` 是普通 `remember`，Activity 重建后又从头执行一次，与用户真实的
"旋转一次"无法区分。

**修法**：形态改由窗口尺寸类决定后，两个分支的**状态提升到同一处**（`SearchViewModel` 已经是这个位置），
`SearchBarValue` 与文本都进 VM；补丁整段删除。这也顺带修掉"旋转后搜索结果面板形态突变但不重新布局"的观感问题。

---

### D7 · 收藏页在大屏不分栏（同一个动作两种表现）

- 首页：点邮件 → `HomeScreen.kt:61` `scaffoldNavigator.navigateTo(Detail, contentKey)` → **同屏分栏**，且不经 Nav3 返回栈。
- 收藏页：`FavoritesScreen` → `MainNavEntry.kt:36` 的 `navigateToDetail` → `AppNavHost.kt:122` `Nav3.Navigate(EmailDetailDestination)` → **整页替换**，列表消失。

也就是说：同一台平板上，从"首页"点开邮件得到列表+详情；从"收藏"点开邮件得到全屏详情、还要按返回。
两套机制还导致上一轮已经记录过的后果：**首页的列表-详情不在 Nav3 返回栈里**（返回键行为与收藏页不同、
预测性返回不参与）。

**修法**：统一到 Nav3 + `ListDetailSceneStrategy`（见 §3）。首页那套 `rememberListDetailPaneScaffoldNavigator`
（一个 entry 内的"私有的、不在返回栈里的"导航器）整个消失。

---

### D8 · 没有任何内容宽度约束

全仓只有一处宽度约束 —— `feature/login/.../LoginScreen.kt:69,125` 的 `FormMaxWidth = 480.dp`
（注释还准确引用了"Android 17 起大屏不再允许锁定方向/尺寸"）。其余：

| 位置 | 问题 |
|---|---|
| `feature/settings/.../SettingsScreen.kt:60-71` | `LazyColumn` 行铺满窗口宽 → 1600dp 窗口上一行设置项横跨 1600dp |
| `feature/email/.../component/EmailDetail.kt:62-76` | 正文 `LazyColumn` 只有 16dp 横向内边距；Expanded 窗口下详情栏可宽到 800dp 以上 |

**两处经复核后撤回**（原先的判断不成立，记录在此以免再被引用）：

- ~~`NewEmailSheet`：`ModalBottomSheet` + `fillMaxWidth` → 输入框横跨整屏~~
  **撤回**。`ModalBottomSheet` 有 `sheetMaxWidth` 参数，默认取
  `BottomSheetDefaults.SheetMaxWidth = 640.dp`，库内部以 `maxWidth = sheetMaxWidth`
  施于弹层容器并居中（`ModalBottomSheet.kt:100,162`）。宽窗口下它本来就收窄，
  不是"横跨整屏"。这里唯一值得记的是 `rememberModalBottomSheetState` 已被
  `rememberBottomSheetState` 取代（编译告警）——该告警已于 `004f9188` 处理：
  改为显式声明 `enabledValues = {Hidden, Expanded}`（旧 API 会**按运行期测出的弹层高度**
  自行剔除 `PartiallyExpanded`，同一份代码在不同屏幕上档位可能不同）。
- ~~搜索历史 `FlowRow` 铺满~~ **撤回**。它渲染在 `ExpandedDockedSearchBar`
  （宽窗口）/`ExpandedFullScreenSearchBar`（紧凑窗口）内部，两者自身的宽度已经受限，
  不存在"铺满窗口"的路径。
- ~~`EmailPagedList` 列表项无上限~~ **降级**。列表项是行式内容而非正文，
  行宽上限不是它的诉求；且经 S5 之后列表总是处在列表栏或紧凑窗口内。

**修法**：在 `core:theme` 增加一个通用宽度约束组件 `AdaptiveContent(maxWidth)`，
设置页与详情页正文用它。注意这不是"多形态适配"——它是**可读性常量**，
与窗口形态无关（形态适配是 `LocalWindowAdaptiveInfo` 与场景策略的职责）；
两处都用到，符合项目已有判据「同一段 Compose 渲染在 ≥2 处出现 → 下沉 `core:theme`」。

---

### D9 · insets 由四套机制并行处理，且互相不知情

| 机制 | 位置 | 说明 |
|---|---|---|
| ① 库消费 | `NavigationSuiteScaffold` 内部 `consumeWindowInsets` | 但只覆盖 `NavigationBar / NavigationRail / NavigationDrawer`；本应用用的 `ShortNavigationBar* / WideNavigationRail*` 落到 `else -> NoWindowInsets`（`NavigationSuiteScaffold.kt:345-366`），**什么都没消费** |
| ② 各屏自己的 `Scaffold` | `EmailList.kt:41` / `EmailDetail.kt:51` / `SettingsScreen.kt:53` / `LoginScreen.kt:93` | 各自根据 `contentWindowInsets` 产出 `padding` |
| ③ 手写归零 | `core/theme/.../layout/NavigationPlacement.kt:49-68` `toContentPadding()` | 「哪几边保留」的第二套实现，注释里也承认"原先在三个消费方各写一遍并已漂移" |
| ④ 组件级硬编码 | `feature/email/.../component/EmailTopBar.kt:59` `Modifier.statusBarsPadding()` | 多选操作栏自己贴状态栏；而 `CenteredTopAppBar` 走 `TopAppBarDefaults` 的 inset —— 同类组件两种做法 |

③ 是 ① 的**平行实现**：它会因为 ① 的行为变化而与实际布局脱节；而它现在**恰好没出问题**，
只是因为"① 什么都没消费" + "③ 无条件把那一侧归零"这两件互相不知道的事凑巧互补。

另外 `NavigationPlacement.kt:50,56,65` 把 `LayoutDirection.Ltr` 写死：

```kotlin
val ltr = LayoutDirection.Ltr
... start = calculateStartPadding(ltr), end = calculateEndPadding(ltr) ...
NavigationPlacement.Side -> PaddingValues(..., start = 0.dp, end = calculateEndPadding(ltr))
```

manifest 里 `android:supportsRtl="true"`。在 RTL 下 Rail 会落在**右侧**，而 `Side` 仍然归零 `start`（左）
→ 内容会在右侧被 Rail 压住。当前只发行 en / zh-Hans，属潜在缺陷，但它是"宿主布局信息被压缩成一个
无方向概念"的直接产物。

**修法**：宿主**真正消费**自己占用的那部分 insets，内容区自然看到 0，不需要"约定归零哪一侧"：

```kotlin
// AppShell 内部
CompositionLocalProvider(LocalWindowAdaptiveInfo provides adaptiveInfo) {
  Box(Modifier.consumeWindowInsets(navSuiteInsetsFor(layoutType))) { content() }
}
```

`toContentPadding()` 与 `NavigationPlacement` 一并删除。RTL 问题随之消失（`consumeWindowInsets` 是按
`WindowInsetsSides` 的，方向语义由框架负责）。

---

### D10 · 没有一处 IME inset 处理

全仓 `imePadding` / `WindowInsets.ime` **零命中**。而应用有三个文本输入面：登录页、写邮件 sheet、搜索。

- `LoginScreen.kt:93-163`：外层 `Scaffold { padding -> Column(...) }`，只用了
  `padding.calculateTopPadding()`（`:105`），底部与 IME 都不处理；表单虽有 `verticalScroll`，
  但没有 `imePadding()`，键盘出现时可视区不会为键盘让位。
- `androidApp/src/main/AndroidManifest.xml` 的 `<activity>` 既没有 `windowSoftInputMode` 也没有
  `configChanges`。合并后的清单（`androidApp/build/intermediates/merged_manifest/.../AndroidManifest.xml`）
  同样没有 → 走系统默认；配合 `MainActivity.onCreate` 的 `enableEdgeToEdge()`（`decorFitsSystemWindows=false`），
  窗口不会被 IME 顶起，只有主动消费 `WindowInsets.ime` 的布局才会让位。

这与 daily log 里记录的实机现象吻合：**键盘完全盖住下方输入框，键盘开着时点下方字段会落在键盘上**。

**修法**：登录页 / 写邮件页加 `Modifier.imePadding()`（并考虑 `Modifier.imeNestedScroll()` 让滚动容器
在键盘弹出时跟随）；manifest 显式写 `android:windowSoftInputMode="adjustResize"`，把"窗口是否为 IME 让位"
这件事从平台默认值变成项目约定。

---

### D11 · 无 `configChanges`：可接受，但状态补偿散落各处

`<activity>` 没有 `android:configChanges`，所以旋转 / 分屏拖拽 / 折叠都会**重建 Activity**。
对 Compose 应用这本身是合理默认（`rememberSaveable` + ViewModel 是既定路径），不列为缺陷。
但要注意它把"状态必须可恢复"变成全局约束，而当前有三处靠手工补偿：

1. `EmailSearchBar.kt:153-182`（D6）；
2. `AppNavHost.kt:89-95` 用**引用比较**区分首帧 / 运行期 DeepLink —— 前提是
   `App.kt:32` 用 `remember(deepLinkUrl)` 记住解析结果，`MainActivity` 在重建后重新取 `intent.dataString`；
3. `MainViewController.kt:15,26-50` 用 `LocalIosComposeRebuildToken` 处理 iOS 语言切换重建。

这三处都是"窗口/环境变化"引起的，收敛到统一的状态来源后（D5/D6 的修法）第 1 处可直接删。

---

### D12 · 没有可覆盖 / 可测试的窗口信息入口

- `currentWindowAdaptiveInfo()` 在三个地方被独立调用（D1 / D3），彼此无法被同一个测试夹具替换。
- 没有 `@Preview` 覆盖宽屏：`CenteredTopAppBar` 只有一个默认宽度预览；`AppShell` / `HomeScreen` /
  `SettingsScreen` 都没有多形态预览。
- 结果是"宽屏行为"只能靠 `wm size` + 实机/模拟器验证。daily log 里记录的验证流程
  （`wm size 1600x1000 && wm density 240` 等）正是这个缺口的代价。
- 顺带一处隐性耦合：`core/theme/.../Theme.kt:25-28` 覆写 `LocalDensity`（为了 `FontSizeConfig`），
  而 `currentWindowAdaptiveInfo()` 恰恰是用 `LocalDensity` 把 `containerSize` 换算成 dp 的
  （`WindowAdaptiveInfo.kt:63-64`）。当前只改 `fontScale`、`density` 不变，所以数值正确；
  但只要将来为了"显示大小"设置去改 `density`，**窗口尺寸类会静默算错**。

**修法**：见 §3 的 `LocalWindowAdaptiveInfo`。一旦它成为唯一入口，测试/预览里提供它即可，
不需要真机旋转。

---

### D13 · `WindowSizeClass` 来自未声明的传递依赖

`AppShell.kt:12` `import androidx.window.core.layout.WindowSizeClass`，而
`feature/main/build.gradle.kts` 只声明了 `material3-window-size` / `adaptive` / `adaptive-navigation-suite`
（注释还写着"adaptive-layout 与 adaptive-navigation 由本模块的代码直接用不到"）。
`androidx.window:window-core` 是通过 `adaptive` 传递进来的，属**越级使用传递依赖**；
`adaptive` 换实现或收窄依赖时，`feature:main` 会以"未解析的引用"形式突然失败。

**修法**：要么把 `WindowSizeClass` 的使用收敛到 `core:theme`（推荐，与 D12 同一动作），
要么在 `kmp.versions.toml` 显式声明 `androidx-window-core` 并 `implementation` 引入。

---

## 3. 目标架构

### 3.1 三条原则

1. **窗口形态是应用级事实，只在组合根算一次。**
   全仓只允许一处调用 `currentWindowAdaptiveInfoV2()`，结果经 `LocalWindowAdaptiveInfo` 下发。
   与项目已有的 `LocalNavigationPlacement`（替代 `isBottomNavigationBar`）是同一手法，
   但**下发的是原始事实（尺寸类 / 姿态），而不是某个推导结果** —— 这才是上次重构没走完的那一步：
   上次把"布局细节"换成了"方位承诺"，方向对，但仍然是**推导后的**值。

2. **布局决策只有一处，且用库的能力，不手写分档。**
   `NavigationSuiteScaffoldDefaults.navigationSuiteType()` 决定导航套件形态；
   `ListDetailSceneStrategy`（内部用 V2 directive）决定列表-详情。feature 里不再出现任何 `isWidthAtLeastBreakpoint`。

3. **宿主消费 insets，内容只看到剩余。**
   宿主把自己占用的那部分 `consumeWindowInsets` 掉，内容区用标准 `Scaffold` / `windowInsetsPadding`
   自然得到正确内边距。**不**再下发"哪一侧归零"。

### 3.2 目录与职责

```
core:theme/layout/
  WindowAdaptiveInfo.kt          ← 新增：LocalWindowAdaptiveInfo + ProvideWindowAdaptiveInfo
  (删除 NavigationPlacement.kt 的 toContentPadding；NavigationPlacement 视需要整体删除)

feature:main/AppShell.kt
  ← 唯一调用 currentWindowAdaptiveInfoV2() 的地方
  ← navigationSuiteType(adaptiveInfo) 决定形态
  ← 提供 LocalWindowAdaptiveInfo + consumeWindowInsets(自己占用的部分)

feature:email/
  ← HomeScreen / FavoritesScreen 不再各自决定"分栏还是整页"
  ← EmailList / EmailDetail 只做内容，读 LocalWindowAdaptiveInfo 做"要不要限宽"
  ← PaneScaffold.kt / .android.kt / .ios.kt 整组删除

composeApp/AppNavHost.kt
  ← NavDisplay(sceneStrategies = listOf(rememberListDetailSceneStrategy(), SinglePaneSceneStrategy()))
  ← 列表项与详情项通过 NavEntry metadata 标注 listPane / detailPane
```

### 3.3 `AppShell` 改写（示意）

```kotlin
@Composable
fun AppShell(selected: TabDestination?, onSelectTab: (TabDestination) -> Unit, content: @Composable () -> Unit) {
  if (selected == null) { content(); return }

  // 全应用唯一的窗口形态读取点
  val adaptiveInfo = currentWindowAdaptiveInfoV2()
  val layoutType = NavigationSuiteScaffoldDefaults.navigationSuiteType(adaptiveInfo)
  val navSuiteInsets = when (layoutType) {
    NavigationSuiteType.ShortNavigationBarCompact,
    NavigationSuiteType.ShortNavigationBarMedium,
    NavigationSuiteType.NavigationBar -> WindowInsets.systemBars.only(WindowInsetsSides.Bottom)
    NavigationSuiteType.NavigationDrawer -> WindowInsets(0) // 模态抽屉不占位
    else -> WindowInsets.systemBars.only(WindowInsetsSides.Start)
  }

  NavigationSuiteScaffold(navigationSuiteItems = { /* MainTab.entries */ }, layoutType = layoutType) {
    CompositionLocalProvider(LocalWindowAdaptiveInfo provides adaptiveInfo) {
      Box(Modifier.consumeWindowInsets(navSuiteInsets)) { content() }
    }
  }
}
```

> 注：`NavigationSuiteScaffold` 对 `ShortNavigationBar* / WideNavigationRail*` 自己**不消费** insets
> （`NavigationSuiteScaffold.kt:352-364` 的 `else -> NoWindowInsets`），所以这里必须由外壳消费。
> 这也是为什么"让内容自己少留白"（上一轮的 `toContentPadding`）会暂时奏效 —— 但代价是第二套真相。

### 3.4 列表-详情改接 Nav3 场景策略

库已提供跨平台实现（`adaptive-navigation3 1.3.0-rc01` `commonMain/…/ListDetailSceneStrategy.kt:79-101`）：

```kotlin
@Composable
public fun <T : Any> rememberListDetailSceneStrategy(
  shouldHandleSinglePaneLayout: Boolean = false,
  backNavigationBehavior: BackNavigationBehavior = BackNavigationBehavior.PopUntilScaffoldValueChange,
  directive: PaneScaffoldDirective = calculatePaneScaffoldDirective(currentWindowAdaptiveInfoV2()),
  adaptStrategies: ThreePaneScaffoldAdaptStrategies = ListDetailPaneScaffoldDefaults.adaptStrategies(),
  ...
): ListDetailSceneStrategy<T>
```

Nav3 `1.1.1` 的 `NavDisplay` 已有 `sceneStrategies: List<SceneStrategy<T>>` 参数，直接可用。

收益（一次改动同时消掉 D3 / D7 与两条历史遗留）：

| 维度 | 现在 | 改用 SceneStrategy 后 |
|---|---|---|
| directive 口径 | iOS V1 / Android V2 | 统一 V2 |
| 平台代码 | `PaneScaffold` 需要 expect/actual（因为 `NavigableListDetailPaneScaffold` 只在 `androidMain`，`AndroidThreePaneScaffold.android.kt:63`） | 全 commonMain，expect/actual 删除 |
| 返回行为 | 首页走私有 navigator、收藏页走 Nav3 | 都走 Nav3 返回栈，`BackNavigationBehavior` 统一 |
| 预测性返回 | 只有 Android 通过 `Navigable*` 包装接上 | NavDisplay 统一处理，两端一致 |
| 折叠 / 旋转 | 首页的 pane 状态在私有 navigator 里 | 随返回栈序列化恢复 |
| 首页 vs 收藏页 | 两种表现 | 同一种 |
| 栏宽 | 360dp vs 412dp 分叉 | 由 V2 directive 一致决定 |

### 3.5 内容宽度约束（下沉 `core:theme`）

```kotlin
// core/theme/component/AdaptiveContent.kt
@Composable
fun AdaptiveContent(
  modifier: Modifier = Modifier,
  maxWidth: Dp = 840.dp,          // 正文类内容的可读上限
  content: @Composable () -> Unit,
) {
  Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Box(Modifier.widthIn(max = maxWidth).fillMaxWidth()) { content() }
  }
}
```

消费点：`SettingsScreen`（列表限宽或按尺寸类切双栏设置）、`EmailDetail` 正文、
（写邮件弹层不需要：`ModalBottomSheet` 自带 `sheetMaxWidth = 640.dp`，见 D8 的撤回说明）。

### 3.6 测试 / 预览入口

```kotlin
// core/theme/layout/WindowAdaptiveInfo.kt
val LocalWindowAdaptiveInfo = staticCompositionLocalOf<WindowAdaptiveInfo> {
  error("LocalWindowAdaptiveInfo 未被提供：必须由 AppShell 提供")
}

/** 测试与 @Preview 用：给定尺寸类/姿态伪装当前窗口。 */
@Composable
fun ProvideWindowAdaptiveInfo(
  windowSizeClass: WindowSizeClass,
  posture: Posture = Posture(),
  content: @Composable () -> Unit,
) = CompositionLocalProvider(
  LocalWindowAdaptiveInfo provides WindowAdaptiveInfo(windowSizeClass, posture),
  content = content,
)
```

- `@Preview` 直接给 600 / 840 / 1200 / 1600 四档，宽屏布局进 CI 截图。
- `commonTest`（项目已有 `:composeApp:iosSimulatorArm64Test` 与 `NavBackStackSerializationTest` 的先例）
  可断言："1600dp 下收藏页与首页给出一致的分栏结构""Compact 下不出现 Rail"。
- 无默认值 + `error()` 与 `LocalNavigationPlacement` 保持一致：接线错误在开发期立刻暴露，而不是静默取默认档。

---

## 4. 建议的落地顺序（每步独立可验证）

| 步骤 | 改动 | 立刻可验证的结果 |
|---|---|---|
| **S1** | `AppShell`：`currentWindowAdaptiveInfo()` → `currentWindowAdaptiveInfoV2()`；`navigationSuiteType()` 取代手写链 | 1200dp 以上不再是死分支；1600dp 窗口 Rail 展开；折叠半开不再给 Rail |
| **S2** | 新增 `LocalWindowAdaptiveInfo` + `ProvideWindowAdaptiveInfo`；`AppShell` 唯一提供，去掉 feature 里的直接调用 | 宽屏 `@Preview` 可用；调用点从 3 处收敛到 1 处 |
| **S3** | 宿主 `consumeWindowInsets`，删 `toContentPadding`；`EmailTopBar` 的 `statusBarsPadding()` 改为统一 inset 策略 | 内边距只剩一套真相；RTL 隐患消失 |
| **S4** | `EmailSearchBar` 改读尺寸类，删 `initial` 同步补丁 | 旋转不再需要补丁；搜索栏形态与"是否有横向空间"直接对应 |
| **S5** | 列表-详情接 `rememberListDetailSceneStrategy`；删 `PaneScaffold` expect/actual 与首页私有 navigator | 收藏页获得分栏；两端 directive 一致；预测性返回两端一致 |
| **S6** | `AdaptiveContent` 下沉 + 设置页/详情正文接入；`LoginScreen` 加 `imePadding()`，manifest 加 `windowSoftInputMode` | 宽屏可读；键盘不再遮挡 |
| **S7** | 多尺寸 `@Preview` + `commonTest` 断言；补 `androidx-window-core` 显式依赖（或把 `WindowSizeClass` 收进 core） | 回归门禁 |

S1 是**单文件、零架构风险**的净收益，建议先落地。

---

## 5. 验证矩阵

```
每档都跑一遍：360dp / 600dp / 840dp / 1200dp / 1600dp
  adb shell wm size 1080x2340 ; adb shell wm density 480     # 360dp Compact
  adb shell wm size 1600x1000 ; adb shell wm density 240     # 666dp Medium (Rail)
  adb shell wm size 1920x1200 ; adb shell wm density 200     # 960dp Expanded
  adb shell wm size 2560x1600 ; adb shell wm density 213     # 1200dp Large   ← S1 之前 Rail 不会展开
  adb shell wm size 3840x2160 ; adb shell wm density 240     # 1600dp XLarge
  adb shell wm size reset ; adb shell wm density reset       # 验完必须复位
```

另外必查：**旋转**（横竖各一轮，确认无状态丢失、无重复压栈）、**折叠半开**（AVD Resizable / 折叠模拟器，
确认 `windowPosture.isTabletop` 生效）、**iPad Split View 1/2、1/3**（确认 iOS 与 Android directive 一致）、
**RTL 伪本地化**（确认 `Side` 归零不再是左侧硬编码）。

---

## 6. 证据索引

| 结论 | 出处 |
|---|---|
| V1 废弃、V2 才支持 L/XL | `adaptive-1.3.0-rc01` `commonMain/…/WindowAdaptiveInfo.kt:53-74` |
| V1 档位只有 {0,600,840} | 同 jar `commonMain/…/WindowSizeClassHelper.kt:90-96,162-170` |
| `isWidthAtLeastBreakpoint` 用 `minWidthDp` | `window-core-1.5.0` `commonMain/…/WindowSizeClass.kt:56-61,98-100` |
| directive 分档与栏宽 360/412 | `adaptive-layout-1.3.0-rc01` `commonMain/…/PaneScaffoldDirective.kt:52-84,345-346` |
| navigator 默认 directive 用 V2 | `adaptive-navigation-1.3.0-rc01` `commonMain/…/ThreePaneScaffoldNavigator.kt:202-204` |
| 库推荐导航套件策略 | `material3-adaptive-navigation-suite-1.12.0-alpha03` `…/NavigationSuiteScaffold.kt:1223-1236` |
| 库对 Short/Wide 类型不消费 insets | 同文件 `:345-366` |
| iOS 无姿态 | `adaptive-1.3.0-rc01` `nonAndroidMain/…/WindowAdaptiveInfo.nonAndroid.kt:31-35` |
| `NavigableListDetailPaneScaffold` 仅 Android | `adaptive-navigation-android-1.3.0` `androidMain/…/AndroidThreePaneScaffold.android.kt:63` |
| 跨平台列表-详情场景策略 | `adaptive-navigation3-1.3.0-rc01` `commonMain/…/ListDetailSceneStrategy.kt:79-101` |
| NavDisplay 支持 sceneStrategies | `navigation3-ui-1.1.1` `commonMain/…/NavDisplay.kt:334-358` |
| 合并后清单无 configChanges / windowSoftInputMode | `androidApp/build/intermediates/merged_manifest/…/AndroidManifest.xml` |

---

## 7. 追加：measure 机制收敛与登录页分区

审计原本把 D8 的修法写成"加一个 `AdaptiveContent`"。落地后发现真正的结构问题是**同一个概念被两套机制表达**，
现在收敛掉，并顺手处理了登录页"一列居中 + 四周留白"。

### 7.1 measure（宽度上限）集中到一处

`core:theme/layout/ContentWidth.kt`：`ContentWidth.Form`(480) / `.Article`(720) / `.Prose`(840)
+ `Modifier.contentWidth()`（窄窗口填满、宽窗口封顶）。原先三处各自为政 ——
`AdaptiveContent` 的默认值 840、`EmailDetail` 的私有 `DetailMaxWidth = 720`、
登录页的私有 `FormMaxWidth = 480` + 裸 `widthIn` —— 没人能一眼看出"这个 480 与那个 480 是不是一回事"。

### 7.2 派生事实也只在组合根算一次

`LocalPaneScaffoldDirective` + `rememberPaneScaffoldDirective(adaptiveInfo)` +
`canShowSideBySidePanes`（+ `WindowAdaptiveInfo.isTabletopPosture`）。原先 `AppNavHost` 自己算 directive；
登录页若也要用就成了第二个派生点 —— 纯函数今天必然一致，但参数一旦被谁改掉就会静默分叉，
那正是"窗口形态三处各读一次"的病根以另一种形式重来。
注意 `calculatePaneScaffoldDirective` 是 `@ExperimentalMaterial3AdaptiveApi`，而 `PaneScaffoldDirective`
本身不是，所以 `@OptIn` 留在 core:theme 内部，公开 API 保持稳定类型。

### 7.3 登录页：由分区能力决定排布，而不是靠一个数字

`loginArrangementFor(adaptiveInfo, directive)`（纯函数，见 `feature/login` 的 commonTest）：

| 条件 | 排布 | 实测窗口（应用自己看到的 dp） |
|---|---|---|
| `isTabletopPosture`（横向铰链切分） | 竖排：品牌在上、表单在下 | 桌面支架（模拟器无法复现，由 commonTest 覆盖） |
| `canShowSideBySidePanes` | 横排：品牌占左侧剩余、表单在右且封顶 | 841×701dp、1600×900dp、914×411dp（横屏手机） |
| 其余 | 单栏（同改造前） | 411×914dp（竖屏手机）、360×780dp、700×800dp |

- **竖排的判据是姿态，不是容量** —— 这一条是复查时用实机纠正过来的，见 §8.2。
- **横排下表单取精确 measure**：`Row` 的剩余空间已由 `weight` 交给品牌区，表单再 `fillMaxWidth`
  会把整行吃掉。栏间留白用 `directive.horizontalPartitionSpacerSize`（24dp），与列表-详情同一套间隔。
- 顶部两个全局入口（深色模式 / 语言）归入**表单区**：原先 `align(End)` 贴窗口右缘，
  841dp 上离表单约 400dp、1600dp 上约 800dp；归入后窄窗口下（表单与窗口同宽）观感不变。

### 7.4 明确否决的两条

- **`SupportingPaneSceneStrategy`**：它的窗格必须来自**返回栈条目**
  （`SupportingPaneSceneStrategy.calculateScene` 从栈顶收集带 pane 元数据的 entry，
  `adaptive-navigation3-1.3.0-rc01` `commonMain/…/SupportingPaneSceneStrategy.kt:135,145`）。
  品牌栏是静态 UI 不是目的地，塞进导航模型会让返回栈表达出并不存在的"用户去过品牌页"。
  对照 `EmailDetailDestination.scene`：那里 `scene` 是**路由**信息（哪张列表打开的），所以它该在 key 里。
- **`BoxWithConstraints` 插值 / `fillMaxWidth(fraction)`**：版式随窗口连续变化、不可预期，
  且会让表单在大窗口上突破那个真正该守的可读上限。

### 7.5 对本文档的一处更正

原正文（D12 一节）说登录页表单"垂直居中于整幅高度 → 上下两头空"。**这个说法不准确**：
`verticalArrangement = Arrangement.Center` 在 `verticalScroll` 内部是**失效的** ——
子项被无界高度测量，Column 只包裹内容，没有余量可分配。现状其实是**顶部对齐 + 底部留白**
（实测 841dp 窗口：内容到 y≈1452 结束，而窗口高 1840，底部余 388px）。这也解释了为什么大屏上
表单看起来"往上挤"。本轮的排布改造顺带让两区各自居中，不再依赖这个失效参数。

---

## 8. 复查：还有没有没适配多窗口形态的页面

### 8.1 方法：先量准"应用自己看到的窗口"

复查中两次得出相反结论，根因是**拿 `wm size` / `wm density` 推算应用窗口的 dp 尺寸**。
本机 AVD 是桌面窗口模式（且窗口相对显示器有旋转），应用窗口的尺寸与密度是它**自己的**：

```
adb shell dumpsys window windows | grep -m1 -oE "w[0-9]+dp h[0-9]+dp [0-9]+dpi [a-z]+"
```

实测同一时刻：显示器是 `wm size 2400x1350 / density 240`，而应用看到的是
`w514dp h914dp 420dpi`（窗口 1350×2400px、物理密度 420、`mDisplayRotation=ROTATION_90`）。
按 `wm` 值推算会得到"900×1600dp、应该横排"，与实机（Compact 宽度 → 单栏）矛盾。
**一切"应用在什么窗口下做了什么"的判断，都要以这条 Configuration 为准。**

### 8.2 复查结果

| 界面 | 大/异常窗口下的行为 | 结论 |
|---|---|---|
| 外壳（Rail / 底部栏） | `navigationSuiteType()`：Compact·Medium 或桌面支架 → 底部栏；Expanded → Rail；≥1600dp → 展开 Rail | ✓ |
| 首页 / 收藏（列表-详情） | 按 directive：≥2 横分区 → 左右双栏 + 详情空态；1 分区 → 单栏，详情整页并给返回键 | ✓ |
| 邮件详情正文 | `ContentWidth.Article`(720dp) 封顶居中（实测 777dp 详情栏内正文收在 ~708dp） | ✓ |
| 搜索 | `isCompactWidth` → 全屏搜索栏 / docked 面板 | ✓ |
| 写邮件弹层 | `ModalBottomSheet` 自带 640dp 上限 | ✓ |
| 设置 | `ContentWidth.Prose`(840dp) 封顶居中；**有意**不做 list-detail（该列表是扁平的，没有分类层级） | ✓ |
| 加载遮罩 / 对话框 / 头像 | 与窗口无关（固定尺寸、居中、或被系统弹窗承载） | ✓ |
| 登录 | 见 §7.3 | ✓ |

**没有发现"完全没适配"的页面**；但查出一个**本轮自己引入的回归**，见下。

### 8.3 修掉的回归：竖屏手机被摊成上下半屏

初版把"竖排"判据写成 directive 的 `maxVerticalPartitions >= 2`。它在两种窗口上都为真：
桌面支架（横向铰链 —— 该竖排），以及"窄而高"（1 个横向分区 且 高度为 Expanded）——
而 **411×914dp 的普通竖屏手机正是后者**。于是手机上的登录页变成：品牌占上半屏、
两个字段落到下半屏、中间空出一大段（实机截图可见）。

改法：竖排只认 `WindowAdaptiveInfo.isTabletopPosture`（真的被横向铰链切分），
并删掉 `canShowStackedPanes` —— 它只在登录页用过，而用法本身把"容量"当成了"理由"。
**容量够 ≠ 应该这么排**，这条已钉进 commonTest：

- `tallPhoneStaysSingleColumn`：411×914dp —— 先断言它的 `maxVerticalPartitions >= 2`（容量确实够），
  再断言决策为单栏；
- `verticalCapacityIsNotAReasonToStack`：700×1200dp 同理。

**反向验证**：把判据临时改回 `maxVerticalPartitions >= 2`，恰好且仅这两条失败
（`expected:<Single> but was:<Stacked>`），其余 8 条通过；恢复后 10 条全过。

### 8.4 顺带观察（非缺陷，但值得记）

- 桌面窗口模式 + 窗口旋转会让应用报出**桌面支架姿态**（`isTabletop = true`）。这是本机 AVD
  的自由窗口与旋转组合造成的，不是应用逻辑；应用的反应（不跨越"铰链"）自洽。
- 桌面支架下列表-详情退化为单栏详情：`calculateThreePaneScaffoldValue` 只在
  `maxHorizontalPartitions == 1` 时才考虑重排（`checkReflowedPane`），且
  `ListDetailPaneScaffoldDefaults.adaptStrategies()` 三个角色都是 `AdaptStrategy.Hide`
  —— 是库的行为，应用侧没有可改的开关。


