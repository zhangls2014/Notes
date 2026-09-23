# 折叠屏铰链避让与姿态稳定性检查

日期：2026-09-23。基线：`f640d0f6`。状态：方案已实施并完成自动化、Android/iOS 编译和折叠模拟器验证。

## 实施结果

- `core:theme/layout` 现在统一提供窗口像素坐标下的安全区域、稳定 directive 和可复用安全容器。竖向窗格明确使用 `HingePolicy.AlwaysAvoid`，并规范化乱序、重叠和零宽折痕。
- 登录页的横向折叠按真实铰链区域分配品牌与表单，不再依赖 Adaptive 库尚未实现铰链避让的 `Reflow`。
- Nav3 列表/详情仍由场景策略装配；横向折叠先限定安全带，单页和设置页再从实际安全区域布局。导航栏/导轨也放进物理安全区域。
- 搜索、写邮件、设置选择和确认对话框在折叠设备上使用同一安全弹层；查询、滚动位置、草稿和选择状态留在形态分支之外。
- tooltip popup 在存在铰链时降级为保留无障碍标签的普通图标按钮，避免窗口级提示跨越铰链。
- 铰链适配现在与普通路径严格隔离：空铰链列表不创建安全布局节点，也不替换搜索栏、设置控件、底部弹层、导航套件或确认对话框。

验证结果见文末；以下问题章节保留为修复前的根因记录。

## 验收边界

窗口尺寸、可见屏幕和铰链几何位置相同时，半开与全开只改变折叠状态，不应改变内容窗格的数量、位置、尺寸或导航形态。旋转、分屏、小窗缩放、内外屏切换确实改变了可用几何时，应重新布局，不能冻结旧尺寸来掩盖变化。

交互内容不得横跨任一铰链，包括宽度/高度为零的折痕。必须处理多条铰链，以及窗口坐标到内容局部坐标的转换。系统不报告的铰链无法可靠推导；不能缓存上一次折痕去替代旋转、小窗或屏幕切换后的真实窗口数据。

## 已确认问题

### 1. 全局 directive 随分离状态改变避让区域（高优先级）

位置：`core/theme/src/commonMain/kotlin/me/zhangls/theme/layout/WindowAdaptiveInfo.kt:116`。

`calculatePaneScaffoldDirective(adaptiveInfo)` 默认使用 `HingePolicy.AvoidSeparating`。依赖源码只把 `isSeparating` 的竖向铰链放进 `excludedBounds`。软折痕半开时可能分离、全开时不分离，因此同一位置的折痕可能从避让区域消失。登录页、首页/收藏的列表与详情共同受到影响。

修复：统一明确使用 `HingePolicy.AlwaysAvoid`；保留全部竖向铰链，按几何位置排序。不要用 `isFlat` / `isSeparating` 过滤显示区域。

### 2. 登录页的横向折叠避让并未实现（高优先级）

位置：`feature/login/src/commonMain/kotlin/me/zhangls/login/LoginScreen.kt:165`、`:180`。

登录页用 `isTabletop` 决定上下 Reflow 或左右/单窗格；同尺寸横向折痕从半开变全开时布局必然切换。更重要的是，当前 `adaptive-layout:1.3.0-rc01` 的 `measureAndPlacePanesInPartition` 使用可用高度和窗格首选高度分配上下区域，源码仍有 `TODO(conradchen): Avoid hinges`，没有读取真实横向铰链坐标。因此现有注释“Reflow 避开横向铰链”不成立。铰链偏离分配线、有厚度或存在两条横向铰链时都不能依赖这个算法。

修复：横向折痕是否存在按几何判断，半开/全开均保留同样分区。按实际横向安全矩形测量、摆放内容，而不是用 Reflow 近似避让。同时更正相关注释和测试。

### 3. 导航外壳本身会随姿态换形态（高优先级）

位置：`feature/main/src/commonMain/kotlin/me/zhangls/main/AppShell.kt:129`。

`NavigationSuiteScaffoldDefaults.navigationSuiteType(this)` 包含 tabletop 分支。现有 `NavigationSuitePolicyTest` 明确要求 1000×800 半开时为底部栏；同尺寸普通姿态为 Rail。即使只修复 `excludedBounds`，外壳仍会改变内容区宽高，导致窗格跳动。

修复：折痕几何相同时使用稳定的导航决策；存在横向折痕时半开/全开都使用底部导航，并确保底部导航落在实际安全区域内。普通设备继续使用库的尺寸策略。

### 4. 设置页的宽度上限不提供铰链避让（高优先级）

位置：`feature/settings/src/commonMain/kotlin/me/zhangls/settings/SettingsScreen.kt:51`、`:61`。

设置页走普通单页场景，`AdaptiveContent` 仅“居中 + 宽度上限”，不读取铰链。列表行和标题仍可能跨过窗口中央折痕。邮件列表/详情虽然有竖向窗格脚手架，横向折痕仍未被该脚手架处理。

修复：单页和横向折叠场景使用统一的安全区域容器；先确定安全显示区域，再施加可读宽度。不要把铰链处理混进 `ContentWidth`。

## 尚需渲染验证的风险

| 页面/元素 | 当前情况 | 必须验证或补齐 |
|---|---|---|
| 搜索 | `EmailSearchBar.kt:82` 按窗口宽度选 fullscreen/docked；无显式铰链约束 | 列表窗格较窄时搜索展开的可用区域；弹出内容不可越过铰链；旋转时保留查询和展开状态 |
| 写邮件 | `NewEmailSheet.kt:73` 使用窗口级 `ModalBottomSheet` | 不会自动继承列表窗格边界；需按弹层宿主坐标避让，覆盖键盘展开 |
| 退出确认 | `SimpleDialog.kt:82` 使用普通 `Dialog` | 独立窗口居中可能落在折痕上；单页容器的约束不会自动约束它 |
| 设置选项 | `PreferenceRow` 委托依赖库的 `ListPreference` | 检查依赖内部弹窗，使用本模块包装提供安全显示区域，避免实现库外泄 |
| 登录下拉菜单 | 使用锚定的 `DropdownMenuPopup` | 校验弹出方向、边界回退及窗口/局部坐标；不能仅凭锚点安全就断言菜单安全 |
| 三折屏 | directive 支持矩形列表，但当前没有多铰链回归 | 库的竖向分区实现假定矩形按左到右排列且不重叠；入口规范化，覆盖乱序、两铰链、零宽折痕和窗口裁剪 |

上述弹层属于缺少适配保证的风险；本次没有设备截图证据，不将其描述为已实机复现。

## 推荐修复方案

保留现有模块边界与唯一窗口读取点，在 `core:theme/layout` 统一提供安全几何和布局策略，组合根负责向下提供。feature 契约与 MVI State 不增加窗口参数。

1. 统一竖向 `AlwaysAvoid`，对多铰链规范化排序；按实际窗口坐标裁剪，不缓存历史铰链。
2. 提供横向/单页安全区域容器，按实际边界切分；同几何使用确定性顺序。单页优先选最大可用区域，面积相同时按固定阅读顺序选择，避免姿态触发位置变化。
3. 登录页在横向安全区域中安放品牌和表单；区域不足时优先表单并允许滚动。列表/详情继续由 Nav3 场景装配，不在 feature 内创建第二套导航或窗格系统。横向折叠时宿主限制到安全带，再由场景决定该带内布局。
4. 设置页整体（含顶栏）限制到安全区域。导航外壳使用同几何稳定策略。
5. 弹层在自己的坐标空间应用相同安全几何规则；背景遮罩可以铺满，正文和交互控件限定在安全区域。键盘影响可用高度，不改变铰链事实。
6. 对布局切换期间也检查内容边界，防止动画从铰链上穿过。窗口实际改变时允许必要重排；半开/全开状态变化本身不触发重排。

方案比较：仅改 `AlwaysAvoid` 改动最小，但不能满足横向折叠、设置和弹层要求；逐页写避让逻辑会重复并易漂移；推荐上述统一几何方案，覆盖范围完整且符合项目的 core/feature 边界。

## 验证矩阵

- 普通手机：360×800、411×914、914×411，检查滚动、IME、导航以及状态恢复。
- 平板和小窗：宽度 599/600、839/840、1199/1200、1599/1600 的边界前后，连续拖动缩放。
- 单折痕：竖向/横向、零厚度/有厚度、居中/偏心；固定几何切换半开/全开，断言安全矩形、窗格与导航形态相同。
- 三折：两条竖向与旋转后的两条横向铰链；乱序、不同折叠状态、部分在窗口外；不遗漏任何可见铰链。
- 顶栏、导航和系统 insets 改变原点后，验证转换后的铰链位置；覆盖 RTL。
- 搜索、设置弹窗、写邮件在打开时旋转/折叠/缩窗并唤起键盘；验证内容、焦点、选中项与草稿状态。
- 现有 `LoginPanePlanTest` 只验证 Reflow 枚举和竖向 excludedBounds，不验证实际像素边界；应增加几何和 UI 层检查，不能用旧测试通过证明铰链安全。

实施后串行运行相关 commonTest、Android 构建和 iOS 编译。UI 结论需通过折叠模拟器或真机验证；三折硬件缺失时明确标记为模拟姿态验证。

## 验证结果

- `:core:theme:testAndroidHostTest`、`:feature:login:testAndroidHostTest`、`:feature:main:testAndroidHostTest` 通过；覆盖半开/全开稳定性、偏心横向铰链、两条乱序铰链、零宽折痕、窗口裁剪和局部原点。
- `:androidApp:assembleDevFullDebug` 与 `:composeApp:compileKotlinIosSimulatorArm64` 通过。
- Android 折叠模拟器（2208×1840）在竖向铰链与旋转后的横向铰链上，半开/全开布局树逐字节一致。语言选择弹层保持打开并固定在同一安全窗格。
- 900×1600 紧凑显示区域与 IME 场景已目视检查，表单保持滚动可达。
- 模拟器只提供单折设备；三折结论来自双铰链纯几何测试，没有冒充三折真机验证。

本次隔离重做先反向应用原实现提交，恢复普通 UI 基线，再从该基线重新加入铰链分支。`HingeGeometryTest` 增加空铰链与相关方向过滤断言；普通路径继续使用原 Material 组件。最终构建与模拟器证据以本次任务的验证记录为准。

隔离重做的本次模拟器检查覆盖：CLOSED 外屏上的首页搜索框、设置列表与原生 Preference 选择弹窗，以及 HALF_OPENED 内屏上的安全搜索弹层。模拟器在随后切换 OPENED 时从 ADB 断连；重启后未在本次验证窗口内重新注册，因此没有把这次 HALF_OPENED → OPENED 连续切换记为通过。测试期间没有设置 `wm size` 或 `wm density` Override；断连前查询只显示 Physical 值。

## 本次证据范围

已核对项目入口、登录、首页、收藏、详情、搜索、设置、写邮件、通用对话框、设置控件、Manifest 和已有布局测试；并读取项目实际缓存版本的 Adaptive 源码。旧的 `docs/adaptive-layout-audit.md` 是历史审查，其“没有统一窗口来源”等描述已不符合当前代码，不应当作本次现状。

官方 API 对 `AlwaysAvoid` 的定义：[HingePolicy](https://developer.android.com/reference/kotlin/androidx/compose/material3/adaptive/layout/HingePolicy)。当前实现细节以本地 `org.jetbrains.compose.material3.adaptive:adaptive-layout:1.3.0-rc01` 源码为核查依据。
