# 无障碍适配审计

- 日期：2026-10-02
- 状态：审计第 1–5 项均已实施修复；第 4 项通过新增个人信息页对齐标签与实际导航动作。修复验证见文末记录；个人信息功能后续已拆分至 profile-api / profile。
- 范围：共享 Compose UI、Android Manifest、iOS 宿主与提示消息、既有宿主测试、Android 当前界面的无障碍节点。

## 结论

审计时应用具备基础无障碍支持，但不能认定已完整适配。标准 Material 控件、按钮标签、输入框标签和部分选择状态已经接入语义；系统字号被应用字号覆盖是明确缺陷。自定义加载层、iOS 短时提示消息和交互动作说明仍有缺口。以下发现保留审计时证据，后续修复记录见文末。

这是源码与有限运行验证的结论，不是 TalkBack / VoiceOver 完整验收。Android 当前安装包与工作树源码的版本一致性未验证，因此节点快照只作辅助证据。

## 发现

### 1. P1：主题覆盖系统字号

- `core/theme/src/commonMain/kotlin/me/zhangls/theme/Theme.kt:26-29` 读取外层 density 后，只保留 `density.density`，将 `fontScale` 替换为调用参数。
- `composeApp/src/commonMain/kotlin/me/zhangls/entry/App.kt:44` 传入 `state.fontSize.value`。
- `core/model/src/commonMain/kotlin/me/zhangls/model/FontSizeConfig.kt` 仅提供 1 / 1.5 / 2，默认设置为 1。

因此系统字体放大不能传入主题子树。应用内字号选项不能替代用户的系统无障碍偏好。建议保留系统缩放，并明确应用内字号是相对倍率还是提供“跟随系统”的策略；对组合后的极端字号验证登录、搜索、导航、设置和弹窗。

既有 `AdaptiveUiTest` 直接向主题注入 1.5 / 2，绕过 `App()`；它证明应用内缩放场景，不能证明系统字号跟随。`docs/testing/adaptive-ui.md` 另已记录 Android 独立 Dialog / Sheet window 未必继承应用内字号的边界。

### 2. P2：登录加载层未约束无障碍与键盘操作

- `core/theme/src/commonMain/kotlin/me/zhangls/theme/component/ContainedLoadingIndicator.kt:27-38` 添加普通 Box 和 `pointerInput`，没有模态焦点隔离或隐藏底层语义。
- `feature/login/src/commonMain/kotlin/me/zhangls/login/LoginScreen.kt` 在页面之外叠加加载层；登录按钮仅检查 `state.isInputValid`，输入框没有因 `isLoading` 禁用。
- `LoginViewModel.handleIntent()` 对登录动作也没有检查正在加载。

指针层不能约束读屏直接执行语义动作或键盘激活，加载时底层表单仍可用。建议加载期间显式禁用表单及登录动作，补充有名称的加载状态，并验证焦点和状态播报。Material 指示器本身可能已有进度语义；本发现不以“缺少自定义 semantics”推断其无语义。

### 3. P2：iOS 提示消息缺少主动播报及 Dynamic Type

- `core/framework/src/iosMain/kotlin/me/zhangls/framework/toast/SystemToast.ios.kt:82-85` 只显示独立 UIWindow 和淡入 UILabel，没有发出无障碍 announcement。
- `:104` 使用固定 14pt 系统字体，没有 preferred text style 或动态字体缩放。
- 提示仅显示 2 / 3.5 秒。

源码未提供可靠的 VoiceOver 自动播报路径，用户可能错过登录失败、保存反馈等消息；固定字体也不跟随 Dynamic Type。建议采用原生无障碍播报 API 和动态字体，并为重要结果提供可再次读取的界面状态。是否在当前系统上偶然被自动朗读仍需 VoiceOver 验证。

### 4. P2：头像选择器标签与实际动作不一致

`feature/email/src/commonMain/kotlin/me/zhangls/email/component/AvatarPicker.kt:44-49` 点击打开图片文件选择器，标签却引用 `email_action_owner_info`（中文“个人信息”，英文“Profile”）。Android 当前展开搜索界面的节点也暴露为“个人信息”。

建议使用“更换头像”资源，并赋予合适的按钮角色或动作说明。头像可保持 40dp 视觉尺寸，将交互布局区域明确设为至少 48dp。

### 5. P3：邮件卡片操作缺少具体动作说明

`feature/email/src/commonMain/kotlin/me/zhangls/email/component/EmailListItem.kt:47-51` 的 `combinedClickable` 未设置 `onClickLabel` / `onLongClickLabel`。单击在普通模式打开邮件，在多选模式切换选择；长按也切换选择。已有 `selected` 语义，长按本身也有 Foundation 默认动作，因此不能说读屏无法长按。

缺口是读屏无法从动作提示明确知道长按用于选择，以及当前单击的含义。建议按模式和选择状态提供本地化动作说明，评估多选模式的选择控件角色。

## 已有基础

- 登录输入框有 Account / Password 标签、校验支持文本和 `isError`；清空账号、显示 / 隐藏密码按钮有本地化描述。
- 返回、收藏 / 取消收藏、新建邮件等按钮提供描述；邮件列表有 `selected` 语义。
- 主题和语言菜单使用带 checked 状态的 Material 菜单项；设置依赖标准 Preference 控件。
- 登录、邮件详情、设置和草稿内容采用滚动容器。既有宿主测试覆盖部分大字号、键盘搜索入口、输入操作和弹层按钮可达性。
- 装饰性图标、已有文字标签的收件人头像和用于测量的不可见搜索副本隐藏语义是合理做法。
- Android Manifest 启用 RTL；iOS 使用标准 `ComposeUIViewController`，源码未主动停用 Compose 无障碍桥接。

## 配色与触控抽查

根据 `core/theme/Color.kt` 的不透明 sRGB 值计算内置配色的相对亮度比：

| 前景 / 背景 | 浅色 | 深色 |
| --- | ---: | ---: |
| onSurface / surface | 16.33:1 | 14.32:1 |
| onSurfaceVariant / surfaceContainerLow | 8.45:1 | 10.11:1 |
| onPrimary / primary | 6.48:1 | 7.70:1 |
| onPrimaryContainer / primaryContainer | 7.21:1 | 7.21:1 |
| error / surface | 6.14:1 | 10.93:1 |

这些是主要文本组合抽查，不涵盖动态配色、透明叠加、禁用态、所有边框、图片或系统高对比度模式，不能当作整页合规证明。

头像 40dp、历史删除按钮 `AssistChipDefaults.IconSize` 的视觉布局尺寸偏小，需要验证相邻目标是否重叠。但 Compose 会扩展小控件的触控目标，当前 Android 节点也呈现扩展边界，因此不将视觉尺寸直接判为实际触控区域不达标。历史删除动作还可补充对应关键词，以区分多个“删除”。

## 验证记录

- Android `emulator-5554` 当前前台为 Notes，系统 font_scale=1.0，enabled_accessibility_services=null。
- 读取当前展开搜索页的 UIAutomator 节点：可见搜索、关闭搜索、个人信息、历史词 Package 和删除动作。
- 未开启或修改系统读屏与字号，未修改模拟器显示尺寸或密度；没有进行端到端读屏验收。
- 定向宿主测试构建成功（BUILD SUCCESSFUL，17 秒）：`ExpressiveThemeTest`，以及 `AdaptiveUiTest` 的 `loginInputActionsAreAccessible`、`collapsedSearchRemainsKeyboardAccessible`、`draftActionsRemainReachableAboveIme`、`settingsLogoutDialogKeepsCancelAndConfirmReachable`。测试 XML 记录总计 70 项：实际执行 10 项、跳过 60 项、失败和错误均为 0；跳过来自参数化场景的 Assume 条件。
- 审计执行命令：`ANDROID_HOME=/Users/zhangls/Library/Android/sdk ./gradlew :composeApp:testAndroidHostTest --tests '*ExpressiveThemeTest*' --tests '*AdaptiveUiTest.loginInputActionsAreAccessible*' --tests '*AdaptiveUiTest.collapsedSearchRemainsKeyboardAccessible*' --tests '*AdaptiveUiTest.draftActionsRemainReachableAboveIme*' --tests '*AdaptiveUiTest.settingsLogoutDialogKeepsCancelAndConfirmReachable*' -Dnotes.screenshots.candidates=true --console=plain`。实际任务从 Gradle project property 读取截图选项；后续修复回归使用正确的 `-Pnotes.screenshots.candidates=true`，确保截图写构建候选目录。

## 后续验收

1. 修复系统字号策略后，从系统设置放大字体验证实际 `App()` 路径，补充回归测试，覆盖弹窗字体。
2. TalkBack / VoiceOver 完成登录、搜索、详情、收藏、多选、草稿、设置和退出登录流程，检查顺序、焦点恢复、错误与加载播报。
3. 外接键盘和 Switch Access / Full Keyboard Access 验证激活、退出弹层与焦点隔离。
4. 测量小控件相邻触控区域，检查大字号及高对比度状态，并在 iOS 检查 Dynamic Type 与 Reduce Motion。

## 前两项修复（2026-10-02）

- 系统字号作为基础，应用中／大字号作为相对倍率。标准字号复用系统 Density，保留平台转换实现；中／大字号通过 Compose 的 Density 组合 fontScale，保留 Android 库的非线性字体换算。
- 登录 UI 在加载时禁用输入、附属图标按钮和提交按钮，释放输入焦点并隐藏键盘。ViewModel 在启动协程之前同步设置 isLoading；加载期间拒绝排队的重复提交和表单修改，失败及取消通过 finally 恢复。
- 加载指示器提供中英文名称及 polite live region，保留 Material 进度语义；移除不能约束语义操作的指针层，主题／语言入口仍可操作。
- 新增回归测试先在旧实现失败：系统 1.3 被覆盖为 1.0、三次排队提交产生三次调用、加载期间字段可修改、登录按钮仍启用。
- 修复后的扩大宿主回归实际执行 48 项、跳过 60 项、0 失败，包括主题 3 项、登录业务 2 项、加载 UI 1 项、自适应 UI 42 项。随后新增系统 2×应用 2 的登录可达性测试；最终 7 项定向测试、iOS 模拟器 Kotlin 编译、Android 整包构建及 lint 均成功，lint 为 0 Error / 10 Warning（依赖版本、AGP 和既有 SDK 属性提示）。独立只读代码审查未发现阻塞问题。
- Android 独立 Dialog / Sheet 的应用倍率继承仍是既有边界；未将其、iOS Toast 或完整读屏验收计入本次修复。

## 第三项修复（2026-10-02）

- iOS 标签改用 `UIFont.preferredFontForTextStyle(UIFontTextStyleSubheadline)` 并启用 `adjustsFontForContentSizeCategory`；字号变化通知在主队列请求窗口根视图重新布局，关闭时移除通知观察者。
- 显示协程在淡入 250ms 后发送 `UIAccessibilityPostNotification(UIAccessibilityAnnouncementNotification, text)`，保持原有 2／3.5 秒显示时长。发送前确认窗口可见且场景处于前台；提示替换或 presenter 关闭会取消等待中的播报。
- 无障碍通知使用 announcement，不使用 screenChanged／layoutChanged 或主动请求焦点。系统对播报的声音和调度仍由 VoiceOver 决定。
- 原生回归测试在旧实现上以“Toast must track Dynamic Type changes”失败。修复后 6 项 iOS 测试通过：既有布局 4 项、文本样式与自动缩放配置 1 项、真实 UILabel 从 Large 切换到 AccessibilityExtraExtraExtraLarge 后字体增大 1 项。`composeApp:compileKotlinIosSimulatorArm64` 成功。
- 独立只读审查检查了替换、取消、场景断开、通知观察者清理和播报焦点行为，未发现阻塞问题。
- 原生独立测试没有验证实际声音、读屏焦点或真实场景中的提示替换播报；这些验收步骤记录在 [iOS Toast 无障碍验证](../testing/ios-toast-accessibility.md)。本次未扩大到重要消息的持久化反馈设计。

## 第五项修复（2026-10-02）

- `EmailListItem` 增加本地化 `onClickLabel` 与 `onLongClickLabel`：普通模式点击“打开邮件”，多选模式按状态提示“选择邮件”或“取消选择邮件”，首页长按也按选中状态提示对应操作。
- 选择回调改为可空，以 null 表示不支持选择。收藏列表传 null，不再暴露原先回调直接返回的无效长按动作；点击仍打开邮件。
- 保留邮件文本、selected 状态和独立收藏按钮语义，没有用整张卡片的 contentDescription 覆盖后代内容。
- 新增 `EmailListAccessibilityTest` 通过真实分页列表验证模式切换、选中状态、点击和长按回调、独立收藏动作，以及中英文标签；旧实现的 3 项测试均按预期失败（缺少动作标签、收藏长按仍存在）。修复后邮件模块 7 项宿主测试全部通过（3 项新增语义测试、4 项既有草稿弹层测试），`composeApp:compileKotlinIosSimulatorArm64` 成功。未执行真实 TalkBack / VoiceOver 流程。

## 第四项修复（2026-10-03）

- 按用户选择新增独立个人信息页：设置列表首项「个人信息」与首页搜索栏头像进入同一页面，展示 `UserModel.nickname` 与当前头像，用户名只读；明确的「更换头像」按钮启动系统图片选择器。搜索结果发件人已有文字标签，其头像隐藏重复描述，避免误报为个人信息入口。
- 搜索头像使用显式 48dp IconButton 并将描述放在按钮上，头像保留 40dp 视觉尺寸；页面更换按钮最小高度 48dp。页面可滚动、遵循共享内容宽度，2×字号与 400×320dp 的短窗口测试覆盖用户名和按钮可达性。
- `ProfileDestination` 保存入口来源，设置与首页各自在自己的 Tab 栈中导航和返回；页面受登录保护并登记 NavKey / Destination 序列化。展开搜索进入前等待收起并清空查询，返回不恢复搜索弹层。
- 头像状态与保存职责迁至 settings，文件每次保存到唯一私有路径。新头像原子持久化后才删除旧文件；用户或原头像变化时拒绝提交，失败/取消清理新副本。保存中禁用重复选择，状态以 polite live region 呈现，失败可重试。Android 文件复制修复空输入流误报成功和输出流未关闭的问题。
- 回归测试先复现设置入口缺失和搜索头像不进入个人信息页。settings 的 14 项宿主测试覆盖保存成功、失败、取消、账户变化、重复操作、重试、原文件回收、私有路径和页面语义。组合根定向宿主回归实际执行 22 项、跳过 45 项、0 失败，包含个人信息的两个入口、展开搜索返回、返回栈恢复、登录保护及既有搜索/设置行为。
- iOS 组合根 24 项模拟器测试与 data 模块 8 项实际测试通过（另 1 项跳过），包含新增目的地序列化、入口 ABI 和仓库的原子头像更新；Android 整包构建与 lint 通过。lint 0 Error / 12 Warning，均为依赖/AGP 版本和既有清单属性提示。独立只读审查未发现具体缺陷。
- 后续按用户要求将个人信息改为次级全屏页面：显示期间隐藏底部导航与大屏侧栏，仍保留所属 Tab 返回栈和 Entry 状态；返回时恢复对应导航。扩充后的 4 项个人信息宿主测试覆盖手机与 1200×900dp 大屏、两个入口、恢复及顶部/系统返回，与 13 项既有导航/序列化测试合计 17 项通过，iOS 编译通过。
- 后续独立模块拆分将个人信息页面、MVI、头像存储和宿主测试迁至 `feature:profile`，入口/目的地/导航贡献迁至 `feature:profile-api`；settings 保留点击事件，composeApp 统一装配。目的地保留旧序列化名称，升级后的返回栈与登录重定向继续兼容。
- 已检查更新后的全屏候选页面截图；真实系统图片选择器和 TalkBack / VoiceOver 的完整流程尚未进行人工验收。

## 参考

- [Compose 默认无障碍行为、触控目标与动作标签](https://developer.android.com/develop/ui/compose/accessibility/api-defaults)
- [Compose Multiplatform iOS 无障碍支持](https://kotlinlang.org/docs/multiplatform/compose-ios-accessibility.html)
- [UIKit 无障碍 announcement](https://developer.apple.com/documentation/uikit/uiaccessibility/notification/announcement)
