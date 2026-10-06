# 摘星（Notes）

Notes 是一个使用 Kotlin Multiplatform 与 Compose Multiplatform 构建的邮件管理示例应用。Android 与 iOS 共享业务逻辑和 UI，项目重点展示模块边界、Navigation 3、MVI、分页数据、自适应布局和多平台数据层。

## 功能

- 登录与受保护页面拦截
- 首页、收藏、邮件详情和搜索
- 邮件分页、批量操作与写邮件
- 语言、主题、动态取色、字号和退出登录设置
- 个人信息展示与头像修改
- 关于应用、版本信息与构建号展示
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
│   ├── settings / settings-api # 设置清单、行为与入口结果
│   ├── profile / profile-api   # 个人信息与头像修改
│   └── about / about-api       # 关于应用与版本信息
├── android/
│   ├── baselineprofile         # Android 基线配置文件
│   └── output/login            # 实验性 Fused Library
├── build-logic                 # KMP、Compose、Koin、detekt 约定插件
└── docs                        # 架构、操作指南和历史审计
```

每个 Feature 的 `-api` 模块保存目的地和 Entry 等稳定契约，实现模块保存页面、MVI 和 DI 实现。Feature 实现之间不直接依赖，`composeApp` 是最终装配点。

详细规则见 [当前架构](docs/architecture.md)，其他资料见 [文档索引](docs/README.md)。供 AI 使用的命令与硬约束见 [AGENTS.md](AGENTS.md)。

## 技术栈

版本以 `gradle/kmp.versions.toml` 为唯一来源。以下版本对应当前版本目录，升级依赖时同步此表：

| 类别 | 技术或版本 |
|---|---|
| Kotlin | 2.4.20 |
| Gradle | 9.7.0 |
| JDK toolchain / JVM target | 25 / 25（分别维护） |
| Android Gradle Plugin | 9.3.1 |
| Android SDK | compile 37.0，target 37，min 28 |
| Compose Multiplatform | 1.12.1 |
| Material 3 | 1.12.0-alpha03，Expressive 主题 |
| Material 3 Adaptive | 1.3.0 |
| Navigation 3 | 1.1.2 |
| Koin | 4.2.2，注解与编译器插件 1.2.1 |
| Room | 3.0.3，KSP 生成 |
| Paging | 3.5.1 |
| Ktor | 3.6.0 |

后续升级遵循 [依赖与工具链升级规范](docs/dependency-upgrades.md)。Material 3（现用菜单 API）与 detekt 当前保留必要预览版；当次版本决策、官方兼容范围和验证状态见 [2026-10-07 升级记录](docs/audits/2026-10-07-dependency-upgrade.md)。

## 架构概览

### 依赖与数据边界

依赖从 `composeApp` 流向 Feature 实现、Feature API 和 Core。`core:data` 以 `implementation` 依赖 `core:database`，因此 Room 类型不会传递到 Feature；Repository 接口只暴露 `core:data` 的公开模型。

`core:preference` 以同样方式隐藏底层设置控件库。设置元数据和通用渲染位于 Core，当前值来自数据层，行为和清单由 `feature:settings` 拥有。个人信息和关于应用分别由独立 Feature 实现；设置页通过 `SettingsResult` 通知宿主打开页面，由 `composeApp` 装配导航。

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
- JDK 25（编译 toolchain；建议运行 Gradle 的 JDK 与其一致）
- Android SDK Platform 37.0 与 Build Tools 37.0.0
- Xcode 26.4（当前 Kotlin 官方兼容表所列版本；本机 Xcode 27 不作为该范围的验收环境）
- Android 9（API 28）或更高版本的设备/模拟器

### 本地配置

先设置当前进程的 `JAVA_HOME` 指向已有 JDK 25，再执行 Gradle。本机 Android Studio 内置 JBR 25.0.3，本次使用：

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
"$JAVA_HOME/bin/java" -version
```

这只是当前终端的环境设置，其他机器按实际 JDK 25 路径调整。IDE 的 Gradle JDK 也选择 JDK 25；编译 toolchain 与字节码目标分别维护。版本上限按官方兼容表确定，不以项目实测通过为由越界。

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

Debug 和 Release 共用正式签名，签名信息保存在用户级 `~/.gradle/gradle.properties`，供所有 worktree 使用：

```properties
notes.signing.path=/absolute/path/to/your.jks
notes.signing.storePassword=***
notes.signing.keyAlias=***
notes.signing.keyPassword=***
```

CI 可分别使用 `NOTES_SIGNING_PATH`、`NOTES_SIGNING_STORE_PASSWORD`、
`NOTES_SIGNING_KEY_ALIAS` 和 `NOTES_SIGNING_KEY_PASSWORD` 环境变量。
优先级为 Gradle 属性、环境变量，不再从 `local.properties` 读取签名信息。
Debug 和 Release 始终使用正式签名配置，构建前必须补全上述配置；
缺失或无效的签名配置会导致构建失败。Android Studio 仍可能生成被 Git 忽略的
`local.properties`，无需把它复制到新 worktree，也不要提交凭据。

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
# 共享逻辑与 Android UI 宿主测试
./gradlew :composeApp:testAndroidHostTest :core:data:testAndroidHostTest \
  :feature:profile:testAndroidHostTest :feature:email:testAndroidHostTest

# iOS 平台及共享测试
./gradlew :composeApp:iosSimulatorArm64Test :core:data:iosSimulatorArm64Test

# Android 静态检查
./gradlew :androidApp:lintDevFullDebug
```

KMP 约定插件通过 `withHostTest {}` 为共享模块启用 `testAndroidHostTest`，可在宿主 JVM 运行 `commonTest` 与 `androidHostTest`；`iosTest` 仍通过 iOS target 执行。按修改范围选择模块，不依赖统一的根 `test` task。源码分布、导航定向回归与检查命令见 [测试指南](docs/testing/README.md)，更多硬约束和 OOM 参数见 [AGENTS.md](AGENTS.md)。

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
- DataStore 使用 Kotlinx Serialization 保存普通设置和用户资料；`UserModel` 不含 token，登录凭据由数据层内部的 `SecureTokenStore` 单独保存。
- Android 使用 Keystore 管理的 AES/GCM 密钥和随机 IV 加密凭据，通过 `AtomicFile` 写入 `noBackupFilesDir`；iOS 使用不启用同步的 Keychain，访问策略为 `WhenUnlockedThisDeviceOnly`。这些保护针对登录凭据，不代表 Room 邮件数据或整个 DataStore 已加密。
- 旧用户 JSON 在首次访问时迁移：安全写入并读回确认后，才移除明文 token。退出登录清除凭据和用户资料；资料与凭据的账号不匹配时，不公开登录态。
- Android 排除旧偏好文件的云备份和设备迁移，iOS 排除 DataStore 目录备份。普通设置也不会随该文件备份迁移，已存在的旧系统备份无法由此次升级追溯删除。
- Ktor 的网络调用统一映射为 `NetworkResult`。
- `MviViewModel` 默认不持久化 State，敏感信息不会因为开发者忘记关闭恢复而落入 instance state。

测试覆盖范围和平台人工验收步骤见 [登录凭据存储验证](docs/testing/secure-token-storage.md)。真实 iOS Keychain 集成测试因当前独立测试环境不可用而标记 `@Ignore`，需要在应用宿主中验证，不能以编译通过替代。

## Fused Library

`:android:output:login` 是实验性 Android Fused Library 输出：

```bash
./gradlew :android:output:login:assemble
```

## 当前实现边界

- 登录仍是本地模拟，尚未接入真实认证和 token 刷新端点。安全凭据存储不等同于完整的服务端会话管理。
- 网络客户端当前固定输出请求头日志，并隐藏 Authorization；尚未按 Debug/Release 区分日志策略。
- Android 17 与多形态适配的历史验证范围和限制记录在 [审计目录](docs/audits/)。
