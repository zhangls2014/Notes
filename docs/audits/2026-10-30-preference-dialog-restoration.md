# 设置选择弹窗旋转恢复调查

日期：2026-10-30。状态：已复现并完成第三方组件隔离对照；设置页已改为普通滚动容器，宿主与 Android 模拟器回归通过。

## 结论

Compose Preference 2.2.0 的 `ListPreference` 在内部用 `rememberSaveable` 保存
`openSelector`，并在同一个组合函数中创建弹窗。它处于懒加载列表项内时，
弹窗恢复依赖该列表项是否参与组合。窗口缩小后目标行未被组合，弹窗不会显示；
滚动使该行重新参与组合，弹窗才恢复。不是保存的打开状态丢失。

该版本同时提供 `LazyListScope.listPreference()`，内部也是在列表项中调用
`ListPreference`。当前受控取值接口没有暴露弹窗打开状态。源码依据是 Gradle
缓存中该版本 sources.jar 的 `commonMain/ListPreference.kt`。

## 隔离测试

测试位于 [PreferenceDialogRestorationTest.kt](../../composeApp/src/androidHostTest/kotlin/me/zhangls/entry/PreferenceDialogRestorationTest.kt)。
直接调用未修改的第三方组件，仅使用 MaterialTheme、空内存 Preferences 和列表容器，
不使用 Notes 导航、ViewModel、Repository 或 PreferenceRow。

环境：Robolectric API 37、Native Graphics、400×900dp；使用
`StateRestorationTester` 模拟保存状态恢复，缩小组将容器高度改为 160dp。
目标设置项前有 10 个 64dp 占位行，以明确控制其是否参与组合。

| 对照 | 实际断言结果 |
|---|---|
| LazyColumn，恢复后目标行在视口外 | 弹窗不存在；滚动到目标行后弹窗显示 |
| 普通滚动 Column，相同缩小条件 | 弹窗立即恢复 |
| LazyColumn，恢复时不缩小高度 | 弹窗立即恢复 |

执行 `./gradlew :composeApp:testAndroidHostTest --tests '*PreferenceDialogRestorationTest*'`：
3 项通过，0 跳过、0 失败、0 错误。测试名称和断言描述当前缺陷，测试通过不代表缺陷修复。

## 当前源码包设备复现

`:androidApp:assembleDevFullDebug` 构建通过，产物覆盖安装到 `emulator-5554`，
未操作已连接真机。安装后执行设置页的实际语言弹窗流程：

- 默认 1080×2364、390 dpi：该次竖屏打开后旋转，弹窗保持显示，未复现。
- 覆盖为 1080×1920、480 dpi：竖屏打开语言弹窗，横屏后布局树只剩设置列表，
  没有语言选择项和取消按钮；向下滑动 250px 后，语言标题、三个选项和取消按钮重新出现。
- 未选择任何语言，最后取消弹窗。脚本在 `finally` 中恢复分辨率、密度和旋转；
  收尾查询为 Physical size 1080×2364、Physical density 390，无 Override，自动旋转为 1。

设备布局采集由 Android CLI 完成。宿主测试证明组件组合与状态恢复的因果边界，
设备流程证明当前应用在较小手机窗口下能触发同一现象。未进行 iOS 或真机验收。

## 项目侧最小修复与验证

设置页仅有少量固定项目，将 `LazyColumn` 替换为使用 `rememberScrollState()` 的
可滚动 `Column`，让第三方选择控件始终参与组合。保留原有 2dp 行间距、16dp 行留白、
分组形状、内容宽度和控件回调。Scaffold padding 放在滚动内容内部，底部安全距离不缩短视口。

函数长度门禁要求将既有退出确认弹窗渲染原样提取为同文件私有函数；仅传入 UI 模型和
结果回调，不下传 ViewModel，不改退出行为，不增加公共接口或修改静态规则。
未发布的本地更新记录同步增加修复说明，没有升级版本。

新增真实 `SettingsEntryImpl` 恢复回归：400×900dp 打开语言弹窗后，以 160dp 高度恢复组合。
修改前“English 选项可见”断言失败，修改后无需滚动即通过。
三组第三方隔离对照仍保留，用于区分库本身的限制与页面侧规避。

执行四项弹窗宿主测试及 Settings 模块五项测试：9 项通过，0 跳过、0 失败、0 错误；
Settings detekt、Android DevFull Debug 构建、iOS 模拟器 Kotlin 编译通过。
iOS 编译不代表 iOS 弹窗行为验收，未更新或验证正式截图基线。

修复包模拟器复验：此前 AVD 已关闭，本次临时启动同一 AVD 的只读实例，重新安装当前
DevFull Debug 产物。在 1080×1920、480 dpi 下，分别打开语言、字体大小、深色模式弹窗，
每个均执行竖屏→横屏→竖屏，断言三个阶段的取消按钮存在，横屏不执行任何滚动即显示
选项。三组均通过，最后均取消，未改变设置值。脚本 `finally` 恢复显示设置，查询无
Override、自动旋转为 1；随后关闭本次临时实例。真机、iOS 行为与正式视觉基线仍未验收。

## 本地版本交付（2026-10-30）

按 Xander 指示，发布范围为版本配置、更新日志、本地构建产物和 master 合并，不上传外部渠道。
沿用累计中的 1.1.0，将 Android/iOS 构建号从 2 同步增至 3，发布日期标记为 2026-10-30；
About 摘要和首次启动弹窗测试同步检查该日期。合并前 rebase 到最新 master，保留其 Android
弹窗居中和 JDK 工具链配置改动。

最终版本配置下，组合根 40 项、About 19 项、Settings 5 项宿主测试全部通过，无失败或跳过；
Settings/About detekt、iOS 模拟器 Kotlin 编译通过。初次合并执行 Release 打包与回归时，
默认 2GB Gradle 堆因 GC overhead 内存不足中断；按仓库指引使用 8GB 堆、限制两个 worker
并串行分开验证和打包，不修改共享构建配置。上述 64 项是重试后的实际成功结果。

生产完整变体 `:androidApp:assemblePrdFullRelease` 在 8GB 堆下构建通过，APK 为
`Notes_v1.1.0(3)_PrdFull_release.apk`，大小 7,613,010 字节。aapt2 确认包名
`me.zhangls.notes`、版本 1.1.0、构建号 3；apksigner 验证通过，使用 APK v2 签名。
APK 内 releases.json 与源码逐字节一致。SHA-256：
`6270f7569192724ca97bf576f41e378cc085a2c7b10379b9cc75f7315db5d775`。
该 Release APK 未另行安装验收，前述旋转设备证据来自修复后的 DevFull Debug 包；
iOS 本次仅编译验证，未生成 IPA。
