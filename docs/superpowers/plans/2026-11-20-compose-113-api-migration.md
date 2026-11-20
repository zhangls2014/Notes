# Compose 1.13 API 迁移实施方案

状态：维护者已于 2026-11-20 确认具体方案及捕获退出画面行为；五项实现已落地，双端行为回归通过，静态门禁与设备/正式视觉验收仍有缺口。

**Goal:** 完成 computedStateOf、失败诊断、捕获退出动画、TextFieldState 和六处 FlexBox 迁移，保留既有业务、布局和凭据边界。

**Architecture:** 在现有 Feature/API/Core 边界内修改。页面 ViewModel 持有内存中的输入编辑状态，core:theme 只接收状态与渲染参数；MVI 保留校验、请求及流程状态。输入的权威值为 TextFieldState，不用延迟到达的 StateFlow 字符串持续反向覆盖编辑器。

**Tech Stack:** 已升级的 Kotlin 2.4.21、Compose Multiplatform 1.13.0-rc01、Android Compose 1.13.0 / Material 3 1.5.0、现有 JUnit4/Robolectric/Roborazzi。

执行方式：确认后在当前 worktree 串行实施，不派发子代理，不并发执行 Gradle。此前强制升级的未提交改动完整保留；不混入无关静态告警清理。

## 范围与保持项

- 处理前次文章审查的全部五项；不引入 Blur、PolygonShape、A2UI 或新的网页引擎。
- LazyColumn 继续使用新版默认 cacheWindow，不凭文章样例设置缓存比例，不宣称未经测量的性能收益。
- 保留描边输入框的颜色、形状、字号、标签、错误提示、按钮、字段顺序、8dp 间距、尾端对齐和现有安全区/IME 策略。
- 保留账号自动填充、密码显隐、Tab/Next/Done、加载禁用及业务侧拒绝重复提交。密码默认完整遮蔽，明确显示时才显示；不自动采用 System 或 RevealLastTyped 改变当前体验。
- 输入对象不放入 SavedStateHandle、rememberSaveable、可序列化 MVI 状态或仓库；编辑草稿仅在原导航条目/编辑会话生命周期内存活。
- 不新增依赖、不改 CI/凭据/数据库，不删除文件、不合入 master、不公开发布、不更新正式截图基线。

## 1. 导航派生状态

Files: `feature/main/src/commonMain/kotlin/me/zhangls/main/AppShell.kt`。

- [x] 将唯一一处 `derivedStateOf { navigationProgress() > 0f }` 换为 `computedStateOf`，保留 remember key、measure policy 和可见性规则。
- [x] 使用既有导航显隐、预测返回、Tab 切换与布局用例验证 0/非0 阈值；保留布局阶段读取连续进度的实现。

## 2. UI 测试失败诊断

Files: `composeApp/src/androidHostTest/kotlin/me/zhangls/entry/ComposeTestDiagnostics.kt`（新建测试专用配置）；调用 createComposeRule 的宿主测试；`composeApp/build.gradle.kts`（仅必要的测试报告路径设置）；`docs/testing/adaptive-ui.md`。

- [x] 创建测试专用的 ComposeUiTestConfig / TestFailurePolicy 配置，失败时保存截图和语义树到模块 build 目录；文件名来自测试类与用例，避免参数用例互相覆盖。
- [x] 先用一个含预期失败的测试探针验证 Robolectric 下两种产物均可读、原始异常仍被抛出，成功测试不产生失败产物；探针使用假数据，不保存真实账号画面。
- [x] 再接入现有 Compose 宿主测试。保留自定义 Activity、原调度器和 MainLooper 清理规则；若 v2 默认调度器不同，显式匹配当前调度策略，不靠延时或放宽断言迁就迁移。
- [x] 验证原超时路径及普通断言路径，确认截图采集异常不会覆盖原始失败；文档说明产物路径，不改 CI 配置。

## 3. 捕获资料编辑的退出动画

Files: `feature/profile/src/commonMain/kotlin/me/zhangls/profile/ProfileIdentityEditor.kt`；相应资料编辑宿主测试。

- [x] 将外层 SlidingProfileContent 迁移到 CapturedAnimatedVisibility，保留进入 300ms、退出 200ms、原展开/收缩/位移曲线。
- [x] 去掉只为退出 Composition 保留数据的 closingEdit 局部快照；业务关闭时立即释放密码、验证码和编辑会话。
- [x] 明确画面变化：退出期间绘制关闭前最后一帧，最长为现有 200ms；已显示的验证码/明文密码模式也可能留在该帧中。退出内容不再响应手势、焦点或内部动画。此变化随本方案单独确认。
- [x] 覆盖关闭时状态清空、退出期间不可操作、动画完成后节点消失、快速关闭后重开使用新会话，以及用户名/邮箱不同步骤。
- [x] 不替换组合根 AnimatedPaneScope/保存状态动画；EmailFieldVisibility 的 ExitTransition.None 无捕获退出收益，保持原实现。

## 4. 文本输入迁移

Files:

- `core/theme/src/commonMain/kotlin/me/zhangls/theme/component/AccountTextField.kt`：提供状态式普通输入与安全输入，共享外观。
- `feature/login/src/commonMain/kotlin/me/zhangls/login/LoginScreen.kt`、`RegistrationScreen.kt` 及 `mvi/LoginViewModel.kt`、`RegistrationViewModel.kt` 和必要的 Action/Reducer/State：登录及注册会话输入。
- `feature/profile/src/commonMain/kotlin/me/zhangls/profile/ProfileIdentityEditor.kt`、`mvi/ProfileViewModel.kt` 及当前编辑数据：每次打开编辑生成会话状态，关闭/账号切换清空。
- `feature/email/src/commonMain/kotlin/me/zhangls/email/component/NewEmailSheet.kt`、`EmailList.kt`、`mvi/EmailViewModel.kt` 及必要的 Intent/State：邮件草稿输入和发送/取消后的清理。
- 对应 commonTest、Android 宿主和 iOS Entry 回归。

- [x] 先更新 architecture.md 的页面状态/表单规则：TextFieldState 属页面内存编辑状态；MVI 中若保留字符串，只作为业务快照，不持续驱动字段。
- [x] core:theme 输入组件改为接收 TextFieldState；普通字段使用 state-based OutlinedTextField，密码使用 OutlinedSecureTextField，显式指定当前遮蔽规则。IME 事件迁移到 onKeyboardAction，不复用已弃用 VisualTransformation。
- [x] ViewModel 持有字段的稳定实例，业务校验和提交读取编辑器当前值。现有清空/重置 Intent 显式编辑对应对象，不把状态放进序列化记录。
- [x] 需要逐字校验的地方单向观察 snapshotFlow；不得使用“编辑器 -> String -> 编辑器”的逐字回写。保持程序更新能清空/重置，且不因 busy、校验错误或重组重建编辑器、丢失选择与输入法组合文本。
- [x] 按登录、注册、资料编辑、草稿分批迁移及验证。搜索已有 TextFieldState，保留其独立 SearchBar 状态及 debounce，不顺带重写搜索业务。
- [x] 宿主回归覆盖文本输入/替换、清空、密码显隐、自动填充语义及高亮、IME/加载禁用、重复提交、旋转/条目重建、注册步骤、资料切换账号和草稿交互；真实中文组合输入与系统自动填充服务留在设备验收清单。
- [x] 用 SavedStateHandle 与宿主重建回归确认密码/验证码不跨进程保存；iOS 运行真实 Entry，不能只编译。

## 5. 六处 FlowRow 迁移

Files:

- `feature/email/src/commonMain/kotlin/me/zhangls/email/search/EmailSearchBar.kt`：搜索历史。
- `feature/email/src/commonMain/kotlin/me/zhangls/email/component/NewEmailSheet.kt`：收件人。
- `feature/profile/src/commonMain/kotlin/me/zhangls/profile/ProfileIdentityEditor.kt`：底部操作。
- `web/src/wasmJsMain/kotlin/me/zhangls/web/Pages.kt`：页面标签。
- `web/src/wasmJsMain/kotlin/me/zhangls/web/notices/NoticeDocument.kt`：两处声明标签。

- [x] 使用稳定 FlexBox 和 Wrap 配置等价布局；按各调用保留横纵间距、尾端对齐、顺序和现有内容宽度，不引入裸断点或新的窗口事实。
- [x] 明确 flex grow/shrink 与当前尺寸的对应，防止默认收缩改变按钮/Chip 宽度；保留内容溢出后的换行，核对 LTR/RTL 阅读与焦点顺序。
- [x] 通过窄窗、大字号、长标签、深浅色和旋转的行为/几何测试，再生成构建目录候选图人工比较；正式基线不更新。
- [x] 运行网站 Node 测试与 Wasm buildSite，检查两端声明入口和字体/脚本产物。

## 6. 验证与交付

- [x] 先跑受影响模块的 common/宿主测试，定点失败先定位，不关闭规则或抹去断言。
- [x] 串行执行 `./gradlew testAndroidHostTest iosSimulatorArm64Test --continue`，保留执行、复用、失败、跳过统计。
- [x] 执行 Android assembleDebug、assembleDevGlobalRelease/R8、lintDevGlobalDebug；iOS 两个 ARM64 编译及 Xcode 模拟器整包。
- [x] 对本次源码运行相应语义 detekt、theme/profile/login/email/main 普通 detekt；存量告警与新增行分开核对，不扩大为全仓清理。
- [x] 运行 `node --test web/build.test.mjs`、`./gradlew :web:buildSite -Pkotlin.js.yarn=false`；无新增/变更分发依赖时不重复修改许可证清单。
- [x] releases.json 更新未发布记录；验证证据和未验收设备项归入现有审计/集中清单，整理文档引用，运行 git diff --check。

完成标准：五项适用迁移均落地，既有业务/输入/导航回归通过；若存在平台阻碍，保留可复现证据并标注未完成，不以降级、跳过验证或录制新基线制造通过。

勾选表示实现或对应检查已执行，不代表静态门禁全绿。设备、正式视觉和存量告警统一跟踪于 docs/testing/pending-acceptance.md。未执行合入、公开发布或额外删除。
