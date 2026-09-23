# 摘星（Notes）

Notes 是一个使用 Kotlin Multiplatform 与 Compose Multiplatform 构建的邮件管理示例应用。Android 与 iOS 共享业务逻辑和 UI，项目重点展示模块边界、Navigation 3、MVI、分页数据、自适应布局和多平台数据层。

## 功能

- 登录与受保护页面拦截
- 首页、收藏、邮件详情和搜索
- 邮件分页、批量操作与写邮件
- 语言、主题、动态取色、字号和退出登录设置
- Deep Link 打开邮件详情
- 手机、平板、桌面窗口与折叠设备自适应布局
- Android 与 iOS 共享 Compose UI

## 项目结构

```text
Notes/
├── androidApp                  # Android 应用入口
├── iosApp                      # iOS Xcode 工程
├── composeApp                  # 共享应用入口、根导航和 Koin 组合根
├── core/
│   ├── model                   # 稳定的跨层值类型
│   ├── database                # Room entity、DAO、迁移和建库
│   ├── data                    # 公开模型、Repository 与数据实现
│   ├── theme                   # 主题、通用组件和自适应布局环境
│   ├── preference              # 设置元数据和通用设置控件
│   ├── network                 # Ktor 网络基础设施
│   └── framework               # MVI、导航、Deep Link 和全局 Effect
├── feature/
│   ├── main / main-api         # 应用外壳和顶层 Tab
│   ├── login / login-api       # 登录
│   ├── email / email-api       # 邮件列表、收藏、详情、搜索和写信
│   └── settings / settings-api # 设置
├── android/
│   ├── baselineprofile         # Android 基线配置文件
│   └── output/login            # 实验性 Fused Library
├── build-logic                 # KMP、Compose、Koin 约定插件
└── docs                        # 架构、操作指南和历史审计
```

每个 Feature 的 `-api` 模块保存目的地和 Entry 等稳定契约，实现模块保存页面、MVI 和 DI 实现。Feature 实现之间不直接依赖，`composeApp` 是最终装配点。

详细规则见 [当前架构](docs/architecture.md)，其他资料见 [文档索引](docs/README.md)。供 AI 使用的命令与硬约束见 [AGENTS.md](AGENTS.md)。

## 技术栈

版本以 `gradle/kmp.versions.toml` 为唯一来源。当前主要版本：

| 类别 | 技术或版本 |
|---|---|
| Kotlin | 2.4.20 |
| JVM target | 21 |
| Android Gradle Plugin | 9.3.2 |
| Android SDK | compile/target 37，min 28 |
| Compose Multiplatform | 1.12.0 |
| Material 3 Adaptive | 1.3.0-rc01 |
| Navigation 3 | 1.1.1 |
| Koin | 4.2.2，注解与 KSP |
| Room | 3.0.2 |
| Paging | 3.5.1 |
| Ktor | 3.6.0 |

## 架构概览

### 依赖与数据边界

依赖从 `composeApp` 流向 Feature 实现、Feature API 和 Core。`core:data` 以 `implementation` 依赖 `core:database`，因此 Room 类型不会传递到 Feature；Repository 接口只暴露 `core:data` 的公开模型。

`core:preference` 以同样方式隐藏底层设置控件库。设置元数据和通用渲染位于 Core，当前值来自数据层，行为和清单由 `feature:settings` 拥有。

### MVI

`core:framework` 的 `MviViewModel` 提供 Intent → State (+ Effect) 单向数据流。Feature 将 `Intent`、`Action`、`State`、`Reducer` 和 `ViewModel` 放在各自实现模块。

状态跨进程持久化是 opt-in。`savedKey` 默认 `null`，避免密码、token 或大对象意外写入 instance state；只有经过确认的小型非敏感状态才显式启用恢复。

### Navigation 3

Feature API 定义实现 `Destination` 的类型安全目的地和导航贡献，`composeApp/AppNavHost.kt` 聚合 Nav Entry、维护返回栈并处理 Deep Link。

实现 `RequireLogin` 的目的地会经过登录拦截。未登录时导航到 `LoginDestination`，原目标保存在可序列化的 `redirectTo` 中，登录成功后恢复。

首页与收藏分别是列表场景，邮件详情是独立目的地。根导航通过 Navigation 3 `SceneStrategy` 在合适的窗口中组合列表与详情。

### 自适应布局

应用在组合根读取一次 `WindowAdaptiveInfo`，再通过 CompositionLocal 下发窗口事实和 `PaneScaffoldDirective`。各 Feature 使用这些事实决定局部形态，不维护自己的窗口断点。

`ContentWidth` 统一限制表单、正文和设置列表的可读宽度。登录页使用 `SupportingPaneScaffold` 适配普通手机、宽屏和铰链排除区。

## 快速开始

### 环境要求

- 支持当前 KMP/Compose 版本的 Android Studio
- JDK 21
- Android SDK 37 与 Build Tools 37.0.0
- Xcode（构建 iOS 时）
- Android 9（API 28）或更高版本的设备/模拟器

### 本地配置

Android SDK 路径使用机器级环境变量，所有 Git worktree 共用。例如 macOS 默认安装位置：

```bash
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$PATH:$ANDROID_HOME/platform-tools"
```

请确保启动 Android Studio、Codex 或运行 Gradle 的进程能读取到 `ANDROID_HOME`。
仅在终端的交互式 shell 配置该变量，可能不会传给从桌面启动的应用。
也可以在执行构建时显式传入，例如：

```bash
ANDROID_HOME="$HOME/Library/Android/sdk" ./gradlew :androidApp:assembleDebug
```

Release 签名信息保存在用户级 `~/.gradle/gradle.properties`，供所有 worktree 使用：

```properties
notes.signing.path=/absolute/path/to/your.jks
notes.signing.storePassword=***
notes.signing.keyAlias=***
notes.signing.keyPassword=***
```

CI 可分别使用 `NOTES_SIGNING_PATH`、`NOTES_SIGNING_STORE_PASSWORD`、
`NOTES_SIGNING_KEY_ALIAS` 和 `NOTES_SIGNING_KEY_PASSWORD` 环境变量。
优先级为 Gradle 属性、环境变量、现有 `local.properties` 中的 `signing.*` 值。
Debug 构建不需要 Release 签名；
缺少任意签名值时，不配置 Release 签名。Android Studio 仍可能生成被 Git 忽略的
`local.properties`，无需把它复制到新 worktree，也不要提交凭据。已有的
`local.properties` 签名配置仍然可用。

## 构建与运行

### Android

```bash
# 构建 APK；composeApp 是 library，整包任务位于 androidApp
./gradlew :androidApp:assembleDebug

# 安装到连接的设备
./gradlew :androidApp:installDebug
```

### iOS

编译共享代码：

```bash
./gradlew :composeApp:compileKotlinIosSimulatorArm64
```

运行应用时使用 Xcode 打开 `iosApp/iosApp.xcodeproj`。

### 测试与静态检查

```bash
./gradlew :composeApp:iosSimulatorArm64Test
./gradlew :core:data:iosSimulatorArm64Test
./gradlew :androidApp:lintDevFullDebug
```

Android 宿主没有统一的根 `test` task；共享测试通过 iOS 模拟器 target 执行。更多命令和 OOM 参数见 [AGENTS.md](AGENTS.md)。

## Deep Link 调试

应用支持 `https://notes.zhangls.me/email?id=<id>`。未登录打开受保护目标时会先进入登录页，成功后恢复目标：

```bash
adb shell am start \
  -a android.intent.action.VIEW \
  -d "https://notes.zhangls.me/email?id=1" \
  me.zhangls.notes
```

## Android 多形态测试

调整模拟器分辨率、密度或旋转后，必须在结束操作前恢复默认设置。完整命令、`trap` 清理示例和验证标准见 [Android 模拟器测试](docs/testing/android-emulator.md)。

## 数据与安全

- Room 保存账户和邮件数据，并导出 schema 以支持 AutoMigration。
- DataStore 保存设置和当前用户，多平台序列化使用 Kotlinx Serialization。
- DataStore 当前只做 JSON 序列化，用户 token 会以明文写入本地偏好文件；Android 的 `AESUtils` 尚未接入该读写链路。
- Ktor 的网络调用统一映射为 `NetworkResult`。
- `MviViewModel` 默认不持久化 State，敏感信息不会因为开发者忘记关闭恢复而落入 instance state。

## Fused Library

`:android:output:login` 是实验性 Android Fused Library 输出：

```bash
./gradlew :android:output:login:assemble
```

## 当前实现边界

- DataStore 静态加密仍待实现；在此之前不要把当前本地存储描述为已由 Keystore 保护。
- Android 17 与多形态适配的历史验证范围和限制记录在 [审计目录](docs/audits/)。
