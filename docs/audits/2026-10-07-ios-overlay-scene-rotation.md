# iOS 弹层与 Scene 旋转共性审计

日期：2026-10-07。状态：首页根 Scene 的无效退场动画与停靠搜索最终宽度同步已修复；当前源码 Debug 包的 iPhone 模拟器旋转回归通过。其他设备、软键盘与独立 Constraints 异常的完整验收仍待完成。

## 结论与证据边界

iOS 首页在弹层打开时跨单栏／列表—详情 Scene 旋转，至少有两条独立入口触发同一异常：搜索与写邮件。不能只修搜索的全屏／停靠分支就宣称解决共性问题。

```text
kotlin.IllegalArgumentException:
Key (HomeDestination, class me.zhangls.main.api.HomeDestination) was used multiple times
SaveableStateHolderImpl.SaveableStateProvider
CompositionImpl.applyLateChanges
```

异常证明同一个保存状态 key 出现重叠注册。后续隔离验证确认：项目 `DeviceCornerScene` 在根页面不执行缩放时，仍注册 220ms 退场动画，使窗口变化的旧 Scene 继续滞留；取消该无效动画后，相同搜索与写邮件旋转入口通过。该触发边界已验证，但没有定位或修改 Compose/Navigation 内部的具体搬移失效机制，不能宣称已证明某个上游缺陷。

## 修复与新增验证

保持 SafeArea 包装、单栏／列表—详情切换、页面组件、尺寸和交互。`DeviceCornerNavDisplay` 仅将 `previousEntries` 为空的根页面进度动画改为 `snap()`；详情页仍使用原有 220ms tween，预测返回与取消恢复不变。

### 隔离对照

以下均从当前工作区源码重新构建、安装 Debug 应用，同一 iPhone 18 Pro / iOS 27.0 模拟器，不沿用未校验的旧二进制。

| 对照 | 实际结果 |
|---|---|
| 原始包装与动画 | 搜索竖屏展开转横屏再次闪退，`HomeDestination` key 重复；`notes-fresh-baseline.stdout.log` |
| 移除 DeviceCorner 与 SafeArea 包装 | 展开搜索横屏转正常竖屏、再转横屏通过 |
| 恢复 SafeArea，只移除 DeviceCorner 包装 | 展开搜索横屏转正常竖屏通过 |
| 恢复全部包装，仅取消根页面 220ms 无效动画 | 以下设备回归全部通过 |

新增 `AppSceneStrategiesTest.resizingRootListDoesNotKeepPageExitAnimationRunning`：暂停测试时钟，在根列表由窄窗口切到宽窗口后推进 8 帧，断言原单栏场景的进度动画总时长为 0。修复前实际失败（220000000ns），修复后通过。此测试验证项目动画触发条件，不能替代 iOS 浮层行为验收。

### 修复包设备回归

- 搜索展开：正常竖屏→两种横屏方向→正常竖屏；搜索词 `Google` 与结果保留。
- 搜索连续两轮四次设备方向切换，关闭后重新展开全屏搜索；未闪退。倒置竖屏仍不计为应用支持的竖屏方向。
- 点击搜索结果进入详情，返回首页后四次设备方向切换；搜索保持关闭。
- 写邮件展开旋转往返，主题 `Rotation` 保留；随后正文输入 `Draft`，连续四次设备方向切换，两项均保留；取消草稿，没有保存邮件。
- 设置深色模式选择弹窗旋转往返并取消；没有修改设置。
- 最后恢复正常竖屏首页，所有测试弹层关闭；没有修改模拟器分辨率、密度或系统配置。

修复包日志 `notes-snap.stdout.log` 为空，stderr 没有 Kotlin 异常或超大 Constraints 错误。软键盘完整布局/焦点交接、首页多选删除确认框、登录下拉框和其他设备仍未验收。横屏停靠搜索的完整视觉对齐不是本次崩溃修复的验收结论。

### 编译与自动测试

Xcode Debug 整包构建通过；保持正常签名，随后安装同一产物。`composeApp:iosSimulatorArm64Test` 实际执行 29 项，全部通过，包括真实 Entry 分发与 Preference 初始化。

Android 宿主实际执行以下筛选：`AppSceneStrategiesTest`、`DeviceCornerNavigationTest`、`PageMotionNavigationTest`，以及下文列出的五个 `AdaptiveUiTest` 方法。JUnit XML 共 97 项：19 通过、78 条件跳过、0 失败、0 错误。既有其他导航透明度断言未在这组测试中执行，不把它们标记修复。

Android `:androidApp:assembleDebug` 整包构建通过。随后启动已有 Pixel 10 Pro Fold AVD，安装 DevFull Debug 包并执行启动检查：安装成功、Activity 冷启动 `Status: ok`，应用进程仍在。当前界面控制工具不能连接该模拟器窗口，搜索展开旋转与 Activity/真实键盘设备行为没有完成验收；启动成功不替代旋转回归。没有修改分辨率、密度或旋转设置，结束时查询无 Override，并关闭本次启动的 AVD。

独立 Constraints 异常在本次连续旋转和重开搜索中未再出现；没有单独确认其根因，继续保留跟踪。未更新正式截图基线。

## 后续：展开输入框宽度不同步

Xander 在提交 `8fe827c5` 后指出，iOS 旋转回归又出现展开输入框与原框不一致、两份头像露出的画面。该画面在上面的崩溃回归中已经出现，但当时未继续核对视觉对齐，属于漏检。

本次仍使用 iPhone 18 Pro / iOS 27.0，当前源码增加临时 `onGloballyPositioned` 坐标日志后重新构建安装，未改变布局行为。对照结果：

| 操作 | 锚点 / 浮层输入宽度 | 左上角与画面 |
|---|---|---|
| 横屏直接展开 | 933px / 933px | 都是 (498px, 66px)，对齐 |
| 展开状态横屏→正常竖屏→横屏 | 最终 933px / 830px | 最终都在 (498px, 66px)，浮层宽度停在过渡值；原框右侧与头像露出 |
| 在错位状态输入 `x` | 933px / 933px | 内容变化触发重测后对齐 |
| 在错位状态关闭再展开 | 对齐 | 前序未加日志的正常包也已做设备对照 |

日志明确记录锚点过渡宽度 411→443→471→571→635→782→830→933px；浮层跟到 830px 后，位置继续更新到最终值，但宽度仍为 830px。数据来自实际布局回调，不是按 Device Hub 缩放截图估算。日志 `notes-search-coordinates.stdout.log` 保存在本模拟器 data/tmp 下，临时诊断代码已恢复为已提交版本。

上次的 `usePlatformInsets = false` 仍然存在，本次不是重复叠加安全区：最终位置一致，差异在宽度。当前 Material `DockedSearchBarLayoutImpl` 测量时直接读取 `state.collapsedCoords?.size?.width`；`collapsedCoords` 保存的是可变 `LayoutCoordinates` 引用。锚点同一坐标对象的尺寸变化不是独立的可观察尺寸值，iOS Popup 的独立布局没有在最后一次锚点变化后及时重测。输入变化或重开浮层会重新读取最终宽度，与本次对照一致。

因此，已确认的失效边界是旋转/窗格过渡后锚点最终宽度与浮层测量不同步。没有证据证明 `snap()` 直接改坏了宽度计算；此前该展开旋转路径会崩溃，不能把当时无法完成的流程当作视觉对齐已通过的依据。正确修复方向是让锚点最终几何变化明确驱动浮层重测，同时保留自然宽度、原有留白和安全区坐标约定；不能以隐藏原框或硬编码浮层宽度作为修复。宽度同步修复及验证见下文。

### 宽度同步修复与回归

`MeasuredSearchInput` 接收 Modifier，使用 `onSizeChanged` 将实际锚点宽度保存为可观察的像素值；停靠浮层按当前密度换算为 dp 并应用该宽度。首次尚未获得有效尺寸时继续使用 Material 默认测量。空查询自然宽度、长查询测量策略、留白、全屏分支与 iOS 安全区配置保持原约定；根 Scene 的 `snap()` 修复保留。

新增 `dockedSearchTracksAnchorAcrossSuccessiveWindowWidths`：900×500dp 展开搜索后依次调整至 610、650、720、900dp，不输入文字或重新打开，比较锚点与浮层输入宽度。修复前实际失败（360px / 344px），修复后通过，表明宽度失效也能在 Android 宿主复现；不能据此声称 Android 设备旋转已验收。

Android 宿主筛选上述新增方法，以及 `longSearchQueryKeepsMeasuredWidth`、`restoredSearchUsesEmptyQueryMeasuredWidth`、`measuredSearchWidthShrinksAndRecoversWithWindow`、`expandedSearchKeepsQueryWhenWindowWidthChanges`、`landscapeSearchExpansionDoesNotJumpToFinalHeight`、`searchResultNavigationKeepsSearchClosedAfterReturnAndRotation`，同时执行 `AppSceneStrategiesTest`、`DeviceCornerNavigationTest`、`PageMotionNavigationTest`。JUnit XML 共 131 项：24 通过、107 条件跳过、0 失败、0 错误。iOS 自动测试 29 项全部通过，Android 整包构建与最终无诊断日志的 Xcode Debug 整包构建通过。

同一 iPhone 18 Pro / iOS 27.0 模拟器的修复版布局日志确认：旋转时锚点最终为 933px，浮层自动从过渡尺寸更新到 933px，两者最终左上角均为 (498px, 66px)，无需输入或重开。日志 `notes-search-width-fixed.stdout.log` 在该模拟器 data/tmp 下；`onSizeChanged` 在布局后通知，不宣称所有过渡帧都同步。

设备流程通过两种横屏方向、连续旋转、长查询保留、关闭重开与清空后的对齐检查；空草稿弹层旋转未闪退，随后取消。移除全部诊断代码后重新构建安装，最终包再次通过竖屏展开→两个横屏方向→正常竖屏和关闭重开检查。没有修改正式截图基线。真机、iPad、其他 iOS 版本、软键盘完整流程及 Android 设备旋转仍待验收。

## 初始调查实际执行

源码检查基于 `bec1a268`，开始时工作区干净。设备操作使用此前已安装的 Notes Debug 应用，本次没有重新构建／安装，也没有校验安装二进制与该提交的对应关系。设备为 iPhone 18 Pro 模拟器、iOS 27.0；结论不外推至真机、Release、其他 iOS 版本或 iPad。未修改模拟器分辨率或密度。

| 场景 | 结果 | 证据与限制 |
|---|---|---|
| 竖屏展开搜索再转横屏 | 闪退 | 前序同会话实际复现，首页 key 重复；日志 `notes-search-target.stdout.log` |
| 横屏直接展开搜索 | 本次正常 | 前序对照，只证明该次打开正常 |
| 横屏展开搜索再转正常竖屏 | 闪退 | 前序同会话实际复现，首页 key 重复；日志 `notes-search-rotation.stdout.log` |
| 搜索收起时横竖屏切换 | 本次正常 | 前序同会话对照，不是全量旋转验收 |
| 横屏打开写邮件，再转另一横屏方向和正常竖屏 | 转正常竖屏时闪退 | 本次实际复现，首页 key 重复；日志 `notes-common-audit.stdout.log` |
| 设置退出确认框：竖屏→两种横屏方向→正常竖屏 | 未闪退，返回后可取消 | 本次实际执行；没有确认退出，保持登录 |

iPhone 不支持的倒置竖屏只经过设备框旋转，不计为应用竖屏布局验收。结束时设备恢复正常竖屏，设置确认框已取消。

日志在模拟器数据目录 `~/Library/Developer/CoreSimulator/Devices/6339160B-A0ED-4AD3-B1F9-F0B99921100F/data/tmp/`，属于临时证据，不纳入仓库。共性结论与关键异常已在本文提炼。

前序连续旋转后再次展开搜索还捕获一次独立异常：`FullScreenSearchBarLayout` 创建宽 `48698575`、高 `48697688` 的 `Constraints` 失败。日志为 `notes-search-portrait.stderr.log`。不能假设它与 key 重复有同一根因；修复后需单独覆盖旋转后重新打开搜索。

## 初始静态检查范围

| 位置 | 检查结果 |
|---|---|
| `AppNavigationState` / `AppNavHost` / `TabFadeThrough` | 每个根有独立返回栈与装饰器，只有当前根绘制 NavDisplay；Tab 顺序淡出／淡入。未发现手写第二个并行 NavDisplay 或直接重复调用 SaveableStateProvider 的路径 |
| `AppSceneStrategies` | 首页跨窗口断点时从 SinglePane 切到 ListDetail；这是真实的内容位置变化，不能按普通重组理解 |
| `DeviceCornerNavDisplay` / `SafeAreaSceneStrategy` | 委托 Scene 的 entries/key，包装 content；交由 NavDisplay 与 rememberSceneState 处理 Entry 搬移。需要隔离包装器与底层搬移的责任，目前不能宣称包装器有错 |
| `EmailSearchBar` / `MeasuredSearchInput` | 初始展开值只读取一次，测量输入使用独立空状态。展开态随宽度更换 Dialog／Popup；两种形态与 Scene 同时切换，是已复现入口 |
| iOS `DraftModalBottomSheet` / `DraftSheetLayout` | `key(viewport)` 在视口变化时重建 Dialog，SheetState 在外部保留；已复现入口。尚未证明 key(viewport) 是根因，不能直接删除作为修复 |
| `EmailList` / `EmailTopBar` | 首页还包含多选删除确认框，需列入同一 Scene 搬移回归；收藏页当前没有搜索与写邮件入口，不把它们列成实际可复现功能 |
| `SimpleDialog` / 设置选择项 / `SelectIconButton` | 包含 AlertDialog、底层 ListPreference 弹窗和 DropdownMenuPopup；设置退出确认框已做对照，其余只做静态检查，未完成设备旋转验收 |
| 保存状态与窗口事实 | 没有新增额外 SaveableStateHolder；Feature 消费组合根窗口 locals。AppViewModel 显式启用小型状态保存，搜索、草稿、登录状态仍按既有约束不做整份持久化 |

## 初始 Android 宿主回归

使用项目 README 指定的已有 JBR 25，实际执行以下命令，任务重新执行、结果通过：

```bash
env JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
  ./gradlew :composeApp:testAndroidHostTest \
  --tests '*AdaptiveUiTest.expandedSearchKeepsQueryWhenWindowWidthChanges*' \
  --tests '*AdaptiveUiTest.measuredSearchWidthShrinksAndRecoversWithWindow*' \
  --tests '*AdaptiveUiTest.restoredSearchUsesEmptyQueryMeasuredWidth*' \
  --tests '*AdaptiveUiTest.tabStateRestoresInactiveStacksAndScrollAfterRecreation*' \
  --tests '*AdaptiveUiTest.searchResultNavigationKeepsSearchClosedAfterReturnAndRotation*'
```

初始调查的 JUnit XML 共 85 项：7 通过、78 按场景条件跳过、0 失败、0 错误。覆盖搜索宽度往返、恢复长查询、多返回栈恢复和结果导航返回后的旋转；不覆盖 iOS Scene Layer。初始调查没有连接的 Android 设备，未运行 iOS 编译／自动测试；后续修复验证另见本文上方。未采集或批准视觉基线。

## 剩余关闭条件

1. 在其他 iOS 环境和真机复核已修复的搜索／写邮件旋转入口，包含软键盘显示期间的焦点与内容可达性。
2. 当前模拟器的查询／草稿文本、关闭重开、导航返回及连续旋转已通过，不外推为完整输入与视觉验收。
3. 回归首页多选确认框、设置选择弹窗与登录下拉菜单，区分实际 Scene 搬移和同 Scene 窗口调整。
4. 单独追踪超大 Constraints 异常；重复 key 消失不等于该异常已解决。
5. Android 验证常规 Activity 重建及不重建的窗口变化；iOS 自动 Entry 调用测试不能替代弹层搬移的设备流程。
