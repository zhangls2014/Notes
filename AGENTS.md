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
- `:core:theme`: 主题、颜色、图标、通用组件（Toast/Dialog）
- `:core:preference`: 设置项**展示元数据**的登记处（`me.zhangls.preference`）
  - 内容 = `PreferenceSpec` 词表（`Toggle` / `Select<T>` / `Action` + `PreferenceOption<T>`）+ 每个设置项一个 `object`（`LanguagePreference` / `DarkThemePreference` / `DynamicColorPreference` / `FontSizePreference` / `LogoutPreference`，统一只暴露 `spec`）+ 专属文案（composeResources）+ "由取值推导展示结果"的纯函数（如 `DarkThemePreference.isDark`）
  - **硬规则一：不含取值与行为** —— `value`、`onValueChange`、点击回调、消费方的 Intent 一律留在消费 feature。`SettingsIntent` 定义在 `feature:settings` 的 **impl** 模块内（不在 `-api`），一旦带进本模块立刻形成 impl → impl 依赖环
  - **硬规则二：不含渲染** —— 不依赖 `androidx.compose.preference` 的任何组件
  - **硬规则三：不含清单与顺序** —— 哪些设置项存在、以什么顺序展示是**平台相关**的（动态取色仅 Android 有），那份 expect/actual 必须留在 feature
  - 依赖方向：`api(projects.core.theme)`（复用图标与 `ThemeColor`，图标本身不搬）+ `api(projects.core.model)`（设置取值枚举作为元数据的类型参数出现在公开 API）
  - 消费方式：消费方把 `spec` 与自己的取值 / Intent 组装成表现层模型再渲染。参考 `feature:settings` 的 `ui/PreferenceUiModel.kt`（`spec` + `value` + `SettingsIntent` 三元组）与 `ui/SettingsPreferenceMapper.kt`（纯装配层）
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

`build-logic/` included build 提供三个预编译脚本插件，各 KMP 模块构建脚本只保留自身真实差异：

- `me.zhangls.kmp-library`: KMP + Android/iOS 目标基础配置（namespace 与 iOS framework 名由模块路径推导，见 `KmpConventions.kt`）
- `me.zhangls.kmp-compose`: Compose Multiplatform 依赖
- `me.zhangls.kmp-koin`: Koin 注解 + KSP 编译器（koin-bom 以 `api` 传播版本约束）

### api / impl 解耦模式

准确表述是：**impl 之间不得互相依赖，但可以依赖兄弟 feature 的 `-api` 契约**（`feature:main` 依赖 `feature:emailApi` + `feature:settingsApi` 即既有先例）。实现由 impl 模块通过 Koin 绑定到接口；仅组合根（`:composeApp`）依赖全部实现模块。入口契约位于 `feature/<name>-api/src/commonMain/kotlin/me/zhangls/<name>/api/`。

**跨 feature 共享的静态 UI 数据（如设置项元数据）不放任何 feature 的 `-api`**，而是放 core 层（`core:preference`）—— 属横向基础设施，挂在某个纵向业务 feature 的契约上属 ownership 错位。

### 导航

使用 Jetpack Navigation 3（`NavDisplay` / `NavBackStack` / `NavKey`）：

- `core:framework` 的 `Destination` 是各 feature 导航目的地的抽象（如 `MainDestination`、`LoginDestination`、`EmailDetailDestination`），导航操作统一为 `NavEffect`（Navigate/Replace/Restart/Popup）
- `composeApp` 的 `AppNavHost.kt` 装配各 feature 的 nav entry（`mainNavEntry`/`loginNavEntry`/`emailNavEntry`），并实现 `RequireLogin` 登录拦截：未登录访问受限页面时先跳登录，登录成功后恢复目标页面
- 根据登录状态决定首屏（`MainDestination` 或 `LoginDestination`），支持 DeepLink

### MVI 架构

- `core/framework/src/commonMain/kotlin/me/zhangls/framework/mvi/MviViewModel.kt`：Intent → State (+ Effect) 单向数据流，State 可选持久化到 `SavedStateHandle`（含敏感数据的 State 应传 `savedKey = null` 保持纯内存）
- 各 feature 的 MVI 文件统一组织在 `mvi/` 子包（`XxxIntent` / `XxxAction` / `XxxState` / `XxxReducer` / `XxxViewModel`），UI 组织在 `home/`、`search/`、`detail/` 等按职责划分的子包
- ViewModel 使用 Koin 注解 `@KoinViewModel` 注册

### 主要组件

#### 主屏幕 (MainScreen)

位于 `feature/main/src/commonMain/kotlin/me/zhangls/main/MainScreen.kt`，使用 `NavigationSuiteScaffold` 自适应布局（小屏底部导航栏，中大屏 Navigation Rail），包含三个 Tab，通过 Koin 注入的 `EmailEntry` / `SettingsEntry` 内联渲染：

- HOME: 首页，显示邮件列表（`EmailEntry.HomeScreen`）
- FAVORITES: 收藏页（`EmailEntry.FavoritesScreen`）
- SETTINGS: 设置页（`SettingsEntry.Screen`）

#### 邮件相关 (feature:email)

- 首页列表：`feature/email/src/commonMain/kotlin/me/zhangls/email/home/HomeScreen.kt`（Android/iOS 平台差异仅 Scaffold 选择，共享逻辑在 commonMain）
- 收藏页：`feature/email/src/commonMain/kotlin/me/zhangls/email/favorites/FavoritesScreen.kt`
- 邮件列表组件：`feature/email/src/commonMain/kotlin/me/zhangls/email/component/`（`EmailList` 为编排层，`EmailTopBar`/`EmailFab`/`EmailPagedList`/`NewEmailSheet` 等按职责拆分）
- 邮件详情：`feature/email/src/commonMain/kotlin/me/zhangls/email/detail/EmailDetailScreen.kt`
- 搜索：`feature/email/src/commonMain/kotlin/me/zhangls/email/search/`

#### 设置相关 (feature:settings + core:preference)

设置项拆成"元数据 / 取值 / 行为"三段：

- **元数据**（标题 / 图标 / 选项 / 摘要）→ `core:preference` 的对应 `object`，跨 feature 复用（登录页也用语言与深色模式两项）
- **当前取值** → `SettingsModel`（`core:data`）经 `SettingsRepository.settingsFlow` 流到 `SettingsState`
- **行为** → `SettingsIntent` + `SettingsViewModel`（`feature:settings` impl）
- 三者由 `ui/SettingsPreferenceMapper.kt` 组装为 `ui/PreferenceUiModel.kt` 的 `Toggle` / `Select<T>` / `Action`，`SettingsScreen.kt` 只按 spec 渲染（`Toggle`→`SwitchPreference`、`Select`→`ListPreference`、`Action`→`Preference`）
- **清单与顺序**由 `SettingsPreferenceMapper.android.kt` / `.ios.kt` 两个 actual 决定（iOS 无动态取色）

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
