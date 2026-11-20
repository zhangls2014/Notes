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
├── web                         # Compose Multiplatform 官网与发布文件生成
├── core/
│   ├── model                   # 稳定的跨层值类型
│   ├── database                # Room entity、DAO、迁移和建库
│   ├── data                    # 公开模型、Repository 与数据实现
│   ├── theme                   # 主题、通用组件和自适应布局环境
│   ├── preference              # 设置元数据和通用设置控件
│   ├── network                 # Ktor 网络基础设施
│   ├── download                # HTTP 分片下载、断点续传和完整性校验
│   ├── framework               # MVI、导航契约、Deep Link 和全局 Effect
│   └── navigation              # 导航显示、预测返回转场、圆角和窗格安全区
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
| Kotlin | 2.4.21 |
| Gradle | 9.9.1 |
| JDK toolchain / JVM target | 25 / 25（分别维护） |
| Android Gradle Plugin | 9.5.1 |
| Android SDK | compile 37.2，target 37，min 28 |
| Compose Multiplatform | 1.13.0-rc01；Android Compose BOM 2026.11.01（Compose 1.13.0） |
| Material 3 | JetBrains 1.13.0-rc01 / AndroidX 1.5.0，Expressive 主题 |
| Material 3 Adaptive | 1.3.0 |
| Navigation 3 | JetBrains 1.2.0 / AndroidX 1.2.2 |
| NavigationEvent | 1.2.0 |
| Koin | 4.2.2，编译器插件 1.2.3 |
| Room | 3.0.3，KSP 生成 |
| Paging | 3.5.3 |
| Ktor | 3.6.1 |
| DataStore | 1.3.0 |
| Kotlinx Serialization | 1.13.0 |
| Roborazzi | 1.79.0 |
| UI Automator | 2.4.1 |

后续升级遵循 [依赖与工具链升级规范](docs/dependency-upgrades.md)。Material 3 和 detekt 当前保留必要预览版。本次为维护者授权的一次性强制升级，允许超出官方兼容表，以实测记录为准；版本与验证状态见 [2026-11-20 升级记录](docs/audits/2026-11-20-dependency-upgrade.md)。

## 快速开始

### 环境要求

- 支持当前 KMP/Compose 版本的 Android Studio
- 可启动 Gradle 的 JDK（例如 Android Studio 的 JBR 21）；编译 toolchain JDK 25 由 Gradle 自动准备
- Android SDK Platform 37.2 与 Build Tools 37.0.0
- 本次强制升级使用本机 Xcode 27.2 验证；官方表仍列 Xcode 26.4，后续常规升级继续遵守官方范围
- Android 9（API 28）或更高版本的设备/模拟器

### 本地配置

项目通过 [Foojay resolver](https://github.com/gradle/foojay-toolchains) 为已有的 `jvmToolchain` 配置下载源。Gradle 优先使用检测到的匹配 JDK；缺少 JDK 25 时首次构建自动下载，缓存到 `$GRADLE_USER_HOME/jdks`（默认 `~/.gradle/jdks`）。使用同一 Gradle 用户目录的终端、IDE 和 worktree 可以复用缓存，无需逐次设置 `JAVA_HOME`，不修改系统 JDK 注册。首次准备需要访问 Foojay API 与发行版下载站点；不要在用户级 Gradle 配置中关闭自动检测或自动下载。

可先检查启动 JVM 和已检测的 toolchain，再运行测试以触发缺失 JDK 的下载：

```bash
./gradlew --version
./gradlew -q javaToolchains
./gradlew :core:framework:testAndroidHostTest
```

`javaToolchains` 只列出安装，不会主动下载；实际编译或测试任务解析 toolchain 时才下载。`--version` 显示的是 Gradle 启动与运行 JVM，不代表编译或测试使用的 JDK。IDE 的 Gradle JDK 可继续使用兼容的 JBR；编译 toolchain 与字节码目标分别维护，版本上限遵循[升级规范](docs/dependency-upgrades.md)。离线环境需提前完成一次联网构建以准备 JDK 和依赖。

遇到 JDK 缺失或版本不匹配时，先检查 Android Studio 自带的 JBR/JDK。macOS 默认位置可执行：

```bash
"/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/java" -version
"/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/javac" -version
```

自定义安装或其他系统使用实际安装路径。确认版本满足项目要求及工具链支持范围后，可用命令级参数提供给 Gradle，无需系统注册；例如只补充编译 toolchain 的搜索路径：

```bash
./gradlew -q javaToolchains \
  -Porg.gradle.java.installations.paths="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

后续构建或测试命令需带上同一参数。如果需要 Gradle 运行 JVM 本身也是该版本，则为该命令设置 `JAVA_HOME`。不能仅凭 `java_home -V` 未列出或 shell 默认版本不同，就判定本机没有所需 JDK；没有匹配的现有安装时，再使用上述自动下载机制。

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

项目本机配置统一放在仓库根目录的 `.local/`，整个目录由 Git 忽略。每个 checkout 或
worktree 单独准备需要的文件；目录权限为 `0700`，文件为 `0600`。

- `server.properties`：应用构建使用的三个 HTTPS 服务地址。
- `signing.properties` 和 `signing.jks`：Debug 和 Release 共用的 Android 正式签名配置与签名库。
- `production-access.local.md`：生产源站和访问记录，只供人工运维参考，构建不读取。
- `release.json`：本地发布命令的目标目录、公开来源和可信 APK 身份；格式及操作见 [发布工具](tools/release/README.md)。

`.local/server.properties` 示例（将示例域名替换为实际应用服务地址）：

```properties
notes.authBaseUrl=https://auth.example.com
notes.siteBaseUrl=https://notes.example.com
notes.apkBaseUrl=https://downloads.example.com
```

`.local/signing.properties` 示例，文件中的签名库路径相对于 `.local/`：

```properties
notes.signing.path=signing.jks
notes.signing.storePassword=***
notes.signing.keyAlias=***
notes.signing.keyPassword=***
```

读取顺序为 Gradle 属性、环境变量、`.local/` 文件。自动构建可通过
`NOTES_AUTH_BASE_URL`、`NOTES_SITE_BASE_URL`、`NOTES_APK_BASE_URL` 提供服务地址，
Android 签名对应 `NOTES_SIGNING_PATH`（建议用绝对路径）、`NOTES_SIGNING_STORE_PASSWORD`、
`NOTES_SIGNING_KEY_ALIAS` 和 `NOTES_SIGNING_KEY_PASSWORD`。
缺少必需值时 Gradle 会报出缺失字段。官网和认证地址的域名须与平台关联配置一致；
Android Studio 生成的 SDK 用 `local.properties` 仍留在原位置。

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
./gradlew :androidApp:lintDevGlobalDebug
```

KMP 约定插件通过 `withHostTest {}` 为共享模块启用 `testAndroidHostTest`，可在宿主 JVM 运行 `commonTest` 与 `androidHostTest`；`iosTest` 仍通过 iOS target 执行。按修改范围选择模块，不依赖统一的根 `test` task。源码分布、导航定向回归与检查命令见 [测试指南](docs/testing/README.md)，更多硬约束和 OOM 参数见 [AGENTS.md](AGENTS.md)。

## Deep Link 调试

演示构建支持 `https://notes.zhangls.me/email?id=<id>`。未登录时先进入登录页，成功后恢复目标。启用 [Notes 认证服务](server/auth/README.md) 的构建还要求链接携带 `accountId=<账号 UUID>`，并在目标账号的邮件仓库就绪后打开详情。

演示构建调试命令如下。

```bash
adb shell am start \
  -a android.intent.action.VIEW \
  -d "https://notes.zhangls.me/email?id=1" \
  me.zhangls.notes
```

## Android 多形态测试

调整模拟器分辨率、密度或旋转后，必须在结束操作前恢复默认设置。完整命令、`trap` 清理示例和验证标准见 [Android 模拟器测试](docs/testing/android-emulator.md)。

## 数据与安全

- 模拟邮件由认证服务按用户生成并保存在 PostgreSQL，客户端通过网络分页查询；旧 Room 邮件实现与文件保留，正式入口不再读取本地示例数据。
- 使用模拟邮箱前，需要经单独授权更新服务端与数据库；没有配置认证服务的本地登录演示不提供邮件数据。
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

本地 Maven 发布与独立消费验证见 [Android 输出说明](android/output/README.md)。单独复制 AAR 不会自动传递第三方依赖。

## 当前实现边界

- 登录仍是本地模拟，尚未接入真实认证和 token 刷新端点。安全凭据存储不等同于完整的服务端会话管理。
- 网络客户端当前固定输出请求头日志，并隐藏 Authorization；尚未按 Debug/Release 区分日志策略。
- Android 17 与多形态适配尚未覆盖的验证范围见 [待验收清单](docs/testing/pending-acceptance.md)，原始记录按 [文档历史追溯](docs/README.md#历史追溯) 查询。
