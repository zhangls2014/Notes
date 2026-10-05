# 自适应 UI 回归

## UI 变更验收清单

以下清单用于选择本次改动需要执行的验收，不代表后文历史测试结果已在本次任务重新执行。

- [ ] **视觉保持**：修改前列明要保持的组件、尺寸、留白和交互，回归普通设备与受影响设备。铰链修复曾改变普通设备的搜索和设置样式，搜索重叠修复也曾使锚点占满窗口；布局稳定性修复不能隐含改变视觉设计。需要改变设计或更新正式截图基线时，单独说明差异并取得用户确认。
- [ ] **动画过程**：开启动画并使用固定时钟检查中间帧、手势提交与取消、快速连续操作；预测返回提交后继续检查连续帧，确认没有重复进入动画。静态截图只检查稳定画面，不能替代动效验收。
- [ ] **状态恢复**：覆盖搜索进入详情后返回、Tab 切回、尺寸变化和保存状态恢复。需要先收起浮层的导航等待收起完成；FAB 等控件从恢复首帧开始检查，避免使用尚未初始化的列表状态重算外观。
- [ ] **输入容器**：覆盖键盘增高、焦点切换空档、变矮、正常关闭与失焦。正常关闭跟随真实 IME，不受切换保护额外延迟；检查系统栏和键盘占位没有重复消费。厂商安全键盘效果在受影响真机验收，Android 保护策略不作为 iOS 验收依据。
- [ ] **弹层窗口**：在 Android 与 iOS 分别检查首次打开、关闭后旋转、再次打开、滚动和操作按钮可达性。iOS 不只检查表单高度，还检查完整弹层容器及拖动锚点采用有限的完整视口约束。
- [ ] **验证证据**：分别记录行为断言、截图 verify、候选图审阅和设备验收。空白候选图不能作为视觉证据；不以失败截图覆盖批准基线，不通过放宽阈值掩盖问题。实际执行、跳过和未验收项按 [交付记录规则](README.md#验证证据与交付记录) 报告。

历史依据见 [铰链重新实现设计](../superpowers/specs/2026-09-23-hinge-isolated-reimplementation-design.md)、[搜索宽度计划](../superpowers/plans/2026-09-28-measured-search-width.md)、[登录键盘切换计划](../superpowers/plans/2026-09-29-login-ime-switch.md)，以及本页的搜索导航、预测返回和 FAB 恢复记录。当前布局与输入策略以 [架构文档](../architecture.md) 为准。

## Tab Fade through 过渡（2026-10-02）

首页、收藏和设置之间切换使用同级页面常用的分段淡入淡出：导航选中指示立即更新，旧内容淡出 90ms，透明度为零时切换活动返回栈、释放旧 NavDisplay，再将目标内容淡入 210ms。动画只更新内容绘制层透明度，不逐帧重组导航，也不移动导航侧栏或底部栏。同一时刻只有一个 Tab 内容参与组合和返回处理；外层 Entry 装饰器继续保留所有 Tab 的保存状态与 ViewModel。快速选择取消旧动画并以最后目标为准，重选当前 Tab 不启动动画；离开 Tab 外壳时，组合协程取消未完成的切换。

`tabsFadeThroughAndRapidSelectionKeepsLastTarget` 在 400×1000dp 与 900×1000dp 开启动画，检查淡出阶段仍显示旧内容且新内容尚未组合、淡入阶段旧内容已移除；使用排除导航栏区域的像素比较确认内容透明度发生变化，并验证快速连续选择最终只显示最后目标。新增断言修复前两组失败，修复后通过。

在途详情转场回归改为检查淡出结束后旧显示已释放；FAB 恢复回归从目标 Tab 首次恢复帧开始检查至少 12 帧的宽度。各 Tab 的详情、滚动位置、多选状态、返回归属及保存状态恢复测试继续通过。完整自适应回归 88 项实际执行、524 项按场景跳过，0 失败；Android DevFull Debug 构建和 iOS 模拟器 Kotlin 编译通过。本次尚未完成真机视觉验收。

```bash
./gradlew :composeApp:testAndroidHostTest --tests '*AdaptiveUiTest*' \
  :composeApp:compileKotlinIosSimulatorArm64 :androidApp:assembleDevFullDebug
```

## 单窗格详情进入与普通返回缩放（2026-10-02）

单窗格详情进入与按钮/系统普通返回复用预测返回的中心缩放几何和曲线：进入时从缩小状态放大至完整页面，返回时缩小退出；时长 220ms，水平缩减 48dp、纵向缩减 24dp，内部比例补偿保持文字和图片比例。列表在转场期间保持原位，导航侧栏和底部栏不参与缩放；物理圆角与遮罩沿用预测返回的绘制层。取消预测返回后再执行普通返回，会恢复圆角可见度，避免沿用取消动画结束时的零圆角状态。

`singlePaneDetailAndToolbarBackUsePredictiveScale` 在 400×500dp 和 610×400dp 下开启动画、固定时钟，检查进入与工具栏返回的中间帧缩放、页面中心及纵横比，并确认退出后详情移除、列表保持原位。修改前两组均在页面中心断言失败，修改后通过。`landscapeDetailTransitionsStayOutsideNavigationRail` 另检查取消手势后的普通返回仍有中心缩放，并以像素断言检查导航侧栏不被覆盖。既有首页/收藏预测返回提交和安全区回归通过。

全套自适应行为回归 86 项实际执行、509 项按场景跳过，0 失败；Android DevFull Debug 构建与 iOS 模拟器 Kotlin 编译通过。补充取消手势后的缩放断言另在两组横屏配置通过。真机已断开，当前改动尚未完成设备视觉验收。

```bash
./gradlew :composeApp:testAndroidHostTest --tests '*AdaptiveUiTest*' \
  :composeApp:compileKotlinIosSimulatorArm64 :androidApp:assembleDevFullDebug
```

## 搜索结果导航后返回与旋转恢复（2026-10-02）

搜索结果点击原先并行启动收起动画与详情导航。单窗格导航移除列表组合时，会取消仍在进行的动画；Navigation Entry 保存的 `SearchBarState` 因而包含未完成的展开进度。返回后搜索弹层再次出现，旋转重建还会继续恢复这个进度。

现在等待搜索收起完成后再进入详情，并清空查询、回写收起状态。展开状态监听只清理查询，不再重复启动收起动画，避免取消结果点击正在等待的动画。

`searchResultNavigationKeepsSearchClosedAfterReturnAndRotation` 开启动画，覆盖 400×1000dp 竖屏与 610×400dp 单窗格横屏：搜索结果进入详情、返回列表、宽高交换及保存状态恢复后，搜索弹层均须消失，重新展开仍可输入。修复前两组均在返回后的收起断言失败，修复后通过。相关搜索测试共 15 项实际执行、138 项按场景跳过，0 失败；Android DevFull Debug 构建和 iOS 模拟器 Kotlin 编译通过。

已在连接的 Android 真机上观察到旧版本返回时的残留弹层，并覆盖安装修复版；用户随后在真机验证，确认问题已修复。

```bash
./gradlew :composeApp:testAndroidHostTest \
  --tests '*AdaptiveUiTest.*Search*' --tests '*AdaptiveUiTest.search*' \
  :composeApp:compileKotlinIosSimulatorArm64 :androidApp:assembleDevFullDebug
```

## 首页新建邮件按钮状态恢复（2026-10-02）

`tabSwitchPreservesNewEmailFabSize` 开启动画后检查切回首页的连续 12 帧，覆盖收起状态、列表未到顶部时的展开状态，以及页面重建后展开状态恢复和继续滚动。修复前收起按钮在恢复帧从 56dp 变为 141dp，修复后保持原宽度。按钮单独使用 `rememberSaveable` 保存展开状态，只在实际滚动时更新，避免使用列表恢复期间未初始化的滚动方向和可滚动状态。

该回归与现有 `tabStateRestores*` 测试、iOS 模拟器 Kotlin 编译均通过。定向命令：

```bash
./gradlew :composeApp:testAndroidHostTest \
  --tests '*AdaptiveUiTest.tabSwitchPreservesNewEmailFabSize*' \
  --tests '*AdaptiveUiTest.tabStateRestores*' \
  :composeApp:compileKotlinIosSimulatorArm64
```

## Material 3 Expressive 迁移（2026-10-01）

共享主题已迁移为 MaterialExpressiveTheme，字体、容器和按钮形变也已统一。新增 `ExpressiveThemeTest` 验证普通组合子树的动态色、深浅色和字号切换，以及强调字重与正文尺寸；迁移前强调字重断言失败，迁移后两项通过。`Typography(fontFamily = FontFamily.Default)` 避免兼容无参构造器将强调样式映射回普通样式。

新增 `settingsLogoutDialogKeepsCancelAndConfirmReachable`，覆盖紧凑、深色和大字号页面中的退出行滚动、取消保留登录态与确认后回到登录页。大字号下使用 LazyColumn 的 `performScrollToNode` 查找尚未组合的行，避免将惰性组合行为误判为入口消失。

最终检查：Android 整包构建、iOS 模拟器 Kotlin 编译、Android lint 均通过；自适应测试 510 项中 76 项执行、434 项按场景跳过，主题测试另有 2 项通过，共 78 项实际执行，0 失败。lint 报告 22 条依赖/SDK 相关 Warning，0 Error。

候选录制生成 190 张图片，位于 `composeApp/build/adaptive-candidates/`。已检查紧凑登录、设置、深色设置与详情、大字号设置与草稿、宽屏详情，以及退出确认弹窗。正式截图基线未覆盖，全量视觉 verify 未作为通过结论；前文记录的既有空白截图问题仍需独立处理。宿主截图关闭动画，不能代替 Android/iOS 设备上的按压形变、弹簧动效与真实输入体验验收。

既有字号边界：Android 的独立 Dialog / Sheet window 重新提供窗口 density，因此父主题的应用内 `fontScale` 不一定进入弹窗。旧 SimpleDialog 与新 AlertDialog 最终使用同一 Dialog 机制，本迁移未新增该现象。主题测试只证明普通组合子树字号正确；弹窗截图和按钮可达性测试不证明弹窗字号已随应用设置缩放。

### 系统字号与登录加载回归（2026-10-02）

应用字号现作为系统字号的相对倍率。`ExpressiveThemeTest.systemFontScaleIsPreservedAndAppSizeIsRelative` 注入系统 Density，验证标准字号跟随系统、应用倍率叠加和系统运行期更新，density 不变。`LoginLoadingAccessibilityTest` 在系统 2 × 应用 2 的字号下确认登录入口可滚动到达，并在延迟登录期间检查输入／附属按钮／提交禁用、输入焦点释放、本地化 polite live region 及失败后恢复。禁用输入框可能不再提供 Focused 属性，不应把缺省值误当作仍持有焦点；全局主题／语言入口可以取得键盘焦点。

`LoginSubmissionTest` 使用可延迟 Repository 与 StandardTestDispatcher，验证排队的三次提交只启动一次、加载中表单 Intent 被忽略、失败后重试和取消后加载恢复。旧实现上的新增测试因字号被覆盖、重复调用、表单仍可编辑及按钮未禁用而失败，修复后通过。

扩大回归实际执行 48 项（自适应 42、主题 3、登录业务 2、加载 UI 1），另 60 项按场景跳过，0 失败。追加极端组合字号用例后的最终定向测试共 7 项通过，Android 整包构建、iOS 模拟器 Kotlin 编译及 Android lint 同一 Gradle 进程顺序执行成功；lint 为 0 Error / 10 Warning（依赖版本、AGP 和既有 SDK 属性提示）。宿主语义测试不代替 TalkBack / VoiceOver 端到端播报及导航焦点顺序验收；前述 Dialog / Sheet 应用倍率继承边界仍保留。

```bash
./gradlew :composeApp:testAndroidHostTest \
  --tests '*ExpressiveThemeTest*' \
  --tests '*LoginSubmissionTest*' \
  --tests '*LoginLoadingAccessibilityTest*' \
  :composeApp:compileKotlinIosSimulatorArm64 \
  :androidApp:assembleDebug :androidApp:lintDevFullDebug
```

最终命令（一个 Gradle 进程顺序装配检查，不并发启动构建）：

```bash
./gradlew :androidApp:assembleDebug :composeApp:compileKotlinIosSimulatorArm64 \
  :composeApp:testAndroidHostTest --tests '*AdaptiveUiTest*' --tests '*ExpressiveThemeTest*' \
  :androidApp:lintDevFullDebug \
  -Proborazzi.test.record=true -Pnotes.screenshots.candidates=true
```

## 测试边界

`composeApp/src/androidHostTest` 通过 Robolectric、Compose UI Test 与 Roborazzi 运行真实导航和 Feature，实现模块依赖仍只由组合根聚合。用户、设置、邮件使用确定性的内存仓库；测试不启动生产数据初始化、不访问数据库或网络。

测试使用固定英语资源和固定时间数据。宽高矩阵为 400/610/900dp × 400/500/1000dp，并包含 1600dp 桌面、大字号、深色、竖向与横向铰链的半开/全开场景，以及 610×320dp、双倍字体的矮窗口场景，共 17 组配置；另有导航可达性、安全区与搜索交互测试。截图用于发现遮挡、过宽、溢出与分栏退化；尺寸变化后的输入和详情状态、键盘 Tab 焦点、同一折叠几何下的详情位置通过行为断言守卫。测试宿主关闭动画，并在截图前等待绘制稳定，避免截取转场中间帧。

安全区测试通过分发真实的 `WindowInsetsCompat.Type.displayCutout()` 验证根导航和邮件页面：900×400dp 无铰链时，先扣除 96dp 折叠侧栏与 62dp 的左侧或右侧安全区，列表保留 360dp 首选宽度、详情分得剩余 382dp，详情空态与打开邮件后的分配策略一致；非居中的竖向铰链决定真实分界，安全区变化与半开/全开切换不能移动该分界；400×400dp 单页仍保留屏幕边缘的安全区。参数化执行会跳过不适用配置。安全区断言相对无刘海时的实际内容位置验证，确保侧栏占位与系统安全区只扣除一次。

IME 宿主测试在 400×500dp 场景分别向登录窗口和写邮件弹层窗口注入 220px 键盘 inset，验证表单滚动视口缩小、密码框可滚动显示，以及弹层保存按钮可滚动到达。弹层使用 Material 3 默认 IME 处理，无需叠加额外的 `imePadding()`。

登录键盘切换回归使用固定测试时钟：220 → 0 → 260px 的切换空档内连续检查视口不回落，更高键盘立即避让；260 → 0 → 180px 验证较矮键盘稳定 200 ms 后释放多余占位；连续切换取消旧释放任务，失焦立即释放，保留焦点的真实关闭也能释放。另注入 24px 导航栏验证 inset 消费不重复。正常收起回归逐帧注入 220 → 160 → 80 → 0px，要求每帧立即释放，并覆盖同高度键盘切换后保护窗口自动到期。新建邮件页同样模拟主题框到正文框的 220 → 0 → 260px 键盘切换，并验证正常收起当帧释放；默认与共享保护下 220px IME 的滚动视口均为 216px。宿主注入不模拟厂商安全键盘进程，实际手感需在受影响设备上验收。定向命令：`./gradlew :composeApp:testAndroidHostTest --tests '*AdaptiveUiTest.login*' --tests '*AdaptiveUiTest.draft*'`。

预测返回提交回归在 400×500dp 单栏场景单独开启动画，分别从首页和收藏的详情页返回列表。测试将手势推进到 `1f`，确认列表已完整显示，再提交返回，并检查随后连续 30 帧的列表卡片位置，防止弹栈后重新播放进入动画。静态 `listPane` 元数据必须复用：当前依赖内部的窗格元数据按实例比较，重新创建会使预览 Scene 与弹栈后的 Scene 不相等。该测试在修复前均于提交后第 3 帧失败，复用元数据后通过；设备上的真实手势仍需补充验证。

## 当前基线

2026-09-27 用户审阅并批准当前布局，160 张截图已保存到 `composeApp/src/androidHostTest/screenshots/`。登录保持左右分栏，不在窗格内避让横向铰链。后续变更先运行 verify，审阅差异后再决定是否更新基线。

## 执行

运行行为断言（没有记录或更新视觉基线）：

```bash
./gradlew :composeApp:testAndroidHostTest --tests '*AdaptiveUiTest*'
```

渲染候选图到构建目录，供人工检查：

```bash
./gradlew :composeApp:testAndroidHostTest --tests '*AdaptiveUiTest*' \
  -Proborazzi.test.record=true -Pnotes.screenshots.candidates=true
```

候选图位于 `composeApp/build/adaptive-candidates/`，不作为已批准基线。确认页面显示正确后，开发者可录制基线：

```bash
./gradlew :composeApp:testAndroidHostTest --tests '*AdaptiveUiTest*' \
  -Proborazzi.test.record=true
```

基线位于 `composeApp/src/androidHostTest/screenshots/`，应随实现提交。之后只验证，不自动更新：

```bash
./gradlew :composeApp:testAndroidHostTest --tests '*AdaptiveUiTest*' \
  -Proborazzi.test.verify=true
```

缺少基线或图像变化时应失败。审阅实际图、基线及差异后才能决定更新，不能把自动录制后的成功当作视觉正确的证据。

## 设备补充验证

宿主测试不能代替真实 IME、自由窗口、系统栏和物理折叠设备验证。设备上还需覆盖 Tab/Shift+Tab、Enter、鼠标点击与滚轮，以及键盘弹出后表单/弹层操作按钮可达性。按 [模拟器指南](android-emulator.md) 执行并恢复显示设置。

## 工具选择

Compose Preview Screenshot Testing 的 Android 插件没有提供本项目所需的 KMP 非 Android 目标支持；本项目使用现有 Android 宿主源集，便于同时测试状态切换。没有为截图额外新增 JVM 目标，也没有向 Feature API 添加测试入口。

参考：[Robolectric](https://robolectric.org/getting-started/)、[Roborazzi](https://github.com/takahirom/roborazzi)、[Compose Preview Screenshot Testing](https://developer.android.com/studio/preview/compose-screenshot-testing)。

## 横向铰链回归

用户审阅候选图后决定保留左右分栏，但不在窗格内避开横向铰链。登录品牌与表单继续使用完整可用高度，保留原有居中和滚动行为。900×1000dp 横向铰链场景保留半开/全开位置稳定与登录按钮可达性断言，不再要求密码框避开横向折痕。登录截图已按此决定更新，并纳入用户批准的正式基线。

## 当前验证限制

2026-09-27 提交基线前，全量 verify 未通过：部分邮件列表/收藏场景的实际截图出现空白列表，而批准的基线含完整列表。登录场景通过。调整 Compose 动画时长与逐帧等待均未消除问题，实验性调整已撤回；根因仍需进一步定位。160 张正式基线保留用户审阅的原图，没有以失败截图覆盖，也没有放宽比较阈值。现阶段不能把整套截图任务作为稳定的合入门禁。

## 低高度导航侧栏（2026-09-27）

窗口宽度 ≥600dp 且高度 <480dp 时，应用使用折叠侧栏，包括超大宽度的矮窗口；其他尺寸维持原策略。策略测试覆盖 599/600dp 宽度与 479/480dp 高度边界。宿主测试验证 610×400、900×400 和 610×320dp 双倍字体下三个入口纵向排列、完整可见且能够切换，候选截图名称为 `*-navigation-rail.png`。

本次候选图写入 `composeApp/build/adaptive-candidates/`，未覆盖已批准基线。横屏原基线仍是底栏，正式视觉 verify 会产生预期差异；需审阅候选图后更新。双倍字体下英文 Favorites 会换行，所有入口仍可见可点击。

本次策略测试 13 项通过，自适应宿主测试 47 项通过（157 项因配置不适用而跳过），Android 整包构建与 iOS 模拟器编译通过。候选图仍可观察到上节记录的部分列表空白问题，未将候选录制成功视为视觉基线验证通过。

## 搜索框首次测量宽度（2026-09-28）

搜索框不使用固定 dp 上限：首次在当前父约束下测量空查询输入框，将实际宽度保留在当前 UI 实例中，随后输入长文本、缩短或清空均不改变锚点宽度。停靠展开填满锚点提供的宽度；紧凑窗口的全屏搜索仍使用窗口空间。父约束变窄时临时收缩，空间恢复后回到首次宽度；密度、字号、布局方向或提示文本变化时重新测量。恢复长查询也用独立空查询基准，不修改真实查询或焦点。

新增宿主断言覆盖 400/610/900dp 与桌面窗口的长查询稳定性、610dp 与桌面窗口的长查询恢复，以及 900→320→900dp 的窗口变化。断言以首次真实尺寸为基准，不硬编码 Material 的默认宽度。未放置的测量节点显式清除语义，现有搜索入口唯一性、键盘焦点与返回动画测试同时守卫它不会干扰交互。

开启候选录制时，新增 `*-measured-search-collapsed.png` 和 `*-measured-search-long-query-*.png`，仅写入 `composeApp/build/adaptive-candidates/`，不覆盖正式基线。

本次 `AdaptiveUiTest` 全量执行 357 项，65 项通过、292 项按场景条件跳过，0 失败；iOS 模拟器 Kotlin 编译通过。候选图在桌面与 610dp 场景确认搜索框保持原有留白，长查询的停靠面板与锚点对齐。正式视觉基线未自动更新。

## 登录输入框 UI（2026-10-01）

账户和密码使用圆角描边与浮动标签，错误支持文本按需显示。`loginInputActionsAreAccessible` 在 400×500dp、深色、大字号场景验证清空账户、密码显隐、字段标签保留、短密码错误提示可达和登录禁用。原有 `loginFitsAndRetainsInputOnResize`、登录 IME 系列继续守卫 Tab 焦点、窗口变化、输入保留和键盘交接。候选图包括 `*-login.png`、`*-login-input-visible.png` 和 `*-login-input-error.png`，正式基线未更新。

## iOS 停靠搜索坐标回归（2026-10-01）

Material 停靠搜索使用收起输入框的 `positionInWindow()` 定位。iOS Popup 默认启用
`usePlatformInsets`，在位置提供器返回值上再次加安全区原点，导致横屏展开输入框与
原输入框错位。邮件停靠搜索在 iOS 单独关闭该选项；锚点继续按页面安全区布局，
Android 保持 Material 默认弹层配置。

模拟器回归步骤：在带侧边安全区的 iPhone 上进入邮件列表，分别向左、向右旋转到横屏，
点击搜索框，确认展开输入框与收起锚点的左上角及宽度重合；输入长查询并清空，
关闭后重新展开；展开期间旋转回竖屏，再转回横屏，确认查询保留且停靠输入框对齐。
同时检查点击外部关闭与键盘输入。Android 宿主测试不能验证 iOS Popup 的安全区坐标转换。

本次验证：iOS 模拟器 Kotlin 编译通过；现有搜索展开动画、长查询宽度及窗口缩放宿主测试通过。
首次只完成 Kotlin 编译，未安装完整应用。随后使用当前工作区的 Xcode 工程完成 Debug 模拟器整包构建（含 Kotlin framework 链接），安装到 iPhone 17e（iOS 27.0）后，由用户点击搜索并读取模拟器原始截图复验。2532×1170px 横屏下，收起与展开输入区域的左边界均为 453px、上边界均为 54px、右边界均为 1385px；安装前的偏移画面中弹层左边界为 594px。当前构建在该方向横屏展开时已对齐。另一横屏方向、长查询、软键盘和旋转往返尚未完成 iOS 视觉回归。

补充 iPhone 18 Pro（iOS 27.0）验证：安装前 2622×1206px 横屏截图仍可见两框错位，原锚点左边界为 498px，弹层左边界为 684px。将同一工作区完整构建安装到该设备并由用户重新展开后，截图中弹层与原锚点重合，弹层左边界为 498px；已在两台模拟器上确认当前横屏方向的展开对齐。
