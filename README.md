# 摘星（Notes）

一个基于 **Kotlin + Jetpack Compose + 多模块架构** 的 Android 应用示例，聚焦于以下能力：

- 模块化拆分（`app / core / feature / output`）
- MVI 状态管理
- Navigation3 导航与登录拦截
- Room + Paging3 邮件列表/详情
- DataStore（JSON）+ Android Keystore（AES）本地数据加密
- Retrofit + OkHttp 网络层统一封装
- Hilt 依赖注入
- 自适应布局（Material3 Adaptive）

---

## 目录

- [项目结构](#项目结构)
- [技术栈](#技术栈)
- [架构设计](#架构设计)
- [核心功能](#核心功能)
- [快速开始](#快速开始)
- [构建与运行](#构建与运行)
- [Deep Link 调试](#deep-link-调试)
- [Fused Library 打包（实验）](#fused-library-打包实验)
- [数据与安全](#数据与安全)
- [当前实现边界](#当前实现边界)
- [常见问题](#常见问题)

---

## 项目结构

```text
Notes
├── app                 # 应用入口、全局导航、全局状态、注入聚合
├── core
│   ├── framework       # MVI 基类、导航协议、全局 Effect
│   ├── data            # Room、DataStore、Repository、加密工具
│   ├── network         # Retrofit/OkHttp、鉴权、错误映射
│   └── theme           # Compose 主题与通用组件
├── feature
│   ├── login           # 登录功能（MVI）
│   ├── main            # 首页/收藏/邮件详情（Paging + 自适应）
│   └── settings        # 设置页（偏好项映射 + 退出登录）
└── output
    └── login           # Fused Library 实验打包模块
```

`settings.gradle.kts` 中启用模块：

- `:app`
- `:core:data`
- `:core:theme`
- `:core:network`
- `:core:framework`
- `:feature:main`
- `:feature:login`
- `:feature:settings`
- `:output:login`

---

## 技术栈

### Android / Kotlin

- AGP: `9.0.1`
- Kotlin: `2.3.10`
- JDK: `17`
- compileSdk: `36`
- minSdk: `28`

### UI

- Jetpack Compose（BOM: `2026.02.01`）
- Material3 + Material3 Adaptive
- Navigation3（`androidx.navigation3`）

### 架构与异步

- MVI（自定义 `MviViewModel`）
- Kotlin Coroutines + Flow
- Hilt DI

### 数据层

- Room (`2.8.4`) + Paging3 (`3.3.6`)
- DataStore (`1.2.0`) + Kotlinx Serialization (`1.10.0`)
- Android Keystore + AES/GCM

### 网络层

- Retrofit (`3.0.0`)
- OkHttp (`5.3.2`)
- kotlinx serialization converter

---

## 架构设计

### 1) 分层与职责

- `app`：组装所有模块，承接全局导航、主题、登录态和 Deep Link 入口。
- `feature:*`：按业务功能拆分 UI + ViewModel + Intent/Action/State。
- `core:data`：本地数据源、模型与仓储。
- `core:network`：网络客户端、拦截器、错误统一处理。
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
- JDK 17
- Android SDK / Build Tools 36
- 可用 Android 模拟器或真机（Android 9+）

### 本地配置

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

- 当前 `app/build.gradle.kts` 中 `debug` 与 `release` 都绑定 `release` 签名配置。
- 如果本地不需要签名打包，可自行调整 `buildTypes.debug.signingConfig`。

---

## Deep Link 调试

通过 ADB 触发邮件详情页（若未登录会先走登录拦截，登录成功后回跳）：

```bash
adb shell am start \
  -a android.intent.action.VIEW \
  -d "https://notes.zhangls.me/email?id=1&token=debug" \
  me.zhangls.notes
```

---

## Fused Library 打包（实验）

项目包含 `:output:login` 模块（`com.android.fused-library`），用于融合导出登录相关能力。

```bash
./gradlew :output:login:assemble
```

注意：该能力目前在 Android 官方仍属实验性质，仓库内也已标注“仅供测试”。

---

## 数据与安全

### 本地数据

- Room：`AccountEntity`、`EmailEntity`，并启用 schema 导出。
- DataStore：
  - `settings.json` -> `SettingsModel`
  - `user.json` -> `UserModel?`

### 加密策略

- DataStore 序列化读写前后使用 `AESUtils` 处理。
- 密钥由 Android Keystore 管理（`AES/GCM/NoPadding`）。

### 网络结果统一

- `safeApiCall` 将异常和业务 code 映射到 `NetworkResult`：
  - `Success<T>`
  - `Failure(NetworkError.*)`

---

## 当前实现边界

以下内容是当前代码中的真实状态，便于二次开发时快速判断：

- Room 使用 `inMemoryDatabaseBuilder`，应用重启后数据库数据会重置。
- `NotesApp` 每次启动都会向数据库插入本地邮件样例数据。
- `TokenProviderImpl` 的 `getAccessToken/refreshToken` 仍为占位实现（返回 `null`）。
- `JokeViewModel` 中 `appId/appSecret` 为空，网络示例默认不可用。
- 测试代码目前多为模板样例（`ExampleUnitTest` / `ExampleInstrumentedTest`）。

---

## 常见问题

### 1. 为什么启动后总是有初始邮件？

`NotesApp.onCreate()` 会调用 `initData()`，将 `LocalEmailsDataProvider` 数据写入仓储。

### 2. 为什么我明明配置了账号仍可能回到登录页？

`RequireLogin` 页面会统一走导航拦截；当 `UserRepository.userFlow` 为空时，首屏会是登录页。

### 3. 为什么打包时提示签名配置问题？

因为当前 `debug/release` 都依赖 `local.properties` 中的签名字段，需保证路径与密码有效。

---

