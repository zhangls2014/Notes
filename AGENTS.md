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

```bash
./gradlew detekt   # 配置位于 config/detekt/detekt.yml
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

- `:core:model`: 公共领域模型（`AppLanguage` 等跨层共享类型）
- `:core:data`: 数据层，分**公开面**与**内部面**两层：
  - 公开面：`model/`（对外模型 `EmailModel` / `AccountModel` / `UserModel` / `SettingsModel` / `EmailDraft`）、`repository/`（仓库**接口**）、`type/`、`util/`、`DataModule.kt`
  - 内部面：`impl/`（`database/`（Room entity/dao/auto-migration）、`datastore/`、`mapper/`、`repository/`（实现）），多数实现类为 `internal`
  - **硬规则：对外模型与仓库接口签名中不得出现任何 Room 类型**（实体 / 查询载体），映射统一走 `impl/mapper/EmailMappers.kt`
  - 注意：`AppDatabase` 与其包名耦合 —— 移动它必须同步迁移 `core/data/schemas/<全限定名>.AppDatabase/`，否则 KSP 无法生成 AutoMigration
- `:core:theme`: 主题、图标、通用组件（Toast/Dialog）、多语言目录
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

兄弟 feature 之间只依赖对方的 `-api` 接口（如 `EmailEntry`、`SettingsEntry`），实现由 impl 模块通过 Koin 绑定到接口；仅组合根（`:composeApp`）依赖全部实现模块，以此切断 feature 间的直接依赖。入口契约位于 `feature/<name>-api/src/commonMain/kotlin/me/zhangls/<name>/api/`。

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

### 数据加载

应用使用分页数据加载（Paging），通过 `EmailViewModel` 管理：

- `emailPaging` / `emailFavoritePaging` 流提供分页数据（`cachedIn(viewModelScope)`）
- 使用 `collectAsLazyPagingItems()` 在 Compose 中收集数据
- 加载状态通过 `LoadState` 监控；初始加载显示 `LoadingIndicator`，列表为空显示 `email_msg_no_emails` 资源

## 注意事项

- 项目使用 Koin 注解进行依赖注入（`@Module` / `@ComponentScan` / `@KoinViewModel`，KSP 生成），组合根在 `composeApp` 的 `NotesModule.kt`
- 使用 Compose Multiplatform 进行 UI 构建，Material 3（含 Expressive 与 Adaptive API）
- 数据库为 Room KMP 版本，字段变化必须配置 AutoMigration（见 `core/data` 的 `AppDatabase.kt`，schema 导出在 `core/data/schemas/`）
- 数据通过分页加载，支持无限滚动
- 支持多种设备尺寸和方向适配
- 依赖版本统一在 `gradle/kmp.versions.toml`（typesafe 访问器为 `kmp.` 前缀）
- 同一仓库目录下不要并发执行两个 Gradle 构建（守护进程地址注册表会锁冲突）
