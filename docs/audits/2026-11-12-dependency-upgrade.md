# 2026-11-12 应用依赖升级

日期：2026-11-12（Asia/Shanghai）

状态：已实施下列四组升级。验证结果见本文末节。

基线为 `1f54fb94`，开始时工作区干净。检查 [版本目录](../../gradle/kmp.versions.toml)、Gradle Wrapper、构建约定、Android SDK 配置和 iOS 工程。范围为 Android/iOS 应用直接声明的库、构建与测试插件及其 BOM，未扫描独立的 `server/auth` 或全量传递依赖漏洞。按 [升级规范](../dependency-upgrades.md) 以官方支持范围和工件发布状态判断候选。

## 本次采用的版本

| 组件 | 当前 → 候选 | 发布证据与回归重点 |
|---|---|---|
| kotlinx.serialization Core、JSON | 1.12.0 → 1.13.0 | 两库的 JVM、iosArm64、iosSimulatorArm64 共六个工件 URL 均返回 HTTP 200，目标元数据声明 Kotlin stdlib 2.4.10。新版撤回 1.12.0 的 JSON 异常包装变更，验证导航栈往返、请求与响应解析、无效输入异常处理。Kotlin Serialization 编译插件仍随 Kotlin。 [发布说明](https://github.com/Kotlin/kotlinx.serialization/releases/tag/v1.13.0) |
| Koin Compiler Plugin | 1.2.1 → 1.2.3 | 已核对 marker 和实现 module。官方列出 Kotlin 2.4.20、Koin 4.2.0+，项目的 Koin BOM 4.2.2 满足要求。新版修复 1.2.2 的 Native 编译回归，并调整跨模块依赖图诊断。验证双端编译及 Koin 装配。 [发布说明](https://github.com/InsertKoinIO/koin-compiler-plugin/releases/tag/1.2.3) |
| Roborazzi 插件和测试库 | 1.77.0 → 1.78.0 | 已核对 marker、实现和 Android 库 module。新版将不透明截图编码为 RGB PNG，运行现有截图验证并保留正式基线，必要时单独检查候选图。 [发布说明](https://github.com/takahirom/roborazzi/releases/tag/1.78.0) |
| UI Automator | 2.4.0 → 2.4.1 | 本库与 Shell Android AAR 均要求 compileSdk ≥ 34、AGP ≥ 8.1.1，当前配置满足。新版修复 ShellServer 随机端口冲突，验证 Baseline Profile 测试模块及设备启动流程。 [发布说明](https://developer.android.com/jetpack/androidx/releases/test-uiautomator#2.4.1) |

## 当前保留项

| 组件 | 当前 | 查到的正式版 | 保留依据 |
|---|---|---|---|
| Kotlin、随附编译插件和 kotlin-test | 2.4.20 | 2.4.21 | Koin Compiler 1.2.3 的官方兼容清单最高列到 2.4.20。 [Koin Compiler 兼容列表](https://github.com/InsertKoinIO/koin-compiler-plugin/releases/tag/1.2.3) |
| BuildKonfig | 0.23.0 | 0.24.0 | 插件的运行时元数据直接声明 Kotlin Gradle Plugin 和 stdlib 2.4.21，单独升级也会引入新 Kotlin 插件依赖。与 Kotlin 一起评估。 [实现工件元数据](https://repo.maven.apache.org/maven2/com/codingfeline/buildkonfig/buildkonfig-gradle-plugin/0.24.0/buildkonfig-gradle-plugin-0.24.0.module) |
| KSP | 2.3.12 | 2.3.13 | 上游确认并发执行时有符号解析回归，修复已合入源码，仓库最新正式版仍是 2.3.13。本项目开启 Gradle 并行，并为 Room 注册 Android 和两个 iOS 目标的 KSP。等修复版发布后重查。 [发布说明](https://github.com/google/ksp/releases/tag/2.3.13) · [上游问题](https://github.com/google/ksp/issues/3286) |
| AGP、Gradle Wrapper | 9.3.1、9.7.0 | 9.5.1、9.9.1 | JetBrains 对 Kotlin 2.4.20–2.4.21 列出的上限分别是 AGP 9.3.1、Gradle 9.7.0。 [AGP 元数据](https://dl.google.com/dl/android/maven2/com/android/tools/build/gradle/maven-metadata.xml) · [Gradle 发布](https://services.gradle.org/versions/current) · [KMP 兼容表](https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html) |
| Compose Multiplatform、Material 3 Adaptive | 1.12.2、1.3.0 | 1.12.2、1.3.0 | Compose 1.13.0-beta02 和 Adaptive 1.4.0-alpha01 均为预览。 [组件发布说明](https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.13.0-beta02) |
| Compose Multiplatform Material 3 | 1.12.0-alpha03 | 1.9.0 | 正式版源码工件中缺少项目使用的 `ContainedLoadingIndicator`；当前预览版与 Compose 1.12.2 的组件表一致。 [1.9.0 源码工件](https://repo.maven.apache.org/maven2/org/jetbrains/compose/material3/material3/1.9.0/material3-1.9.0-sources.jar) · [Compose 1.12.2](https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.12.2) |
| detekt、Compose Rules | 2.0.0-alpha.6、0.6.8 | 当前 detekt 坐标无正式版、0.6.8 | 插件与 ktlint wrapper 的 Maven 元数据均仍以 alpha.6 为最新发布。 [detekt 发布说明](https://github.com/detekt/detekt/releases/tag/v2.0.0-alpha.6) |

其余已核对坐标中，Room 3.0.3、SQLite 2.7.1、Paging 3.5.3、DataStore 1.3.0、Navigation 3 1.2.0、Lifecycle 2.11.0、Coil 3.6.3 和 Compose Preference 2.2.0 的配置版本与最新正式版一致。Ktor 3.6.1 和 Koin 4.2.2 的 BOM POM 分别覆盖目录中的 8 和 6 个受管别名。

## 环境与验证边界

- 三个官方仓库共成功读取 78 个去重坐标的 `maven-metadata.xml`，其中 55 个与当前正式版本一致，21 个最新正式版本与当前不同（含 3 个 Material 3 坐标的正式版低于当前预览版），2 个 detekt 坐标无正式版。Gradle Wrapper 单独核对。
- Android Studio JBR 的 `java` 和 `javac` 均为 25.0.4。当前 compileSdk 37.0、targetSdk 37、Build Tools 37.0.0。已安装 37.2，Google 有该级别的 API 差异报告，但本轮未找到 AGP 9.3.1 明确支持该小版本的证据。 [Android SDK API 差异](https://developer.android.com/sdk/api_diff/37.2-incr/changes/changes-summary) · [AGP 9.3 支持说明](https://developer.android.com/build/releases/agp-9-3-0-release-notes#compatibility)
- 本机选用 Xcode 27.2（27B52），未找到官方 KMP 表所列的 Xcode 26.4 安装。此次受限环境下 `simctl list devices booted` 无法连接 CoreSimulator，未取得模拟器清单。 [KMP 兼容表](https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html)

## 实施与验证结果

四组候选已写入版本目录。Koin Compiler 的插件别名和构建约定使用同一版本键。升级后按 [开源声明工具流程](../../tools/open-source/README.md) 从 Android 生产 Release 和 iOS 设备 ARM64 解析图重新生成声明。两端清单仅变更 Serialization 系列坐标，原许可证文本及详情页内容一致。

Gradle 调用使用 Android Studio JBR 25.0.4 串行执行。依赖图导出通过，10 个任务中 4 个执行、6 个 up-to-date。Python 生成器 10 项和四页的 1,920 组外观／导航、32 组语言及详情链接脚本检查通过。

```bash
./gradlew :androidApp:assembleDevFullDebug :androidApp:assembleDevFullRelease :androidApp:lintDevFullDebug \
  :composeApp:testAndroidHostTest :core:database:testAndroidHostTest :core:data:testAndroidHostTest \
  :core:framework:testAndroidHostTest :feature:about:testAndroidHostTest \
  :android:baselineprofile:assemble :feature:about:syncIosNoticesResources \
  --continue --console=plain --max-workers=4 \
  -Dkotlin.daemon.jvmargs=-Xmx4g '-Dorg.gradle.jvmargs=-Xmx4g -Dfile.encoding=UTF-8 -XX:+UseParallelGC'
```

- Android DevFull Debug、Release/R8、lint、Baseline Profile 模块打包均通过。逐字节比对两个 APK 内的 Android 声明及 iOS Sync 输出的 iOS 声明，每份产物只包含本平台两页。
- database、data、framework、about 四个模块宿主测试合计 36 通过／0 失败／0 跳过。composeApp 全量宿主 257 通过／20 失败／661 跳过，整批 Gradle 因该任务失败结束。966 个任务中 892 个执行、60 个来自构建缓存、14 个 up-to-date。日志在 `/private/tmp/notes-20261112-android.log`。
- 导航栈序列化 6 项全部通过。用临时 Gradle init script 恢复 Serialization 1.12.0、Koin Compiler 1.2.1、Roborazzi 1.77.0，重跑全量宿主中失败的相关方法，旧组合为 26 通过／20 失败／13 跳过。20 个失败的方法与参数完全相同，19 条异常文本一致，另一条只有 Robolectric 节点编号不同，断言与坐标一致。旧组合任务共 409 个，361 个执行、40 个来自缓存、8 个 up-to-date。日志在 `/private/tmp/notes-20261112-baseline.log`，XML 分别保存在 `/private/tmp/notes-20261112-candidate-xml/` 与 `/private/tmp/notes-20261112-baseline-xml/`。
- Roborazzi 对照用 `-Proborazzi.test.verify=true` 执行 `AdaptiveUiTest.loginFitsAndRetainsInputOnResize` 和 `AdaptiveUiTest.compactHeightNavigationIsVerticalAndAllDestinationsRemainReachable`。仅切换 Roborazzi 1.78.0／1.77.0，两版均为 0 通过／20 失败／14 跳过。20 张实际图经 RGBA 解码后逐像素一致，对正式基线的差异比例也逐项一致。未更新基线。新版任务为 256 执行／146 来自缓存／8 up-to-date，旧版为 25 执行／0 来自缓存／385 up-to-date。日志在 `/private/tmp/notes-20261112-screenshot.log` 和 `/private/tmp/notes-20261112-screenshot-old.log`。
- iOS 本机诊断使用 Xcode 27.2。运行 `:composeApp:compileKotlinIosArm64`、`:composeApp:compileKotlinIosSimulatorArm64`、`:composeApp:iosSimulatorArm64Test`、`:core:database:iosSimulatorArm64Test`，两个目标编译通过，模拟器 54 通过／0 失败／0 跳过，包含真实 Entry 和 Preference 初始化。模拟器测试资源中的两份 iOS 声明与源文件逐字节一致。354 个 Gradle 任务中 279 个执行、75 个 up-to-date。日志在 `/private/tmp/notes-20261112-ios-diagnostic.log`。官方支持的 Xcode 26.4、Xcode 整包、Android/iOS 设备和 Baseline Profile 生成仍待验收。
