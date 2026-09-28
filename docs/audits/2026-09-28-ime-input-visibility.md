# 输入页面 IME 遮挡排查与修复

日期：2026-09-28。状态：登录布局已修复并通过 Android 宿主 IME 回归测试；Android 模拟器已检查写邮件正文聚焦，登录实机与 iOS 键盘仍待验收。

## 范围与结论

全仓业务源码有 5 个输入控件调用，分布在 3 个场景：登录页 2 个、写邮件弹层 2 个、邮件搜索 1 个。当前确认 **1 个问题页面：登录页**。写邮件弹层的静态风险已由 Android 宿主 IME 测试排除，邮件搜索未发现同类输入框遮挡路径。

| 场景 | 结论 | 依据 |
| --- | --- | --- |
| 登录页 | 已确认并修复 | 原实现将 `verticalScroll()` 放在 `imePadding()` 之前；宿主测试注入 220px IME 后，表单滚动视口仍为 500px，断言失败。将 IME 避让移到 `BoxWithConstraints` 后，同一测试通过。 |
| 写邮件弹层 | Android 宿主测试通过，未改业务代码 | `NewEmailSheet.kt` 内容列没有显式 `imePadding()`，但 Material 3 弹层在独立窗口内处理 IME。宿主测试向弹层窗口注入 220px IME 后，滚动视口缩小，保存按钮可滚动到达。 |
| 邮件搜索 | 未发现输入框遮挡路径 | `EmailSearchBar.kt` 的输入框位于展开界面顶部；Material 3 停靠展开实现自带 `imePadding()`。仍需在设备上检查搜索结果列表末项的键盘可达性。 |

Android `MainActivity` 已调用 `enableEdgeToEdge()`，Manifest 已配置 `adjustResize`。登录修复位于表单布局层，无需改动这两项。

## 实施与验证

1. 登录页将 `imePadding()` 移到 `BoxWithConstraints`，使可用高度和滚动视口一起重测；父级 `Scaffold` 的 padding 同时被消费，避免 IME 与系统栏重复留白。保留 `SupportingPaneScaffold` 的左右分栏与既有铰链约束。
2. 写邮件弹层保留 Material 3 默认 inset 行为，并新增 Android 宿主回归测试；不叠加第二份 IME padding。
3. 设备验收需分别点账户、密码、主题、正文，弹出键盘后检查光标、输入框底部和登录/保存按钮可见或可滚动到达。覆盖手机竖屏、横屏短窗口、双倍字号、宽屏分栏、搜索全屏与停靠形态；iOS 软件键盘需单独验证。

Android 模拟器 `emulator-5554` 已安装本次 Debug 包。横屏写邮件正文聚焦后，Gboard 可见，正文光标位于键盘上方；测试字符已删除，弹层已关闭。模拟器原有账号已登录，因此没有清除应用数据或退出账号来强制进入登录页。显示设置未修改，结束时 `wm size` / `wm density` 均只有 Physical 值、没有 Override。登录页真实键盘和 iOS 软件键盘仍待验收。
