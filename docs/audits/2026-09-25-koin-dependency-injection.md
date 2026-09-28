# Koin 依赖注入边界审查与优化建议

日期：2026-09-25

状态：历史审查；2026-09-25 已落实启动任务、平台服务定位器和编译检查优化。以下“当前”描述为改动前快照；现行规则以 [当前架构](../architecture.md) 为准。

## 后续实施结果（2026-09-25）

- 复查 SwiftUI `iOSApp.init()` 后，更正了“iOS 未初始化 Koin”的误判；Android/iOS 入口均保持先启动 Koin 再调用 `initData()`。
- `InitData` 改为 `@Factory` 构造注入、挂起 `run()`；应用组合根取得任务并管理协程，行为测试覆盖首次插入与后续不重复插入。
- `AppInfo` 与 `SystemToast` 改为平台实现的接口绑定。Android 实现通过构造函数取得 application context；代码库已不再使用 `KoinComponent`。
- `composeApp` 和 `core:network` 的 `compileSafety` 已打开。`:androidApp:assembleDebug`、`:composeApp:compileKotlinIosSimulatorArm64` 和 `:composeApp:iosSimulatorArm64Test` 通过；尚未做设备冷启动验证。
- `SettingsHandler` 与 DAO 的生命周期调整属于可选优化，未发现需要立即变更的行为问题，本次保留原定义。

2026-09-28 按问题分别提交：启动任务与版本信息、系统 Toast、编译检查。每个提交阶段均完成 Android/iOS 构建验证，最后再次通过 `:androidApp:assembleDebug` 与 `:composeApp:iosSimulatorArm64Test`。本次未新增设备冷启动验证。

## 范围与判断标准

原始审查覆盖 `androidApp`、`iosApp`、`composeApp`、Core 和四个 Feature 的 Koin 定义、解析入口与生命周期；审查阶段没有修改运行时代码，也没有执行 Android/iOS 应用。下文的“风险”是当时的静态审查结论，不等同于已复现故障。

DI 用来组装**具有外部依赖、需要替换的实现、共享生命周期或平台资源**的对象。首选构造函数传入依赖；只在应用入口、导航/页面入口等组合边界从容器取对象。一个类型能被 Koin 注入，并不意味着应当注册到容器。实例寿命必须由使用者的寿命决定，不能为了少写构造代码一律设为 `@Singleton`。这与 [Android 架构建议](https://developer.android.com/topic/architecture/recommendations) 的构造注入、必要时限定共享对象范围，以及 [Koin 定义与生命周期](https://insert-koin.io/docs/reference/koin-core/definitions/) 一致。

## 当前装配图

```text
Android NotesApp.onCreate -> initKoin -> NotesModule
                                  |-> DataModule -> DatabaseModule
                                  |-> FrameworkModule
                                  |-> EmailModule / LoginModule / MainModule / SettingsModule
                                  |-> MainModule -> NetworkModule

iOS iOSApp.init -> doInitKoin -> doInitData -> MainViewController -> App()
```

`composeApp/NotesModule.kt` 是应用组合根。`DataModule` 负责 Room、DAO 和 DataStore provider，Repository 实现由扫描注册；`NetworkModule` 提供 Ktor `HttpClient`；Feature 模块注册 ViewModel、Entry 和导航贡献。Feature 的 `@Module(includes = [...])` 将依赖模块纳入生成的图；只有组合根依赖所有实现模块，不能因为 Koin 能解析接口就增加 Feature 实现间的 Gradle 依赖。[Koin 的模块包含机制](https://insert-koin.io/docs/reference/koin-annotations/modules/)支持这种分层装配。

## 可操作的边界

| 对象或场景 | 处理规则 | 当前例子 |
|---|---|---|
| 数据库、DataStore、网络客户端及其平台建造能力 | 注册到 DI；有共享状态或创建成本时使用应用级单例。Android `Context` 只在平台入口提供，平台实现内使用 application context。 | `DataModule.provideDatabase`、`provideDataStore`，`NetworkModule.provideKtorClient`，`AppDatabaseFactory`、`AppFileManager` |
| Repository / 跨模块服务契约 | 接口由消费模块依赖，具体实现由所属模块绑定；构造函数注入协作者。需要全局共享数据源时用单例。 | `EmailsRepositoryImpl`、`SettingsRepositoryImpl`、`TokenProviderImpl` |
| 屏幕 ViewModel | 用 `@KoinViewModel`，在目的地或屏幕入口用 `koinViewModel()`；状态跟随 `ViewModelStoreOwner`，不能改成应用单例。 | `AppViewModel`、`EmailViewModel`、`SearchViewModel`、`LoginViewModel`、`SettingsViewModel` |
| 跨 Feature 的 UI Entry 契约 | 允许接口绑定，由消费 Feature 在入口解析；API 模块不依赖实现。 | `EmailEntryImpl`、`SettingsEntryImpl`；`mainNavEntries` 使用 `koinInject<EmailEntry>()` |
| 平台相关、可替换的小型协作者 | 可以注入；无共享状态且创建便宜时用 Factory，若只在一个构造点使用且不需要替换，直接构造也可。 | `AvatarSaver` 当前为 Factory，供 `SearchViewModel` 使用 |
| 每次导航、请求或调用才确定的值 | 从调用方传参数，或使用 Koin 显式参数；不要把邮件 ID、搜索词、`KmpFile`、回调注册为全局定义。 | `EmailDetailDestination.emailId`、`EmailViewModel.getEmail(id)`、页面回调 |
| UI 的短暂状态与窗口事实 | 留在 `remember`、`rememberSaveable`、ViewModel 或现有 CompositionLocal；不放进应用 DI 图。 | `LocalWindowAdaptiveInfo`、`LocalPaneScaffoldDirective`、输入焦点、弹层状态 |
| 纯函数、不可变值、静态元数据 | 直接调用/常量/普通对象；无资源所有权或可替换协作者时不注册 DI。 | Reducer、mapper、`ContentWidth`、`EmailNavigation` 本体 |

最后一行有一个**有意的例外**：`EmailNavigation`、`LoginNavigation`、`MainNavigation` 虽是静态对象，Feature 模块仍以带唯一 `@Named` 的 `NavigationContribution` 定义注册，由组合根 `getAll()` 汇总。这是跨模块插件登记，不是对象创建需求；保留此机制，同时给新增贡献加注册完整性检查。`NavigationRegistry` 本身是从贡献派生的纯对象，当前 `remember(scope) { NavigationRegistry(scope.getAll()) }` 合理，不必为了统一风格再注册一次。Koin 的 `single`、`factory`、`scoped` 和 ViewModel 语义见[官方定义](https://insert-koin.io/docs/reference/koin-core/definitions/)；[Compose 接入](https://insert-koin.io/docs/reference/koin-compose/compose/)明确区分普通注入与 ViewModel 生命周期。

## 源码发现与建议顺序

### 已核实：iOS 启动装配完整

Android `androidApp/NotesApp.kt` 在 `initData()` 前调用 `initKoin { androidContext(...) }`。**更正初次审查**：iOS 的 `iosApp/iosApp/iOSApp.swift` 在 SwiftUI `App.init()` 中依次调用 `NotesModuleKt.doInitKoin()` 和 `doInitData()`，再由 `ContentView` 创建 `MainViewController`。因此不能从 `MainViewController.kt` 内没有初始化调用推断 iOS 缺失 Koin 启动；这一项不需要代码修复。仍应在后续改动后做 iOS 冷启动验证。

### P1：收敛全局服务定位器

`composeApp/data/InitData.kt` 用 `object : KoinComponent` 加 `by inject()` 获取两个 Repository，同时内部持有永久 `CoroutineScope`。建议改为构造注入的启动任务，并在应用入口取得该任务后显式执行；由入口确定运行次数、异常处理和 scope 生命周期。`AppInfo.android.kt` 和 `core/framework/toast/SystemToast.android.kt` 也用 `KoinComponent` 从全局容器取 `Context`，使函数签名看不出依赖，并让调用时机受 Koin 启动顺序约束。可将平台信息和系统 Toast 封装成平台接口/实现，由入口或 `App` 注入，再按需要传给使用点。`Context` 留在平台层，ViewModel 不持有 UI `Context`；[Android ViewModel 建议](https://developer.android.com/topic/architecture/recommendations)对此有明确限制。

此项是可测试性和生命周期优化，不要求一次性删除所有 `KoinComponent`；优先处理 `InitData`，再处理两个平台桥接点。

### P1：恢复依赖图的编译检查

`composeApp/build.gradle.kts` 设置 `compileSafety = false`、`unsafeDslChecks = false`；`core/network/build.gradle.kts` 也关闭 `compileSafety`，其注释仍提到旧版本和 `@Provided`。仓库目前使用 Koin BOM 4.2.2、Compiler Plugin 1.2.1，不能直接沿用旧注释判断当前限制仍存在。先在独立变更中打开检查并运行 Android 与 iOS 编译，逐项区分真正缺失的绑定、平台提供的类型和编译器限制。确需外部提供的参数按当前[编译安全文档](https://insert-koin.io/docs/reference/koin-compiler/compile-safety/)标注或局部处理，避免长期全局关闭；是否可启用以本项目实际编译结果为准。新增导航贡献时补一项组合根检查，验证预期贡献类型/数量和唯一 qualifier；编译检查不能替代运行时 `getAll()` 结果完整性验证。

### P2：按寿命审视现有定义，不做机械改写

- `SettingsHandler` 只通过构造函数依赖 `UserRepository`，目前为 `@Singleton`。它没有自身可见的共享可变状态；可改 Factory，或保留单例以复用轻量对象。先明确其语义，再决定，收益低于上述两项。
- `DataModule` 的 `AccountDao`、`EmailDao` 为 Factory，而数据库为单例。DAO 来自同一数据库，可评估改为单例以表达同寿命；当前写法并未证明有功能错误。
- `ToastGlobalNotifier` 持有共享事件流，使用单例合理，但消费端需要明确只有一个展示宿主，避免多个宿主重复展示。
- `HttpClient` 是长寿命资源，单例合理。若引入显式应用关闭流程，再审视客户端 `close()` 的所有权。
- 页面级 `koinViewModel()` 保留；`EmailViewModel` 在首页与收藏使用不同 key，详情处由自己的导航条目取得实例。修改导航所有权或 key 时必须回归验证状态不串用。可复用 UI 子组件优先接收 state/callback，避免自行解析 ViewModel；[Android UI 层建议](https://developer.android.com/topic/architecture/recommendations)将 ViewModel 放在屏幕级。

## 后续变更的验收项

1. Android 与 iOS 冷启动都能在首次 `App()` 之前完成一次 Koin 初始化；重复创建 iOS 控制器不重复启动容器（现有入口设计已满足调用顺序，仍需运行验证）。
2. 入口可控制样例数据初始化的执行、失败与取消；测试可直接传入假 Repository，无需启动全局 Koin。
3. 运行编译安全检查后，缺失绑定、错误 qualifier 和跨模块解析能在构建阶段暴露；导航贡献还需独立核对完整性。
4. 首页、收藏、详情、搜索和登录的 ViewModel 生命周期及状态隔离维持现有行为；iOS/Android 平台对象都不泄漏到 `commonMain` 的 ViewModel 构造参数。
5. 任何新增 DI 定义都回答：谁创建、谁持有、何时释放、为何不能直接构造、测试如何替换。若这些问题没有明确答案，优先普通构造或参数传递。

原始审查只写文档；后续构建与测试结果见文首“后续实施结果”。设备运行结果仍未验证。
