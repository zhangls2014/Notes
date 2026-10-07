# iOS 弹层与 Scene 旋转共性审计

日期：2026-10-07。状态：首页根 Scene 的无效退场动画已修复；当前源码 Debug 包的 iPhone 模拟器旋转回归通过。其他设备、软键盘与独立 Constraints 异常的完整验收仍待完成。

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
