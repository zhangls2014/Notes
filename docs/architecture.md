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

:core:model
:core:database
:core:data
:core:theme
:core:preference
:core:network
:core:framework

:feature:main       :feature:main-api
:feature:login      :feature:login-api
:feature:email      :feature:email-api
:feature:settings   :feature:settings-api

:android:baselineprofile
:android:output:login
```

`iosApp` 是 Xcode 工程，不是 `settings.gradle.kts` 中的 Gradle 模块。

## 2. 模块职责

### 应用与平台入口

| 目录或模块 | 职责 |
|---|---|
| `androidApp` | Android `Application`、Activity、Manifest、平台初始化和 APK 输出 |
| `iosApp` | iOS 应用入口和 Xcode 工程 |
| `composeApp` | 共享应用入口、Koin 组合根、根返回栈、Deep Link、Feature 导航装配和启动数据初始化 |
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
| `core:network` | Ktor 客户端、认证、响应和错误封装 |
| `core:framework` | MVI 基类、Navigation 3 抽象、Deep Link 和全局 Effect |

`core:model` 的准入条件是：类型出现在数据模块的公开签名中，或同时被数据层与含 Compose 的基础模块消费。只服务一个 Feature 的类型留在该 Feature。

### Feature

每个 Feature 由同级的 API 模块和实现模块组成：

| API / 实现 | 职责 |
|---|---|
| `feature:main-api` / `feature:main` | 顶层 Tab 目的地和应用外壳 |
| `feature:login-api` / `feature:login` | 登录目的地、入口契约和登录 UI |
| `feature:email-api` / `feature:email` | 邮件详情目的地、列表场景、邮件列表/详情/搜索/写信 UI |
| `feature:settings-api` / `feature:settings` | 设置入口契约和设置行为/UI |

API 模块只包含其他模块有理由知道的稳定契约，例如 `Destination`、Feature Entry 接口、导航贡献和必要参数。页面、ViewModel、Reducer、Repository 实现、DI provider 和内部模型都属于实现模块。

## 3. 依赖规则

### Feature 边界

- Feature 实现必须依赖自身 `-api`。
- Feature 实现可以依赖其他 Feature 的 `-api`，不得依赖其他 Feature 的实现。
- Feature API 不依赖任何 Feature 实现；跨 Feature 共享的静态 UI 数据应下沉到合适的 Core 模块。
- 只有 `composeApp` 依赖全部实现模块，并将实现绑定到 API 契约。
- Core 不依赖 Feature。

当前先例是 `feature:main` 依赖 `feature:email-api` 和 `feature:settings-api`，通过 `EmailEntry`、`SettingsEntry` 渲染内容，而不引用对方实现类型。

### `api` 与 `implementation`

默认使用 `implementation`。只有依赖模块的类型出现在当前模块公开 Kotlin API 中时才使用 `api`。

两个关键隔离边界：

1. `core:data` 必须以 `implementation` 依赖 `core:database`。这样 Feature 无法看到 Room entity、DAO 或查询载体。
2. `core:preference` 必须以 `implementation` 依赖底层 Compose Preference 库。Feature 只看到本项目的 `PreferenceSpec`、`PreferenceUiModel` 和通用控件。

`internal` 不能代替模块边界。Room 生成代码会要求部分数据库类型公开，真正阻止外泄的是 Gradle `implementation`。

### 数据公开面

`core:data` 分成两层：

- 公开面：`model/`、`repository/`、`util/` 和 `DataModule.kt`；
- 内部面：`impl/datastore/`、`impl/mapper/`、`impl/repository/`，实现类与 provider 为 `internal`。

Repository 接口和对外模型不得出现 Room 类型。数据库模型与公开模型的转换集中在 mapper 中。

## 4. 构建约定

`build-logic` 提供四个预编译约定插件：

| 插件 | 用途 |
|---|---|
| `me.zhangls.kmp-library` | KMP、Android/iOS target、namespace 和 framework 基础配置 |
| `me.zhangls.kmp-compose` | Compose runtime、foundation、UI、resources 和 Material 3 |
| `me.zhangls.kmp-compose-api` | 仅为 Feature API 注入 Compose runtime 与编译器 |
| `me.zhangls.kmp-koin` | Koin 注解、BOM 和 KSP 编译器 |

新增 Feature API 使用 `me.zhangls.kmp-compose-api`，不要自行重复配置 Compose 插件。依赖版本统一来自 `gradle/kmp.versions.toml`。

## 5. 运行时架构

### Koin 装配

各模块声明自己的 Koin 注解和 provider，`composeApp` 的 `NotesModule` 是组合根。实现模块负责实现并提供自己的 Entry；调用方只依赖 API 接口。

在 `androidApp/NotesApp.onCreate` 与 `iosApp/iOSApp.init` 中先初始化 Koin，再执行启动数据任务。启动任务 `InitData` 通过构造函数接收 Repository 和平台 `AppInfo`，由组合根解析并在应用级协程中执行；它本身不从全局容器查找依赖，也不持有协程作用域。Android `Context` 只在平台入口传给 Koin，`AndroidAppInfo` 和 `AndroidSystemToast` 仅保存 application context；ViewModel 不依赖平台 `Context`。

应用组合根通过内部平台 `rememberSystemToast()` 装配 `SystemToast`。Android 使用 Koin 的 application-context 单例；iOS 每个 Compose 宿主创建并持有自己的 `IosSystemToast`，组合销毁时取消显示任务、关闭额外窗口。iOS Toast 从实际宿主视图的 `windowScene` 创建触摸穿透窗口，不枚举全局场景或读取主屏幕；根视图布局回调根据当前 bounds 与 safeAreaInsets 重新定位标签，支持窗口缩放与旋转。后台或未附着到场景的宿主不显示 Toast。全局 Toast 事件仍沿用应用的 SharedFlow 机制，多个活跃宿主可各自在自身窗口呈现；它不携带某个业务窗口的目标标识。

Repository、数据库、DataStore、网络客户端和需要共享的服务按其生命周期注册；屏幕 ViewModel 使用 `@KoinViewModel` 并由目的地获取。纯函数、Reducer、mapper、短暂 UI 状态、窗口事实和每次导航才确定的参数不注册为应用级依赖。导航静态贡献是例外：Feature 以唯一 qualifier 注册 `NavigationContribution`，组合根通过 `getAll()` 收集。`composeApp` 与 `core:network` 启用 Koin `compileSafety`；新增绑定需通过 Android 与 iOS 编译验证。审查背景与逐项判断见 [Koin 依赖边界审查](audits/2026-09-25-koin-dependency-injection.md)。

### MVI

`MviViewModel` 提供 Intent → State (+ Effect) 的单向数据流。Feature 的 `Intent`、`Action`、`State`、`Reducer` 和 `ViewModel` 放在实现模块的 `mvi/` 包。

状态跨进程持久化是 opt-in：`savedKey` 默认 `null`。只有确认状态体积小、可序列化且不含密码、token 或其他敏感数据时，才允许传入 key。`AppViewModel` 显式使用 `savedKey = "state"`；邮件、搜索和登录状态有意保持为纯内存态。

### Navigation 3

顶层导航保留首页、收藏、设置三个独立 Entry，各自拥有返回栈。Tab 根之间没有返回历史；根页面返回交还平台，详情返回所属列表。点击不同 Tab 调用组合根的 `NavHandler.selectTab`，恢复目标 Tab 的页面、滚动位置和多选状态；重选当前 Tab 不改变当前详情。显式导航到 Tab 目的地（如深链）则回到该 Tab 根页面。

`AppNavigationState` 使用 `rememberNavBackStack` 分别保存各 Tab 栈和当前根选择。各栈的 `rememberDecoratedNavEntries`、SaveableStateHolder 和 ViewModelStore 装饰器在内容切换区域外持续存在；非活动 Tab 不绘制，但保留 Entry 的可保存状态及 ViewModel。邮件首次加载指示在列表外绘制，避免单个占位条目把待恢复滚动索引重置为 0。滚动位置可随保存状态恢复；邮件多选等 ViewModel 状态仅在当前进程保留，不启用敏感/大型 MVI 状态的跨进程保存。退出登录清空全部栈并释放对应 Entry；登录成功释放登录 Entry。

`AppShell` 始终包裹内容区，不裁剪内部 UI；页面自身负责预测返回时的轮廓裁剪。侧栏和底部导航保持固定；`NavDisplay` 仅接收当前 Tab 的装饰后 Entry，并以当前栈根作为组合 key。切换 Tab 释放旧显示及其在途转场，不播放跨 Tab 动画，也不清除外层保存的 Entry 状态。同一 Tab 内的详情导航保留页面动画和预测返回。深链与登录恢复依据详情的 `scene` 选择所属列表根。导航状态规则位于 `composeApp/NavHandler.kt`，由 `AppNavHost` 装配。

`core:framework` 定义：

- `Destination`：所有目的地的共同抽象；
- `NavEffect`：`Navigate`、`Replace`、`Restart`、`Popup`；
- `RequireLogin`：受登录状态保护的目的地；
- Deep Link 与导航贡献注册机制。

Feature API 定义具体目的地和 Entry 契约。`composeApp/AppNavHost.kt` 聚合导航条目、维护根返回栈、执行登录拦截并恢复受保护目标。

首页和收藏是两个独立的列表场景。邮件详情是独立目的地，列表—详情组合由根导航的 `SceneStrategy` 负责；Feature 内不再创建第二套 pane scaffold。根导航通过 `rememberAppSceneStrategies` 为所有目的地装配统一场景策略：宽屏存在多个水平分区或平台报告横向铰链时，允许保留自适应单窗格场景及详情空态；仅由窄窗口高度达到 Expanded 而产生的多个纵向分区，不作为保留自适应单窗格场景的依据。实际多窗格仍由 Adaptive 接管，普通单页统一交给 Navigation 3 的页面场景。登录页内部的 `SupportingPaneScaffold` 独立消费原有 directive，继续保持左右分栏、纵向分区数限制为 1 和既有铰链处理，不受根场景选择影响。普通页面进入与返回直接切换，不使用转场动画。根导航通过 `DeviceCornerNavDisplay` 装配库的 SceneState 与返回事件；单页预测返回采用与新建邮件 Material 3 BottomSheet 相同的 CubicBezier(0.1, 0.1, 0, 1) 曲线、48dp 横向/24dp 纵向缩减和页面中央缩放原点，内容反向补偿以保持文字与图片比例。单页使用自身内容区尺寸计算缩放与页面中央原点，不测量或补偿页面相对窗口的偏移。背景和圆角裁剪放在内容比例修正后的可见页面层内，以该层的实际尺寸为裁剪边界，避免底部圆角落在页面之外或残留背景遮挡上一页。只有页面参与变换，外壳不缩放。缩放时在上一页与当前页面之间绘制灰色遮罩（最大 24%），遮罩不参与缩放或覆盖导航套件；取消或完成时于 220ms 内淡出。单页邮件详情的 Scaffold 全尺寸绘制，包括状态栏下方的安全区域背景；安全区仅用于页面内部内容避让，不在缩放层之外预留顶部 padding。系统状态栏图标不属于应用页面。绘制层直接读取当前手势状态，拖动不经过弹簧动画的时间采样；场景就绪前保留完整页面，释放后的取消/提交尾段由线性 Scene transition 管理。Android 12+ 在系统提供圆角数据时，对退出的单页 Scene 添加以页面实际边界为轮廓的物理圆角裁剪，半径按页面最终缩放比例补偿，拖动期间显示半径不随手势进度变化；取消返回时以 220ms 动画恢复原有未裁剪轮廓，动画完成后解除页面裁剪，避免非窗口角处突然变为直角。分栏 Scene、应用外壳与浮层不增加该变换或裁剪；旧 Android、未提供圆角的设备和 iOS 不做设备圆角匹配。详情返回键由 `LocalListDetailSceneScope` 判断，而不是用窗口宽度推测。

### 自适应布局

窗口事实只有一个读取点：`composeApp` 调用 `rememberWindowAdaptiveInfo()`，再通过 `LocalWindowAdaptiveInfo` 下发。`PaneScaffoldDirective` 也在组合根从同一份信息计算，并通过 `LocalPaneScaffoldDirective` 提供。directive 使用 `HingePolicy.AlwaysAvoid`，所以同一铰链几何在半开和全开之间不会从排除区消失。

邮件编辑弹层通过 `core:theme/LocalModalViewport` 获取完整应用视口尺寸。`AppNavHost` 在导航外壳和列表—详情分栏之外使用 `ProvideModalViewport` 下发根布局的有限约束；它不再次读取平台窗口事实，也不增加安全区或铰链包装。iOS 邮件弹层使用显式完整视口大小的 Dialog 容器，并由 Material 3 `BottomSheet` 处理拖动和展开；Android 保留标准 `ModalBottomSheet`。完整容器和滚动表单均使用有限约束，保证旋转后弹窗的无界首选尺寸测量不会产生无限滚动约束、内容宽度溢出或错误的拖动锚点。

窗格之间的普通水平、垂直间距统一为 `0.dp`；真实铰链的排除区域仍然保留，不因取消间距而压缩。页面内容（例如邮件卡片）自身的内外边距独立保留。

`core:theme/layout` 的 `HingeGeometry` 只负责为窗格脚手架生成稳定的排除区域。登录页的 `SupportingPaneScaffold` 与邮件列表—详情 SceneStrategy 是仅有的铰链感知消费者。邮件场景可以根据窗口能力形成两栏或上下窗格；登录页只支持左右分栏，始终把竖向分区数限制为 1。Material directive 的排除区域仅包含竖向铰链；按用户审阅截图后的决定，登录页不在窗格内避让横向铰链，品牌与表单使用完整可用高度，保持原有居中与滚动行为。旋转、小窗和内外屏切换改变真实几何时正常重排，只改变折叠状态而几何不变时保持窗格稳定。

登录页设置入口位于表单区域右上角。Logo、输入框与按钮组成的表单主体在高度足够时居中；高度不足时保持原有尺寸和间距，由纵向滚动与 IME 留白处理。

登录与新建邮件页的 IME 切换保护由 `core:theme/layout` 的 `ImeHandoffFocusState` 和 `rememberImeHandoffInsets` 共享。Android 在焦点回调中记录同一表单内的输入框切换，仅在切换后的 400 ms 窗口内保留下降中的键盘占位，待高度连续稳定 200 ms 后释放，以吸收普通键盘与系统安全键盘切换时的短暂空档。高度增加立即避让，失焦立即取消保护；保护窗口外的正常收起逐帧跟随真实 IME，不额外等待。登录表单通过 `windowInsetsPadding` 消费受保护的 inset；新建邮件页把它与 `ModalBottomSheet` 默认内容 inset 合并，让弹层继续在自己的窗口中避让键盘。iOS 使用原生 IME inset，不应用 Android 保护策略。

单栏 UI 不参与铰链适配。应用导航、设置、搜索、普通页面、`ModalBottomSheet`、Preference 弹窗、tooltip 和 `Dialog` 始终使用标准组件，不读取铰链，也不因铰链存在而切换组件树、尺寸、滚动或弹出位置。是否避让铰链由实际窗格布局决定，而不是由整个应用统一套一层安全区域决定。

约束如下：

- Feature 不直接调用 `currentWindowAdaptiveInfo*()`，也不自己维护窗口断点。
- Feature 不缓存铰链、不按 `isSeparating` 过滤铰链，也不另算安全区；非窗格 Feature 不读取铰链。
- 搜索栏等局部形态读取窗口事实，不得从“导航在底部还是侧边”反推宽度。
- 搜索锚点以空查询输入框的首次自然测量宽度为基准，不复制 Material 默认宽度常量。基准仅保存在当前 UI 实例，密度、字号、布局方向或提示文本变化时重测；窗口变窄时服从父约束，不覆盖基准。停靠展开沿用锚点宽度，全屏展开沿用库的动画约束；测量输入框不绘制、不参与焦点或无障碍语义，也不修改真实查询。
- `AppShell` 根据统一窗口尺寸选择导航形态：宽度 ≥600dp 且高度 <480dp 优先使用折叠 Rail，为内容保留纵向空间；其余沿用库策略，并在高度充足的 ExtraLarge（≥1600dp）窗口展开 Rail。窄窗口保留底栏，不比较尺寸档位下限来推断横竖屏；折叠状态变化不切换底栏/侧栏，避免相同几何下内容窗格移动。
- `AppShell` 消费导航套件占用的系统内边距；Feature 不接收导航方位参数，外壳不另算铰链安全区域。
- 列表—详情场景的安全区策略由 `AppNavHost` 按唯一窗口事实选择：没有平台报告的铰链时，`SafeAreaSceneStrategy` 先预留并消费整个场景剩余的 `WindowInsets.safeDrawing`，再采用库默认的列表首选宽度（通常 360dp，ExtraLarge 为 412dp），详情占剩余宽度。详情空态也采用相同分配策略。有铰链时不预扣全局安全区，仍按 directive 的物理排除区域分区；NavEntry 装饰器通过 `PaneWindowInsets` 消费窗格外的空间，各窗格的标准 `Scaffold` 仅避让实际相交的系统安全区。铰链几何优先于首选宽度，单页场景不增加包装。
- 可读宽度统一用 `ContentWidth.Form`、`Article`、`Prose` 和 `Modifier.contentWidth()`。
- Nav3 SceneStrategy 消费组合根提供的 directive，负责列表—详情分区；无铰链时仅场景整体预留系统安全区，不增加铰链安全带包装。
- 登录页只按 `maxHorizontalPartitions` 决定左侧品牌区与右侧表单区是否同时展开。横向铰链不触发上下重排：宽度足够时仍左右分栏，宽度不足时使用单列表单。
- 搜索、写邮件、设置选择和确认对话框使用标准组件，不增加铰链安全弹层；查询、草稿、滚动和选择状态独立于布局形态。

### Material 3 Expressive 主题

共享 UI 的主题入口是 `core:theme/ComposeAppTheme`，使用 `MaterialExpressiveTheme` 和 `MotionScheme.expressive()`。字体与形状分别由同模块的 `AppTypography`、`AppShapes` 提供；标题与操作标签使用强调字重，正文保留 Material 原有尺寸。Typography 显式使用带 fontFamily 的构造器，避免兼容无参构造器把强调样式映射回普通样式。

主题保留内置品牌配色、动态色、深浅色和用户字号设置。Feature 从 `MaterialTheme` 读取颜色、字体与形状；关键按钮和共享图标按钮显式采用形变重载，自定义头像空间动画使用主题 MotionScheme。邮件列表与详情卡片使用主题 large 圆角和容器色，打开、多选、收藏状态各有语义颜色。确认对话框使用标准 AlertDialog，保留字符串与 AnnotatedString 重载，以及显式按钮回调关闭语义。

`core:preference/PreferenceRow` 接收 `isGroupStart / isGroupEnd` 渲染外角与内角；分组清单、顺序和独立操作的选择仍由 `feature:settings` 负责。底层 Compose Preference 控件继续隐藏在 implementation 依赖之后。主题迁移不引入第二套窗口事实、导航策略或铰链避让。

### 设置项

设置项拆成四部分：

1. 展示元数据和通用渲染位于 `core:preference`；
2. 当前值来自 `core:data` 的 `SettingsModel`；
3. 行为由 `feature:settings` 的 Intent/ViewModel 拥有；
4. `SettingsPreferenceMapper` 将 spec、值和回调装配为 `PreferenceUiModel`。

`PreferenceSpec` 不持有状态或 Feature Intent。设置清单和顺序属于 `feature:settings`，平台差异通过能力位过滤。

## 6. 数据与资源规则

- Room 字段或 schema 变化必须配置 AutoMigration，并更新 `core/database/schemas/`。
- `AppDatabase` 的包名与 schema 目录耦合；移动类时必须同步迁移以全限定名命名的 schema 目录。
- 数据库文件路径由 `core:data` 的 `AppFileManager` 提供，平台建库差异由 `AppDatabaseFactory` 的 expect/actual 承担。
- `AppDataStore` 只负责普通资料/设置的 JSON 序列化，不存储登录凭据。`UserModel` 不含 Token；`UserRepository.login(user, tokens)` 串行协调资料与凭据，`getTokens()` 仅供组合根网络适配使用，Token 不进入 UI State。
- `core:data/impl/security/SecureTokenStore` 是内部契约：Android 用 Keystore AES/GCM 和随机 IV 加密整组账号凭据，经 `AtomicFile` 写入 `noBackupFilesDir`；iOS 用 Generic Password Keychain，`WhenUnlockedThisDeviceOnly`，不启用同步。永久密钥失效或损坏密文会清除凭据；临时访问失败不回退明文。登录态 Flow 保留最近状态并执行可取消重试；请求级 getTokens 仍抛出失败，不使用旧凭据兜底。
- 旧 `user` JSON 首次访问时迁移，安全写入并读回确认后才移除明文 Token。退出先清除旧明文字段，再清除凭据并删除资料；账号与凭据不匹配时不公开登录态。Ktor 禁用鉴权 Token 缓存，每次整组读取当前凭据。当前仍为模拟登录，无真实刷新端点。
- Android 排除旧 `notes.preferences_pb` 的云备份及设备迁移，iOS 排除 DataStore 目录备份，避免未完成迁移的旧明文被备份。这也意味着普通设置不会随该文件备份迁移；旧有备份中的数据无法由本次升级追溯删除。
- Compose Resources 的格式化占位符必须带位置，例如 `%1$s`；无位置的 `%s` 不会被正确替换。
- 同一段通用 Compose 渲染在两个或更多位置出现时，评估下沉到 `core:theme` 或 `core:preference`，但单一 Feature 的业务组件仍留在 Feature。

## 7. 变更检查清单

### 新增 Feature

- 创建同级的 `feature/<name>` 与 `feature/<name>-api`。
- API 只放目的地、Entry 和必要参数；实现依赖自身 API。
- 需要其他 Feature 时只依赖其 API。
- 在 `composeApp` 加入实现模块并完成 Koin/导航装配。
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

- 使用 `LocalWindowAdaptiveInfo` 或 `LocalPaneScaffoldDirective`，不新增窗口读取点。
- 优先表达实际事实或库计算出的能力，不用其他 UI 形态作代理。
- 覆盖紧凑、横屏、宽屏和折叠姿态；模拟器步骤见 [Android 模拟器测试](testing/android-emulator.md)。
- 结束测试前恢复模拟器分辨率、密度和旋转设置。
