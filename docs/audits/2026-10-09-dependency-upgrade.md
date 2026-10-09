# 2026-10-09 全项目依赖升级检查

日期：2026-10-09（Asia/Shanghai）
状态：已升级 Paging 3.5.2 与 NavigationEvent 1.2.0。Kotlin 因 Koin compiler 最高验证版本限制保留 2.4.20；Android 构建通过，宿主测试与 detekt 有既有失败，iOS 官方环境验收未完成。

## 最终采用版本

Xander 授权升级后，最终只修改版本目录中的 Paging 3.5.1 → 3.5.2、NavigationEvent 1.1.2 → 1.2.0。Kotlin 2.4.21 配置与 Android 编译通过，但 Koin compiler 1.2.1 明确最高验证版本为 2.4.20；按项目上限约束保留 Kotlin 2.4.20。本次没有改变任何业务源码、AGP、Wrapper、SDK、JDK、Xcode、预览版或数据库 schema；开始时的源码与检查配置差异完整保留。

## 检查阶段结论

按[升级规范](../dependency-upgrades.md)盘点版本目录的库与插件（包括内联版本和 marker），去重 71 个显式版本坐标，全部成功读取官方 Maven/Plugin Portal 发布元数据并确认当前版本存在。其中 53 个已为最新正式版，13 个存在更新正式版，5 个是需要保留的预览版坐标。13 个更新坐标归为 Kotlin、AGP、NavigationEvent、Paging 四组；另外 Gradle Wrapper 有更新。

建议候选为 Kotlin 2.4.21、Paging 3.5.2、NavigationEvent 1.2.0。AGP 与 Gradle 保留当前版本，Material 3 与 detekt 保留必要预览版。检查只确认发布、支持范围及候选平台声明，不代表依赖解析或行为已经通过。

本次发现 KMP 官方兼容表已明确列出 2.4.20–2.4.21；相比同日较早 Inspect Code 评估只引用通用 KGP 表，本次补齐 KMP 表后将 Kotlin 2.4.21 调整为升级候选。通用 KGP 页仍只列 2.4.20，两页存在更新差异；以明确覆盖项目 KMP 版本的表作为候选依据，不改写先前核对历史，也不据此扩大 Gradle/AGP 上限。

## 候选及保留决定

| 组件 | 当前 | 最新正式版 | 建议与限制 |
|---|---|---|---|
| Kotlin、Compose/Serialization compiler、kotlin-test | 2.4.20 | 2.4.21 | 进入候选，统一跟随 Kotlin 版本；须验证 Koin compiler、KSP/Room、detekt、Native Entry，未实测 |
| Paging common/compose | 3.5.1 | 3.5.2 | 优先作为业务库补丁候选；修复首次刷新完成前 append 抛异常，需分页/Room 与 Native 回归 |
| NavigationEvent Compose | 1.1.2 | 1.2.0 | 候选；发布说明含事件输入与测试能力变化，需根返回栈、预测返回、取消手势与 Native 回归 |
| AGP 的六个插件坐标 | 9.3.1 | 9.4.1 | 保留；KMP 2.4.20–2.4.21 的明确上限仍为 9.3.1，同系列 9.3.3 也越界 |
| Gradle Wrapper | 9.7.0 | 9.8.1 | 保留；KMP 上限仍为 9.7.0 |
| Material 3 三个坐标 | 1.12.0-alpha03 | 1.9.0 | 保留；现有 SelectIconButton 使用 DropdownMenuPopup/Expressive，既有正式版源码核对未提供所需 API；本次复核实际调用及 CMP 1.12.1 发布组件表。不降级删功能、不追 1.13 alpha |
| detekt 插件及 ktlint wrapper | 2.0.0-alpha.6 | 当前 dev.detekt 坐标无正式版 | 保留；现有 2.x 插件/API 与规则配置，旧 1.23.8 属不同坐标/编译链，不能当同坐标升级；本次没有做旧插件迁移可行性实验 |

依据：[KMP 兼容表](https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html)、[通用 KGP 表](https://kotlinlang.org/docs/gradle-configure-project.html)、[Compose 与 Kotlin 兼容说明](https://kotlinlang.org/docs/multiplatform/compose-compatibility-and-versioning.html)、[Compose 1.12.1 发布组件表](https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.12.1)、[Paging 发布说明](https://developer.android.com/jetpack/androidx/releases/paging)、[NavigationEvent 发布说明](https://developer.android.com/jetpack/androidx/releases/navigationevent)、[Gradle 正式版 API](https://services.gradle.org/versions/current)。

Koin compiler 1.2.1 官方要求 Kotlin ≥2.3.20、Koin ≥4.2.0、Gradle ≥8；这些是最低要求，未声明 2.4.21 组合已经在项目通过。[Koin compiler 设置](https://insert-koin.io/docs/setup/compiler-plugin/)。KSP 2.3.12 与 Koin compiler 1.2.1 已为最新正式版，候选 Kotlin 不自动要求再升级它们，但编译插件与生成代码仍需实测。[KSP 2.3.12 发布页](https://github.com/google/ksp/releases/tag/2.3.12)。

## 平台工件与 BOM

本次重新读取候选 Gradle module 元数据：

| 候选 | Android / iosArm64 / iosSimulatorArm64 | 声明的主要依赖 |
|---|---|---|
| [Paging common 3.5.2](https://dl.google.com/dl/android/maven2/androidx/paging/paging-common/3.5.2/paging-common-3.5.2.module) | 均有对应 variant | Kotlin stdlib 2.1.20、coroutines 1.9.0 |
| [Paging compose 3.5.2](https://dl.google.com/dl/android/maven2/androidx/paging/paging-compose/3.5.2/paging-compose-3.5.2.module) | 均有对应 variant | Paging common 3.5.2、Compose runtime 1.9.0 |
| [NavigationEvent compose 1.2.0](https://dl.google.com/dl/android/maven2/androidx/navigationevent/navigationevent-compose/1.2.0/navigationevent-compose-1.2.0.module) | 均有对应 variant | NavigationEvent 1.2.0、Compose runtime 1.11.2、stdlib 2.2.20、coroutines 1.9.0 |

平台声明不是构建结果。NavigationEvent 与 Paging 使用 Google AndroidX 坐标，项目使用 JetBrains CMP/Navigation 3；没有运行 Gradle 解析，不能仅比较 runtime 数字即判定没有变体或传递依赖冲突。发布库的 createdBy Gradle 版本也不能当消费方最低或上限。

14 个无显式版本的目录别名已逐项核对 BOM dependencyManagement：8 个 Ktor 由 [3.6.0 BOM](https://repo.maven.apache.org/maven2/io/ktor/ktor-bom/3.6.0/ktor-bom-3.6.0.pom) 管理，6 个 Koin 由 [4.2.2 BOM](https://repo.maven.apache.org/maven2/io/insert-koin/koin-bom/4.2.2/koin-bom-4.2.2.pom) 管理，全部有精确条目。两个 BOM 已为最新正式版，不给这些库另加版本，不删除用户决定保留的 koin-ktor 别名。

## 工具链与本机环境

- KMP 2.4.20–2.4.21：Gradle 7.6.3–9.7.0、AGP 8.5.2–9.3.1、Xcode 26.4；候选 Kotlin 补丁不扩大这些边界。
- JDK/toolchain/jvmTarget 当前均为 25。detekt 2.0.0-alpha.6 明确最高测试 JDK 为 25，按项目规则保留；其表中 Gradle/Kotlin/AGP 是插件自身编译版本，不能机械当作消费方上限或要求降级。依据 [detekt 字段语义](https://detekt.dev/docs/introduction/compatibility/)。
- Android 当前 compileSdk 37.0、targetSdk 37、Build Tools 37.0.0、minSdk 28。官方 [SDK 仓库元数据](https://dl.google.com/android/repository/repository2-3.xml) 有 37.1、37.2 发布项，但 [API 工具支持表](https://developer.android.com/build/releases/about-agp#api-level-support) 仍只明确列 37.0，[AGP 9.3](https://developer.android.com/build/releases/agp-9-3-0-release-notes) 最大 API 为 37；本次保留 37.0，不把“工件可下载”视为 37.2 明确受支持。Build Tools 37.0.0 已为该系列正式版。
- 本机 `xcodebuild -version` 实际返回 Xcode 27.0（27A266a），仍缺受支持 Xcode 26.4 的验收环境。Xcode 工程 deployment target 包含项目层 18.2 与应用配置 18.5，Swift 模式 6.0；它们是支持范围/语言设置，不因追版本提高或改动。
- 已扫描仓库 Gradle 脚本和 Xcode 配置，版本声明集中在版本目录；没有发现目录之外新增的硬编码第三方依赖版本。项目已采用约定插件 marker；检查未安装 JDK/SDK/Xcode 或全局工具。

## 实施前验证要求

本次未运行 Gradle 构建、依赖解析、单元测试、iOS 模拟器或设备测试。之前 Inspect Code 清理的通过结果不能作为新候选版本的证据；其中应用导航测试 3 个失败、邮件 detekt 75 条问题仍须在升级前建立对照，详见[待验收事项](../testing/pending-acceptance.md#inspect-code-清理验证缺口2026-10-09)。

若后续实施，按规范先验证 Kotlin 2.4.21 配置/编译与编译插件，再分别验证 Paging、NavigationEvent；保留 AGP/Wrapper 和必要预览版。至少覆盖 Android Debug、代表性 Release R8、lint/模块 detekt、共享宿主回归、iOS 编译/行为与真实 Entry 调用；Xcode 整包需在支持的 26.4 环境验收。当前 NavigationEvent 涉及预测返回，必须保留取消和快速反向场景，不能以编译替代行为。

检查原始摘要暂存 `/private/tmp/notes-dependency-check-2026-10-09.json` 与 `notes-dependency-supplement-2026-10-09.json`。首轮沙箱网络 DNS 不可用；获准只读访问官方仓库后 71 坐标全部核对成功。没有修改依赖或其他代码，没有删除文件、合入或发布。

## 实施前显式版本坐标完整核对表

同一坐标的插件与 build-logic marker 别名合并。无正式版表示当前坐标没有纯数字正式版本；预览版本不按元数据 latest 一键采用。以下 71 行均已确认当前工件出现在版本元数据中，链接指向实际读取的仓库记录。

| 目录别名 | 当前 | 最新正式版 | 处理 |
|---|---|---|---|
| [roborazzi](https://repo.maven.apache.org/maven2/io/github/takahirom/roborazzi/io.github.takahirom.roborazzi.gradle.plugin/maven-metadata.xml) | 1.76.0 | 1.76.0 | 保留：已为最新正式版 |
| [android-application](https://dl.google.com/dl/android/maven2/com/android/application/com.android.application.gradle.plugin/maven-metadata.xml) | 9.3.1 | 9.4.1 | 保留：工具链上限 |
| [android-kmp-library, android-gradle-plugin](https://dl.google.com/dl/android/maven2/com/android/kotlin/multiplatform/library/com.android.kotlin.multiplatform.library.gradle.plugin/maven-metadata.xml) | 9.3.1 | 9.4.1 | 保留：工具链上限 |
| [android-lint](https://dl.google.com/dl/android/maven2/com/android/lint/com.android.lint.gradle.plugin/maven-metadata.xml) | 9.3.1 | 9.4.1 | 保留：工具链上限 |
| [android-library](https://dl.google.com/dl/android/maven2/com/android/library/com.android.library.gradle.plugin/maven-metadata.xml) | 9.3.1 | 9.4.1 | 保留：工具链上限 |
| [android-fusedlibrary](https://dl.google.com/dl/android/maven2/com/android/fused-library/com.android.fused-library.gradle.plugin/maven-metadata.xml) | 9.3.1 | 9.4.1 | 保留：工具链上限 |
| [android-test](https://dl.google.com/dl/android/maven2/com/android/test/com.android.test.gradle.plugin/maven-metadata.xml) | 9.3.1 | 9.4.1 | 保留：工具链上限 |
| [androidx-room](https://dl.google.com/dl/android/maven2/androidx/room3/androidx.room3.gradle.plugin/maven-metadata.xml) | 3.0.3 | 3.0.3 | 保留：已为最新正式版 |
| [androidx-baselineprofile](https://dl.google.com/dl/android/maven2/androidx/baselineprofile/androidx.baselineprofile.gradle.plugin/maven-metadata.xml) | 1.5.0 | 1.5.0 | 保留：已为最新正式版 |
| [jetbrains-compose, compose-gradle-plugin](https://repo.maven.apache.org/maven2/org/jetbrains/compose/org.jetbrains.compose.gradle.plugin/maven-metadata.xml) | 1.12.1 | 1.12.1 | 保留：已为最新正式版 |
| [jetbrains-kotlin-compose-compiler, kotlin-compose-plugin](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/plugin/compose/org.jetbrains.kotlin.plugin.compose.gradle.plugin/maven-metadata.xml) | 2.4.20 | 2.4.21 | Kotlin 2.4.21 候选 |
| [jetbrains-kotlin-serialization, kotlin-serialization-plugin](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/plugin/serialization/org.jetbrains.kotlin.plugin.serialization.gradle.plugin/maven-metadata.xml) | 2.4.20 | 2.4.21 | Kotlin 2.4.21 候选 |
| [jetbrains-kotlin-multiplatform, kotlin-gradle-plugin](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/multiplatform/org.jetbrains.kotlin.multiplatform.gradle.plugin/maven-metadata.xml) | 2.4.20 | 2.4.21 | Kotlin 2.4.21 候选 |
| [google-ksp, ksp-gradle-plugin](https://repo.maven.apache.org/maven2/com/google/devtools/ksp/com.google.devtools.ksp.gradle.plugin/maven-metadata.xml) | 2.3.12 | 2.3.12 | 保留：已为最新正式版 |
| [koin-compiler, koin-compiler-gradle-plugin](https://repo.maven.apache.org/maven2/io/insert-koin/compiler/plugin/io.insert-koin.compiler.plugin.gradle.plugin/maven-metadata.xml) | 1.2.1 | 1.2.1 | 保留：已为最新正式版 |
| [buildKonfig](https://repo.maven.apache.org/maven2/com/codingfeline/buildkonfig/com.codingfeline.buildkonfig.gradle.plugin/maven-metadata.xml) | 0.23.0 | 0.23.0 | 保留：已为最新正式版 |
| [detekt, detekt-gradle-plugin](https://repo.maven.apache.org/maven2/dev/detekt/dev.detekt.gradle.plugin/maven-metadata.xml) | 2.0.0-alpha.6 | 无 | 保留必要预览版 |
| [robolectric](https://repo.maven.apache.org/maven2/org/robolectric/robolectric/maven-metadata.xml) | 4.17 | 4.17 | 保留：已为最新正式版 |
| [roborazzi](https://repo.maven.apache.org/maven2/io/github/takahirom/roborazzi/roborazzi/maven-metadata.xml) | 1.76.0 | 1.76.0 | 保留：已为最新正式版 |
| [jetbrains-compose-ui-test-junit4](https://repo.maven.apache.org/maven2/org/jetbrains/compose/ui/ui-test-junit4/maven-metadata.xml) | 1.12.1 | 1.12.1 | 保留：已为最新正式版 |
| [kotlin-test](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-test/maven-metadata.xml) | 2.4.20 | 2.4.21 | Kotlin 2.4.21 候选 |
| [androidx-test-uiautomator](https://dl.google.com/dl/android/maven2/androidx/test/uiautomator/uiautomator/maven-metadata.xml) | 2.4.0 | 2.4.0 | 保留：已为最新正式版 |
| [androidx-benchmark-macro-junit4](https://dl.google.com/dl/android/maven2/androidx/benchmark/benchmark-macro-junit4/maven-metadata.xml) | 1.5.0 | 1.5.0 | 保留：已为最新正式版 |
| [junit](https://repo.maven.apache.org/maven2/junit/junit/maven-metadata.xml) | 4.13.2 | 4.13.2 | 保留：已为最新正式版 |
| [androidx-test-ext-junit](https://dl.google.com/dl/android/maven2/androidx/test/ext/junit/maven-metadata.xml) | 1.3.0 | 1.3.0 | 保留：已为最新正式版 |
| [androidx-test-espresso](https://dl.google.com/dl/android/maven2/androidx/test/espresso/espresso-core/maven-metadata.xml) | 3.7.0 | 3.7.0 | 保留：已为最新正式版 |
| [squareup-leakCanary](https://repo.maven.apache.org/maven2/com/squareup/leakcanary/leakcanary-android/maven-metadata.xml) | 2.14 | 2.14 | 保留：已为最新正式版 |
| [androidx-activity-compose](https://dl.google.com/dl/android/maven2/androidx/activity/activity-compose/maven-metadata.xml) | 1.13.0 | 1.13.0 | 保留：已为最新正式版 |
| [androidx-lifecycle-compose](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-runtime-compose/maven-metadata.xml) | 2.11.0 | 2.11.0 | 保留：已为最新正式版 |
| [androidx-viewmodel-compose](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-viewmodel-compose/maven-metadata.xml) | 2.11.0 | 2.11.0 | 保留：已为最新正式版 |
| [androidx-profileinstaller](https://dl.google.com/dl/android/maven2/androidx/profileinstaller/profileinstaller/maven-metadata.xml) | 1.4.1 | 1.4.1 | 保留：已为最新正式版 |
| [androidx-appcompat](https://dl.google.com/dl/android/maven2/androidx/appcompat/appcompat/maven-metadata.xml) | 1.8.0 | 1.8.0 | 保留：已为最新正式版 |
| [androidx-core](https://dl.google.com/dl/android/maven2/androidx/core/core/maven-metadata.xml) | 1.19.1 | 1.19.1 | 保留：已为最新正式版 |
| [androidx-navigationevent-compose](https://dl.google.com/dl/android/maven2/androidx/navigationevent/navigationevent-compose/maven-metadata.xml) | 1.1.2 | 1.2.0 | NavigationEvent 1.2.0 候选 |
| [androidx-lifecycle-viewmodel-compose](https://repo.maven.apache.org/maven2/org/jetbrains/androidx/lifecycle/lifecycle-viewmodel-compose/maven-metadata.xml) | 2.11.0 | 2.11.0 | 保留：已为最新正式版 |
| [androidx-lifecycle-runtime-compose](https://repo.maven.apache.org/maven2/org/jetbrains/androidx/lifecycle/lifecycle-runtime-compose/maven-metadata.xml) | 2.11.0 | 2.11.0 | 保留：已为最新正式版 |
| [androidx-lifecycle-viewmodel-navigation3](https://repo.maven.apache.org/maven2/org/jetbrains/androidx/lifecycle/lifecycle-viewmodel-navigation3/maven-metadata.xml) | 2.11.0 | 2.11.0 | 保留：已为最新正式版 |
| [androidx-window-core](https://repo.maven.apache.org/maven2/org/jetbrains/androidx/window/window-core/maven-metadata.xml) | 1.5.1 | 1.5.1 | 保留：已为最新正式版 |
| [jetbrains-navigation3-ui](https://repo.maven.apache.org/maven2/org/jetbrains/androidx/navigation3/navigation3-ui/maven-metadata.xml) | 1.1.2 | 1.1.2 | 保留：已为最新正式版 |
| [androidx-room-runtime](https://dl.google.com/dl/android/maven2/androidx/room3/room3-runtime/maven-metadata.xml) | 3.0.3 | 3.0.3 | 保留：已为最新正式版 |
| [androidx-room-compiler](https://dl.google.com/dl/android/maven2/androidx/room3/room3-compiler/maven-metadata.xml) | 3.0.3 | 3.0.3 | 保留：已为最新正式版 |
| [androidx-room-paging](https://dl.google.com/dl/android/maven2/androidx/room3/room3-paging/maven-metadata.xml) | 3.0.3 | 3.0.3 | 保留：已为最新正式版 |
| [androidx-sqlite-bundled](https://dl.google.com/dl/android/maven2/androidx/sqlite/sqlite-bundled/maven-metadata.xml) | 2.7.1 | 2.7.1 | 保留：已为最新正式版 |
| [androidx-datastore-preferences](https://dl.google.com/dl/android/maven2/androidx/datastore/datastore-preferences/maven-metadata.xml) | 1.2.1 | 1.2.1 | 保留：已为最新正式版 |
| [androidx-paging-compose](https://dl.google.com/dl/android/maven2/androidx/paging/paging-compose/maven-metadata.xml) | 3.5.1 | 3.5.2 | Paging 3.5.2 候选 |
| [androidx-paging-common](https://dl.google.com/dl/android/maven2/androidx/paging/paging-common/maven-metadata.xml) | 3.5.1 | 3.5.2 | Paging 3.5.2 候选 |
| [jetbrains-compose-material3](https://repo.maven.apache.org/maven2/org/jetbrains/compose/material3/material3/maven-metadata.xml) | 1.12.0-alpha03 | 1.9.0 | 保留必要预览版 |
| [jetbrains-compose-material3-window-size](https://repo.maven.apache.org/maven2/org/jetbrains/compose/material3/material3-window-size-class/maven-metadata.xml) | 1.12.0-alpha03 | 1.9.0 | 保留必要预览版 |
| [jetbrains-compose-material3-adaptive-navigation-suite](https://repo.maven.apache.org/maven2/org/jetbrains/compose/material3/material3-adaptive-navigation-suite/maven-metadata.xml) | 1.12.0-alpha03 | 1.9.0 | 保留必要预览版 |
| [jetbrains-compose-material3-adaptive](https://repo.maven.apache.org/maven2/org/jetbrains/compose/material3/adaptive/adaptive/maven-metadata.xml) | 1.3.0 | 1.3.0 | 保留：已为最新正式版 |
| [jetbrains-compose-material3-adaptive-layout](https://repo.maven.apache.org/maven2/org/jetbrains/compose/material3/adaptive/adaptive-layout/maven-metadata.xml) | 1.3.0 | 1.3.0 | 保留：已为最新正式版 |
| [jetbrains-compose-material3-adaptive-navigation](https://repo.maven.apache.org/maven2/org/jetbrains/compose/material3/adaptive/adaptive-navigation/maven-metadata.xml) | 1.3.0 | 1.3.0 | 保留：已为最新正式版 |
| [jetbrains-compose-material3-adaptive-navigation3](https://repo.maven.apache.org/maven2/org/jetbrains/compose/material3/adaptive/adaptive-navigation3/maven-metadata.xml) | 1.3.0 | 1.3.0 | 保留：已为最新正式版 |
| [jetbrains-compose-runtime](https://repo.maven.apache.org/maven2/org/jetbrains/compose/runtime/runtime/maven-metadata.xml) | 1.12.1 | 1.12.1 | 保留：已为最新正式版 |
| [jetbrains-compose-foundation](https://repo.maven.apache.org/maven2/org/jetbrains/compose/foundation/foundation/maven-metadata.xml) | 1.12.1 | 1.12.1 | 保留：已为最新正式版 |
| [jetbrains-compose-components-resources](https://repo.maven.apache.org/maven2/org/jetbrains/compose/components/components-resources/maven-metadata.xml) | 1.12.1 | 1.12.1 | 保留：已为最新正式版 |
| [jetbrains-compose-ui](https://repo.maven.apache.org/maven2/org/jetbrains/compose/ui/ui/maven-metadata.xml) | 1.12.1 | 1.12.1 | 保留：已为最新正式版 |
| [jetbrains-compose-ui-tooling-preview](https://repo.maven.apache.org/maven2/org/jetbrains/compose/ui/ui-tooling-preview/maven-metadata.xml) | 1.12.1 | 1.12.1 | 保留：已为最新正式版 |
| [jetbrains-kotlinx-coroutines-core](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-core/maven-metadata.xml) | 1.11.0 | 1.11.0 | 保留：已为最新正式版 |
| [jetbrains-kotlinx-coroutines-test](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-test/maven-metadata.xml) | 1.11.0 | 1.11.0 | 保留：已为最新正式版 |
| [jetbrains-kotlinx-coroutines-android](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-android/maven-metadata.xml) | 1.11.0 | 1.11.0 | 保留：已为最新正式版 |
| [jetbrains-kotlinx-collections-immutable](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-collections-immutable/maven-metadata.xml) | 0.5.2 | 0.5.2 | 保留：已为最新正式版 |
| [jetbrains-kotlinx-serialization-core](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-serialization-core/maven-metadata.xml) | 1.11.0 | 1.11.0 | 保留：已为最新正式版 |
| [jetbrains-kotlinx-serialization-json](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-serialization-json/maven-metadata.xml) | 1.11.0 | 1.11.0 | 保留：已为最新正式版 |
| [coil-compose](https://repo.maven.apache.org/maven2/io/coil-kt/coil3/coil-compose/maven-metadata.xml) | 3.6.3 | 3.6.3 | 保留：已为最新正式版 |
| [coil-network-ktor](https://repo.maven.apache.org/maven2/io/coil-kt/coil3/coil-network-ktor3/maven-metadata.xml) | 3.6.3 | 3.6.3 | 保留：已为最新正式版 |
| [ktor-bom](https://repo.maven.apache.org/maven2/io/ktor/ktor-bom/maven-metadata.xml) | 3.6.0 | 3.6.0 | 保留：已为最新正式版 |
| [koin-bom](https://repo.maven.apache.org/maven2/io/insert-koin/koin-bom/maven-metadata.xml) | 4.2.2 | 4.2.2 | 保留：已为最新正式版 |
| [compose-preference](https://repo.maven.apache.org/maven2/me/zhanghai/compose/preference/preference/maven-metadata.xml) | 2.2.0 | 2.2.0 | 保留：已为最新正式版 |
| [calf-file-picker](https://repo.maven.apache.org/maven2/com/mohamedrejeb/calf/calf-file-picker/maven-metadata.xml) | 0.14.0 | 0.14.0 | 保留：已为最新正式版 |
| [detekt-formatting](https://repo.maven.apache.org/maven2/dev/detekt/detekt-rules-ktlint-wrapper/maven-metadata.xml) | 2.0.0-alpha.6 | 无 | 保留必要预览版 |

## 授权后的实施

范围为上述三个候选，按 Kotlin 配置/编译 → 业务库升级 → Android 综合回归顺序执行。保留开始时全部源码与检查配置差异，版本目录原先无差异。AGP、Wrapper、SDK、JDK、Xcode 与必要预览版不变；不涉及 schema、CI/CD、安装或发布。iOS 官方环境缺口与测试失败按实际结果交付。

### Kotlin 候选的编译插件上限

Kotlin 2.4.21 的 `help`、composeApp/database Android 编译实际通过（1m17s，229 个任务：216 执行、7 缓存、6 up-to-date）。但官方发布的 Koin compiler 1.2.1 在编译时明确报告 `newest verified version (2.4.20)`，2.4.21 使用 2.4.20 adapter；因此按最高测试版本上限保留 Kotlin 2.4.20，不以编译成功扩大允许范围。前面的检查候选仅依据最低要求，未发现这一工件内限制；本条取代 Kotlin 候选建议。日志 `/private/tmp/notes-upgrade-kotlin-2026-10-09.log`。

### 最终组合验证

使用已有 Android Studio JBR 25.0.3；所有 Gradle 调用串行。最终批次整体 `BUILD FAILED`（exit 1），1m44s，1060 个任务中 268 实际执行、106 缓存、686 up-to-date。失败任务仅 composeApp 宿主测试和三个模块 detekt，不能宣称全量通过。

```bash
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
  ./gradlew :androidApp:assembleDebug :androidApp:assembleDevFullRelease \
  :composeApp:testAndroidHostTest :core:data:testAndroidHostTest \
  :core:database:testAndroidHostTest :core:theme:testAndroidHostTest \
  :feature:main:testAndroidHostTest :feature:login:testAndroidHostTest \
  :feature:email:testAndroidHostTest :feature:profile:testAndroidHostTest \
  :androidApp:lintDevFullDebug :feature:email:detekt \
  :feature:profile:detekt :feature:about:detekt \
  :android:output:login:assemble :android:baselineprofile:assemble \
  :androidApp:dependencies --configuration devFullDebugRuntimeClasspath \
  -Dkotlin.daemon.jvmargs=-Xmx8g \
  '-Dorg.gradle.jvmargs=-Xmx8g -Dfile.encoding=UTF-8 -XX:+UseParallelGC' \
  --max-workers=4 --continue --console=plain
```

| 验证 | 结果与执行范围 |
|---|---|
| Android Debug 四个 flavor | 通过，assemble 任务实际执行 |
| DevFull Release / R8 | 通过，minify 与 assemble 实际执行 |
| Android lintDevFullDebug | 通过，任务实际执行 |
| composeApp 宿主测试 | 165 通过、1 失败、617 跳过，实际执行 |
| data/theme/main/login/email/profile 宿主测试 | 分别 8/5/13/13/7/14 通过，共 60，无失败或跳过，任务实际执行 |
| database 宿主测试 | UP-TO-DATE；复用已有 3 通过报告，不计为本次重新执行 |
| email/profile/about detekt | 分别失败 75/16/35 条，与升级前保留 XML 按文件、行、列、规则和描述逐条一致 |
| Fused Library assemble | 通过，assemble 任务实际执行 |
| Baseline Profile 模块 assemble | UP-TO-DATE；本次复用产物，未生成设备 Profile |
| Android runtime 依赖图 | 实际执行，Paging common/compose 解析为 3.5.2，NavigationEvent/compose 解析为 1.2.0，未发现 FAILED 解析项 |
| iOS 编译、行为、Entry、Xcode 整包 | 未执行；仅有 Xcode 27，缺官方表列出的 26.4，不能替代验收 |

本次实际执行宿主用例共 225 通过、1 失败、617 跳过；另复用数据库 3 通过。升级前 XML 报告保存在 `/private/tmp/notes-upgrade-baseline-2026-10-09/`，应用有 3 个失败；升级后仅 `DeviceCornerOpacityTest.ordinaryPushAndPopSwitchWithoutMotionOrBlending` 的 `Page stays fully opaque (push)` 失败，类、方法及断言与前报告相同。两个预测返回灰色背景断言本轮通过，但没有重复对照实验，不能归因于库升级或宣称已修复底层问题。此次对照基于升级前已有报告，没有重新运行一轮旧版本，单轮通过不说明稳定性。

未改断言、baseline、业务布局或检查规则来绕过失败。日志 `/private/tmp/notes-upgrade-final-validation-2026-10-09.log`。版本目录只新增两行版本差异；README 同步当前表。文档链接、代码围栏、`git diff --check` 与实施前源码差异保留检查通过。集中未验收项见[待验收清单](../testing/pending-acceptance.md#依赖升级验收2026-10-09)。按 Xander 授权整理为一个提交；未合入或发布。
