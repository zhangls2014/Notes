# Notes 当前架构

本文描述仓库当前实现，是模块职责和依赖边界的权威说明。构建与测试命令见 [AGENTS.md](../AGENTS.md)，历史问题和验证证据见 [文档索引](README.md)。

## 1. 总体结构

Notes 是 Kotlin Multiplatform 应用，共享业务逻辑和 Compose UI 位于 `commonMain`，Android 与 iOS 复用同一套 Feature 实现。依赖从应用组合根流向 Feature，再流向 Core；实现模块之间不直接依赖。

```text
androidApp / iosApp
        |
   :composeApp                 应用组合根、导航、Koin 装配
        |
        +-- feature 实现 ------ feature API
        |         |                  |
        |         +------------------+
        |                            |
        +----------------------- core 基础模块
```

Gradle 中启用的模块：

```text
:androidApp
:composeApp
:web

:core:model
:core:database
:core:data
:core:theme
:core:preference
:core:network
:core:download
:core:framework
:core:navigation

:feature:main       :feature:main-api
:feature:login      :feature:login-api
:feature:email      :feature:email-api
:feature:settings   :feature:settings-api
:feature:profile    :feature:profile-api
:feature:about      :feature:about-api

:android:baselineprofile
:android:output:login
```

`settings.gradle.kts` 当前包含 25 个模块；`build-logic` 通过 `includeBuild` 提供约定插件，不计入这 25 个模块。`iosApp` 是 Xcode 工程，不是 Gradle 模块。

## 2. 模块职责

### 应用与平台入口

| 目录或模块 | 职责 |
|---|---|
| `androidApp` | Android `Application`、Activity、Manifest、平台初始化和 APK 输出 |
| `iosApp` | iOS 应用入口和 Xcode 工程 |
| `composeApp` | 共享应用入口、Koin 组合根、根返回栈、Deep Link、Feature 导航装配和启动数据初始化 |
| `web` | Kotlin/Wasm 官网，复用 Core 设置模型和主题，装配 Web 导航与浏览器历史 |
| `android:baselineprofile` | Android 基线配置文件生成 |
| `android:output:login` | 登录能力的实验性 Fused Library 输出 |

`composeApp` 可以依赖所有 Feature 实现以完成装配，但不承载 Feature 业务实现。平台入口只负责启动共享应用和提供平台能力。

### Core

| 模块 | 所有权与边界 |
|---|---|
| `core:model` | 跨数据层和 UI 基础模块使用的稳定值类型；不含 Compose |
| `core:database` | Room entity、DAO、converter、迁移、数据库工厂和事务助手 |
| `core:data` | 对外模型、Repository 接口和数据实现；隐藏数据库实现类型 |
| `core:theme` | 主题、图标、通用 Compose 组件、内容宽度和窗口事实 |
| `core:preference` | 设置项展示元数据、通用设置控件和渲染契约 |
| `core:network` | 跨平台 Ktor 客户端工厂、HTTP 客户端绑定 |
| `core:download` | 通用 HTTP 分片下载、磁盘断点恢复和完整文件校验 |
| `core:framework` | MVI 基类、Navigation 3 契约、Deep Link 和全局 Effect |
| `core:navigation` | 导航显示、预测返回转场、设备圆角和窗格安全区；不依赖 Feature，不拥有应用返回栈或业务导航策略 |

`core:model` 的准入条件是：类型出现在数据模块的公开签名中，或同时被数据层与含 Compose 的基础模块消费。只服务一个 Feature 的类型留在该 Feature。

### Feature

每个 Feature 由同级的 API 模块和实现模块组成：

| API / 实现 | 职责 |
|---|---|
| `feature:main-api` / `feature:main` | 顶层 Tab 和应用外壳 |
| `feature:login-api` / `feature:login` | 登录目的地、入口契约和登录 UI |
| `feature:email-api` / `feature:email` | 邮件详情目的地、列表场景、邮件列表/详情/搜索/写信 UI |
| `feature:settings-api` / `feature:settings` | 设置入口/结果契约和设置行为/UI |
| `feature:profile-api` / `feature:profile` | 个人信息与通行密钥管理的入口、导航契约、页面与状态 |
| `feature:about-api` / `feature:about` | 关于应用目的地/入口契约、品牌信息、安装包版本展示、版本更新摘要展开与离线更新列表 |

新增页面时先确定其业务职责、状态所有者、入口和依赖方向，并在设计或实施计划中记录模块归属与理由。现有 Feature 能承担完整职责时复用该 Feature；独立职责需要独立依赖边界时，可新增 API/实现模块对。设置列表、工具栏等访问入口可以属于其他 Feature，由宿主装配导航。

API 模块只包含其他模块有理由知道的稳定契约，例如 `Destination`、Feature Entry 接口、导航贡献和必要参数。页面、ViewModel、Reducer、Repository 实现、DI provider 和内部模型都属于实现模块。

当前 Native 编译链下，跨模块 `@Composable` 抽象 Entry 方法不声明默认参数，调用方显式传参；默认值可以留在模块内部的顶层页面函数。邮件、设置入口曾因此触发 `IrLinkageError`，同类检查还发现登录入口风险（修复提交 `7f203ca8`、`1d3dfb34`）。新增或修改契约时，在 `composeApp` 的 iOS 测试中通过接口调用真实实现；编译成功不能证明运行时分派正确。编译链升级后，只有对应回归证明问题消失，才可重新评估该限制。

### 服务地址配置

应用构建从 Gradle 属性、环境变量、集中默认值依次解析认证和官网地址。组合根把官网来源注入 About Feature，更新清单请求、Android 下载页校验、发布历史和在线声明链接共用该来源。Android App Links 使用相同构建参数生成 host；iOS 的关联域名通过 Xcode 构建设置提供，并为框架构建派生对应地址。测试环境及网站发布参数见 [部署配置](notes-web-deployment.md#服务地址配置)。

### 源码定位

| 内容 | 当前路径 |
|---|---|
| 共享入口与装配 | `composeApp/src/commonMain/kotlin/me/zhangls/entry/`：`App.kt`、`AppNavHost.kt`、`NotesModule.kt`、`NavigationRegistry.kt` |
| 返回栈与导航策略 | 同一包下的 `AppNavigationState.kt`、`NavHandler.kt` |
| 列表—详情窗格 | [`entry/panes/`](../composeApp/src/commonMain/kotlin/me/zhangls/entry/panes/) 存放分栏规则、边界动画和目的地配置 |
| 导航显示与转场 | `core/navigation/src/commonMain/kotlin/me/zhangls/navigation/`；设备圆角读取分别位于 `androidMain` / `iosMain` |
| Feature 稳定契约 | `feature/<name>-api/src/commonMain/kotlin/me/zhangls/<name>/api/` |
| Feature 页面与实现 | `feature/<name>/src/commonMain/kotlin/me/zhangls/<name>/`，业务状态放在 `mvi/`，页面放在 `ui/` 或业务子包 |
| 数据公开面与内部实现 | `core/data/src/commonMain/kotlin/me/zhangls/data/` |
| 共享品牌与头像 | `core/theme/src/commonMain/kotlin/me/zhangls/theme/component/`：`AppBrand.kt`、`UserAvatar.kt` |
| 构建约定 | `build-logic/src/main/kotlin/`；路径派生工具位于 `me/zhangls/convention/KmpConventions.kt` |

共享模块的平台实现位于 `src/androidMain` 和 `src/iosMain`；共享资源位于拥有该 UI 的模块的 `src/commonMain/composeResources`，仅单个平台使用的资源放入该模块对应的 `src/androidMain/composeResources` 或 `src/iosMain/composeResources`。平台安装包契约 `AppInfo` 位于 `core:framework` 根包，只声明版本查询；Android/iOS 实现仍由宿主装配。关于应用页由 `AboutViewModel` 注入该契约获取安装包信息，版本更新摘要展开状态由同模块的 MVI 管理，保持默认 `savedKey = null`。

「官方网站」进入 About 所属的全屏页面，从同一宿主配置加载官网首页，复用更新日志和在线声明的 WebView。官网导航保持可用，同一 HTTPS 来源的页面在应用内加载。

应用冷启动时，组合根触发 About 在后台从同一官网来源优先检查 `/web-assets/manifest-v2.json`，不可用时读取旧版 `/web-assets/manifest.json`。清单仅包含 Skiko Wasm 和 MiSans 字体，大小与 SHA-256 一致时复用缓存，缺失资源交给 `core:download`。预缓存使用 256 KiB 分片，并行处理最多三个不同文件。同一目标文件串行写入，各文件独立重试。Android 在应用启动时直接尝试续传，并登记一个有联网约束的 WorkManager 工作负责后台重试。两者共用下载协调器和磁盘断点。iOS 通过一个后台 URLSession 批量排入缺失分片，系统接管传输，宿主将会话事件交还下载模块，校验并合并分片后才完成系统回调。下载器校验 `Content-Range`，断点按目标、大小和摘要隔离。设备强制停止应用后，下一次启动重新检查并恢复可用的断点。服务器忽略 Range 时回退为全量下载。只有完整文件通过大小和 SHA-256 校验后才原子提交，临时文件只用于显示已保存的断点进度。APK 更新仍走 Android 系统下载。WebView 只复用同一官网来源和精确版本的缓存文件，未命中时从 HTTPS 加载。网页、脚本、Notes Web Wasm 和业务数据继续通过 HTTPS 获取。iOS 仅接受官网主框架发出的这两类资源请求。

WebView 使用平台的默认 HTTP 缓存。官网 HTML 和数据接口每次使用前重新验证，静态文件按内容生成地址并允许长期复用。原生刷新动作重新加载当前页面，仍按官网的 HTTP 缓存策略处理资源。配置及发布顺序见[官网缓存策略](notes-web-deployment.md#官网缓存策略)。

「关于应用」按版本信息、网站与声明、存储分组。「缓存管理」属于 About 的全屏目的地，由导航条目作用域的 ViewModel 在进入页面和官网资源校验通过后读取本机缓存快照。网页资源的下载状态由应用级缓存服务按文件提供，进度包含当前分片已收到的字节，重试时回到已保留的断点。任务尚未恢复时，页面从本地断点显示已保存的进度。已统计占用只包含完成的官网资源、Android 应用专属目录中 DownloadManager 已完成的更新安装包，以及旧版本留下的安装副本。iOS WebKit 磁盘缓存按站点列出；系统网页引擎的缓存大小不纳入总占用。页面只读，不遍历应用数据目录或修改缓存。

About 的版本号展开区和首次启动提示从包内 `releases.json` 读取匹配安装版本的摘要。「查看更新日志」进入独立导航页面，通过宿主配置的官网加载 `/releases/`，复用在线声明的 WebView 和外观参数。维护规则见 [应用版本与更新记录](app-releases.md)。

## 3. 依赖规则

### Feature 边界

- Feature 实现必须依赖自身 `-api`。
- Feature 实现可以依赖其他 Feature 的 `-api`，不得依赖其他 Feature 的实现。
- Feature API 不依赖任何 Feature 实现；跨 Feature 共享的静态 UI 数据应下沉到合适的 Core 模块。
- 只有 `composeApp` 依赖全部实现模块，并将实现绑定到 API 契约。
- Core 不依赖 Feature。

当前 `feature:main` 依赖 `feature:email-api`、`feature:settings-api` 和 `feature:profile-api`：通过 `EmailEntry`、`SettingsEntry` 渲染 Tab 内容，通过 `ProfileOrigin` 标识个人信息入口。设置页通过 `SettingsResult.OpenProfile` 和 `OpenAbout` 请求导航；main 将结果交给宿主回调，`composeApp` 注册 profile/about 的 Nav Entry。

设置页通过 `SettingsResult.ManagePasskeys` 请求宿主把通行密钥管理页压入设置 Tab 的返回栈。共享管理页由 `composeApp` 装配认证客户端，作为全屏页面处理返回。创建时，平台提供设备品牌，服务端在验证注册成功后保存品牌和 AAGUID，再由列表接口返回品牌及匹配到的密码管理器标识。

### `api` 与 `implementation`

默认使用 `implementation`。只有依赖模块的类型出现在当前模块公开 Kotlin API 中时才使用 `api`。

两个关键隔离边界：

1. `core:data` 必须以 `implementation` 依赖 `core:database`。这样 Feature 无法看到 Room entity、DAO 或查询载体。
2. `core:preference` 必须以 `implementation` 依赖底层 Compose Preference 库。Feature 只看到本项目的 `PreferenceSpec`、`PreferenceUiModel` 和通用控件。

`internal` 不能代替模块边界。Room 生成代码会要求部分数据库类型公开，真正阻止外泄的是 Gradle `implementation`。

### 数据公开面

`core:data` 分成两层：

- 公开面：`model/`、`repository/`、`util/` 和 `DataModule.kt`；
- 内部面：`impl/datastore/`、`impl/mapper/`、`impl/repository/`、`impl/security/`，实现类与 provider 为 `internal`。

Repository 接口和对外模型不得出现 Room 类型。数据库模型与公开模型的转换集中在 mapper 中。

## 4. 构建约定

`build-logic` 提供五个预编译约定插件：

| 插件 | 用途 |
|---|---|
| `me.zhangls.kmp-library` | KMP、JDK toolchain、Android/iOS target、Android 宿主测试、namespace、framework 和 detekt 配置 |
| `me.zhangls.kmp-compose` | Compose runtime、foundation、UI、resources 和 Material 3 |
| `me.zhangls.kmp-compose-api` | 仅为 Feature API 注入 Compose runtime 与编译器 |
| `me.zhangls.kmp-koin` | Koin 注解、BOM、Koin 编译器插件，并应用 KSP 插件供 Room 使用 |
| `me.zhangls.detekt` | 根配置、各模块 baseline、手写 KMP 源集扫描、平台 compilation 类型解析、Compose Rules 和报告 |

新增 Feature API 使用 `me.zhangls.kmp-compose-api`，不要自行重复配置 Compose 插件。依赖版本统一来自 `gradle/kmp.versions.toml`。

Koin 由 `io.insert-koin.compiler.plugin` 处理注解，通过 `org.koin.plugin.module.dsl` 装配，不使用 Koin KSP 生成模块。Room 的 KSP compiler 依赖由 `core:database` 显式配置到 Android、iOS 设备和模拟器 target，schema 由 `room3` 插件导出。

共享模块统一启用 Android `withHostTest {}`；包含 Compose UI 的宿主测试目前位于 `composeApp`、`feature:email` 和 `feature:profile`，由各模块启用 Android 资源并声明 Robolectric/Compose 测试依赖。iOS target 为 `iosArm64` 与 `iosSimulatorArm64`，均输出静态 framework。测试选择与命令见 [测试指南](testing/README.md)。

`me.zhangls.detekt` 的普通任务扫描各模块实际存在的手写 Kotlin 源集，排除模块实际 `build` 目录，使用模块自己的 `config/detekt/baseline.xml`。类型解析任务保留插件提供的 Android compilation 源集、classpath 和 friend paths，生成类通过 classpath 参与符号解析；Native 源码的普通扫描不能代替 Native 编译和运行验证。Compose Rules 版本只在版本目录维护，当前版本全部规则启用，根配置仅保留偏离默认值的选项。任务入口与覆盖边界见[测试指南](testing/README.md#类型解析与-compose-检查)。根项目的无源码结果不能代表项目检查通过。 类构造函数允许按可读性选择单行或多行；根 detekt 配置关闭 `ktlint.ClassSignature` 的强制换行形状检查，缩进、行长与参数列表检查继续由其他规则负责。

IO dispatcher 由 `DataModule` 构造参数提供，默认使用 `Dispatchers.IO`，作为模块成员 provider 注册后供数据库、文件、邮件和头像流程注入。当前只注册一个 `CoroutineDispatcher`；测试显式传入测试 dispatcher。`core:database.create()` 的公开参数也允许注入，默认值保留原无参调用行为。模块 provider 放在 `DataModule` 内，避免 Native 跨模块顶层函数按返回类型包过滤的限制。

## 5. 运行时架构

### 网页打开能力

`composeApp` 在实际 UI 宿主中创建平台 `UriHandler`，通过根节点的 `LocalUriHandler` 提供给共享 UI。Feature 使用 `LocalUriHandler.current.openUri(url)` 或 Compose 的 URL 链接注解，不持有平台控制器，也不增加浏览器导航目的地。本能力不自动识别正文链接或新增页面入口。

HTTP/HTTPS 网页使用 Android Custom Tabs 与 iOS `SFSafariViewController`；其他协议交回 Compose 原有 `UriHandler`，保留邮件、电话等系统处理方式。网页地址须包含有效主机，不接受空地址、控制字符或缺少主机的 URL。无法打开时遵循 `UriHandler` 的 `IllegalArgumentException` 契约，由调用方处理失败提示，不静默吞掉失败。

Android 不绑定特定浏览器；未提供 Custom Tabs 时沿用其普通浏览器回退。iOS 仅从当前 Compose 宿主呈现 Safari 控制器；宿主未附着窗口或已有模态控制器时拒绝打开，避免跨窗口展示和重复弹出，组合销毁时关闭本能力持有的控制器。本能力不用于 OAuth 登录或 JS 交互。

关于应用的「开源软件声明」进入 About 所属的全屏 `OnlineNoticesDestination`，许可证与 NOTICE 使用独立的 `OnlineNoticeDetailDestination`，保存校验过的声明片段 ID 或 HTTP(S) 项目地址。组合根用相同的场景键装配列表与详情，并复用窗口和铰链策略。宽窗保留列表并替换所选详情，窄窗用全屏详情，返回恢复列表位置。Android 通过 `AndroidView` 嵌入系统 WebView，iOS 通过 `UIKitView` 嵌入 WKWebView。两栏从注入的 `NotesWebsite` 加载在线网页，详情链接由原生导航接管，项目来源复用详情窗格。状态栏随应用窗口，底部安全距离放在可滚动内容内。旧版声明目的地仅用于解码历史导航状态。

About 将组合中解析的语言、系统与应用叠加字号、深浅色和 Material 配色附在启动 URL 上。站内链接保留这些参数。官网的 `:web` Compose 入口负责平台选择，并传递官网的外观参数。官网和应用的 WebView 共用 `:web` 的 Kotlin/Wasm Compose 声明列表和详情。声明由 Compose Canvas 布局和绘制，浏览器桥接负责链接激活和浏览器历史。网站构建保留固定地址及详情片段 ID，只嵌入声明数据与 Wasm 启动入口。已安装 App 读取原生 WebView 滚动位置，不能直接读取 Canvas 列表位置；上线前需要完成原生导航与滚动恢复验收。生成规则见[声明维护指南](../tools/open-source/README.md)，固定声明的发布流程见[网站部署](notes-web-deployment.md)。

关于页面入口仅接收导航参数；`AboutAppInfo` 只描述版本号和构建号。

新版本首次启动提示由 `composeApp/App.kt` 在登录状态及设置加载后协调，调用 `AboutEntry.ReleaseNotesDialog`；About 复用本地更新记录并只展示当前版本及平台内容，关闭后通过 `AppIntent` 写入 `SettingsRepository` 的已读版本号。首次安装同样提示，去重按版本名称而非构建号，规则见 [应用版本与更新记录](app-releases.md#新版本首次启动提示)。
### Koin 装配

各模块声明自己的 Koin 注解和 provider，`composeApp` 的 `NotesModule` 是组合根。实现模块负责实现并提供自己的 Entry；调用方只依赖 API 接口。

在 `androidApp/NotesApp.onCreate` 与 `iosApp/iOSApp.init` 中先初始化 Koin，再执行启动数据任务。启动任务 `InitData` 通过构造函数接收 Repository 和平台 `AppInfo`，由组合根解析并在应用级协程中执行；它本身不从全局容器查找依赖，也不持有协程作用域。Android `Context` 只在平台入口传给 Koin，`AndroidAppInfo` 和 `AndroidSystemToast` 仅保存 application context；ViewModel 不依赖平台 `Context`。

关于应用页面同样复用组合根的 `AppInfo`。宿主将版本名称与原始字符串构建号映射为 `AboutAppInfo`，通过 `AboutEntry` 传入 Feature；缺失值显示本地化“未知”。iOS 构建号展示保留 `CFBundleVersion` 原值，启动初始化的数值版本语义不变。共享品牌图标与名称由 `core:theme` 的 `AppIcon` / `appName` 提供，不向 Feature 暴露内部生成资源。设置页只发送 `SettingsResult.OpenAbout`，经 main 的宿主回调交给根导航。About 不要求登录，不带窗格标注；外壳隐藏导航套件，返回后保留设置 Tab 和滚动位置。页面按 `ContentWidth.Form` 单栏居中，滚动视口覆盖工具栏下方的全部可用宽度，左右留白与中间内容共用同一滚动状态。手动更新由页面作用域的 AboutViewModel 管理；检查和新版内容使用匿名客户端，安装交接由 About 的 Android 实现负责。渠道与发布约定见 [手动检查更新](app-releases.md#手动检查更新)。

应用组合根通过内部平台 `rememberSystemToast()` 装配 `SystemToast`。Android 使用 Koin 的 application-context 单例；iOS 每个 Compose 宿主创建并持有自己的 `IosSystemToast`，组合销毁时取消显示任务、关闭额外窗口。iOS Toast 从实际宿主视图的 `windowScene` 创建触摸穿透窗口，不枚举全局场景或读取主屏幕；根视图布局回调根据当前 bounds 与 safeAreaInsets 重新定位标签，支持窗口缩放与旋转。后台或未附着到场景的宿主不显示 Toast。全局 Toast 事件仍沿用应用的 SharedFlow 机制，多个活跃宿主可各自在自身窗口呈现；它不携带某个业务窗口的目标标识。

iOS Toast 标签使用 UIKit subheadline 文本样式并启用 Dynamic Type，系统字号变化通知触发宿主根视图重新布局。每个窗口的通知观察者随关闭移除。显示协程在 250ms 淡入后通过 UIAccessibilityAnnouncementNotification 发送文本，仅播报仍可见且位于前台场景的提示；替换、销毁会取消尚未发送的播报。announcement 不使用屏幕切换通知或请求读屏焦点，原有短／长显示时长保持不变。

Repository、数据库、DataStore、网络客户端和需要共享的服务按其生命周期注册；屏幕 ViewModel 使用 `@KoinViewModel` 并由目的地获取。纯函数、Reducer、mapper、短暂 UI 状态、窗口事实和每次导航才确定的参数不注册为应用级依赖。导航静态贡献是例外：Feature 以唯一 qualifier 注册 `NavigationContribution`，组合根通过 `getAll()` 收集。`composeApp` 与 `core:network` 启用 Koin `compileSafety`；新增绑定需通过 Android 与 iOS 编译验证。选择理由见 [平台服务决策](decisions.md#平台服务由入口与宿主拥有)。

### MVI

`MviViewModel` 提供 Intent → State (+ Effect) 的单向数据流。Feature 的 `Intent`、`Action`、`State`、`Reducer` 和 `ViewModel` 放在实现模块的 `mvi/` 包。

状态跨进程持久化是 opt-in：`savedKey` 默认 `null`。只有确认状态体积小、可序列化且不含密码、token 或其他敏感数据时，才允许传入 key。`AppViewModel` 显式使用 `savedKey = "state"`；邮件、搜索和登录状态有意保持为纯内存态。

#### 页面状态

页面级业务状态统一由所属导航条目的 ViewModel 持有，包括请求结果、加载与错误状态、
表单草稿、业务步骤和待确认操作。首次加载和用户操作的协程使用 `viewModelScope`。
旋转和宿主重建时复用已有状态与进行中的请求，首次进入新条目时重新初始化。
`remember` 用于动画、焦点对象、菜单等短暂控件状态和派生对象；
滚动位置等纯展示状态沿用 Compose 的可保存状态机制。跨进程恢复仍遵循上述持久化约束。

### Navigation 3

导航显示机制位于 `core:navigation`：`DeviceCornerNavDisplay`、页面缩放/淡出与遮罩、设备圆角、`SafeAreaSceneStrategy` 和 `PaneWindowInsets`。模块不读取第二套窗口事实，不依赖 `core:framework` 或 Feature，也不持有应用返回栈。公开面仅包括显示入口、宿主协调外壳所需的只读页面进度、动画时长和安全区包装；设备圆角值、几何计算与事件桥接保持内部实现。设备圆角的 expect/actual 与显示机制一起维护。

应用策略继续由 `composeApp` 拥有：`AppNavigationState`、`NavHandler`、`AppSceneStrategies`、`LayeredNavDisplay` 和 `TabFadeThrough`。`LayeredNavDisplay` 调用 `feature:main` 的 `AppShell`，并通过 `PageMotionState` 的只读进度协调外壳；显示模块内部维护进度读取器。`Destination`、`NavEffect`、`RequireLogin`、Deep Link 和导航贡献仍位于 `core:framework`，与显示机制分开维护。

顶层导航保留首页、收藏、设置三个独立 Entry，各自拥有返回栈。Tab 根之间没有返回历史；根页面返回交还平台，详情返回所属列表。点击不同 Tab 调用组合根的 `NavHandler.selectTab`，恢复目标 Tab 的页面、滚动位置和多选状态；重选当前 Tab 不改变当前详情。显式导航到 Tab 目的地（如深链）则回到该 Tab 根页面。

`AppNavigationState` 使用 `rememberNavBackStack` 分别保存各 Tab 栈和当前根选择。各栈的 `rememberDecoratedNavEntries`、SaveableStateHolder 和 ViewModelStore 装饰器在内容切换区域外持续存在；非活动 Tab 不绘制，但保留 Entry 的可保存状态及 ViewModel。邮件首次加载指示在列表外绘制，避免单个占位条目把待恢复滚动索引重置为 0。滚动位置可随保存状态恢复；邮件多选等 ViewModel 状态仅在当前进程保留，不启用敏感/大型 MVI 状态的跨进程保存。退出登录清空全部栈并释放对应 Entry；登录成功释放登录 Entry。

写信弹层仍由首页邮件列表及其 ViewModel 持有。Email Entry 通过 `onDraftVisibilityChange` 将可见状态通知组合根，组合根仅将这个布尔标记纳入可保存状态，以跨越折叠触发的 Activity 重建；窗口收窄到 directive 仅允许一个水平窗格且草稿展开时，组合根移除首页栈顶的邮件详情，使首页与写信弹层保持可见。草稿内容不搬到导航层，未打开草稿及其他场景的详情保持原导航行为。

`AppShell` 包裹邮件与设置的内容区；个人信息和关于应用由组合根放到外壳上方的全屏转场层，底层 Entry 保持组合，转场层不消费外壳的 Insets。页面自身负责预测返回时的轮廓裁剪。邮件导航期间侧栏和底部导航保持固定；当前 Tab 的装饰后 Entry 按底层内容与全屏页分配给两个 `NavDisplay`，两者以当前栈根作为组合 key；全屏层的透明根只作绘制锚点，不写入应用返回栈。切换 Tab 使用 300ms 的 Fade through：选中指示立即更新，旧内容先淡出 90ms，在不可见的中点切换活动返回栈、释放旧显示及其在途转场，再将新内容淡入 210ms。内容区仅调整透明度，侧栏和底部导航保持固定；同一时刻只有活动层处理输入与返回，外层保存的 Entry 状态继续保留。连续选择取消前一次动画，以最后目标为准；重选当前 Tab 不播放动画，登录页不参与 Tab 过渡。同一 Tab 内的详情导航保留页面动画和预测返回。深链与登录恢复依据详情的 `scene` 选择所属列表根。导航状态规则位于 `composeApp/NavHandler.kt`，由 `AppNavHost` 装配。

`core:framework` 定义：

- `Destination`：所有目的地的共同抽象；
- `NavEffect`：`Navigate`、`Replace`、`Restart`、`Popup`；
- `RequireLogin`：受登录状态保护的目的地；
- Deep Link 与导航贡献注册机制。

个人信息为 `feature:profile-api` 的 `ProfileDestination`，由 `ProfileNavigation` 登记序列化。`ProfileOrigin` 随 key 保存首页或设置入口，组合根据此选择对应 Tab 栈；两个入口共用 profile 的独立页面，返回与登录重定向均恢复发起入口。页面受 `RequireLogin` 保护，不带窗格标注。`ProfileDestination` 通过显式 `@SerialName` 保留拆分前的 wire identity（`me.zhangls.settings.api.ProfileDestination`），兼容已保存的 NavKey 和登录 redirectTo；代码和导航贡献均属于 profile-api，不依赖旧 settings 类型。连续全屏页面切换时，仍在返回栈中的下层页面保持原尺寸；进入只缩放新的栈顶页，返回只缩放并淡出真正出栈的页面，预测返回取消恢复当前栈顶。个人信息和关于应用显示期间，组合根向 `AppShell` 传入页面进度读取器，保持窗口对应的导航形态，由全屏页面的实际缩放进度直接驱动底部导航或侧栏的位置与内容区占位；复用原导航组件及其测量尺寸，不另启显隐动画。全屏页面与遮罩按完整窗口计算，遮罩作为页面背后的同级节点承接空白区域输入，不在原生 WebView 的父级消费拖动事件；底层邮件／设置内容保留原 Entry、返回栈和组合。拖动、反向、取消、进入和普通返回共用页面的缩放进度与曲线；缩放进度从 0 到 1 对应导航从隐藏到显示，页面提交淡出期间导航保持显示。上层可见及退出期间，底层不接收触摸、无障碍焦点或返回事件。隐藏导航终态不消费导航侧系统 Insets，恢复页面时导航仍保持隐藏。

Feature API 定义具体目的地和 Entry 契约。`composeApp/AppNavHost.kt` 聚合导航条目、维护根返回栈、执行登录拦截并恢复受保护目标。

首页和收藏是两个独立的列表场景。邮件详情是独立目的地，列表—详情组合由根导航的 `SceneStrategy` 负责；Feature 内不再创建第二套 pane scaffold。根导航通过 `rememberAppSceneStrategies` 为所有目的地装配统一场景策略：宽屏存在多个水平分区或平台报告横向铰链时，允许保留自适应单窗格场景；首页和收藏共用 `ListDetailPaneController` 按需分栏，半开姿态保留现有窗格策略；仅由窄窗口高度达到 Expanded 而产生的多个纵向分区，不作为保留自适应单窗格场景的依据。实际多窗格仍由 Adaptive 接管，普通单页统一交给 Navigation 3 的页面场景。登录页内部的 `SupportingPaneScaffold` 独立消费原有 directive，继续保持左右分栏、纵向分区数限制为 1 和既有铰链处理，不受根场景选择影响。单窗格进入详情与按钮返回复用预测返回的中心缩放：进入时由缩小状态放大到完整页面，返回时由完整页面缩小退出；进入缩放使用 `PREDICTIVE_MOTION_DURATION`；普通与手势返回都先保持不透明地补完缩小阶段，到达预览终点后才保持尺寸执行 `PAGE_EXIT_FADE_DURATION` 的页面淡出，列表保持原位，不使用横向滑动；拖动预览和取消恢复保持页面不透明；灰色遮罩透明度随缩放进度变化，最高为 24%。根导航通过 `DeviceCornerNavDisplay` 装配库的 SceneState 与返回事件；单页预测返回采用与新建邮件 Material 3 BottomSheet 相同的 CubicBezier(0.1, 0.1, 0, 1) 曲线；终点固定为原尺寸的 85%，横纵轴使用同一比例，不随页面宽度改变，保持文字与图片比例。普通进入／按钮返回在自身内容区内居中缩放；手势从物理左侧开始时锚定视口横向 75%、纵向 50% 处，从右侧开始时锚定横向 25%、纵向 50% 处（距对侧边缘均为视口宽度的 25%），拖动途中及松手后的取消／提交保持该锚点，恢复完整尺寸后清除；尺寸用于计算锚点偏移，缩放终点统一为 85%，不测量或补偿页面相对窗口的偏移。背景和圆角裁剪放在可见页面层内，以该层的实际尺寸为裁剪边界，避免底部圆角落在页面之外或残留背景遮挡上一页。只有页面参与变换，外壳不缩放。缩放时在上一页与当前页面之间绘制灰色遮罩，透明度为 24% 乘以当前缩放进度；手势取消时遮罩直接随缩放进度回退；普通／手势提交均在剩余缩小完成后，与页面共用 `commitProgress()` 淡出，淡出时长由 `PAGE_EXIT_FADE_DURATION` 决定。遮罩透明度统一为 `0.24 × motionProgress × 页面不透明度`，不维护独立计时器，页面消失时归零。遮罩不参与缩放；邮件详情的遮罩只覆盖内容区，个人信息／关于应用的遮罩覆盖包含底层导航在内的完整窗口。单页邮件详情的 Scaffold 全尺寸绘制，包括状态栏下方的安全区域背景；安全区仅用于页面内部内容避让，不在缩放层之外预留顶部 padding。系统状态栏图标不属于应用页面。绘制层直接读取原始手势状态，拖动不经过弹簧动画的时间采样；仅供 NavDisplay 使用的事件输入将预览映射到 `PREDICTIVE_MOTION_DURATION / (PREDICTIVE_MOTION_DURATION + PAGE_EXIT_FADE_DURATION)`，为手势达到 100% 后仍保留完整的页面淡出时长。取消时反向映射为原始缩放进度；提交时从松手姿态保持不透明地补完剩余缩小，再固定最终姿态淡出，淡出结束后移除页面与遮罩。普通返回自动播放完整的缩小阶段，两条路径使用同一缩小/淡出阶段边界；进入和取消不淡出。当前缩小时长为 220ms、页面淡出时长为 120ms；十倍调试时长已用于过程验证，测试按实际时长取样。独立事件分发器仅驱动场景动画，不拥有导航栈，随显示容器销毁而释放。Android 通过 AndroidX Core 的 `ViewCompat.getRootWindowInsets()` 和 `WindowInsetsCompat.getRoundedCorner()` 读取当前窗口四角半径（底层圆角能力要求 Android 12+），对参与缩放的单页 Scene 添加以页面实际边界为轮廓的圆角裁剪，半径按页面最终缩放比例补偿，拖动期间显示半径不随手势进度变化；取消返回时，圆角可见度按当前页面缩放进度相对松手时缩放进度的比例同步回退，页面恢复完整尺寸时一并恢复未裁剪轮廓并解除裁剪，不再追加独立的圆角恢复动画。分栏 Scene、应用外壳与浮层不增加该变换或裁剪；平台读取结果为 `null` 时，两端统一采用 48dp 默认半径（对应 iOS 逻辑长度 48pt），并按当前 Compose density 转为 px；系统提供的零半径保留，不触发默认值。iOS 当前仍未读取原生圆角，因此使用该默认值。默认值由 维护者 指定为 48pt（2026-10-07），作为两端共用的设计值，不再采用机型表中位数；不引入第三方圆角库或调用私有 API。详情返回键由 `LocalListDetailSceneScope` 判断，而不是用窗口宽度推测。

### 自适应布局

窗口事实只有一个读取点：`composeApp` 调用 `rememberWindowAdaptiveInfo()`，再通过 `LocalWindowAdaptiveInfo` 下发。`PaneScaffoldDirective` 也在组合根从同一份信息计算，并通过 `LocalPaneScaffoldDirective` 提供。directive 使用 `HingePolicy.AlwaysAvoid`，所以同一铰链几何在半开和全开之间不会从排除区消失。

邮件编辑弹层通过 `core:theme/LocalModalViewport` 获取完整应用视口尺寸。`AppNavHost` 在导航外壳和列表—详情分栏之外使用 `ProvideModalViewport` 下发根布局的有限约束；它不再次读取平台窗口事实，也不增加安全区或铰链包装。iOS 邮件弹层使用显式完整视口大小的 Dialog 容器，并由 Material 3 `BottomSheet` 处理拖动和展开；Android 保留标准 `ModalBottomSheet`。完整容器和滚动表单均使用有限约束，保证旋转后弹窗的无界首选尺寸测量不会产生无限滚动约束、内容宽度溢出或错误的拖动锚点。

根导航的 `ListDetailPaneController` 消费同一份 directive 和铰链姿态，为首页和收藏提供展开与收起规则。展开时沿用 Material 的窗格首选宽度和纵向铰链排除区；没有详情且无需保持半开布局时，列表占满可用宽度。水平折痕不决定上下窗格比例。

窗格之间的普通水平、垂直间距统一为 `0.dp`；真实铰链的排除区域仍然保留，不因取消间距而压缩。页面内容（例如邮件卡片）自身的内外边距独立保留。

`core:theme/layout` 的 `HingeGeometry` 只负责为窗格脚手架生成稳定的排除区域。登录页的 `SupportingPaneScaffold`、邮件与在线声明的列表—详情 SceneStrategy 消费铰链指令。有邮件详情时，邮件场景根据窗口能力使用单页或左右分栏；登录页只支持左右分栏，始终把竖向分区数限制为 1。Material directive 的排除区域仅包含竖向铰链；按用户审阅截图后的决定，登录页不在窗格内避让横向铰链，品牌与表单使用完整可用高度，保持原有居中与滚动行为。旋转、小窗和内外屏切换改变真实几何时正常重排，只改变折叠状态而几何不变时保持窗格稳定。

登录页设置入口位于表单区域右上角。Logo、输入框与按钮组成的表单主体在高度足够时居中；高度不足时保持原有尺寸和间距，由纵向滚动与 IME 留白处理。

页面默认要求内容延伸至底部安全区域，实现 Edge-to-Edge。滚动视口延伸至底部，底部安全距离作为可滚动内容的末尾留白，保证末项滚到底后完整可见、可操作；固定底部控件仍避让手势区域，已消费的 Insets 不重复计算。顶部和左右仍按安全区布局。

Android 宿主使用 `enableEdgeToEdge()`，MainActivity 显式保留 `android:windowSoftInputMode="adjustResize"`；minSdk 保持 28。移除该属性后，连接的 API 36 真机采用 `adjustPan`，登录按钮被滚动视口裁切；恢复后由 维护者 确认问题消失。键盘避让由实际页面或弹层消费 Compose IME Insets：登录表单消费受保护的 inset，写邮件使用弹层内容 inset；全屏搜索沿用 Material 的 `fullScreenWindowInsets`（`safeDrawing`），停靠搜索沿用组件内置 IME padding，不重复添加占位。API 28/29 的真实窗口分发仍需设备验证，API 30+ 宿主注入回归不能证明旧系统兼容性，范围见 [Android IME 回归](testing/adaptive-ui.md#android-ime-窗口策略2026-10-09)。

登录与新建邮件页的 IME 切换保护由 `core:theme/layout` 的 `ImeHandoffFocusState` 和 `rememberImeHandoffInsets` 共享。Android 在焦点回调中记录同一表单内的输入框切换，仅在切换后的 400 ms 窗口内保留下降中的键盘占位，待高度连续稳定 200 ms 后释放，以吸收普通键盘与系统安全键盘切换时的短暂空档。高度增加立即避让，失焦立即取消保护；保护窗口外的正常收起逐帧跟随真实 IME，不额外等待。登录表单通过 `windowInsetsPadding` 消费受保护的 inset；新建邮件页把它与 `ModalBottomSheet` 默认内容 inset 合并，让弹层继续在自己的窗口中避让键盘。iOS 使用原生 IME inset，不应用 Android 保护策略。

登录表单、搜索结果列表和写邮件表单通过 `core:theme/layout` 的 `Modifier.imeGestureControl()` 联动滚动与软键盘。Android 包装官方 `imeNestedScroll()`，API 30+ 在嵌套滚动中控制键盘并在释放后完成展开或收起；API 28/29 沿用官方无操作行为。iOS 原样返回 Modifier。手势包装不添加 Insets 占位，不改变表单的焦点交接保护；写邮件需要同时验证表单滚动、键盘控制与弹层拖动。

单栏 UI 不参与铰链适配。应用导航、设置、搜索、普通页面、`ModalBottomSheet`、Preference 弹窗、tooltip 和 `Dialog` 始终使用标准组件，不读取铰链，也不因铰链存在而切换组件树、尺寸、滚动或弹出位置。是否避让铰链由实际窗格布局决定，而不是由整个应用统一套一层安全区域决定。

约束如下：

- Feature 不直接调用 `currentWindowAdaptiveInfo*()`，也不自己维护窗口断点。
- Feature 不缓存铰链、不按 `isSeparating` 过滤铰链，也不另算安全区；非窗格 Feature 不读取铰链。
- 搜索栏等局部形态读取窗口事实，不得从“导航在底部还是侧边”反推宽度。
- 搜索锚点以空查询输入框的首次自然测量宽度为基准，不复制 Material 默认宽度常量。基准仅保存在当前 UI 实例，密度、字号、布局方向或提示文本变化时重测；窗口变窄时服从父约束，不覆盖基准。停靠展开沿用锚点宽度，全屏展开沿用库的动画约束；测量输入框不绘制、不参与焦点或无障碍语义，也不修改真实查询。
- `AppShell` 根据统一窗口尺寸选择导航形态：宽度 ≥600dp 且高度 <480dp 优先使用折叠 Rail，为内容保留纵向空间；其余沿用库策略，并在高度充足的 ExtraLarge（≥1600dp）窗口展开 Rail。窄窗口保留底栏，不比较尺寸档位下限来推断横竖屏；折叠状态变化不切换底栏/侧栏，避免相同几何下内容窗格移动。
- `AppShell` 消费导航套件占用的系统内边距；Feature 不接收导航方位参数，外壳不另算铰链安全区域。
- 列表—详情场景复用 `SafeAreaSceneStrategy`。开源声明位于全屏导航层，窗口报告铰链前后保留同一个安全区包装，实际预留空间与消费值同步更新。无铰链时，场景按目的地配置预留并消费剩余的 `WindowInsets.safeDrawing`，使用库默认的列表首选宽度（通常 360dp，ExtraLarge 为 412dp），详情占剩余宽度。有铰链时，场景级预留归零，按 directive 的物理排除区域分区；NavEntry 装饰器通过 `PaneWindowInsets` 消费窗格外的空间，各窗格的标准 `Scaffold` 仅避让实际相交的系统安全区。铰链几何优先于首选宽度，普通单页由自身组件处理安全区。
- 可读宽度统一用 `ContentWidth.Form`、`Article`、`Prose` 和 `Modifier.contentWidth()`。
- Nav3 SceneStrategy 消费组合根提供的 directive，负责列表—详情分区；无铰链时仅场景整体预留系统安全区，不增加铰链安全带包装。
- 登录页只按 `maxHorizontalPartitions` 决定左侧品牌区与右侧表单区是否同时展开。横向铰链不触发上下重排：宽度足够时仍左右分栏，宽度不足时使用单列表单。
- 搜索、写邮件、设置选择和确认对话框使用标准组件，不增加铰链安全弹层；查询、草稿、滚动和选择状态独立于布局形态。

### Material 3 Expressive 主题

应用内功能图标统一来自 [Google Fonts Material Symbols](https://fonts.google.com/icons)，采用 Rounded，默认未填充（`FILL=0`），由 `core:theme` 提供共享矢量。明确的填充变体允许保留，`StarFill` 使用官方 `FILL=1` 版本表达收藏状态。这里的未填充指 Material Symbols 的 FILL 轴，并非要求矢量路径不使用填充绘制。登录页、关于应用页与桌面启动图标属于品牌图标，保留现有设计；品牌图标的深浅色适配独立于功能图标风格约束。

功能图标由 `tools/material-symbols/generate.py` 从固定版本的 Google 官方 Rounded SVG 生成，默认参数为 `FILL=0 / wght=400 / GRAD=0 / opsz=24`，填充例外在 manifest 中显式记录；原始 SVG、名称映射、版本与 SHA-256 一并保存。`--check` 校验来源文件、生成输出和目录清单。生成 Kotlin 去掉原始注释，缓存属性使用 `cached<Name>` 命名并保持原有惰性复用机制，保留 `path { ... }` 指令，不使用 `addPathNodes`，填充颜色统一为 `Color.Black`。邮件收藏保留 Star/StarFill 切换，同时使用主题按钮颜色及无障碍文案表达状态。

共享 UI 的主题入口是 `core:theme/ComposeAppTheme`，使用 `MaterialExpressiveTheme` 和 `MotionScheme.expressive()`。字体与形状分别由同模块的 `AppTypography`、`AppShapes` 提供；标题与操作标签使用强调字重，正文保留 Material 原有尺寸。Typography 显式使用带 fontFamily 的构造器，避免兼容无参构造器把强调样式映射回普通样式。

Android 的 Material 提示弹窗在主题层统一使用 edge-to-edge Dialog 窗口，以整个应用窗口为居中参照，避免隐藏手势提示线后上下系统栏占用不对称造成偏移；通过 `LocalBasicAlertDialogOverride` 控制容器测量，复用 Material 内容版式，保留本地化窗格语义与关闭行为，覆盖更新内容、通用提示和 Preference 选择框。Android/iOS 在主题入口以当前应用窗口宽度的 80% 设置最大宽度，内容按需展开；保留 Material 的 280dp 最小宽度（空间不足时缩小），替代默认 560dp 上限；关闭 `usePlatformDefaultWidth`，避免平台固定/首选宽度截断 80% 上限；Android 默认最小边距单独取自默认弹窗主题背景的左右 padding，取较大侧保持居中，iOS 默认无额外水平边距。两端叠加左右系统安全区的较大值，最大宽度为窗口 80% 与扣除两侧边距后可用宽度的较小值，短提示不得强制填满该上限。垂直方向以上下 `safeDrawing` 安全区的较大值作为对称边界，最大高度不超过窗口高度减去两倍边界；保留全窗口居中，长内容在弹窗内部滚动，不侵入状态栏或底部系统区域。窗口尺寸在弹窗外读取并随窗口变化更新，避免用弹窗自身宽度反复计算；不对弹窗添加铰链避让或固定位置补偿。

主题保留内置品牌配色、动态色、深浅色和用户字号设置。应用字号是相对系统字号的倍率：标准字号复用系统 Density，中／大字号将系统 fontScale 与应用倍率相乘，保留 density；窗口事实仍在主题之外读取。Feature 从 `MaterialTheme` 读取颜色、字体与形状；关键按钮和共享图标按钮显式采用形变重载，自定义头像空间动画使用主题 MotionScheme。邮件列表与详情卡片使用主题 large 圆角和容器色，打开、多选、收藏状态各有语义颜色。确认对话框使用标准 AlertDialog，保留字符串与 AnnotatedString 重载，以及显式按钮回调关闭语义。

登录有效提交在 Intent 同步处理阶段置为加载中，加载期间 ViewModel 拒绝重复提交及表单修改，UI 禁用账号、密码、清空、可见性切换和登录按钮，并释放输入焦点。主题与语言入口继续可用。共享加载指示器只负责视觉与带名称的 polite live region／进度语义，交互约束由调用方的 enabled 状态和业务守卫承担。

`core:preference/PreferenceRow` 接收 `isGroupStart / isGroupEnd` 渲染外角与内角；分组清单、顺序和独立操作的选择仍由 `feature:settings` 负责。底层 Compose Preference 控件继续隐藏在 implementation 依赖之后。主题迁移不引入第二套窗口事实、导航策略或铰链避让。

#### 表单风格

账号表单（登录、注册、个人信息）使用 `core:theme` 的共享描边输入框。字段统一采用
`MaterialTheme.shapes.large`（20 dp）、`typography.bodyLarge` 和未聚焦时的
`colorScheme.outlineVariant`；标签、键盘行为、校验反馈与无障碍语义由各表单提供。
表单底部的成对操作沿用确认弹窗的层级：取消用文字按钮，主操作用有按压形变的实心按钮，
横向间隔 8 dp，靠尾端对齐。单栏表单使用共享内容宽度，并将底部安全距离留在滚动内容内。

### 设置项

设置项拆成四部分：

1. 展示元数据和通用渲染位于 `core:preference`；
2. 当前值来自 `core:data` 的 `SettingsModel`；
3. 行为由 `feature:settings` 的 Intent/ViewModel 拥有；
4. `SettingsPreferenceMapper` 将 spec、值和回调装配为 `PreferenceUiModel`。

`PreferenceSpec` 不持有状态或 Feature Intent。设置清单和顺序属于 `feature:settings`，平台差异通过能力位过滤。个人信息入口的静态元数据位于 `core:preference/ProfilePreference`，点击由 SettingsIntent 与 SettingsResult 通知宿主导航。

`core:preference/ProvidePreferenceLocals` 显式向底层控件库传入 `MutableStateFlow<Preferences>(MapPreferences())` 作为空的内存数据源。实际值和回调由消费方提供，渲染不使用控件库默认的平台偏好存储。默认初始化曾读取整个 iOS `NSUserDefaults` 域，因 UIKit 写入的嵌套字典不受库支持而导致进入设置页闪退（修复提交 `a3655b11`）。修改包装器或升级依赖时，运行 `PreferenceLocalsTest` 验证没有应用偏好域时仍能初始化，并检查显式数据源未被移除。

设置清单统一定义在 common；expect/actual 只提供平台能力位。`SettingsModel.toPreferenceUiModels` 的 `dynamicColorSupported` 参数默认取平台值，允许测试覆盖支持与不支持的清单。Action 与 Select 通过 settings 内的私有、类型安全辅助函数绑定 Intent，UI 仍以当前 SettingsModel 和 ViewModel 为 key 缓存映射结果。

`ProfileEntry` 的实现与 `ProfileModule` 仅由 composeApp 装配；main 依赖 profile-api 传递 `ProfileOrigin`，settings 保留入口点击结果，不依赖 profile 实现。个人信息页面从 `UserRepository.userFlow` 展示账号资料。头像与账号编辑由 `feature:profile` 管理，导航条目作用域的 `ProfileViewModel` 保留编辑草稿、邮箱步骤和请求，旋转后继续使用。编辑状态仅保存在内存中，关闭表单或切换账号时丢弃。邮箱修改通过当前会话分步验证密码、发送验证码和提交邮箱更新，底部主按钮随当前步骤更新文案。认证服务在发送时复核密码，验证码按目标地址和会话校验；通行密钥仍关联固定用户 ID。共享用户头像和默认图片由 `core:theme/UserAvatar` 提供，搜索头像按钮只调用宿主导航；搜索结果的发件人头像不再重复宣告「个人信息」。

通行密钥管理由 profile 实现，目的地和管理操作契约由 profile-api 提供。设置页发出打开页面的结果；组合根装配导航，将现有认证客户端与平台凭据提供方接入。登录 Feature 继续负责使用通行密钥登录。

认证账号在登录后和进入个人资料页时从认证服务读取用户名、已验证邮箱和头像版本，保留私有文件缓存供离线使用。换头像复用系统选图入口，缩放为 512 像素以内的 JPEG。服务端按会话中的账号校验并保存，客户端收到成功响应后才提交新路径和版本。演示账号继续使用设备本地头像。已有设备本地头像在服务端无头像时继续可见，用户主动更换时上传。

`UserRepository` 在同一 mutex 中校验账号、认证来源和资料快照，原子更新服务端资料字段，同时保留设备锁和搜索历史。邮箱变更完成后，使用新邮箱发起密码或通行密钥登录。头像使用唯一文件名，缓存提交成功后回收旧文件，失败或取消时清理未提交副本。保存期间拒绝重复操作，Profile MVI 状态保持在内存。

## 6. 数据与资源规则

- Room 字段或 schema 变化必须配置 AutoMigration，并更新 `core/database/schemas/`。
- `AppDatabase` 的包名与 schema 目录耦合；移动类时必须同步迁移以全限定名命名的 schema 目录。
- 数据库文件路径由 `core:data` 的 `AppFileManager` 提供，平台建库差异由 `AppDatabaseFactory` 的 expect/actual 承担。
- `AppDataStore` 只负责普通资料/设置的 JSON 序列化，不存储登录凭据。`UserModel` 不含 Token；`UserRepository.login(user, tokens)` 串行协调资料与凭据，`getTokens()` 仅供组合根网络适配使用，Token 不进入 UI State。
- `core:data/impl/security/SecureTokenStore` 是内部契约：Android 用 Keystore AES/GCM 和随机 IV 加密整组账号凭据，经 `AtomicFile` 写入 `noBackupFilesDir`；iOS 用 Generic Password Keychain，`WhenUnlockedThisDeviceOnly`，不启用同步。永久密钥失效或损坏密文会清除凭据；临时访问失败不回退明文。登录态 Flow 保留最近状态并执行可取消重试；请求级 getTokens 仍抛出失败，不使用旧凭据兜底。
- 旧 `user` JSON 首次访问时迁移，安全写入并读回确认后才移除明文 Token。退出先清除旧明文字段，再清除凭据并删除资料；账号与凭据不匹配时不公开登录态。认证客户端按请求读取当前凭据，通过组合根注入 `core:network` 创建的 HTTP 客户端。会话刷新、错误码解析与认证地址由 Notes 认证代码管理。认证端点未配置时，演示构建继续使用本地模拟会话。启用认证端点的构建使用 Notes 认证服务；第三方 Ktor 客户端不读取真实会话令牌。
- 应用组合根直接注册 `NetworkModule`，并把构建提供的官网、APK 下载和认证 origin 分别注入 `NotesWebsite` 和 `AuthenticationConfig`。根导航用官网配置校验深链来源，账号会话和认证请求共用认证配置。「关于应用」使用独立的匿名客户端检查更新。
- `composeApp` 的 `AccountSession` 管理账号会话与按需刷新。`core:data` 继续负责平台安全存储和凭据的条件更新。普通请求可并行执行。服务端返回 `401 invalid_session` 时，暂停新请求并等待在途请求完成，再轮换凭据，避免旧 Token 在写入提交前失效。同一批旧 Token 请求共享刷新结果，只在同一会话内重发一次。轮换前将操作 ID 与原凭据一起原子写入安全存储。断线后沿用该 ID，在五分钟内可向服务端取回同一次结果。`403` 保留业务流程自己的处理，超时和连接失败不自动重发。认证失败诊断保留 HTTP 状态、业务码和 `X-Request-Id`，不记录请求正文或凭据。
- 生物识别解锁设置随本机会话资料保存，旧资料和新登录默认关闭。设置入口通过 `SettingsResult` 请求根层验证；Android 与 iOS 宿主发起系统设备验证，支持设备锁屏凭据回退。启动时组合根等待本进程首次确认会话；有锁定会话时仅显示解锁页，导航、待处理深链和更新提示等验证成功再恢复。授权只保留在进程内，退出会话时清除。保护范围是应用内访问，Room 和 Token 存储沿用现有机制。
- 认证在组合根装配，登录 Feature 通过宿主提供的认证契约发起账号密码或 Passkey 登录。所有 Android 和 iOS 构建默认连接 `https://auth.notes.zhangls.me`，提供注册并启用 Passkey，认证配置独立于应用更新渠道。Passkey 入口还要求平台支持，实际使用需要签名应用与认证域名建立关联。注册发码时拒绝已注册邮箱。用户使用已验证邮箱和密码或 Passkey 登录。Passkey 登录挑战绑定当前邮箱和账户 UUID，凭据及本地数据仍以稳定的账户 UUID 标识，独立 HTTPS 客户端负责验证、轮换与撤销会话，刷新仅更新安全凭据。协议和测试域名配置见 [认证服务](../server/auth/README.md)。
- Android 真实账号登录使用 Compose Autofill，账号和密码由系统自动填充服务管理。登录输入框使用 [RoundedAutofillHighlight](../core/theme/src/commonMain/kotlin/me/zhangls/theme/component/RoundedAutofillHighlight.kt) 将高亮绘制为与输入框一致的形状。认证客户端确认服务端响应有效后、发布会话前通知登录表单提交自动填充会话。演示登录和 Passkey 流程不主动提交密码保存。
- 真实账号在 `notes-<uuid>.db` 使用独立 Room 文件，表结构沿用现有 schema。组合根在账号数据库就绪后才挂载受保护导航；切换账号时退役旧数据库和查询。原 `notes.db` 保留给本地演示会话。真实会话的邮件深链须同时携带匹配的 `accountId`，避免数字邮件 ID 在不同数据库间碰撞。
- Android 排除旧 `notes.preferences_pb` 的云备份及设备迁移，iOS 排除 DataStore 目录备份，避免未完成迁移的旧明文被备份。这也意味着普通设置不会随该文件备份迁移；旧有备份中的数据无法由本次升级追溯删除。
- Compose Resources 的格式化占位符必须带位置，例如 `%1$s`；无位置的 `%s` 不会被正确替换。
- 同一段通用 Compose 渲染在两个或更多位置出现时，评估下沉到 `core:theme` 或 `core:preference`，但单一 Feature 的业务组件仍留在 Feature。

## 7. 变更检查清单

### 新增 Feature

- 创建同级的 `feature/<name>` 与 `feature/<name>-api`。
- API 只放目的地、Entry 和必要参数；实现依赖自身 API。
- 需要其他 Feature 时只依赖其 API。
- 在 `composeApp` 加入实现模块并完成 Koin/导航装配。
- 跨模块 `@Composable` 抽象 Entry 方法不提供默认参数，在 iOS 上通过接口调用真实实现验证分派。
- 使用现有约定插件，并运行相关平台编译和测试。

### 移动共享类型

- 先确认真实消费者和所有者，不因“可能复用”提前下沉。
- 检查移动后是否引入 Core → Feature、数据层 → UI 或 Feature 实现互相依赖。
- 若类型出现在公开签名，核对 Gradle 依赖应为 `api` 还是 `implementation`。

### 修改数据库

- 更新 entity/DAO 与 mapper，不向 `core:data` 公开面泄漏 Room 类型。
- 配置 AutoMigration 并核对 schema 导出。
- 运行 `core:data` 的 iOS 模拟器测试和相关应用编译。

### 修改自适应布局

- 修改前列明需要保持的组件、尺寸、留白与交互；改变既有视觉设计时单独说明并取得确认。
- 使用 `LocalWindowAdaptiveInfo` 或 `LocalPaneScaffoldDirective`，不新增窗口读取点。
- 优先表达实际事实或库计算出的能力，不用其他 UI 形态作代理。
- 覆盖紧凑、横屏、宽屏和折叠姿态；模拟器步骤见 [Android 模拟器测试](testing/android-emulator.md)。
- 结束测试前恢复模拟器分辨率、密度和旋转设置。

### 修改导航、输入或浮层

- 导航、搜索与状态恢复覆盖动画中间帧、提交与取消、快速连续操作、返回、尺寸变化和保存状态恢复，按 [UI 变更验收清单](testing/adaptive-ui.md#ui-变更验收清单) 选择场景。
- 依赖前置动画完成的导航明确执行顺序，避免离开组合后保存半完成状态；依赖 Scene 身份的静态窗格元数据保持实例稳定。
- 输入容器覆盖键盘增高、切换空档、变矮、正常关闭及失焦，检查系统栏与 IME inset 没有重复消费。
- iOS 弹层的容器、滚动内容和拖动锚点使用有限的完整视口约束，并在设备上验证关闭后旋转、再次打开和按钮可达性。
- 设置控件初始化显式提供内存数据源，修改包装器或升级依赖时验证不使用默认平台存储。

### 邮件详情窗格

首页邮件列表在全开/普通宽窗未选择详情时铺满内容区；选择后才按根 SceneStrategy 分栏，
列表尺寸和详情进入动画为 250ms，详情退出为 200ms，关闭保留期为 350ms，并随系统动画比例缩放。全开时单列表清空铰链排除区域，
详情展开后恢复原始指令；首页保留原有系统安全区处理。连续选信替换首页
详情，关闭期间保留原详情及列表实例，延迟弹栈仅作用于发起关闭的返回栈。
首页与收藏的双窗格详情右侧均提供关闭按钮，窄屏保留返回；收藏页的分栏与空态策略保持原状。

半开姿态（窗口事实中存在非平展铰链）优先于按需展开：首页列表
保留物理分栏及空详情区域，打开和关闭详情时列表分区不伸缩。详情内容
使用相同的滑入/淡入、滑出/淡出动画。全开时未选详情仍为全宽列表；
已有详情时半开/全开的相同铰链几何继续保持相同分区。半开空详情区域
使用页面底色，避免透出下层页面。

首页半开详情空态复用 Main 模块原有 EmailDetailPlaceholder；提示文本不属于
被滑出/隐藏的邮件正文。未选邮件及完成关闭后显示提示，详情显示和退出
期间不叠加空态。

收藏页分栏详情的关闭按钮回到收藏根列表并清除该栈的详情历史，避免关闭后
露出上一封邮件；窄屏返回仍只弹出当前条目。首页关闭继续使用原窗格动画。

### Android 壁纸主题

Android 宿主通过 WallpaperManager 的桌面壁纸颜色生成 Material 3 配色，支持 Android 9 及以上。颜色读取在后台执行，前台期间监听颜色变化，每次返回前台重新读取；短暂读取失败保留当前颜色，首次不可用时使用内置主题。共享 UI 继续通过 App 的动态配色入口接收 ColorScheme。系统主题预设不参与壁纸主题生成。

## 官网、发布渠道与认证域

官网从 `web/` 构建，在运行时读取 `/releases/published.json`。网站生成器从包内 `releases.json` 生成接口数据，分发清单来自各现行渠道登记的当前构建。各页面的 HTML 仅提供启动入口和所需的数据。首页、下载、更新日志、开源声明和 404 复用同一 Kotlin/Wasm 页面框架，按可用宽度和字号在 Material 3 顶部导航与左侧导航栏间切换。App 内嵌的更新日志和开源声明隐藏官网导航。About 通过 `AppInfo` 读取当前渠道，用独立匿名 Ktor 客户端检查宿主配置的官网更新清单。安装版本的更新摘要随包分发，完整更新日志和开源声明由网站提供。认证会话按服务来源隔离。部署及验收见 [网站部署与应用发布](notes-web-deployment.md)。
