# 2026-11-20 依赖检查与一次性强制升级

日期：2026-11-20（Asia/Shanghai）。状态：一次性强制升级已实施，双端行为回归通过；静态门禁、视觉基线与设备验收尚未全部通过。前半部分保留最初常规检查依据，采用版本和结果以末节为准。

基线：`8b796cc6`，开始时工作区干净。按 [升级规范](../dependency-upgrades.md) 检查应用版本目录、内联版本、Wrapper、构建约定和 Xcode 平台配置；不含独立认证服务、传递依赖漏洞扫描。初次检查仅新增记录与索引，随后按维护者授权进入强制升级。

## 初次常规检查建议（已由后续强制升级取代）

| 组件 | 当前 → 候选 | 判断与后续验证 |
|---|---|---|
| KSP | 2.3.12 → 2.3.14 | [发布说明](https://github.com/google/ksp/releases/tag/2.3.14) 明确修复 #3286 并行执行时库符号消失，上次保留原因已解除。marker 与实现 module 均实际发布；实施时保留并行配置，验证 Room 的 Android、iosArm64、iosSimulatorArm64 代码生成与数据库测试。 |
| Robolectric | 4.17 → 4.18.1 | [发布说明](https://github.com/robolectric/robolectric/releases/tag/robolectric-4.18.1) 修复 shadow 链接竞争、Looper 时间查询和资源清理；POM HTTP 200。跨越 4.18，实施前还需审阅该中间版本并跑宿主行为测试、与旧组合对照失败项。 |
| Roborazzi 插件及库 | 1.78.0 → 1.79.0 | [发布说明](https://github.com/takahirom/roborazzi/releases/tag/1.79.0) 包含截图 bitmap 回收和 Preview 初始化修复。marker、库元数据与 POM 已发布；跑截图 verify，差异须逐图审阅，不更新正式基线。 |
| Android Lifecycle | 2.11.0 → 2.11.1 | [AndroidX 发布说明](https://developer.android.com/jetpack/androidx/releases/lifecycle#2.11.1)。Android 两个坐标已发布，但 JetBrains Lifecycle 三个坐标最新正式版仍为 2.11.0；当前共用版本键，不可直接整体改成 2.11.1。建议暂留，若需要此补丁，先评估拆分版本键及解析图一致性。 |

前三组可作为下一轮最小升级范围；属于待实施候选，工件存在不代表本项目兼容验证通过。

## 保留与阻碍

- Kotlin 2.4.20 保留：[Koin 官方兼容列表](https://insert-koin.io/docs/setup/compiler-plugin/#kotlin-version-compatibility) 和 [1.2.3 发布说明](https://github.com/InsertKoinIO/koin-compiler-plugin/releases/tag/1.2.3) 尚未列入 2.4.21；编译器插件及 kotlin-test 随 Kotlin。
- BuildKonfig 0.23.0 保留：0.24.0 的 [实现 module](https://repo.maven.apache.org/maven2/com/codingfeline/buildkonfig/buildkonfig-gradle-plugin/0.24.0/buildkonfig-gradle-plugin-0.24.0.module) 引入 Kotlin 2.4.21，随 Kotlin 一起评估。
- AGP 9.3.1、Gradle 9.7.0 保留：仓库最新正式版分别为 9.5.1、9.9.1，但 [KMP 兼容表](https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html) 上限仍为 9.3.1、9.7.0。[AGP 9.3 发布说明](https://developer.android.com/build/releases/agp-9-3-0-release-notes) 虽已有 9.3.3，也超出该明确上限。
- Android Compose ui-test-manifest 1.12.1 保留：最新正式版 1.13.0，需随 Compose 测试与运行时组合评估，不能单凭最大版本升级测试工件。
- Compose Material 3 保留 1.12.0-alpha03；最新正式版仍为 1.9.0。历史检查发现正式版缺少项目使用的 ContainedLoadingIndicator，见 [上次调查](2026-11-12-dependency-upgrade.md)；此次未重新下载源码验证 API，不据此宣称正式版回归通过。Material 3 1.13.0-rc01 为预览，不主动追新。
- detekt 继续使用 2.0.0-alpha.6，当前 dev.detekt 坐标无正式版；Compose Rules 仍为 0.6.8。
- Compose 1.12.2、Adaptive 1.3.0、Ktor BOM 3.6.1、Koin BOM 4.2.2、Room 3.0.3、Paging 3.5.3、Coil 3.6.3、Compose Preference 2.2.0 均仍为相应坐标最新正式版。BOM POM 均读取成功，未展开全量传递依赖或重新解析 Gradle 图。

## AndroidX Compose 稳定版补充核对

2026-11-20 核对用户提供的 11 月 19 日更新：AndroidX [Runtime 1.13.0](https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.13.0) 与 [Material 3 1.5.0](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.5.0) 确于 11 月 18 日发布。此前 79 坐标盘点只覆盖显式声明，对这些经 JetBrains 工件引入的 AndroidX 上游变更缺少单独说明；不能把显式坐标最新正式版理解为整个 AndroidX 技术栈没有更新。

[Compose Multiplatform 1.12.2 组件表](https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.12.2) 对应 AndroidX Runtime/UI/Foundation 1.12.1、Material 3 1.5.0-alpha22；项目实际声明的 JetBrains Material 3 版本为 1.12.0-alpha03。AndroidX Material 3 1.5.0 与 JetBrains Material 3 1.9.0 是不同坐标的版本，不能按数字大小比较，也不能直接把 commonMain 的版本改成 AndroidX 的版本。建议将新 AndroidX 版本纳入兼容性跟踪，等待匹配的 Compose Multiplatform 发行组合，或取得明确支持证据后再评估 Android 单端覆盖；本次不主动覆盖解析版本。

源码定点检索发现以下实际回归面：

- Android 邮件草稿 `DraftModalBottomSheet.android.kt` 使用 ModalBottomSheet；iOS 为独立 actual 实现。重点回归 Android 部分展开、返回关闭、拖动、嵌套滚动与恢复，不能把 AndroidX 行为变化直接外推至 iOS。
- 共享邮件 `EmailSearchBar.kt` 使用展开式 SearchBar，主题 `CenteredTopAppBar.kt` 使用 TopAppBarDefaults；后续升级应检查搜索展开/收起、焦点、滚动与状态恢复。
- `AppShell.kt` 使用 derivedStateOf 控制导航可见性。computedStateOf 是可选 API，不构成强制替换理由；保留现有实现，未来按重组与计算成本证据评估。
- 本轮在 core、feature、composeApp 的 Kotlin 源码中未检出 Slider/RangeSlider/SecureTextField 直接调用或 Modifier.border 链式调用；这不涵盖第三方内部实现，不能视为没有相关视觉风险。
- Material 3 发布说明包含特性开关与旧 API 行为差异，BottomSheet 风险需按实际使用的 API、开关及最终解析版本判断，不能把发布说明所有条目一概认作默认启用。

以上为发布说明与源码检查，未运行新 Compose 组合、像素比较或设备行为验证；不修改架构、导航设计或正式截图基线。

## 环境与验证边界

- 实际读取并核对 79 个去重显式插件/库坐标（含 Foojay）；第一次 Foojay 在 Central 返回 404，改用 Plugin Portal 成功。KSP 在 Google 的元数据停留于旧系列，改用 Central 确认 2.3.14，未把旧镜像结果当作最新版。最终 56 个与最新正式版一致、21 个不同、2 个 detekt 坐标无正式版。完整坐标和来源见下表。
- Android Studio JBR 的 java、javac 均为 25.0.4。当前 SDK 37.0、target 37、Build Tools 37.0.0；本机另有 SDK 37.2，未取得当前 AGP 支持该小版本的新证据，因此保留。
- 本机 Xcode 27.2（27B52），Applications 中未发现其他 Xcode；KMP 官方表仍列 26.4。此次 simctl 查询因 CoreSimulator 连接不可用失败，未取得 runtime 清单，也未修改模拟器。
- 本次执行版本元数据检查、候选工件读取及文档静态验证；未运行 Gradle 编译、行为测试、截图、Xcode 整包或设备验收，不引用历史测试作为当前通过证据。
- 实施后须覆盖 Android Debug/Release R8、lint、有效 detekt、共享与宿主测试、双 Native 编译和模拟器 Room/Entry/Preference 回归、Fused Library 与 Baseline Profile 模块构建；同步评估开源声明分发范围并按指南刷新需要变化的清单。Xcode 受支持环境仍是验收缺口。

## 显式坐标清单

正式版按仓库中纯数字点分版本选取；预览版不作为升级目标。每个链接指向本次实际读取的 Maven 元数据。

| 坐标 | 当前 | 最新正式版 |
|---|---|---|
| [io.github.takahirom.roborazzi:io.github.takahirom.roborazzi.gradle.plugin](https://repo.maven.apache.org/maven2/io/github/takahirom/roborazzi/io.github.takahirom.roborazzi.gradle.plugin/maven-metadata.xml) | 1.78.0 | 1.79.0 |
| [com.android.application:com.android.application.gradle.plugin](https://dl.google.com/dl/android/maven2/com/android/application/com.android.application.gradle.plugin/maven-metadata.xml) | 9.3.1 | 9.5.1 |
| [com.android.kotlin.multiplatform.library:com.android.kotlin.multiplatform.library.gradle.plugin](https://dl.google.com/dl/android/maven2/com/android/kotlin/multiplatform/library/com.android.kotlin.multiplatform.library.gradle.plugin/maven-metadata.xml) | 9.3.1 | 9.5.1 |
| [com.android.lint:com.android.lint.gradle.plugin](https://dl.google.com/dl/android/maven2/com/android/lint/com.android.lint.gradle.plugin/maven-metadata.xml) | 9.3.1 | 9.5.1 |
| [com.android.library:com.android.library.gradle.plugin](https://dl.google.com/dl/android/maven2/com/android/library/com.android.library.gradle.plugin/maven-metadata.xml) | 9.3.1 | 9.5.1 |
| [com.android.fused-library:com.android.fused-library.gradle.plugin](https://dl.google.com/dl/android/maven2/com/android/fused-library/com.android.fused-library.gradle.plugin/maven-metadata.xml) | 9.3.1 | 9.5.1 |
| [com.android.test:com.android.test.gradle.plugin](https://dl.google.com/dl/android/maven2/com/android/test/com.android.test.gradle.plugin/maven-metadata.xml) | 9.3.1 | 9.5.1 |
| [androidx.room3:androidx.room3.gradle.plugin](https://dl.google.com/dl/android/maven2/androidx/room3/androidx.room3.gradle.plugin/maven-metadata.xml) | 3.0.3 | 3.0.3 |
| [androidx.baselineprofile:androidx.baselineprofile.gradle.plugin](https://dl.google.com/dl/android/maven2/androidx/baselineprofile/androidx.baselineprofile.gradle.plugin/maven-metadata.xml) | 1.5.0 | 1.5.0 |
| [org.jetbrains.compose:org.jetbrains.compose.gradle.plugin](https://repo.maven.apache.org/maven2/org/jetbrains/compose/org.jetbrains.compose.gradle.plugin/maven-metadata.xml) | 1.12.2 | 1.12.2 |
| [org.jetbrains.kotlin.plugin.compose:org.jetbrains.kotlin.plugin.compose.gradle.plugin](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/plugin/compose/org.jetbrains.kotlin.plugin.compose.gradle.plugin/maven-metadata.xml) | 2.4.20 | 2.4.21 |
| [org.jetbrains.kotlin.plugin.serialization:org.jetbrains.kotlin.plugin.serialization.gradle.plugin](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/plugin/serialization/org.jetbrains.kotlin.plugin.serialization.gradle.plugin/maven-metadata.xml) | 2.4.20 | 2.4.21 |
| [org.jetbrains.kotlin.multiplatform:org.jetbrains.kotlin.multiplatform.gradle.plugin](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/multiplatform/org.jetbrains.kotlin.multiplatform.gradle.plugin/maven-metadata.xml) | 2.4.20 | 2.4.21 |
| [com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin](https://repo.maven.apache.org/maven2/com/google/devtools/ksp/com.google.devtools.ksp.gradle.plugin/maven-metadata.xml) | 2.3.12 | 2.3.14 |
| [io.insert-koin.compiler.plugin:io.insert-koin.compiler.plugin.gradle.plugin](https://repo.maven.apache.org/maven2/io/insert-koin/compiler/plugin/io.insert-koin.compiler.plugin.gradle.plugin/maven-metadata.xml) | 1.2.3 | 1.2.3 |
| [com.codingfeline.buildkonfig:com.codingfeline.buildkonfig.gradle.plugin](https://repo.maven.apache.org/maven2/com/codingfeline/buildkonfig/com.codingfeline.buildkonfig.gradle.plugin/maven-metadata.xml) | 0.23.0 | 0.24.0 |
| [dev.detekt:dev.detekt.gradle.plugin](https://repo.maven.apache.org/maven2/dev/detekt/dev.detekt.gradle.plugin/maven-metadata.xml) | 2.0.0-alpha.6 | 无 |
| [org.robolectric:robolectric](https://repo.maven.apache.org/maven2/org/robolectric/robolectric/maven-metadata.xml) | 4.17 | 4.18.1 |
| [io.github.takahirom.roborazzi:roborazzi](https://repo.maven.apache.org/maven2/io/github/takahirom/roborazzi/roborazzi/maven-metadata.xml) | 1.78.0 | 1.79.0 |
| [org.jetbrains.compose.ui:ui-test-junit4](https://repo.maven.apache.org/maven2/org/jetbrains/compose/ui/ui-test-junit4/maven-metadata.xml) | 1.12.2 | 1.12.2 |
| [androidx.compose.ui:ui-test-manifest](https://dl.google.com/dl/android/maven2/androidx/compose/ui/ui-test-manifest/maven-metadata.xml) | 1.12.1 | 1.13.0 |
| [org.jetbrains.kotlin:kotlin-test](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-test/maven-metadata.xml) | 2.4.20 | 2.4.21 |
| [androidx.test.uiautomator:uiautomator](https://dl.google.com/dl/android/maven2/androidx/test/uiautomator/uiautomator/maven-metadata.xml) | 2.4.1 | 2.4.1 |
| [androidx.benchmark:benchmark-macro-junit4](https://dl.google.com/dl/android/maven2/androidx/benchmark/benchmark-macro-junit4/maven-metadata.xml) | 1.5.0 | 1.5.0 |
| [junit:junit](https://repo.maven.apache.org/maven2/junit/junit/maven-metadata.xml) | 4.13.2 | 4.13.2 |
| [androidx.test.ext:junit](https://dl.google.com/dl/android/maven2/androidx/test/ext/junit/maven-metadata.xml) | 1.3.0 | 1.3.0 |
| [androidx.test.espresso:espresso-core](https://dl.google.com/dl/android/maven2/androidx/test/espresso/espresso-core/maven-metadata.xml) | 3.7.0 | 3.7.0 |
| [com.squareup.leakcanary:leakcanary-android](https://repo.maven.apache.org/maven2/com/squareup/leakcanary/leakcanary-android/maven-metadata.xml) | 2.14 | 2.14 |
| [androidx.work:work-runtime](https://dl.google.com/dl/android/maven2/androidx/work/work-runtime/maven-metadata.xml) | 2.12.0 | 2.12.0 |
| [androidx.activity:activity-compose](https://dl.google.com/dl/android/maven2/androidx/activity/activity-compose/maven-metadata.xml) | 1.13.0 | 1.13.0 |
| [androidx.lifecycle:lifecycle-runtime-compose](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-runtime-compose/maven-metadata.xml) | 2.11.0 | 2.11.1 |
| [androidx.lifecycle:lifecycle-viewmodel-compose](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-viewmodel-compose/maven-metadata.xml) | 2.11.0 | 2.11.1 |
| [androidx.profileinstaller:profileinstaller](https://dl.google.com/dl/android/maven2/androidx/profileinstaller/profileinstaller/maven-metadata.xml) | 1.4.1 | 1.4.1 |
| [androidx.appcompat:appcompat](https://dl.google.com/dl/android/maven2/androidx/appcompat/appcompat/maven-metadata.xml) | 1.8.0 | 1.8.0 |
| [androidx.core:core](https://dl.google.com/dl/android/maven2/androidx/core/core/maven-metadata.xml) | 1.19.1 | 1.19.1 |
| [androidx.biometric:biometric](https://dl.google.com/dl/android/maven2/androidx/biometric/biometric/maven-metadata.xml) | 1.1.0 | 1.1.0 |
| [androidx.credentials:credentials](https://dl.google.com/dl/android/maven2/androidx/credentials/credentials/maven-metadata.xml) | 1.6.0 | 1.6.0 |
| [androidx.browser:browser](https://dl.google.com/dl/android/maven2/androidx/browser/browser/maven-metadata.xml) | 1.10.0 | 1.10.0 |
| [androidx.navigationevent:navigationevent-compose](https://dl.google.com/dl/android/maven2/androidx/navigationevent/navigationevent-compose/maven-metadata.xml) | 1.2.0 | 1.2.0 |
| [org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose](https://repo.maven.apache.org/maven2/org/jetbrains/androidx/lifecycle/lifecycle-viewmodel-compose/maven-metadata.xml) | 2.11.0 | 2.11.0 |
| [org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose](https://repo.maven.apache.org/maven2/org/jetbrains/androidx/lifecycle/lifecycle-runtime-compose/maven-metadata.xml) | 2.11.0 | 2.11.0 |
| [org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-navigation3](https://repo.maven.apache.org/maven2/org/jetbrains/androidx/lifecycle/lifecycle-viewmodel-navigation3/maven-metadata.xml) | 2.11.0 | 2.11.0 |
| [org.jetbrains.androidx.window:window-core](https://repo.maven.apache.org/maven2/org/jetbrains/androidx/window/window-core/maven-metadata.xml) | 1.5.1 | 1.5.1 |
| [org.jetbrains.androidx.navigation3:navigation3-ui](https://repo.maven.apache.org/maven2/org/jetbrains/androidx/navigation3/navigation3-ui/maven-metadata.xml) | 1.2.0 | 1.2.0 |
| [androidx.room3:room3-runtime](https://dl.google.com/dl/android/maven2/androidx/room3/room3-runtime/maven-metadata.xml) | 3.0.3 | 3.0.3 |
| [androidx.room3:room3-compiler](https://dl.google.com/dl/android/maven2/androidx/room3/room3-compiler/maven-metadata.xml) | 3.0.3 | 3.0.3 |
| [androidx.room3:room3-paging](https://dl.google.com/dl/android/maven2/androidx/room3/room3-paging/maven-metadata.xml) | 3.0.3 | 3.0.3 |
| [androidx.sqlite:sqlite-bundled](https://dl.google.com/dl/android/maven2/androidx/sqlite/sqlite-bundled/maven-metadata.xml) | 2.7.1 | 2.7.1 |
| [androidx.datastore:datastore-preferences](https://dl.google.com/dl/android/maven2/androidx/datastore/datastore-preferences/maven-metadata.xml) | 1.3.0 | 1.3.0 |
| [androidx.paging:paging-compose](https://dl.google.com/dl/android/maven2/androidx/paging/paging-compose/maven-metadata.xml) | 3.5.3 | 3.5.3 |
| [androidx.paging:paging-common](https://dl.google.com/dl/android/maven2/androidx/paging/paging-common/maven-metadata.xml) | 3.5.3 | 3.5.3 |
| [org.jetbrains.compose.material3:material3](https://repo.maven.apache.org/maven2/org/jetbrains/compose/material3/material3/maven-metadata.xml) | 1.12.0-alpha03 | 1.9.0 |
| [org.jetbrains.compose.material3:material3-window-size-class](https://repo.maven.apache.org/maven2/org/jetbrains/compose/material3/material3-window-size-class/maven-metadata.xml) | 1.12.0-alpha03 | 1.9.0 |
| [org.jetbrains.compose.material3:material3-adaptive-navigation-suite](https://repo.maven.apache.org/maven2/org/jetbrains/compose/material3/material3-adaptive-navigation-suite/maven-metadata.xml) | 1.12.0-alpha03 | 1.9.0 |
| [org.jetbrains.compose.material3.adaptive:adaptive](https://repo.maven.apache.org/maven2/org/jetbrains/compose/material3/adaptive/adaptive/maven-metadata.xml) | 1.3.0 | 1.3.0 |
| [org.jetbrains.compose.material3.adaptive:adaptive-layout](https://repo.maven.apache.org/maven2/org/jetbrains/compose/material3/adaptive/adaptive-layout/maven-metadata.xml) | 1.3.0 | 1.3.0 |
| [org.jetbrains.compose.material3.adaptive:adaptive-navigation](https://repo.maven.apache.org/maven2/org/jetbrains/compose/material3/adaptive/adaptive-navigation/maven-metadata.xml) | 1.3.0 | 1.3.0 |
| [org.jetbrains.compose.material3.adaptive:adaptive-navigation3](https://repo.maven.apache.org/maven2/org/jetbrains/compose/material3/adaptive/adaptive-navigation3/maven-metadata.xml) | 1.3.0 | 1.3.0 |
| [org.jetbrains.compose.runtime:runtime](https://repo.maven.apache.org/maven2/org/jetbrains/compose/runtime/runtime/maven-metadata.xml) | 1.12.2 | 1.12.2 |
| [org.jetbrains.compose.foundation:foundation](https://repo.maven.apache.org/maven2/org/jetbrains/compose/foundation/foundation/maven-metadata.xml) | 1.12.2 | 1.12.2 |
| [org.jetbrains.compose.components:components-resources](https://repo.maven.apache.org/maven2/org/jetbrains/compose/components/components-resources/maven-metadata.xml) | 1.12.2 | 1.12.2 |
| [org.jetbrains.compose.ui:ui](https://repo.maven.apache.org/maven2/org/jetbrains/compose/ui/ui/maven-metadata.xml) | 1.12.2 | 1.12.2 |
| [org.jetbrains.compose.ui:ui-tooling-preview](https://repo.maven.apache.org/maven2/org/jetbrains/compose/ui/ui-tooling-preview/maven-metadata.xml) | 1.12.2 | 1.12.2 |
| [org.jetbrains.kotlinx:kotlinx-coroutines-core](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-core/maven-metadata.xml) | 1.11.0 | 1.11.0 |
| [org.jetbrains.kotlinx:kotlinx-coroutines-test](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-test/maven-metadata.xml) | 1.11.0 | 1.11.0 |
| [org.jetbrains.kotlinx:kotlinx-coroutines-android](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-android/maven-metadata.xml) | 1.11.0 | 1.11.0 |
| [org.jetbrains.kotlinx:kotlinx-collections-immutable](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-collections-immutable/maven-metadata.xml) | 0.5.2 | 0.5.2 |
| [org.jetbrains.kotlinx:kotlinx-serialization-core](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-serialization-core/maven-metadata.xml) | 1.13.0 | 1.13.0 |
| [org.jetbrains.kotlinx:kotlinx-serialization-json](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-serialization-json/maven-metadata.xml) | 1.13.0 | 1.13.0 |
| [io.coil-kt.coil3:coil-compose](https://repo.maven.apache.org/maven2/io/coil-kt/coil3/coil-compose/maven-metadata.xml) | 3.6.3 | 3.6.3 |
| [io.coil-kt.coil3:coil-network-ktor3](https://repo.maven.apache.org/maven2/io/coil-kt/coil3/coil-network-ktor3/maven-metadata.xml) | 3.6.3 | 3.6.3 |
| [io.ktor:ktor-bom](https://repo.maven.apache.org/maven2/io/ktor/ktor-bom/maven-metadata.xml) | 3.6.1 | 3.6.1 |
| [io.insert-koin:koin-bom](https://repo.maven.apache.org/maven2/io/insert-koin/koin-bom/maven-metadata.xml) | 4.2.2 | 4.2.2 |
| [me.zhanghai.compose.preference:preference](https://repo.maven.apache.org/maven2/me/zhanghai/compose/preference/preference/maven-metadata.xml) | 2.2.0 | 2.2.0 |
| [com.materialkolor:material-kolor-material3](https://repo.maven.apache.org/maven2/com/materialkolor/material-kolor-material3/maven-metadata.xml) | 6.0.0 | 6.0.0 |
| [com.mohamedrejeb.calf:calf-file-picker](https://repo.maven.apache.org/maven2/com/mohamedrejeb/calf/calf-file-picker/maven-metadata.xml) | 0.14.0 | 0.14.0 |
| [dev.detekt:detekt-rules-ktlint-wrapper](https://repo.maven.apache.org/maven2/dev/detekt/detekt-rules-ktlint-wrapper/maven-metadata.xml) | 2.0.0-alpha.6 | 无 |
| [io.nlopez.compose.rules:detekt](https://repo.maven.apache.org/maven2/io/nlopez/compose/rules/detekt/maven-metadata.xml) | 0.6.8 | 0.6.8 |
| [org.gradle.toolchains.foojay-resolver-convention:org.gradle.toolchains.foojay-resolver-convention.gradle.plugin](https://plugins.gradle.org/m2/org/gradle/toolchains/foojay-resolver-convention/org.gradle.toolchains.foojay-resolver-convention.gradle.plugin/maven-metadata.xml) | 1.0.0 | 1.0.0 |

## 一次性强制升级授权与执行

2026-11-20 维护者确认实施强制升级方案。本次以最新版本及项目实测为目标，允许超出官方兼容表；后续升级恢复原规范。此前章节为初次只读检查结论，后续采用版本与结果以本节为准。长期升级规范不修改。

范围：Android/KMP 应用、构建与测试依赖；独立认证服务不包含。最新正式版优先，Compose 因覆盖新 AndroidX 与 Native 组合的需要评估最新 RC。既有审计、索引和计划为本需求先前改动，不覆盖。初次基线因隔离 worktree 缺少本机配置而配置失败；改为临时 Java 入口只读加载主 checkout 配置，未修改凭据。

### 本次采用版本

- Kotlin 2.4.21、AGP 9.5.1、Gradle 9.9.1（发行 SHA-256 已从官方读取）、KSP 2.3.14、BuildKonfig 0.24.0。
- Robolectric 4.18.1、Roborazzi 1.79.0；Android Lifecycle 2.11.1 与 JetBrains Lifecycle 2.11.0 分键。
- Compose Multiplatform/Material 3 1.13.0-rc01，Android Compose BOM 2026.11.01 与 Navigation3 1.2.2；Android compileSdk 小版本 37.2。Adaptive 暂按最新正式版 1.3.0，若 RC API 必须升级则单独记录。
- 已核对 Compose Runtime/UI/Foundation/Material 3 RC 的 iosArm64、iosSimulatorArm64 共八个 module 工件 HTTP 200。插件 marker 的 `.module` 返回 404（marker 使用 POM，待 Gradle 实际解析），不据此认定插件不存在。
- Gradle 配置 Provider 不读取 init script 后设的 extra/startParameter 属性，改用临时 Java 入口通过子进程环境传递既有本机配置，未将其内容输出到终端或写入仓库。

### 升级前基线（本次实际执行）

使用 JBR 25.0.4、Xcode 27.2、Gradle 9.7.0；命令经临时 Java 入口只读加载主 checkout 本机配置。运行 data/database/composeApp 宿主及 composeApp 模拟器测试，日志 `/private/tmp/notes-force-baseline.log`，XML 副本 `/private/tmp/notes-force-baseline-xml/`。

- data 宿主 12 通过，database 宿主 3 通过；composeApp 模拟器 55 通过，包含真实 Entry 与 Preference 初始化。
- composeApp 宿主在遍历自适应尺寸时主动中断其工作进程以控制基线耗时；输出 XML 为 335 项、24 失败、108 跳过，其余 203 通过。这是部分运行结果，不是完整基线。最终任务因工作进程 SIGTERM/143 失败，不能宣称整批通过。
- 整批耗时 4m51s，613 任务：420 执行、184 来自缓存、9 up-to-date。Python 声明生成器现有 2 项测试和网站现有 16 项测试通过，尚未刷新升级后清单。

### 下载通道诊断

新 Gradle 首次配置在 Google Maven 的 builder-test-api 9.5.1 POM、随后 Central 的 BuildKonfig compiler JAR 上分别发生 TLS 握手中断。用系统 HTTPS 客户端读取前者 HTTP 200。进一步检查发现系统客户端使用本机代理，而 JVM 未自动读取该设置；改用命令级 JVM HTTP/HTTPS proxy 参数，不改系统配置、不关闭 TLS 验证、不降级版本。代理参数和本机配置加载均仅在临时运行入口中，仓库不写入本机代理。

补充：新增 Android Navigation3 坐标复扫发现最新正式版已为 1.2.2，目录目标修正为 1.2.2（此前人工提醒中的 1.2.1 不作为最终上限）；JetBrains Navigation3 仍为 1.2.0。构建约定 BOM 声明使用 `project.dependencies.platform` 显式接收者，避免 Kotlin 源集依赖 DSL 的未解析引用。

### Material 3 弹窗行为变更授权

新 AndroidX 1.5.0 与 JetBrains 1.13.0-rc01 源码中，BasicAlertDialog 直接构造 Dialog，未保留项目使用的 BasicAlertDialogOverride / Scope / Local API。维护者明确选择采用新版默认尺寸，并明确授权删除 common/Android/iOS/Wasm 四个 ProvideAlertDialogWindow 文件及旧 AlertDialogWidthTest。已先更新架构规则，再移除主题 Override，调整应用测试为平台 Insets 与 Material 默认 560dp 上限；按钮、业务内容、关闭行为和正式截图基线不变。Tooltip 新版要求 ExperimentalMaterial3Api，按实际 API 契约显式 opt-in，不抑制编译诊断。

### 已取得的新组合验证证据

- `/private/tmp/notes-force-compile-6.log`：Android 全部 Debug 变体、iOS 设备 ARM64/模拟器 ARM64 Kotlin 编译、弹窗专项 14 项（0 失败/0 跳过）及双端依赖导出通过。53s，816 任务：135 执行、5 缓存、676 up-to-date。
- 实际 Android 图：Compose Runtime/UI/Foundation 1.13.0、Material 3 1.5.0、Navigation3 1.2.2；Native 使用 JetBrains 1.13.0-rc01，Skiko 0.156.1。不是仅凭版本目录推断。
- `/private/tmp/notes-force-data-notices.log`：数据层宿主 data 12 / database 3 通过；Native data 16 通过/1 跳过，database 6 通过。首次导出因 Gradle 新的跨项目配置锁检查失败，已由各 owner 项目任务导出、根任务合并修复，后续导出通过。init script 仅在包含应用模块的主构建注册任务，避开 included build。
- `/private/tmp/notes-force-release-static.log`：Release/R8、lint、Fused Library 与 Baseline Profile 模块打包通过。整批失败于 20 个语义 detekt 任务；日志含 706 条诊断行（并非去重问题总数）。大量诊断位于未修改文件，已改 Theme 中的 CompositionLocal 声明及测试长行亦为原有源码；本次未关闭规则或生成 baseline。
- 声明已从 Android 339 / iOS 236 个解析组件刷新，加资源资产后分别为 341 / 239 个条目、9 份去重 NOTICE。Skiko 官方 `skiko/gradle.properties` 指向 Skia m156-6416302ae5；从该版本 iOS Release ARM64 包保留七份原始许可证，初看 SwiftShader 有差异，逐字比较确认仅为 CRLF/LF 换行差异；七份许可证文字均与现有记录一致，保留现有文本。更新 assets 与 sources，不修改上游文本。刷新后生成器 2 项、网站 Node 16 项测试通过。

### 完整回归与后续定位

- 首轮全项目宿主测试 1151 项：402 通过、72 失败、677 跳过；Native 170 项：168 通过、1 失败、1 跳过。宿主失败集中在 composeApp；其他宿主模块全部通过。日志 `/private/tmp/notes-force-all-tests.log`，完整 XML 保存在 `/private/tmp/notes-force-candidate-xml/`。Native 的一项失败为关于页缓存重复请求 manifest-v2，并非旧路径断言；旧运行库约束对照及最终新组合复跑均 30 项通过，因此记录为未稳定复现，不宣称已修复。
- 自适应超时的语义树停留在登录页面。测试创建的 UserModel 未填写 authOrigin，被已有的 sessionMatchesAuthOrigin 拒绝；参照已有正确用例补齐配置来源，不修改生产校验。生物解锁 UI 测试同样补齐用户来源，并在 Robolectric 测试环境初始化 WorkManager（测试作用域显式依赖）。
- 关于页用例仍等待源码已移除的 Build number，改为验证现行版本信息展开/收起语义，保留返回设置 Tab 的验证。不是删除失败断言或放宽业务规则。
- 补齐夹具后的组合根全量 994 项为 312 通过、5 失败、677 跳过：4 项旧关于页断言和 tabletop-half 的重复 Primary 状态键崩溃。该轮 XML 保存在 `/private/tmp/notes-force-retest-host-xml/`。
- ListDetailPaneBounds 的详情占位和 AnimatedVisibility 退出期间同时调用同一 delegate，重复注册 Primary 状态键。修复使用明确的 MutableTransitionState，保留进入/退出动画，仅在当前及目标均不可见时显示占位内容。最初直接互斥分支导致四项逐帧进入动画断言失败，未保留该实现；不调整动画测试预期。
- Gradle 9.9.1 wrapper 任务已执行，更新 JAR 与 Windows 启动脚本，Unix 脚本内容无变化。保留镜像 URL 与官方发行 SHA-256。
- Wasm npm 锁由 kotlinWasmUpgradePackageLock 刷新。新 Compose 插件将 Skiko 输出从 compose/skiko-runtime-processed-wasmjs 改为 compose/skiko-wasmJs-runtime，网站构建脚本按实际产物路径适配；未复制旧产物或跳过文件校验。
- 普通 detekt 扫描覆盖 theme/preference/email 的手写各平台源码：preference 通过，theme/email 因现有规则诊断失败。语义 detekt 的 706 条日志诊断去重为 511 个位置/规则/信息组合；不能报告静态门禁通过，未扩大本次修改为全仓风格清理。
- 初次截图 verify 选择登录调整尺寸与紧凑导航用例，出现图像差异；正式基线未改。具体最终统计及交付状态见下节。


### 最终交付验证（2026-11-20）

环境为 Android Studio JBR/JDK 25.0.4、Android SDK 37.2、API 37 Robolectric、Xcode 27.2 / ARM64 iOS Simulator；Gradle 串行执行，代理和本机配置均为命令级环境。未安装全局工具、修改系统设置、迁移数据库、更新正式截图基线、公开发布或合入 master。

| 范围 | 最终结果与证据 |
|---|---|
| 全项目宿主行为 | 1151 项：474 通过、0 失败、677 按参数场景跳过 |
| 全项目 Native 行为 | 170 项：169 通过、0 失败、1 跳过（Keychain 环境限制）；真实 Entry/Preference 初始化包含在内 |
| 完整回归命令 | testAndroidHostTest iosSimulatorArm64Test 加组合根语义扫描；日志 /private/tmp/notes-force-final-all.log。整批因 detekt 失败退出，行为任务无失败。1m15s，958 任务，97 执行、861 up-to-date，统计包含同一组合已有结果复用 |
| 窗格专项 | 21 项通过；/private/tmp/notes-force-pane-test.log，逐帧进入、折叠布局及原崩溃用例不放宽断言 |
| 最终 Android | assembleDebug、assembleDevGlobalRelease / R8、lintDevGlobalDebug 通过；/private/tmp/notes-force-final-android.log。1m40s，1014 任务，61 执行、4 缓存、949 up-to-date |
| Xcode 最终整包 | iosApp Debug / generic iOS Simulator 正常签名构建通过；/private/tmp/notes-force-final-xcode.log。未使用禁用签名的绕过参数 |
| iOS 编译 / Fused / Baseline | 双 ARM64 编译、Fused 和 Baseline Profile 模块打包通过；不是性能采样或设备验收 |
| 声明与网站 | 生成器 2 项、Node 16 项通过，最终 buildSite 通过；/private/tmp/notes-force-generator-final-2.log、notes-force-node-final-2.log、notes-force-pane-test.log |
| 视觉 verify | 34 个参数用例，20 项图像比较失败、14 项按场景跳过；/private/tmp/notes-force-final-screenshot-static.log。未更新正式基线。已查看大字号登录图，参考为旧填充输入框、实际为现有描边表单；不把该样本归因扩展到全部差异 |
| 最终语义扫描 | 组合根 Main/HostTest 仍失败；本次新加的状态控制、夹具和断言参数的格式已修正并复扫，无这些新增行诊断，原有规则诊断保留 |
| 设备交互 | 未执行，待 Material 默认弹窗及其他 UI 的 Android/iOS 设备验收 |

最终回归 XML（截图 verify 覆盖结果前）已保存在 /private/tmp/notes-force-final-all-xml/composeApp；其他模块对应 build/test-results/。临时日志和构建目录不是永久归档，结论与条件保留在本文。后续缺口统一见 [待验收清单](../testing/pending-acceptance.md#一次性强制依赖升级2026-11-20)。

本次版本目标已落地，不能将行为测试通过扩展为“所有兼容性验证全绿”。保留官方版本告警（例如 Koin 对 Kotlin 2.4.21 的未验证提示），以实测记录范围为准；后续依赖升级继续遵循原规范。

## 文章检查后的五项 API 迁移（2026-11-20）

维护者在阅读 [Compose 1.13 文章](https://mp.weixin.qq.com/s/21e1ZfNLKlX1j8jjGEtEPA) 的代码审查后要求全部处理，并确认 [具体实施方案](../superpowers/plans/2026-11-20-compose-113-api-migration.md)，包含捕获退出阶段最长约 200ms 保留最后画面。下列工作在既有升级改动基础上实施，没有再次更新依赖。

- AppShell 唯一轻量 derivedStateOf 改为 computedStateOf，连续进度仍在布局阶段读取。
- 测试规则通过 ComposeUiTestConfig / TestFailurePolicy 记录异常；Robolectric 的 PNG 与语义树在根释放前采集，不调用设备专属 UiAutomation 截图。旧规则保留 Unconfined 调度，v2 保留 Standard；失败诊断探针也接入既有 MainLooper 清理。产物位于 composeApp/build/compose-test-failures/，采集错误不覆盖原始失败。
- 资料编辑使用 CapturedAnimatedVisibility 保留原进入/退出曲线，移除 closingEdit；退出立即释放可交互节点，最后画面继续退场。退出像素测试使用 Native 图形和固定宿主视口。
- 登录/注册、资料编辑和邮件草稿迁为 TextFieldState；普通与密码输入分别使用共享 AccountTextField / AccountSecureTextField。密码显式 Hidden/Visible，不使用 System 默认改变原体验。输入对象由页面 ViewModel/内存编辑会话持有，ProfileState.edit 保留 @Transient；MVI 字符串仅为业务快照，不逐字反向驱动字段。关闭/切换账号清空敏感编辑器，新增 SavedStateHandle 回归。
- 六处 FlowRow 改为 FlexBox，保留间距和尾端对齐；列表缓存保持新版默认，不照搬文章缓存比例。

状态式输入框新增内部滚动语义，因此视口测试排除 SetText 节点。自动填充回归使用受控高亮色、实际渲染帧和根图像裁剪，验证填充后出现、编辑后消失、圆角外不着色；颜色比较容差为各通道 0.05，避免硬件色彩转换导致完全相等判断不稳定。正式截图基线未修改。没有性能测量，不把文章中的性能数字当作本项目收益。

### 五项迁移的交付证据

- 最终完整宿主结果 1153 项：476 通过、0 失败、677 按参数场景跳过；Native 170 项：169 通过、0 失败、1 跳过。聚合统计保存在 /private/tmp/notes-api-final-counts.json，截图任务覆盖前的组合根 XML 保存在 /private/tmp/notes-api-final-xml/composeApp/。
- /private/tmp/notes-api-host-final.log 的完整宿主复核成功（58s，441 任务：25 执行、416 up-to-date）；随后 /private/tmp/notes-api-verified.log 对最终输入快照和视口匹配器再跑组合根/资料/邮件宿主与真实 iOS Entry 回归，无行为失败。该批因 detekt 失败退出，1m22s，642 任务：73 执行、569 up-to-date。其他模块复用同一依赖组合结果，不把复用说成全部重新执行。
- /private/tmp/notes-api-final-check.log：最终 Release/R8、lint、网站 buildSite 和定点输入/诊断/资料回归完成，整批仅失败于静态规则任务。933 任务：80 执行、853 up-to-date。新增行的格式问题已修正并复扫；存量复杂度、布局深度和格式等告警未批量重构、未关闭规则或生成 baseline。
- /private/tmp/notes-api-xcode.log：最终 iosApp Debug / generic iOS Simulator 正常签名整包成功（进程退出 0）。Android Debug 已在完整回归批次打包通过；JBR 25.0.4、Xcode 27.2，未修改系统配置。
- /private/tmp/notes-api-candidates-static.log：登录 resize 与 mainDestinationsAndOverlays 共 34 个参数用例完成候选采集，无截图任务失败。已查看大字号登录和搜索历史样本。候选位于 composeApp/build/adaptive-candidates/，正式基线未修改，不能将样本审阅当作全图片/设备验收。
- 网站 Node 16 项和声明生成器 2 项通过，日志为 /private/tmp/notes-api-web-tests.log 与 notes-api-notices-tests.log；未新增分发依赖，不重复改变开源声明。
- 过程中出现完整套件特有的调度问题，新增探针接入 MainLooperDrain 后导航回归恢复；自动填充测试显式推进绘制帧并从根图像裁剪，不更改生产逻辑去迎合像素断言。关于页 Native 缓存测试仍有历史间歇失败记录，最终新组合通过，不宣称该独立问题已被本次修复。

五项实现完成；真实中文 IME、系统自动填充服务、RTL 与正式视觉基线仍须设备/人工验收，统一记录在 [待验收清单](../testing/pending-acceptance.md#compose-113-api-迁移2026-11-20)。本次未提交、合入或公开发布，计划保留用于未验收范围追踪，未额外删除过程文件。

共享输入公开面新增 Foundation 状态类型，core:theme 显式使用 api 传播 Foundation（既有分发依赖未新增版本或组件）。/private/tmp/notes-api-api-contract.log 的 Android 消费方与 iOS 模拟器编译通过，10s，353 任务：41 执行、312 up-to-date。最后文档链接/锚点、围栏和 git diff --check 通过。

## 合入前与最新 master 集成（2026-11-20）

维护者明确要求提交并合入 master。本轮先整理为单一提交，再 rebase 到 24136051（服务端邮件和网页视口功能已在主线）。保留主线的邮件编辑入口、异步详情与网页发布历史；状态式草稿输入接入列表及详情入口，PrepareDraft 批量同步到同一 Snapshot，避免回复/转发正文被旧输入快照覆盖。更新记录保留两边条目，Wasm 锁文件通过官方升级任务重新生成。

主线异步详情加载后，导航测试在恢复测试时钟后等待详情并执行原断言，不删除验证。验证码转换先把 CharSequence 转为 String 再过滤，避免比较不同表示造成有效内容被重复重写。

/private/tmp/notes-merge-final.log：完整组合根宿主、邮件/资料宿主、iOS 模拟器 Kotlin 编译、Android Debug 和网站构建通过，58s，812 任务：71 执行、3 缓存、738 up-to-date。合并基线的其他宿主及 Native 模块在 /private/tmp/notes-merge-validation.log 和 notes-merge-validation-2.log 已执行或复用；这两轮曾因锁文件与上述测试问题失败，后续修复复核通过。Node 生成器与设备视口协议 28 项、开源声明生成器 2 项通过。旧的静态告警、正式基线和设备验收缺口继续保留，未据此宣称所有门禁全绿。

前文“未提交/未合入”描述实现阶段交付时的状态；本节记录维护者后续授权的集成批次。过程计划因仍有未验收事项而保留，不额外删除。

集成批次聚合 XML 统计（含同组合任务复用）：{'iosSimulatorArm64Test': {'tests': 175, 'failures': 0, 'errors': 0, 'skipped': 1}, 'testAndroidHostTest': {'tests': 1168, 'failures': 0, 'errors': 0, 'skipped': 677}}。
