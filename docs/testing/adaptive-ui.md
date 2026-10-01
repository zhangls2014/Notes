# 自适应 UI 回归

## Material 3 Expressive 迁移（2026-10-01）

共享主题已迁移为 MaterialExpressiveTheme，字体、容器和按钮形变也已统一。新增 `ExpressiveThemeTest` 验证普通组合子树的动态色、深浅色和字号切换，以及强调字重与正文尺寸；迁移前强调字重断言失败，迁移后两项通过。`Typography(fontFamily = FontFamily.Default)` 避免兼容无参构造器将强调样式映射回普通样式。

新增 `settingsLogoutDialogKeepsCancelAndConfirmReachable`，覆盖紧凑、深色和大字号页面中的退出行滚动、取消保留登录态与确认后回到登录页。大字号下使用 LazyColumn 的 `performScrollToNode` 查找尚未组合的行，避免将惰性组合行为误判为入口消失。

最终检查：Android 整包构建、iOS 模拟器 Kotlin 编译、Android lint 均通过；自适应测试 510 项中 76 项执行、434 项按场景跳过，主题测试另有 2 项通过，共 78 项实际执行，0 失败。lint 报告 22 条依赖/SDK 相关 Warning，0 Error。

候选录制生成 190 张图片，位于 `composeApp/build/adaptive-candidates/`。已检查紧凑登录、设置、深色设置与详情、大字号设置与草稿、宽屏详情，以及退出确认弹窗。正式截图基线未覆盖，全量视觉 verify 未作为通过结论；前文记录的既有空白截图问题仍需独立处理。宿主截图关闭动画，不能代替 Android/iOS 设备上的按压形变、弹簧动效与真实输入体验验收。

既有字号边界：Android 的独立 Dialog / Sheet window 重新提供窗口 density，因此父主题的应用内 `fontScale` 不一定进入弹窗。旧 SimpleDialog 与新 AlertDialog 最终使用同一 Dialog 机制，本迁移未新增该现象。主题测试只证明普通组合子树字号正确；弹窗截图和按钮可达性测试不证明弹窗字号已随应用设置缩放。

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
