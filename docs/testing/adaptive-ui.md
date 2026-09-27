# 自适应 UI 回归

## 测试边界

`composeApp/src/androidHostTest` 通过 Robolectric、Compose UI Test 与 Roborazzi 运行真实导航和 Feature，实现模块依赖仍只由组合根聚合。用户、设置、邮件使用确定性的内存仓库；测试不启动生产数据初始化、不访问数据库或网络。

测试使用固定英语资源和固定时间数据。宽高矩阵为 400/610/900dp × 400/500/1000dp，并包含 1600dp 桌面、大字号、深色、竖向与横向铰链的半开/全开场景，共 16 组配置、32 个测试。截图用于发现遮挡、过宽、溢出与分栏退化；尺寸变化后的输入和详情状态、键盘 Tab 焦点、同一折叠几何下的详情位置通过行为断言守卫。测试宿主关闭动画，并在截图前等待绘制稳定，避免截取转场中间帧。

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
