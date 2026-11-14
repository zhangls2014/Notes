# Notes 的 KMP 应用内网页浏览方案

日期：2026-11-14

状态：已选用平台 WebView 并实现首轮接入，待真机验收

## 结论

优先在 About 模块实现一个全屏网页阅读页。共享 Compose 工具栏、加载与重试状态和返回规则，Android 通过 `AndroidView` 嵌入系统 `WebView`，iOS 通过 `UIKitView` 嵌入 `WKWebView`。先接入在线开源声明及许可证详情，复用 `NotesWebsite`、平台 URL 和网页外观参数。声明 HTML 继续随官网分发。

Android WebView 由系统配置的 provider 提供。Android 10 及以上 AOSP 默认使用独立 WebView；厂商可以配置可用 provider。用户在无 Chrome 设备上观察到 Custom Tabs 跳外部浏览器，这与系统 WebView provider 是否可用是两个独立条件。需在实际目标设备上验证 provider、网页加载和系统栏表现。 [Chromium WebView provider 文档](https://chromium.googlesource.com/chromium/src/+/HEAD/android_webview/docs/webview-providers.md)

## 方案对比

| 方案 | 对 Notes 的影响 |
|---|---|
| 平台原生 WebView + Compose 互操作 | 两端共享页面外壳与交互，加载、导航回调、资源释放和滚动安全区由平台实现。可从仓库旧版的 `ExternalProjectWebView.android.kt`、`ExternalProjectWebView.ios.kt` 提取生命周期及加载处理，参考点为提交 `e3faf70a`。 |
| `compose-webview-multiplatform` | 提供共享状态和导航封装，Android 和 iOS 仍使用平台 WebView。上游 2.0.3 的构建版本是 Compose 1.8.0、Kotlin 2.1.20，本仓库为 Compose 1.12.2、Kotlin 2.4.20。兼容性需要本项目构建与行为验证，尤其是原生滚动、返回和安全区。 |
| Android 独立内核 | GeckoView 将 Gecko 随应用分发。腾讯 X5 引入内核初始化和下载，并可能使用系统内核。这两条路径增加 Android 集成与验收范围。只有系统 WebView 在目标设备上有已复现且无法解决的兼容问题时，再评估。 |

参考：[Android Compose 互操作](https://developer.android.com/develop/ui/compose/migrate/interoperability-apis/views-in-compose)、[Compose UIKit 互操作](https://kotlinlang.org/docs/multiplatform/compose-uikit-integration.html)、[跨平台库发布记录](https://github.com/KevinnZou/compose-webview-multiplatform/releases)、[2.0.3 构建配置](https://github.com/KevinnZou/compose-webview-multiplatform/blob/2.0.3/gradle/libs.versions.toml)、[GeckoView](https://mozilla.github.io/geckoview/)、[X5 初始化 API](https://x5.tencent.com/docs/tbsapi/reference/com/tencent/smtt/sdk/QbSdk.html)。

## 首轮实现边界

- About 拥有页面及状态，About API 声明目的地，组合根接入全屏导航。WebView 实例由当前页面持有和释放。网页有历史时返回上一网页，历史耗尽时弹出应用页面。
- 初版允许 WebView 加载配置官网的 HTTPS 声明页及详情，项目来源通过明确的外部打开动作处理。验证初始地址、主框架跳转、重定向和新窗口请求。声明页保留片段切换和外观所需的 JavaScript，禁用文件访问和原生 JS 桥接，TLS 错误按平台默认安全行为处理。现有全局 `LocalUriHandler` 继续负责其他入口。 [Android WebView URI 校验](https://developer.android.com/privacy-and-security/risks/unsafe-uri-loading)
- 状态栏由原生应用窗口管理，顶部 Compose 工具栏消费顶部 inset。WebView 滚动视口延伸到屏幕底边，底部安全距离放在滚动内容内。分别验证 Android 三键和手势导航，以及 iOS Home Indicator，避免原生滚动 inset 与网页安全区重复计算。

## 原型验收

1. 无 Chrome 的大陆 Android 真机先记录 WebView provider 与版本。点击声明入口，验证加载、详情锚点、站内返回和关闭。验证启动失败、断网重试及主框架 HTTP 错误。
2. Android 浅色和深色、手势和三键导航，以及 iOS 浅色和深色，分别核对状态栏图标、导航区颜色、滚动末项可点击和旋转后的可用性。检查 iOS 边缘返回手势与网页历史的优先级。
3. 编译两端，执行 Android 宿主回归和 iOS 真实 Entry 调用。用测试页覆盖非允许来源跳转、新窗口链接和加载失败。真机验收仍按[集中清单](../testing/pending-acceptance.md#系统应用内浏览器)跟踪。
