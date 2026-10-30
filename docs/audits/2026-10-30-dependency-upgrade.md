# 2026-10-30 依赖升级方案与发布核对

日期：2026-10-30（Asia/Shanghai）
状态：最终采用 7 组升级，含 Navigation3 1.2.0；按 维护者 授权 amend 为同一需求提交。原快速反向问题未在 Android 原生对照复现；前进中途返回场景两版均有 5 项失败，静态检查、应用宿主既有失败和 iOS 官方环境缺口继续保留。下文保留分阶段证据，最终采用决定见末节。

## 范围与候选

遵循 [升级规范](../dependency-upgrades.md)，正式版优先，保持现有功能、布局、最低平台版本和模块边界。开始时 `git status --short` 为空。所有版本在 `gradle/kmp.versions.toml` 修改；需要的 API 适配仅限兼容修复，不顺带重构。

| 版本键或别名 | 当前 | 计划候选 | 验证重点 |
|---|---|---|---|
| jetbrains-compose | 1.12.1 | 1.12.2 | Compose 插件及共用版本库；自适应行为、Native Entry、焦点与弹窗 |
| jetbrains-navigation3-ui | 1.1.2 | 1.2.0 | SceneStrategy、返回栈恢复、预测返回与取消 |
| androidx-datastore | 1.2.1 | 1.3.0 | 设置持久化、并发更新；不新增迁移或改变存储格式 |
| androidx-paging | 3.5.2 | 3.5.3 | Room 分页、邮件列表与边界行为 |
| jetbrains-serialization | 1.11.0 | 1.12.0 | 导航状态及 JSON 往返；编译插件仍跟随 Kotlin |
| ktor-bom | 3.6.0 | 3.6.1 | BOM 管理与 Android/iOS 客户端解析 |
| roborazzi | 1.76.0 | 1.77.0 | 插件和库一致；保留正式截图基线 |

官方依据：[Compose 发布说明](https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.12.2)、[DataStore](https://developer.android.com/jetpack/androidx/releases/datastore)、[Paging](https://developer.android.com/jetpack/androidx/releases/paging)、[Serialization](https://github.com/Kotlin/kotlinx.serialization/releases/tag/v1.12.0)、[Ktor](https://github.com/ktorio/ktor/releases/tag/3.6.1)、[Roborazzi](https://github.com/takahirom/roborazzi/releases/tag/1.77.0)。Navigation 3 的正式工件已由下表 Maven 元数据及 1.2.0 module 确认；已补核 [Google 发布说明](https://developer.android.com/jetpack/androidx/releases/navigation3)，包含预测返回中途接管、contentKey 默认规则和 EntryProvider 匹配次序变化。

## 保留与环境限制

- Kotlin 保留 2.4.20：[Koin compiler 1.2.1 发布说明](https://github.com/InsertKoinIO/koin-compiler-plugin/releases/tag/1.2.1) 明确验证 2.4.20，未列 2.4.21。不因最低要求满足而扩大最高验证版本。
- AGP 保留 9.3.1、Wrapper 保留 9.7.0：[KMP 兼容表](https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html) 对 Kotlin 2.4.20–2.4.21 仍给出这两个上限。
- Material 3 保留 1.12.0-alpha03、Adaptive 保留 1.3.0，均与 Compose 1.12.2 发布组件表一致；旧正式版不用于移除现有 Expressive API。detekt 保留 2.0.0-alpha.6，当前坐标无正式版，Compose Rules 0.6.8 不变。
- 已实际检查 Android Studio 内 `java` / `javac`，均为 25.0.4。compileSdk/targetSdk 保留 37、Build Tools 保留 37.0.0，minSdk 保留 28；本轮不扩大工具链或平台支持范围。
- `xcodebuild -version` 为 Xcode 27.1（27A9275）；官方 KMP 表仍列 26.4，当前环境不能作为受支持组合的 iOS 验收。不得安装全局工具或修改系统选择来绕过此限制；受支持环境不可用时明确记录未验收。

## 工件与兼容证据

实际读取 12 份候选 module 元数据：Compose UI/runtime/foundation/resources、Navigation3 UI、Serialization core/json、Ktor core/darwin、DataStore preferences、Paging common/compose。均成功读取，KMP 通用工件声明了 Android（Serialization 使用 JVM）及 iosArm64/iosSimulatorArm64 变体；Darwin 是 Apple 专用客户端，不要求 Android 变体。这只证明发布声明，不代表 Gradle 解析或平台编译通过。

Serialization 1.12.0 声明 stdlib 2.4.10；Ktor 3.6.1 声明 stdlib 2.3.21、coroutines 1.11.0；Navigation3 1.2.0 声明 Compose runtime 1.10.0。均未显示高于当前直接配置的版本要求，实际传递依赖与变体仍须 Gradle 验证。原始结果暂存 `/private/tmp/notes-candidates-2026-10-30.json`。

## 实施计划与成功标准

1. **建立对照**：使用现有 JBR 25，串行运行受影响宿主测试与配置检查，记录既有失败；不把历史报告视为本轮通过。
2. **UI 组**：修改 Compose、Navigation3、Roborazzi 三个版本。运行 `:androidApp:assembleDebug`、`:composeApp:testAndroidHostTest`、`:core:navigation:testAndroidHostTest`，重点核对导航和自适应用例；不更新截图基线。官方环境具备后执行 `:composeApp:compileKotlinIosSimulatorArm64` 和 `:composeApp:iosSimulatorArm64Test`，包含真实 Entry 调用。
3. **数据网络组**：修改 DataStore、Paging、Serialization、Ktor 四个版本。核对 Ktor BOM 所有别名及 Android runtime 依赖图；运行 data/database 与全部现有共享宿主测试，覆盖序列化、分页和设置。若现有用例不足，仅补与本次升级风险直接相关的回归。
4. **综合验证**：按 [测试指南](../testing/README.md) 覆盖 Android Debug、DevFull Release/R8、lint、有效模块 detekt/语义分析、所有共享及 Android 宿主测试、Fused Library 和 Baseline Profile 模块构建。受支持 Xcode 环境下执行 Native 编译/行为、真实 Entry、Preference 初始化与 Xcode 模拟器整包；环境缺口逐项列为未验收。
5. **交付整理**：同步根 README 版本表、本记录和 `testing/pending-acceptance.md`；用户可感知修复按 [版本规则](../app-releases.md) 维护未发布记录。检查引用、分类、重复与过时内容，运行 `git diff --check`。分别报告通过、失败、跳过、缓存复用及设备/截图未验收，不以编译代替行为验证。

成功标准：候选仅在官方支持范围内采用，必要回归无新增失败，最终版本和未验收项可追溯。不关闭检查、不修改断言或基线掩盖失败。若确认候选不兼容，先记录证据和可审查处理方案；删除、git 回滚、schema 迁移、CI/CD、全局安装及发布仍须单独授权。

## 显式坐标发布元数据

下表为本轮实际读取，合并已有插件 marker 重复别名；补核 Browser 1.10.0、Foojay（Plugin Portal）1.0.0 均与当前一致；Wrapper 最新正式版为 9.8.2，受 KMP 上限限制保留 9.7.0。元数据最新预览版不自动采用。原始结果暂存 `/private/tmp/notes-dependency-check-2026-10-30.json`。

| 官方坐标元数据 | 最新正式版 | 元数据 latest |
|---|---|---|
| [io/github/takahirom/roborazzi/io.github.takahirom.roborazzi.gradle.plugin](https://repo.maven.apache.org/maven2/io/github/takahirom/roborazzi/io.github.takahirom.roborazzi.gradle.plugin/maven-metadata.xml) | 1.77.0 | 1.77.0 |
| [com/android/application/com.android.application.gradle.plugin](https://dl.google.com/dl/android/maven2/com/android/application/com.android.application.gradle.plugin/maven-metadata.xml) | 9.5.0 | 9.6.0-alpha02 |
| [com/android/kotlin/multiplatform/library/com.android.kotlin.multiplatform.library.gradle.plugin](https://dl.google.com/dl/android/maven2/com/android/kotlin/multiplatform/library/com.android.kotlin.multiplatform.library.gradle.plugin/maven-metadata.xml) | 9.5.0 | 9.6.0-alpha02 |
| [com/android/lint/com.android.lint.gradle.plugin](https://dl.google.com/dl/android/maven2/com/android/lint/com.android.lint.gradle.plugin/maven-metadata.xml) | 9.5.0 | 9.6.0-alpha02 |
| [com/android/library/com.android.library.gradle.plugin](https://dl.google.com/dl/android/maven2/com/android/library/com.android.library.gradle.plugin/maven-metadata.xml) | 9.5.0 | 9.6.0-alpha02 |
| [com/android/fused-library/com.android.fused-library.gradle.plugin](https://dl.google.com/dl/android/maven2/com/android/fused-library/com.android.fused-library.gradle.plugin/maven-metadata.xml) | 9.5.0 | 9.6.0-alpha02 |
| [com/android/test/com.android.test.gradle.plugin](https://dl.google.com/dl/android/maven2/com/android/test/com.android.test.gradle.plugin/maven-metadata.xml) | 9.5.0 | 9.6.0-alpha02 |
| [androidx/room3/androidx.room3.gradle.plugin](https://dl.google.com/dl/android/maven2/androidx/room3/androidx.room3.gradle.plugin/maven-metadata.xml) | 3.0.3 | 3.1.0-alpha03 |
| [androidx/baselineprofile/androidx.baselineprofile.gradle.plugin](https://dl.google.com/dl/android/maven2/androidx/baselineprofile/androidx.baselineprofile.gradle.plugin/maven-metadata.xml) | 1.5.0 | 1.6.0-alpha01 |
| [org/jetbrains/compose/org.jetbrains.compose.gradle.plugin](https://repo.maven.apache.org/maven2/org/jetbrains/compose/org.jetbrains.compose.gradle.plugin/maven-metadata.xml) | 1.12.2 | 1.13.0-beta01 |
| [org/jetbrains/kotlin/plugin/compose/org.jetbrains.kotlin.plugin.compose.gradle.plugin](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/plugin/compose/org.jetbrains.kotlin.plugin.compose.gradle.plugin/maven-metadata.xml) | 2.4.21 | 2.5.0-Beta2 |
| [org/jetbrains/kotlin/plugin/serialization/org.jetbrains.kotlin.plugin.serialization.gradle.plugin](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/plugin/serialization/org.jetbrains.kotlin.plugin.serialization.gradle.plugin/maven-metadata.xml) | 2.4.21 | 2.5.0-Beta2 |
| [org/jetbrains/kotlin/multiplatform/org.jetbrains.kotlin.multiplatform.gradle.plugin](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/multiplatform/org.jetbrains.kotlin.multiplatform.gradle.plugin/maven-metadata.xml) | 2.4.21 | 2.5.0-Beta2 |
| [com/google/devtools/ksp/com.google.devtools.ksp.gradle.plugin](https://repo.maven.apache.org/maven2/com/google/devtools/ksp/com.google.devtools.ksp.gradle.plugin/maven-metadata.xml) | 2.3.12 | 2.3.12 |
| [io/insert-koin/compiler/plugin/io.insert-koin.compiler.plugin.gradle.plugin](https://repo.maven.apache.org/maven2/io/insert-koin/compiler/plugin/io.insert-koin.compiler.plugin.gradle.plugin/maven-metadata.xml) | 1.2.1 | 1.2.1 |
| [com/codingfeline/buildkonfig/com.codingfeline.buildkonfig.gradle.plugin](https://repo.maven.apache.org/maven2/com/codingfeline/buildkonfig/com.codingfeline.buildkonfig.gradle.plugin/maven-metadata.xml) | 0.23.0 | 0.23.0 |
| [dev/detekt/dev.detekt.gradle.plugin](https://repo.maven.apache.org/maven2/dev/detekt/dev.detekt.gradle.plugin/maven-metadata.xml) | 无/未取得 | 2.0.0-alpha.6 |
| [org/robolectric/robolectric](https://repo.maven.apache.org/maven2/org/robolectric/robolectric/maven-metadata.xml) | 4.17 | 4.17 |
| [io/github/takahirom/roborazzi/roborazzi](https://repo.maven.apache.org/maven2/io/github/takahirom/roborazzi/roborazzi/maven-metadata.xml) | 1.77.0 | 1.77.0 |
| [org/jetbrains/compose/ui/ui-test-junit4](https://repo.maven.apache.org/maven2/org/jetbrains/compose/ui/ui-test-junit4/maven-metadata.xml) | 1.12.2 | 1.13.0-beta01 |
| [org/jetbrains/kotlin/kotlin-test](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-test/maven-metadata.xml) | 2.4.21 | 2.5.0-Beta2 |
| [androidx/test/uiautomator/uiautomator](https://dl.google.com/dl/android/maven2/androidx/test/uiautomator/uiautomator/maven-metadata.xml) | 2.4.0 | 2.4.0 |
| [androidx/benchmark/benchmark-macro-junit4](https://dl.google.com/dl/android/maven2/androidx/benchmark/benchmark-macro-junit4/maven-metadata.xml) | 1.5.0 | 1.6.0-alpha01 |
| [junit/junit](https://repo.maven.apache.org/maven2/junit/junit/maven-metadata.xml) | 4.13.2 | 4.13.2 |
| [androidx/test/ext/junit](https://dl.google.com/dl/android/maven2/androidx/test/ext/junit/maven-metadata.xml) | 1.3.0 | 1.4.0-alpha03 |
| [androidx/test/espresso/espresso-core](https://dl.google.com/dl/android/maven2/androidx/test/espresso/espresso-core/maven-metadata.xml) | 3.7.0 | 3.8.0-alpha03 |
| [com/squareup/leakcanary/leakcanary-android](https://repo.maven.apache.org/maven2/com/squareup/leakcanary/leakcanary-android/maven-metadata.xml) | 2.14 | 3.0-alpha-9 |
| [androidx/activity/activity-compose](https://dl.google.com/dl/android/maven2/androidx/activity/activity-compose/maven-metadata.xml) | 1.13.0 | 1.14.0-alpha03 |
| [androidx/lifecycle/lifecycle-runtime-compose](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-runtime-compose/maven-metadata.xml) | 2.11.0 | 2.12.0-alpha05 |
| [androidx/lifecycle/lifecycle-viewmodel-compose](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-viewmodel-compose/maven-metadata.xml) | 2.11.0 | 2.12.0-alpha05 |
| [androidx/profileinstaller/profileinstaller](https://dl.google.com/dl/android/maven2/androidx/profileinstaller/profileinstaller/maven-metadata.xml) | 1.4.1 | 1.4.1 |
| [androidx/appcompat/appcompat](https://dl.google.com/dl/android/maven2/androidx/appcompat/appcompat/maven-metadata.xml) | 1.8.0 | 1.8.0 |
| [androidx/core/core](https://dl.google.com/dl/android/maven2/androidx/core/core/maven-metadata.xml) | 1.19.1 | 1.19.1 |
| [androidx/navigationevent/navigationevent-compose](https://dl.google.com/dl/android/maven2/androidx/navigationevent/navigationevent-compose/maven-metadata.xml) | 1.2.0 | 1.2.0 |
| [org/jetbrains/androidx/lifecycle/lifecycle-viewmodel-compose](https://repo.maven.apache.org/maven2/org/jetbrains/androidx/lifecycle/lifecycle-viewmodel-compose/maven-metadata.xml) | 2.11.0 | 2.11.0 |
| [org/jetbrains/androidx/lifecycle/lifecycle-runtime-compose](https://repo.maven.apache.org/maven2/org/jetbrains/androidx/lifecycle/lifecycle-runtime-compose/maven-metadata.xml) | 2.11.0 | 2.11.0 |
| [org/jetbrains/androidx/lifecycle/lifecycle-viewmodel-navigation3](https://repo.maven.apache.org/maven2/org/jetbrains/androidx/lifecycle/lifecycle-viewmodel-navigation3/maven-metadata.xml) | 2.11.0 | 2.11.0 |
| [org/jetbrains/androidx/window/window-core](https://repo.maven.apache.org/maven2/org/jetbrains/androidx/window/window-core/maven-metadata.xml) | 1.5.1 | 1.5.1 |
| [org/jetbrains/androidx/navigation3/navigation3-ui](https://repo.maven.apache.org/maven2/org/jetbrains/androidx/navigation3/navigation3-ui/maven-metadata.xml) | 1.2.0 | 1.2.0 |
| [androidx/room3/room3-runtime](https://dl.google.com/dl/android/maven2/androidx/room3/room3-runtime/maven-metadata.xml) | 3.0.3 | 3.1.0-alpha03 |
| [androidx/room3/room3-compiler](https://dl.google.com/dl/android/maven2/androidx/room3/room3-compiler/maven-metadata.xml) | 3.0.3 | 3.1.0-alpha03 |
| [androidx/room3/room3-paging](https://dl.google.com/dl/android/maven2/androidx/room3/room3-paging/maven-metadata.xml) | 3.0.3 | 3.1.0-alpha03 |
| [androidx/sqlite/sqlite-bundled](https://dl.google.com/dl/android/maven2/androidx/sqlite/sqlite-bundled/maven-metadata.xml) | 2.7.1 | 2.8.0-alpha03 |
| [androidx/datastore/datastore-preferences](https://dl.google.com/dl/android/maven2/androidx/datastore/datastore-preferences/maven-metadata.xml) | 1.3.0 | 1.3.0 |
| [androidx/paging/paging-compose](https://dl.google.com/dl/android/maven2/androidx/paging/paging-compose/maven-metadata.xml) | 3.5.3 | 3.5.3 |
| [androidx/paging/paging-common](https://dl.google.com/dl/android/maven2/androidx/paging/paging-common/maven-metadata.xml) | 3.5.3 | 3.5.3 |
| [org/jetbrains/compose/material3/material3](https://repo.maven.apache.org/maven2/org/jetbrains/compose/material3/material3/maven-metadata.xml) | 1.9.0 | 1.13.0-beta01 |
| [org/jetbrains/compose/material3/material3-window-size-class](https://repo.maven.apache.org/maven2/org/jetbrains/compose/material3/material3-window-size-class/maven-metadata.xml) | 1.9.0 | 1.13.0-beta01 |
| [org/jetbrains/compose/material3/material3-adaptive-navigation-suite](https://repo.maven.apache.org/maven2/org/jetbrains/compose/material3/material3-adaptive-navigation-suite/maven-metadata.xml) | 1.9.0 | 1.13.0-beta01 |
| [org/jetbrains/compose/material3/adaptive/adaptive](https://repo.maven.apache.org/maven2/org/jetbrains/compose/material3/adaptive/adaptive/maven-metadata.xml) | 1.3.0 | 1.4.0-alpha01 |
| [org/jetbrains/compose/material3/adaptive/adaptive-layout](https://repo.maven.apache.org/maven2/org/jetbrains/compose/material3/adaptive/adaptive-layout/maven-metadata.xml) | 1.3.0 | 1.4.0-alpha01 |
| [org/jetbrains/compose/material3/adaptive/adaptive-navigation](https://repo.maven.apache.org/maven2/org/jetbrains/compose/material3/adaptive/adaptive-navigation/maven-metadata.xml) | 1.3.0 | 1.4.0-alpha01 |
| [org/jetbrains/compose/material3/adaptive/adaptive-navigation3](https://repo.maven.apache.org/maven2/org/jetbrains/compose/material3/adaptive/adaptive-navigation3/maven-metadata.xml) | 1.3.0 | 1.4.0-alpha01 |
| [org/jetbrains/compose/runtime/runtime](https://repo.maven.apache.org/maven2/org/jetbrains/compose/runtime/runtime/maven-metadata.xml) | 1.12.2 | 1.13.0-beta01 |
| [org/jetbrains/compose/foundation/foundation](https://repo.maven.apache.org/maven2/org/jetbrains/compose/foundation/foundation/maven-metadata.xml) | 1.12.2 | 1.13.0-beta01 |
| [org/jetbrains/compose/components/components-resources](https://repo.maven.apache.org/maven2/org/jetbrains/compose/components/components-resources/maven-metadata.xml) | 1.12.2 | 1.13.0-beta01 |
| [org/jetbrains/compose/ui/ui](https://repo.maven.apache.org/maven2/org/jetbrains/compose/ui/ui/maven-metadata.xml) | 1.12.2 | 1.13.0-beta01 |
| [org/jetbrains/compose/ui/ui-tooling-preview](https://repo.maven.apache.org/maven2/org/jetbrains/compose/ui/ui-tooling-preview/maven-metadata.xml) | 1.12.2 | 1.13.0-beta01 |
| [org/jetbrains/kotlinx/kotlinx-coroutines-core](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-core/maven-metadata.xml) | 1.11.0 | 1.11.0 |
| [org/jetbrains/kotlinx/kotlinx-coroutines-test](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-test/maven-metadata.xml) | 1.11.0 | 1.11.0 |
| [org/jetbrains/kotlinx/kotlinx-coroutines-android](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-android/maven-metadata.xml) | 1.11.0 | 1.11.0 |
| [org/jetbrains/kotlinx/kotlinx-collections-immutable](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-collections-immutable/maven-metadata.xml) | 0.5.2 | 0.5.2 |
| [org/jetbrains/kotlinx/kotlinx-serialization-core](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-serialization-core/maven-metadata.xml) | 1.12.0 | 1.12.0 |
| [org/jetbrains/kotlinx/kotlinx-serialization-json](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-serialization-json/maven-metadata.xml) | 1.12.0 | 1.12.0 |
| [io/coil-kt/coil3/coil-compose](https://repo.maven.apache.org/maven2/io/coil-kt/coil3/coil-compose/maven-metadata.xml) | 3.6.3 | 3.6.3 |
| [io/coil-kt/coil3/coil-network-ktor3](https://repo.maven.apache.org/maven2/io/coil-kt/coil3/coil-network-ktor3/maven-metadata.xml) | 3.6.3 | 3.6.3 |
| [io/ktor/ktor-bom](https://repo.maven.apache.org/maven2/io/ktor/ktor-bom/maven-metadata.xml) | 3.6.1 | 3.6.1 |
| [io/insert-koin/koin-bom](https://repo.maven.apache.org/maven2/io/insert-koin/koin-bom/maven-metadata.xml) | 4.2.2 | 4.2.2 |
| [me/zhanghai/compose/preference/preference](https://repo.maven.apache.org/maven2/me/zhanghai/compose/preference/preference/maven-metadata.xml) | 2.2.0 | 2.2.0 |
| [com/mohamedrejeb/calf/calf-file-picker](https://repo.maven.apache.org/maven2/com/mohamedrejeb/calf/calf-file-picker/maven-metadata.xml) | 0.14.0 | 0.14.0 |
| [dev/detekt/detekt-rules-ktlint-wrapper](https://repo.maven.apache.org/maven2/dev/detekt/detekt-rules-ktlint-wrapper/maven-metadata.xml) | 无/未取得 | 2.0.0-alpha.6 |
| [io/nlopez/compose/rules/detekt](https://repo.maven.apache.org/maven2/io/nlopez/compose/rules/detekt/maven-metadata.xml) | 0.6.8 | 0.6.8 |

补核依据：[Browser 元数据](https://dl.google.com/dl/android/maven2/androidx/browser/browser/maven-metadata.xml)、[Foojay 元数据](https://plugins.gradle.org/m2/org/gradle/toolchains/foojay-resolver-convention/org.gradle.toolchains.foojay-resolver-convention.gradle.plugin/maven-metadata.xml)、[Gradle 正式版 API](https://services.gradle.org/versions/current)。

## 实施与对照

在 `codex/dependency-upgrade-2026-10-30` 分支实施。开始实施时仅有本需求方案和文档索引差异。没有修改业务源码、测试、断言、正式截图基线、schema、CI/CD、全局配置或安装工具。

升级前执行 `help` 及 composeApp/navigation/data/database 的 `testAndroidHostTest`，整体失败（2m2s，428 任务：61 执行、5 缓存、362 up-to-date）。本轮 XML 汇总：composeApp 254 通过、7 失败、645 跳过；navigation 15 通过、1 失败；data 9 通过、database 3 通过。原始报告保存于 `/private/tmp/notes-upgrade-baseline-2026-10-30/`。

UI 候选组合的 Android 四个 Debug flavor 均完成打包，但宿主测试整体失败（4m9s，626 任务：536 执行、78 缓存、12 up-to-date）：composeApp 仍为同一组 7 失败；navigation 改为 3 个 `DeviceCornerNavigationTest` 失败，均断言快速反向时像素未即时跟随。升级前 `PageMotionNavigationTest` 的背景透明度失败在此轮未再出现，不能据单轮通过宣称修复。

用临时 init script 仅将 `org.jetbrains.androidx.navigation3` 解析固定为 1.1.2，保持新 Compose/Roborazzi，执行 `:core:navigation:testAndroidHostTest --tests '*DeviceCornerNavigationTest*' --tests '*PageMotionNavigationTest*' --no-configuration-cache`：4 项全部通过（15s）。因此本次不采用 Navigation3 1.2.0；版本目录保留 1.1.2，不扩展为导航动效适配需求。对照不修改仓库测试或实现。证据：`/private/tmp/notes-upgrade-ui-results-2026-10-30/`、`/private/tmp/notes-upgrade-navigation-comparison-2026-10-30/` 及同名前缀日志。

首轮解析 Compose/Roborazzi 插件失败；官方 Roborazzi marker HTTP 200，依赖指向 `roborazzi-gradle-plugin:1.77.0`。刷新后未变更的 Kotlin DSL 插件也报解析失败。换用 `--no-daemon` 的单次进程后 `help` 通过（22s），后续构建统一使用该方式。没有修改仓库地址、代理或系统配置；该现象与进程环境有关的归因未独立证明。

## 最终组合与验证结果

最终采用 Compose 1.12.2、DataStore 1.3.0、Paging 3.5.3、Serialization 1.12.0、Ktor BOM 3.6.1、Roborazzi 1.77.0。Navigation3 仍为 1.1.2；候选表只表示实施前建议，不表示最终采用。Kotlin、AGP、Wrapper、SDK/JDK、必要预览版均未变化。

完整批次日志：`/private/tmp/notes-upgrade-final-2026-10-30.log`。实际命令（所有 Gradle 调用串行，使用 Android Studio JBR 25.0.4）：

```bash
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew \
  :androidApp:assembleDebug :androidApp:assembleDevFullRelease \
  :composeApp:testAndroidHostTest :core:navigation:testAndroidHostTest \
  :core:data:testAndroidHostTest :core:database:testAndroidHostTest \
  :core:theme:testAndroidHostTest :core:framework:testAndroidHostTest \
  :core:network:testAndroidHostTest :feature:main:testAndroidHostTest \
  :feature:login:testAndroidHostTest :feature:email:testAndroidHostTest \
  :feature:profile:testAndroidHostTest :feature:about:testAndroidHostTest \
  :feature:settings:testAndroidHostTest :androidApp:lintDevFullDebug \
  detektMainAndroid detektHostTestAndroid :androidApp:detektDevFullDebug \
  :androidApp:detektDevFullDebugUnitTest :androidApp:detektDevFullDebugAndroidTest \
  :android:baselineprofile:detektDevBenchmarkRelease \
  :android:output:login:assemble :android:baselineprofile:assemble \
  :androidApp:dependencies --configuration devFullDebugRuntimeClasspath \
  --no-daemon --continue --console=plain --max-workers=4 \
  -Dkotlin.daemon.jvmargs=-Xmx8g \
  '-Dorg.gradle.jvmargs=-Xmx8g -Dfile.encoding=UTF-8 -XX:+UseParallelGC'
```

整体 `BUILD FAILED`（5m24s，1182 任务：615 执行、32 缓存、535 up-to-date），不能宣称全量通过。

| 验证 | 实际结果 |
|---|---|
| Android 四 flavor Debug | 通过，实际执行打包 |
| DevFull Release / R8 | 通过，实际执行 |
| lintDevFullDebug | 通过，实际执行 |
| composeApp 宿主测试 | 254 通过、7 失败、645 跳过；用例实际执行，失败类/方法/断言与升级前 XML 逐项完全相同 |
| navigation 宿主测试 | 16 通过，无失败或跳过，实际执行 |
| data/database/framework/network/theme | 分别 9/3/3/2/5 通过，实际执行 |
| about/email/login/main/profile/settings | 分别 19/8/16/13/15/5 通过，实际执行 |
| 宿主测试合计 | 368 通过、7 失败、645 跳过；无测试任务 FROM-CACHE/UP-TO-DATE |
| detekt 语义分析 | 11 个任务失败，共 98 条报告；其他有源集任务通过，NO-SOURCE 不计通过。未建立旧版本逐条静态对照，不将全部违规归因为既有问题 |
| Android App 三个 detekt / Baseline Profile detekt | 通过，实际执行 |
| Fused Library / Baseline Profile assemble | 通过；没有执行设备 Profile 生成 |
| Android runtime 依赖图 | 实际执行，新版本解析正确；8 个无版本 Ktor 别名均在 3.6.1 BOM 管理清单，未发现依赖图 FAILED 解析项 |
| iOS Kotlin 编译、模拟器行为、真实 Entry、Preference 初始化 | 未执行：只有 Xcode 27.1，缺少官方表的 26.4 |
| Xcode 模拟器整包、设备/视觉验收 | 未执行；没有更新正式截图基线或修改模拟器显示设置 |

### 开源声明与最终打包

按 [声明工具](../../tools/open-source/README.md) 实际执行 `-I tools/open-source/export-dependencies.gradle exportNoticeDependencies --no-daemon --no-configuration-cache --no-parallel`（13s，1 执行/9 up-to-date）、`python3 tools/open-source/generate.py --refresh`，更新 Android/iOS 清单和列表页面。仅解析 iOS 工件，不代表 Native 编译或 Xcode 验收。上游 NOTICE 文本未变。

生成器单元测试 8 项通过；Node 校验 20 页 × 10 字号 × 4 主题 × 3 来源 × 4 色板，以及 20 页 × 8 语言场景通过，不能替代浏览器/WebView 视觉验收。

刷新后重新执行 Android Debug、DevFull Release、Fused/Baseline assemble，并对 composeApp、core 和 Feature 实现模块运行普通 `detekt` 扫描全部手写源集（含 Native）。Android 重新打包通过；Fused/Baseline 本轮 UP-TO-DATE。8 个普通 detekt 任务失败、共 91 条；整体失败（19s，994 任务：55 执行、3 缓存、936 up-to-date）。日志 `/private/tmp/notes-upgrade-packaging-2026-10-30.log`。这与前批语义报告范围不同，不能相加为去重告警数。

本次没有可独立验收的新增用户功能；不将未验证的上游修复写成应用已修复的更新条目。未修改应用发布版本和已发布历史。源码兼容适配为零，最终版本目录只有 6 行版本变化。

待验收事项同步 [集中清单](../testing/pending-acceptance.md#依赖升级2026-10-30)。按 维护者 授权将本需求整理为一个提交；未合入或发布。

### 交付自检

文档归位、当前版本表与集中待验收清单已同步；本次修改文档的本地文件引用全部存在，`git diff --check` 通过。最终版本目录 diff 为 6 行版本替换；未改业务源码、测试、数据库 schema 或 CI/CD。刷新清单中 Android 101 个、iOS 93 个坐标版本相较旧库存变化（含传递依赖和此前库存未反映的解析结果），没有坐标移除，NOTICE 文本字典逐项相同。已检查新 DevFull Debug/Release APK 内 `android.html`，均包含最终 Ktor 3.6.1 与 Serialization 1.12.0 信息；此检查不替代设备 WebView 验收。

## 提交后 Navigation3 再检查（2026-10-30）

维护者 请求重新检查 1.2.0 的原问题。开始检查时，工作区已有用户的 `navigation3-ui` 1.1.2 → 1.2.0 未提交修改；本轮保留该修改，没有改变版本目录、业务源码、测试、断言或基线，也没有提交。

复用 Android Studio JBR 25.0.4；所有 Gradle 串行执行。临时 init script 用 `resolutionStrategy.eachDependency` 仅覆盖 `org.jetbrains.androidx.navigation3` 版本，并对 Test 禁用 up-to-date 和结果缓存。每轮完整执行导航模块 16 项测试，测试没有 FROM-CACHE/UP-TO-DATE。实际命令：

```bash
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew \
  -I /private/tmp/notes-navigation-recheck.gradle :core:navigation:testAndroidHostTest \
  -Pnotes.navigationCheckVersion=1.2.0 \
  --no-daemon --no-configuration-cache --console=plain --max-workers=4
```

中间对照将同一命令属性值改为 1.1.2，随后再执行一次 1.2.0。实际记录如下：

| 轮次 | 通过/失败/跳过 | 失败 |
|---|---|---|
| 1.2.0 第一轮（36s） | 14 / 2 / 0 | `reportedSquareCornersKeepSquareMotion` 快速反向像素断言；`pageUsesOwnSizeWhileNavigationRemainsFixed` 背景透明度断言 |
| 1.1.2 对照（16s） | 16 / 0 / 0 | 无 |
| 1.2.0 第二轮（15s） | 14 / 2 / 0 | `predictiveExitClipsPhysicalCornersAndCancellationRestoresPage`、`reportedSquareCornersKeepSquareMotion` 均为快速反向像素断言 |

快速反向失败仍为 `Reversal follows the finger without a spring tail`，页面 bounds 为 `Rect.fromLTRB(16.7, 22.3, 394.4, 777.7)`，采样像素红色分量没有达到 >0.99。背景透明度失败期望 0.128、实际 0.047058824，该断言在原升级前也失败过，不能作为新增回归单独归因。

init script 记录实际解析组件：1.2.0 组合的 JetBrains UI 和 Google AndroidX UI/runtime 均为 1.2.0；1.1.2 对照的 JetBrains UI 为 1.1.2、Google AndroidX UI/runtime 为 1.1.7。两组使用相同工作区和新 Compose/Roborazzi。官方 [JetBrains Maven 元数据](https://repo.maven.apache.org/maven2/org/jetbrains/androidx/navigation3/navigation3-ui/maven-metadata.xml) 本轮读取仍显示最新正式版 1.2.0。

结论：原快速反向问题仍可重复出现，但失败场景存在波动，不是每轮固定三项失败。对照支持问题与 Navigation3 版本组合相关；本轮未定位库内部、项目适配或采样时序的根因，不将其直接认定为上游 bug。未运行应用完整回归、设备视觉、iOS 编译或 Native 行为测试。当前工作区的 1.2.0 是用户已有改动，不视为通过升级验收。

原始日志与逐轮 XML：`/private/tmp/notes-navigation-recheck-{120-run1,112-run1,120-run2}.log` 和同名前缀目录；摘要 `/private/tmp/notes-navigation-recheck-summary.json`。文档引用与 `git diff --check` 在交付前复核。

## Navigation3 失败原因调查（2026-10-30，取代此前版本归因）

本轮只调查原因，没有修改生产代码、原测试、断言、截图基线或构建配置。系统临时目录内的 Kotlin 诊断夹具通过 init script 加入测试源集，不入库。用户已有的版本目录 1.2.0 修改保留。

### 官方源码与实际依赖逐项对比

直接读取缓存中的官方 Google Navigation3 UI 1.1.7/1.2.0 sources JAR（Android 实际执行实现），对比 NavDisplay、SceneState、SceneDecoratorStrategy、OverlayScene 以及新增的返回处理帮助函数。项目使用的是显式 `sceneState/navigationEventState` 重载；1.2.0 新增的便捷返回 API 不在当前路径。该重载的 seekTo、AnimatedContent 和转场处理没有发现解释快速反向失败的代码差异；SceneState 的 previousScenes 修复针对 OverlayScene，当前失败夹具是 SinglePaneScene。

两组 Android 宿主运行时完整组件清单逐项相减，只差 Navigation3 本身的 6 个元数据/Android 实现坐标；Compose、NavigationEvent 等实际传递版本相同。因此没有证据归因为升级间接替换了 Compose 版本。

原始依赖图：`/private/tmp/notes-nav-runtime-{1.1.2,1.2.0}.json`；官方源码对照：`/private/tmp/notes-nav-source-comparison/`。单独执行方角失败用例时，1.2.0 和 1.1.2 均通过，说明整组失败受执行条件影响。

### 几何与像素分离的最小对照

临时 `NavigationReversalDiagnosticTest` 保留原来的事件序列，到快速反向采样点记录进度、语义几何、横向像素，随后重复截图 3 次并再推进 3 帧。诊断夹具不承担通过验收的断言，原有测试保持原样且继续同时运行。

- 手势从 0.5 返回 0.1 后，PageMotionState 已为 easing 后的 `0.37170368`，语义 bounds 已为 `(16.7,22.3)-(394.4,777.7)`；这与现有 85% 缩放公式一致，不能称为手势状态仍停留在旧进度。
- 失败采样点 `(40,400)` 已在该页内部，但 Native 截图仍为混合色：方角 `(0.9372549,0.19607843,0.25882354)`，圆角 `(0.8039216,0.2627451,0.44313726)`；背景仍为旧的约 `(0.1137,0.1137,0.8980)`。
- 多次截图及额外 3 帧后仍不变，排除“只要多推进一帧就能恢复”的简单解释。
- 保持事件、进度和时钟不变，通过 AndroidComposeView 的 `invalidateDescendants()` 诊断接口使所有子图层重新失效并重绘。立即得到 `(40,400)=(1,0,0)`，背景变为约 `(0.0471,0.0471,0.9569)`；进度和 bounds 未改变。这直接证明失败图像含有未刷新的图层内容。
- 对 1.1.2 执行相同诊断，也出现相同的混合像素并可由重绘恢复；该轮原有三项快速反向测试同样失败。因此该现象并非 1.2.0 独有，之前有限轮次的版本对照不足以建立版本因果关系。

### 截图后端对照与结论边界

[Compose 同步文档](https://developer.android.com/develop/ui/compose/testing/synchronization)说明虚拟时钟不控制 Android 的测量与绘制。实际 ui-test 1.12.1 `WindowCapture.android.kt` 源码中，`forceRedraw()` 遇到 Robolectric fingerprint 直接返回。Robolectric 4.17 的 [ShadowPixelCopy](https://raw.githubusercontent.com/robolectric/robolectric/robolectric-4.17/shadows/framework/src/main/java/org/robolectric/shadows/ShadowPixelCopy.java)在 Native 模式下可走 [HardwareRenderingScreenshot](https://raw.githubusercontent.com/robolectric/robolectric/robolectric-4.17/shadows/framework/src/main/java/org/robolectric/shadows/HardwareRenderingScreenshot.java)：基于现有 RenderNode/display list 截图；其官方 `robolectric.pixelCopyRenderMode=software` 参数则走 View.draw 路径。

只在临时 init script 中为 Test 设置该软件 PixelCopy 参数，Navigation3 保持 1.2.0，运行原来的完整 16 项导航测试，不加入诊断夹具、不改任何断言：16 通过、0 失败、0 跳过（17s，测试实际执行）。这是截图后端的因果对照，不是正式验证通过或永久切换测试配置的授权。

目前已定位到宿主环境中的硬件截图/缓存图层重绘失效路径：更新后的几何与所捕获的像素不同步。尚未定位哪一个 Compose/Robolectric 内部失效通知具体丢失，也没有真实 Android 渲染对照，不能断言生产端完全正常，不能把所有原失败都定性为测试误报。此前“Navigation3 1.2.0 新增回归、保留旧版即可规避”的版本归因应撤回；历史运行结果保留，但不再作为确认不兼容的结论。

不应通过放宽颜色阈值、多等固定帧、在生产代码强制重绘或直接全局切换软件截图来绕过问题。后续应在原生设备/模拟器运行同一事件与像素夹具，并缩小硬件图层失效的最小复现后再决定修复归属。

证据：`/private/tmp/notes-nav-cause-evidence-{120,112}/`、`/private/tmp/notes-nav-diagnostic-invalidate-{120,112}.log`、`/private/tmp/notes-nav-software-capture-120.log` 及同名前缀 XML 目录。首个临时 init script 因作用到 included build 而失败，修正作用范围后正常执行；没有修改共享构建配置。未提交、合入或发布。

## Android 真实渲染验证（2026-10-30）

维护者 授权执行原生验证后，在导航模块按官方 `withDeviceTest` DSL 启用 `androidDeviceTest`，配置 AndroidJUnitRunner、Compose 测试和 Google `ui-test-manifest:1.12.1`（与 CMP 1.12.2 对应）。先补模块目录约定与测试指南，后新增 `DeviceCornerDeviceTest`，不修改生产转场、宿主测试或既有断言阈值。

### 环境与夹具

设备 `emulator-5554` / Pixel_10_Pro_Fold（当前外屏），API 37；物理 1080×2364、density 390，开始与结束均无 wm size/density Override。未修改分辨率、密度、旋转或动画倍率，也未修改全局设置、清除数据或安装全局依赖。测试局部使用 400×800dp 夹具，采样点按真实截图尺寸映射；指定圆角以像素传参，默认圆角继续由设备密度换算。

三种原有场景复用宿主测试手势：左边缘进度 0→0.5→0.1，检查反向后的第一帧像素，随后完整拖动、取消恢复，再从右边缘返回提交。使用真实 Compose/Android PixelCopy；没有强制子图层重绘、软件 PixelCopy 参数或正式基线变更。每场景用 JUnit 参数化重复五轮，测试实际运行，不以编译缓存计作测试通过。

另外新增“前进动画两帧后开始预测返回”场景，每轮复用相同后续事件和原始初帧像素约束，用来检查动画交接；该场景此前未包含在原 16 项宿主对照中。

### 实际命令与结果

串行执行（`notes.navigationCheckVersion` 只由系统临时 init script 在依赖解析中消费，未改变用户已有的版本目录 1.2.0）：

```bash
ANDROID_SERIAL=emulator-5554 \
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew \
  -I /private/tmp/notes-navigation-recheck.gradle \
  :core:navigation:connectedAndroidDeviceTest \
  -Pnotes.navigationCheckVersion=1.2.0 \
  --no-daemon --no-configuration-cache --console=plain --max-workers=4
```

旧版仅把属性改为 1.1.2；init script 覆盖 `org.jetbrains.androidx.navigation3`，其余依赖相同。原始记录：

| 组合与范围 | 通过/失败/跳过 | 时间 |
|---|---|---|
| 1.2.0 首轮三场景 | 3 / 0 / 0 | 1m35s（含首次打包与设备测试准备） |
| 1.1.2 三场景×五轮 | 15 / 0 / 0 | 50s |
| 1.2.0 三场景×五轮 | 15 / 0 / 0 | 47s |
| 1.2.0 最终四场景×五轮 | 15 / 5 / 0 | 56s，整体失败 |
| 1.1.2 最终四场景×五轮 | 15 / 5 / 0 | 55s，整体失败 |

最终两组都只有 `predictiveBackDuringEntryKeepsThePageOpaque` 的五轮失败，断言均为 `Initial gesture frame must not expose an empty or blended page`。原有三种场景在真实渲染中持续通过，未复现此前的快速反向像素失败。新增的动画交接场景在新旧版本上都稳定失败；该夹具下存在共同未通过项，尚未定位为生产转场还是夹具预期问题，不能归因为 1.2.0 新增回归，也不能据此宣称全量导航验收通过。

XML 和日志：`/private/tmp/notes-nav-device-{120-run1,112-repeated,120-repeated,120-final,112-final}/` 与对应 `.log`；最终 fixture 保留失败断言，无 suppress、放宽阈值或绕过。

配置准备阶段先遇到动态源集的 Kotlin DSL 访问器未生成（改为 `getByName("androidDeviceTest")`），随后 JetBrains 坐标无 ui-test-manifest 工件，按官方 CMP 对应表改用 Google 坐标。均在执行设备用例前解决，不计作设备测试失败。

### 真实应用入口冒烟

在同一设备执行候选 1.2.0 的 `:androidApp:installDevFullDebug`，Debug 构建与安装通过（34s），保留用户应用数据。通过 Android CLI 实际布局定位控件，验证：

- 首页邮件列表→详情→普通返回，详情正文和返回入口可见。
- 设置→About→开源声明；实际 WebView 截图可见标题和组件内容。
- 从声明页注入系统左边缘返回手势，回到 About，版本与声明入口的纵坐标与进入前一致；本轮没有展开长摘要，不能替代长页面滚动恢复专项验收。
- 首页→空白新建邮件弹层→系统返回，收件人/主题/正文控件出现，关闭后回到首页；没有发送或保存邮件。

这些检查验证真实入口和返回终态，不代表人工逐帧动效审阅、物理手指体验、宽屏/折叠切换或前进动画交接的全面通过。声明页候选截图 `/private/tmp/notes-nav-device-notices-120.png` 已视觉检查；没有更新正式截图基线。应用启动后连续点击曾未切换到预期页，按布局确认并分步操作后完成，不将该次点击当成已验证流程。

### 结论与未验收

原先阻碍升级的稳定页面快速反向问题，没有在本次真实 Android 渲染对照中复现；与宿主硬件截图/缓存失效调查相符。当前未发现该范围内 1.2.0 相对 1.1.2 的差异回归。但新增前进中途返回场景两版均未通过，须继续定位；未运行全应用完整回归、iOS 受支持 Xcode 编译/行为/真实 Entry 或实体设备验收。当前 1.2.0 仍作为候选，不据局部通过宣称两端升级全部完成。新增原生测试配置和夹具未提交。

## 最终采用与 amend

维护者 在查看原生对照后授权采用 Navigation3 1.2.0，并通过 amend 合入原依赖升级提交。最终共 7 组升级；此前“保留 1.1.2”是阶段性决定，已由后续截图失效调查和原生对照取代。README 与 Android/iOS 开源声明按最终依赖图同步；新增 ui-test-manifest 仅用于设备测试，不进入生产开源清单。原生夹具、模块测试配置、目录约定与测试指南一并保留。

采用新版本不表示全部验证通过：原生四场景五轮的最终结果仍为两版各 15 通过/5 失败/0 跳过；应用宿主 7 个既有失败、静态检查失败及 iOS 受支持环境缺口均不关闭。没有放宽断言、改生产转场或更新正式截图基线。未合入 master、推送或发布。

### amend 前复核

最终依赖图导出与声明刷新完成，Android/iOS Navigation3 清单均为 1.2.0；生成器 8 项测试和 20 页外观/导航/语言脚本检查通过。声明刷新后 Android Debug 打包通过（同批 core:navigation detekt 失败，不能把整体标为通过）。

新增设备夹具按初始化、反向、取消和提交阶段拆分，断言与事件保持原样。MagicNumber 依照既有测试源集约定补齐 `androidDeviceTest`，生产规则及其他测试规则不变，没有 suppress 或 baseline。整理后 1.2.0 原生测试再次实际执行，20 项仍为 15 通过/5 个相同的中途返回失败/0 跳过（55s）；最终普通 detekt 报告为原有 8 条、新设备源集 0 条。原始日志 `/private/tmp/notes-nav-amend-device-final.log`、`/private/tmp/notes-nav-amend-detekt-final.log`，XML `/private/tmp/notes-nav-amend-device-final/`。

本轮新增本机绝对路径只作为审计环境证据，未写入共享构建配置。修改文档的本地引用与 `git diff --check` 通过。按授权 amend，不额外创建过程提交。
