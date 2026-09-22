# AGENTS.md

本文件为 AI 编码助手（Claude Code 等）在本仓库中工作时提供项目指导。

## 项目概述

这是一个 Kotlin Multiplatform (KMP) 应用，使用 Kotlin、Compose Multiplatform、Koin（注解模式）和模块化架构。应用主要功能是邮件管理，包含登录、首页（邮件列表）、收藏、邮件详情、搜索和设置。共享逻辑与 UI 在 `commonMain` 中实现，并在 Android 与 iOS 端复用。

## 构建和运行

### 构建 Android

```bash
# 整包构建（注意 :composeApp 是 library，没有 assembleDebug 任务）
./gradlew :androidApp:assembleDebug

# 安装到设备
./gradlew :androidApp:installDebug
```

全量冷构建或 dex 合并若报 Java heap space OOM，追加以下参数重试：

```bash
./gradlew :androidApp:assembleDebug \
  -Dkotlin.daemon.jvmargs=-Xmx8g \
  -Dorg.gradle.jvmargs="-Xmx8g -Dfile.encoding=UTF-8 -XX:+UseParallelGC"
```

### 运行 iOS

使用 Xcode 打开 `iosApp/iosApp.xcodeproj` 运行。

### iOS 编译验证

```bash
./gradlew :composeApp:compileKotlinIosSimulatorArm64
```

### 运行测试

Android 宿主测试未启用（`./gradlew test` 无此任务），commonTest 需通过 iOS 模拟器执行：

```bash
./gradlew :composeApp:iosSimulatorArm64Test   # 约 3 分钟，需要模拟器
./gradlew :core:data:iosSimulatorArm64Test
```

### 静态检查

Android 侧用 lint（`detekt` 只 apply 在根项目且根项目无源集，跑起来是 `NO-SOURCE` 空转）：

```bash
./gradlew :androidApp:lintDevFullDebug   # 报告在 androidApp/build/reports/lint-results-*.html / .sarif
```

### Fused Library（实验性）

```bash
./gradlew :android:output:login:assemble
```

## 项目架构

### 模块结构

组合根：

- `:androidApp`: Android 入口应用
- `:iosApp`: iOS 入口应用
- `:composeApp`: 共享应用入口与导航装配（`AppNavHost`、Koin 组合根 `NotesModule`、启动数据初始化）

核心层（`core/`）：

- `:core:model`: 跨层共享的值类型（`AppLanguage` / `DarkThemeConfig` / `FontSizeConfig` / `MailboxType`）。准入判据：该类型出现在 `core:data` / `core:database` 的公开签名里，**或**被含 Compose 的 UI 基础模块（`core:theme` / `core:preference`）消费。两条都推出同一结论 —— 它必须位于 `core:data` **之下**且**不含 Compose**，因为数据层不能反向依赖 UI 基础模块
- `:core:database`: Room 存储层（entity / dao / converter / auto-migration / 建库工厂 / 事务助手）
  - **隔离靠模块边界**：`core:data` 以 `implementation` 依赖本模块，Gradle 的 `implementation` 不向下游传递，feature 层因此看不到任何 Room 类型。这是唯一可靠手段 —— `internal` 在这条链上无效（Room 生成的 `AppDatabaseConstructor` 被硬编码为 `public actual`，会连锁要求 DAO / entity / AppDatabase 也公开）
  - 数据库文件位置不在本模块：由 `core:data` 传入路径（`AppFileManager` 是位置唯一来源，且 feature 层也在使用，故不下沉）；平台建库差异由 `AppDatabaseFactory` 的 expect/actual 承担
  - 注意：`AppDatabase` 与其包名耦合 —— 移动它必须同步迁移 `core/database/schemas/<全限定名>.AppDatabase/`，否则 KSP 无法生成 AutoMigration
- `:core:data`: 数据层，分**公开面**与**内部面**两层：
  - 公开面：`model/`（对外模型 `EmailModel` / `AccountModel` / `UserModel` / `SettingsModel` / `EmailDraft`）、`repository/`（仓库**接口**）、`util/`、`DataModule.kt`
  - 内部面：`impl/`（`datastore/`、`mapper/`、`repository/`（实现）），实现类与 Koin provider 均为 `internal`
  - **硬规则一：对外模型与仓库接口签名中不得出现任何 Room 类型**（实体 / 查询载体），映射统一走 `impl/mapper/EmailMappers.kt`
  - **硬规则二：对 `core:database` 只能用 `implementation`**（见上），一旦改成 `api` 或 `implementation` 丢失，隔离即刻失效
- `:core:theme`: 主题、颜色、图标、通用组件（`TooltipIconButton` / `SimpleDialog` / `CenteredTopAppBar` / `ContainedLoadingIndicator` / `AdaptiveContent`）、布局环境（`layout/`）
  - **通用控件的判据**：同一段 Compose 渲染在 ≥2 处出现时下沉到这里，而不是在各自 feature 里复制。`TooltipIconButton` 就是这么来的 —— "图标按钮 + 提示气泡"曾在三处各写一遍（设置页顶部入口 / 多选操作栏 / 搜索栏返回键）；`AdaptiveContent`（可读宽度上限）是同一判据：设置页与邮件详情正文都要用
  - `layout/ContentWidth.kt`：**宽度上限（measure）的唯一来源**。`ContentWidth.Form`(480) / `.Article`(720) / `.Prose`(840) + `Modifier.contentWidth()`（窄窗口填满、宽窗口封顶；先 `widthIn` 后 `fillMaxWidth`，顺序不能反）。这些是**与窗口无关的可读性常量**，不是多形态适配分支 —— 各 feature 不要再自己写私有常量与裸 `widthIn`（登录页的 `FormMaxWidth` 曾是一例，现已并入这里）
  - `layout/WindowAdaptiveInfo.kt`：**全应用的窗口事实来源**。`LocalWindowAdaptiveInfo`（尺寸类 + 姿态）+ `rememberWindowAdaptiveInfo()`（唯一读取点，只能在组合根调用）+ `ProvideWindowAdaptiveInfo()`（`@Preview` / 测试的替换入口）+ 语义化读法 `isCompactWidth` / `isExpandedWidthOrWider` / `isExtraLargeWidthOrWider`；以及**派生事实** `LocalPaneScaffoldDirective` + `rememberPaneScaffoldDirective(adaptiveInfo)` + `canShowSideBySidePanes` / `canShowStackedPanes`（"能并排/叠放几栏"—— 分区数由库算，应用不维护断点）。**布局信息走 CompositionLocal，不进 feature 契约**（见「导航」一节）
  - `rememberPaneScaffoldDirective` 刻意接收已读到的 `WindowAdaptiveInfo` 而不是自己再读一次窗口：否则同一帧里两次窗口读取，分屏拖拽 / 折叠时会不一致
  - 本模块对 `material3-adaptive`、`material3-adaptive-layout` 与 `androidx-window-core` 都用 `api`：三者的类型都出现在本模块公开 API 里，消费方因此不必各自声明、也就不再可能各自调 `currentWindowAdaptiveInfo*()` 或各自算 directive
- `:core:preference`: 设置项的**展示元数据 + 配套通用控件**（`me.zhangls.preference`）
  - 元数据 = `PreferenceSpec` 词表（`Toggle` / `Select<T>` / `Action` + `PreferenceOption<T>`）+ 每个设置项一个 `object`（`LanguagePreference` / `DarkThemePreference` / `DynamicColorPreference` / `FontSizePreference` / `LogoutPreference`，统一只暴露 `spec`）+ 专属文案（composeResources）+ "由取值推导展示结果"的纯函数（如 `DarkThemePreference.isDark`）
  - 控件（`ui/` 子包）= 基于 `PreferenceSpec` 的通用渲染：`PreferenceRow`（列表行形态，含 `Toggle` / `Select<T>` / `Action` 三类行）、`SelectIconButton`（图标形态：图标按钮 + 长按提示 + 下拉单选）、`ProvidePreferenceLocals`（行控件的 locals 容器）。**同一 spec 的每种形态都在此实现一次** —— 登录页用图标形态、设置页用行形态，展示同一批 spec，两边都不写渲染代码。新增**设置项**不必改任何 feature 的渲染；新增**形态**才需要在这里加控件
  - 渲染契约（`ui/PreferenceUiModel.kt`）= `spec` + 当前取值 + 回调 `(T) -> Unit`。消费方装配它，控件渲染它
  - **硬规则一：不含取值来源与行为主体** —— `value` / `onValueChange` 只作为控件**参数**传入，`spec` 单例自身不持有它们；回调签名一律用 `(T) -> Unit` 而非 `(T) -> SettingsIntent`。`SettingsIntent` 定义在 `feature:settings` 的 **impl** 模块内（不在 `-api`），把它带进本模块会立刻形成 impl → impl 依赖环
  - **硬规则二：实现库不外泄** —— 行形态基于 `me.zhanghai.compose.preference`，但该依赖在本模块是 `implementation`：消费方的签名里不得出现 `SwitchPreference` / `ListPreference` 及库版 `ProvidePreferenceLocals` 等底层类型，只能用本模块的 `PreferenceUiModel` / `PreferenceRow` / `ProvidePreferenceLocals`（同名包装）。换实现库时 feature 不应有任何改动 —— 与 `core:data → core:database` 用 `implementation` 制造可见性边界是同一手法
  - **硬规则三：不含清单与顺序** —— 哪些设置项存在、以什么顺序展示是**平台相关**的（动态取色仅 Android 有），定义留在 `feature:settings`；平台差异用能力位（`supportsDynamicColor`）过滤，不要各平台各写一份清单
  - 依赖方向：`api(projects.core.theme)`（复用图标与 `ThemeColor`，图标本身不搬）+ `api(projects.core.model)`（设置取值枚举作为元数据的类型参数出现在公开 API）+ `implementation(kmp.compose.preference)`（行控件的实现库，不外泄）
  - 消费方式：消费方把 `spec` 与自己的取值 / 回调装配成 `List<PreferenceUiModel>`（回调在此处绑定，见 `feature:settings` 的 `ui/SettingsPreferenceMapper.kt`），再交给 `ProvidePreferenceLocals { LazyColumn { PreferenceRow(...) } }` 渲染
- `:core:network`: 网络层，Ktor、`ApiResponse`/`NetworkResult`、`TokenProvider`
- `:core:framework`: 框架层，MVI 基类（`MviViewModel`）、导航抽象（`Destination`/`NavEffect`/`RequireLogin`）、DeepLink、全局 Toast

功能层（`feature/`），每个功能拆为 `-api` 与实现两个模块：

- `:feature:main` + `:feature:main-api`: 主屏幕（Tab 容器）
- `:feature:email` + `:feature:email-api`: 邮件列表、收藏、详情、搜索、写邮件
- `:feature:login` + `:feature:login-api`: 登录
- `:feature:settings` + `:feature:settings-api`: 设置

Android 专用：

- `:android:baselineprofile`: 基线配置文件生成
- `:android:output:login`: Android Fused Library 实验性打包（见 `android/output/README.md`）

### 构建约定（convention plugins）

`build-logic/` included build 提供四个预编译脚本插件，各 KMP 模块构建脚本只保留自身真实差异：

- `me.zhangls.kmp-library`: KMP + Android/iOS 目标基础配置（namespace 与 iOS framework 名由模块路径推导，见 `KmpConventions.kt`）
- `me.zhangls.kmp-compose`: Compose Multiplatform 全量依赖（runtime / foundation / ui / tooling-preview / resources / material3）
- `me.zhangls.kmp-compose-api`: 只注入 `compose-runtime` + compose 编译器，用于 feature 的 `-api` 契约模块（契约里只有 `@Composable` 入口，不需要 foundation / material3 / resources）。**新增 `-api` 模块一律用它，不要再手工 `alias(kmp.plugins.jetbrains.compose)`**
- `me.zhangls.kmp-koin`: Koin 注解 + KSP 编译器（koin-bom 以 `api` 传播版本约束）

### api / impl 解耦模式

准确表述是：**impl 之间不得互相依赖，但可以依赖兄弟 feature 的 `-api` 契约**（`feature:main` 依赖 `feature:emailApi` + `feature:settingsApi` 即既有先例）。实现由 impl 模块通过 Koin 绑定到接口；仅组合根（`:composeApp`）依赖全部实现模块。入口契约位于 `feature/<name>-api/src/commonMain/kotlin/me/zhangls/<name>/api/`。

**跨 feature 共享的静态 UI 数据（如设置项元数据）不放任何 feature 的 `-api`**，而是放 core 层（`core:preference`）—— 属横向基础设施，挂在某个纵向业务 feature 的契约上属 ownership 错位。

### 导航

使用 Jetpack Navigation 3（`NavDisplay` / `NavBackStack` / `NavKey`）：

- `core:framework` 的 `Destination` 是各 feature 导航目的地的抽象（如 `HomeDestination`、`LoginDestination`、`EmailDetailDestination`），导航操作统一为 `NavEffect`（Navigate/Replace/Restart/Popup）
- `composeApp` 的 `AppNavHost.kt` 装配各 feature 的 nav entry（`mainNavEntries`/`loginNavEntry`/`emailNavEntry`），并实现 `RequireLogin` 登录拦截：未登录访问受限页面时先跳登录，登录成功后恢复目标页面
- 根据登录状态决定首屏（`HomeDestination` 或 `LoginDestination`），支持 DeepLink
- **列表-详情由 Nav3 的 `SceneStrategy` 装配，feature 不判断窗口**：`mainNavEntries` 用 `ListDetailSceneStrategy.listPane(sceneKey = …)` 标注"我是列表栏"，邮件详情条目是详情栏；窗格数量与宽度由 `calculatePaneScaffoldDirective(LocalWindowAdaptiveInfo.current)` 决定。首页与收藏分属两个 `EmailListScene` —— 两处共用一个场景标识时，切 Tab 会退化成内容原地替换、转场动画静默消失。detail 侧要不要返回键由实现读 `LocalListDetailSceneScope` 判断，不拿窗口宽度当代理
- **宿主对内容区唯一的承诺是"消费掉导航套件占用的系统内边距"**（`AppShell` 的 `consumeWindowInsets`），窗口形态经 `LocalWindowAdaptiveInfo` 下发，**路径参数里没有布局信息**：历史上 `isBottomNavigationBar: Boolean` 曾穿透 3 个契约、出现在 15 个文件 47 处，并被当成"内容内边距"、"搜索栏形态（其实是屏幕尺寸）"、"硬编码常量"三种用途。此后又出现过"把 `NavigationPlacement` 再反推回窗口宽度"的同类错位（`EmailSearchBar` 用"导航在不在底部"决定搜索栏形态）—— **推导后的值不要互相代理，要下发事实本身**

### MVI 架构

- `core/framework/src/commonMain/kotlin/me/zhangls/framework/mvi/MviViewModel.kt`：Intent → State (+ Effect) 单向数据流。State 的跨进程持久化是 **opt-in** 的 —— `savedKey` 默认 `null`（纯内存，进程销毁即丢失）。这是刻意的**安全默认**：State 普遍含密码 / token / 大对象，写进 `SavedStateHandle` 会随 instance state 落盘并可能触发 `TransactionTooLarge`；"默认持久化 + 各自记得关掉"必然漏（历史上漏过登录态与搜索态两处）。确需跨进程恢复时才显式给 key（如 `AppViewModel` 传 `"state"`），给 key 的那一刻必须确认该 State 不含敏感数据
- 各 feature 的 MVI 文件统一组织在 `mvi/` 子包（`XxxIntent` / `XxxAction` / `XxxState` / `XxxReducer` / `XxxViewModel`），UI 组织在 `home/`、`search/`、`detail/` 等按职责划分的子包
- ViewModel 使用 Koin 注解 `@KoinViewModel` 注册

### 主要组件

#### 应用外壳 (AppShell)

位于 `feature/main/src/commonMain/kotlin/me/zhangls/main/AppShell.kt`，使用 `NavigationSuiteScaffold` 自适应布局。三个 Tab 的条目装配在 `main/MainNavEntry.kt`（`mainNavEntries`），通过 Koin 注入的 `EmailEntry` / `SettingsEntry` 内联渲染：

- HOME: 首页邮件列表（`EmailEntry.HomeScreen`），同时是列表-详情场景的列表栏
- FAVORITES: 收藏（`EmailEntry.FavoritesScreen`），同样是列表栏，与首页分属两个场景
- SETTINGS: 设置页（`SettingsEntry.Screen`），无窗格标注、无分栏

宿主的职责只有两件：
1. **形态决策交给库** —— `NavigationSuiteScaffoldDefaults.navigationSuiteType(adaptiveInfo)`，本应用只对 ExtraLarge 宽度（≥1600dp）用展开 Rail 这一档做显式覆盖（这一档曾经写了但永远不可达：旧的 `currentWindowAdaptiveInfo()` 走的档位集宽度封顶在 Expanded）；
2. **消费掉导航套件占用的系统内边距** —— `NavigationSuiteScaffold` 只对 `NavigationBar` / `NavigationRail` / `NavigationDrawer` 三种旧形态消费 insets，本应用使用的 `ShortNavigationBar*` / `WideNavigationRail*` 全落在它的 `else -> NoWindowInsets` 分支上，必须由外壳补上。

外壳**不下发**"导航在哪一侧"：内容区直接用 `Scaffold` 给出的 padding（`toContentPadding` 那套手写归零已删除）。

#### 邮件相关 (feature:email)

- 首页列表：`feature/email/src/commonMain/kotlin/me/zhangls/email/home/HomeScreen.kt`（**只渲染列表**；详情是独立目的地 `EmailDetailDestination`，由宿主用 Nav3 列表-详情场景策略装配成同一屏。**不要**再在 feature 内部自建 pane scaffold —— 那会让首页与收藏页表现不一致、详情不进返回栈、两端 directive 口径也不同）
- 收藏页：`feature/email/src/commonMain/kotlin/me/zhangls/email/favorites/FavoritesScreen.kt`（与首页同构，细节见上）
- 邮件列表组件：`feature/email/src/commonMain/kotlin/me/zhangls/email/component/`（`EmailList` 为编排层，`EmailTopBar`/`EmailFab`/`EmailPagedList`/`NewEmailSheet` 等按职责拆分；`EmailHeader` 是列表项与详情项**共用**的卡片头部（头像 + 发件人/时间 + 收藏按钮），差异由 `EmailHeaderVariant` 表达 —— 收藏按钮的图标与无障碍文案只维护这一处）
- 邮件详情：`feature/email/src/commonMain/kotlin/me/zhangls/email/detail/EmailDetailScreen.kt`
- 搜索：`feature/email/src/commonMain/kotlin/me/zhangls/email/search/`（搜索栏形态按 `isCompactWidth` 选全屏 / docked）

#### 设置相关 (feature:settings + core:preference)

设置项拆成"元数据 / 取值 / 行为 / 渲染"四段：

- **元数据**（标题 / 图标 / 选项 / 摘要）→ `core:preference` 的对应 `object`，跨 feature 复用（登录页也用语言与深色模式两项）
- **当前取值** → `SettingsModel`（`core:data`）经 `SettingsRepository.settingsFlow` 流到 `SettingsState`
- **行为** → `SettingsIntent` + `SettingsViewModel`（`feature:settings` impl）
- **渲染** → `core:preference` 的控件（行形态 `PreferenceRow` / 图标形态 `SelectIconButton`），feature 一行渲染代码都不写
- 前三者由 `ui/SettingsPreferenceMapper.kt` 装配为 `core:preference` 的 `PreferenceUiModel`（`Toggle` / `Select<T>` / `Action`，回调在此绑到 `viewModel::sendIntent`），`SettingsScreen.kt` 只做列表装配：`ProvidePreferenceLocals { LazyColumn { PreferenceRow(...) } }`
- **清单与顺序**在 `SettingsPreferenceMapper.kt`（common）定义一次，平台差异用能力位 `supportsDynamicColor` 过滤（iOS 无动态取色）—— 不要退回成 android/ios 两份 actual 清单
- 对话框（`ui/DialogUiModel.kt` + `SettingsDialogMapper.kt`）留在 `feature:settings`：它只有这一个消费方且带确认/取消语义，不是横向资产。但**按钮文案若与某个设置项同源（"退出登录"），直接取 `LogoutPreference.spec.title`**，不要在 strings.xml 里再抄一份 —— 各写一份的结果是改标题时按钮不跟着变。注意跨模块只能取 `core:preference` 的**公开 API**，取不到它的资源字符串（CMP 生成的资源访问器是 `internal`）

### 数据加载

应用使用分页数据加载（Paging），通过 `EmailViewModel` 管理：

- `emailPaging` / `emailFavoritePaging` 流提供分页数据（`cachedIn(viewModelScope)`）
- 使用 `collectAsLazyPagingItems()` 在 Compose 中收集数据
- 加载状态通过 `LoadState` 监控；初始加载显示 `LoadingIndicator`，列表为空显示 `email_msg_no_emails` 资源

## 注意事项

- 项目使用 Koin 注解进行依赖注入（`@Module` / `@ComponentScan` / `@KoinViewModel`，KSP 生成），组合根在 `composeApp` 的 `NotesModule.kt`
- 使用 Compose Multiplatform 进行 UI 构建，Material 3（含 Expressive 与 Adaptive API）
- 数据库为 Room KMP 版本，字段变化必须配置 AutoMigration（见 `core/database` 的 `AppDatabase.kt`，schema 导出在 `core/database/schemas/`）
- compose-resources 的格式化占位符**必须**写带位置的 `%1$s`（CMP 的替换只认 `%(\d+)\$[ds]`，写 `%s` 会在所有平台原样输出）
- 数据通过分页加载，支持无限滚动
- 支持多种设备尺寸和方向适配
- 依赖版本统一在 `gradle/kmp.versions.toml`（typesafe 访问器为 `kmp.` 前缀）
- 同一仓库目录下不要并发执行两个 Gradle 构建（守护进程地址注册表会锁冲突）
