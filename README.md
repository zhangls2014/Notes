# 摘星（Notes）

一个基于 **Kotlin Multiplatform (KMP) + Compose Multiplatform + 模块化架构** 的跨平台应用示例，覆盖 Android 与 iOS，聚焦于以下能力：

- 模块化拆分（`androidApp / iosApp / composeApp / core / feature / android/output`）
- MVI 状态管理
- Navigation3 导航与登录拦截
- 邮件列表分页展示（使用 Paging3）
- DataStore（JSON）本地数据存储（Android 端支持 Android Keystore 加密）
- Ktor 网络层统一封装（Android/ iOS 引擎）
- Koin 依赖注入
- 自适应布局（Material3 Adaptive）

---

## 目录

- [项目结构](#项目结构)
- [技术栈](#技术栈)
- [架构设计](#架构设计)
- [核心功能](#核心功能)
- [快速开始](#快速开始)
- [构建与运行](#构建与运行)
- [Deep Link 调试（Android）](#deep-link-调试android)
- [Fused Library 打包（Android 实验）](#fused-library-打包android-实验)
- [数据与安全](#数据与安全)
- [当前实现边界](#当前实现边界)
- [常见问题](#常见问题)

---

## 项目结构

```text
Notes
├── androidApp           # Android 入口应用
├── iosApp               # iOS 入口应用（Xcode 工程）
├── composeApp           # 共享应用入口与跨平台 UI/导航
├── core
│   ├── framework        # MVI 基类、导航协议、全局 Effect
│   ├── data             # 数据层（多平台实现）
│   ├── network          # Ktor 网络层（多平台实现）
│   └── theme            # Compose 主题与通用组件
├── feature
│   ├── login            # 登录功能（MVI）
│   ├── main             # 首页/收藏/邮件详情
│   └── settings         # 设置页（偏好项映射 + 退出登录）
└── android
    ├── output
    │   └── login         # Fused Library 实验打包模块（Android）
    └── baselineprofile   # Android 基线配置文件
```

`settings.gradle.kts` 中启用模块：

- `:androidApp`
- `:iosApp`
- `:composeApp`
- `:core:data`
- `:core:theme`
- `:core:network`
- `:core:framework`
- `:feature:main`
- `:feature:login`
- `:feature:settings`
- `:android:output:login`
- `:android:baselineprofile`

---

## 技术栈

### KMP / 工程

- AGP: `9.1.0`
- Kotlin: `2.3.20`
- JDK: `21`
- Android compileSdk: `36`
- Android minSdk: `28`

### UI

- Compose Multiplatform（`org.jetbrains.compose`）
- Material3 + Material3 Adaptive
- Navigation3（`androidx.navigation3`）

### 架构与异步

- MVI（自定义 `MviViewModel`）
- Kotlin Coroutines + Flow
- Koin DI

### 数据层

- Room3 + Paging3
- DataStore + Kotlinx Serialization
- Android Keystore + AES/GCM（Android）

### 网络层

- Ktor Client（OkHttp / Darwin 引擎）
- kotlinx serialization

---

## 架构设计

### 1) 分层与职责

- `androidApp`：Android 入口与平台集成。
- `iosApp`：iOS 入口（Xcode 工程）。
- `composeApp`：共享应用入口、跨平台 UI 与导航。
- `feature:*`：按业务功能拆分 UI + ViewModel + Intent/Action/State。
- `core:data`：本地数据源、模型与仓储，多平台实现。
- `core:network`：Ktor 客户端、拦截器、错误统一处理。
- `core:framework`：MVI 与导航协议复用。
- `core:theme`：主题、Toast、Dialog、TopBar 等通用 UI。

### 2) MVI 模式

`core/framework/mvi/MviViewModel` 提供统一能力：

- `intent` 输入（`MutableSharedFlow`）
- `state` 持有（`MutableStateFlow`）
- `effect` 单次事件流（Toast、Dialog、导航等）
- 基于 `SavedStateHandle` 的状态恢复

每个 feature 使用：

- `State`：当前 UI 状态
- `Intent`：用户输入
- `Action + Reducer`：纯状态变换
- `ViewModel`：处理 Intent 与副作用

### 3) 导航与登录拦截

应用使用 Navigation3 + 自定义 `Destination` 协议。

- 需要登录访问的页面实现 `RequireLogin`。
- `AppNavHost` 在导航前执行 `navCheck`：
  - 未登录 + 目标需登录 => 记录 `pendingDestination`，跳转登录页。
  - 登录成功后自动恢复到 `pendingDestination`。

这套机制同样适配 Deep Link（例如直接打开邮件详情）。

### 4) 数据流（简化）

```text
Composable -> Intent -> ViewModel -> Repository
                    <- State <- Reducer <- Action

Repository -> Room/DataStore/Network -> Flow<PagingData/Model>
```

---

## 核心功能

### 登录（`feature:login`）

- 账户/密码输入与本地校验（`LoginValidator`）
- 模拟异步登录，成功后写入 `UserRepository`
- 通过全局 Toast 通知登录成功

### 首页与收藏（`feature:main`）

- 邮件分页展示（Paging3）
- 搜索（防抖 300ms）
- 批量收藏、取消收藏、删除
- 自适应 List-Detail 布局，支持不同窗口尺寸

### 设置（`feature:settings`）

- 动态主题色开关
- 深色模式（跟随系统/浅色/深色）
- 退出登录（二次确认对话框）

### Deep Link

- 支持域名：`https://notes.zhangls.me`
- 支持路径：`/email`（代码内解析 `id` 参数）

---

## 快速开始

### 环境要求

- Android Studio（建议最新稳定版）
- Xcode（建议最新稳定版）
- JDK 21
- Android SDK / Build Tools 36
- 可用 Android 模拟器或真机（Android 9+）

### 本地配置（Android）

在项目根目录创建或更新 `local.properties`：

```properties
sdk.dir=/path/to/Android/sdk

# 打包签名（debug/release 都会读取 release signingConfig）
signing.path=/path/to/your.jks
signing.storePassword=***
signing.keyAlias=***
signing.keyPassword=***
```

说明：

- 当前 `androidApp/build.gradle.kts` 中 `debug` 与 `release` 都绑定 `release` 签名配置。
- 如果本地不需要签名打包，可自行调整 `buildTypes.debug.signingConfig`。

---

## 构建与运行

### 构建项目

```bash
./gradlew build
```

### 运行 Android

```bash
./gradlew :androidApp:installDebug
```

### 运行 iOS

使用 Xcode 打开 `iosApp/iosApp.xcodeproj` 运行。

---

## Deep Link 调试（Android）

通过 ADB 触发邮件详情页（若未登录会先走登录拦截，登录成功后回跳）：

```bash
adb shell am start \
  -a android.intent.action.VIEW \
  -d "https://notes.zhangls.me/email?id=1&token=debug" \
  me.zhangls.notes
```

---

## Fused Library 打包（Android 实验）

项目包含 `:android:output:login` 模块（`com.android.fused-library`），用于融合导出登录相关能力。

```bash
./gradlew :android:output:login:assemble
```

注意：该能力目前在 Android 官方仍属实验性质，仓库内也已标注“仅供测试”。

---

## 数据与安全

### 本地数据

- Android 端使用 Room：`AccountEntity`、`EmailEntity`，并启用 schema 导出。
- DataStore：
  - `settings.json` -> `SettingsModel`
  - `user.json` -> `UserModel?`

### 加密策略（Android）

- DataStore 序列化读写前后使用 `AESUtils` 处理。
- 密钥由 Android Keystore 管理（`AES/GCM/NoPadding`）。

### 网络结果统一

- `safeApiCall` 将异常和业务 code 映射到 `NetworkResult`：
  - `Success<T>`
  - `Failure(NetworkError.*)`

---

## 当前实现边界

以下内容是当前代码中的真实状态，便于二次开发时快速判断：

- `TokenProviderImpl` 的 `getAccessToken/refreshToken` 仍为占位实现（返回 `null`）。
- `JokeViewModel` 中 `appId/appSecret` 为空，网络示例默认不可用。
- 测试代码目前多为模板样例（`ExampleUnitTest` / `ExampleInstrumentedTest`）。

---

## 常见问题

### 1. 为什么启动后总是有初始邮件？

`NotesApp.onCreate()`（Android）会调用 `initData()`，将 `LocalEmailsDataProvider` 数据写入仓储。

### 2. 为什么我明明配置了账号仍可能回到登录页？

`RequireLogin` 页面会统一走导航拦截；当 `UserRepository.userFlow` 为空时，首屏会是登录页。

### 3. 为什么打包时提示签名配置问题？

因为当前 `debug/release` 都依赖 `local.properties` 中的签名字段，需保证路径与密码有效。
