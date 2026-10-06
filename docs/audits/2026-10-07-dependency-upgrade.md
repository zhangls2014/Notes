# 2026-10-07 依赖与工具链升级记录

状态：已按官方上限修正并完成 Android 验证；iOS 缺少 Xcode 26.4，既有检查失败仍保留。最终配置见下方“按官方支持范围收敛”。长期规则见 [升级规范](../dependency-upgrades.md)；本页只记录本次核对，不代表未来最新版本。

## 按官方支持范围收敛（最终决策）

Xander 明确要求以官方支持范围为升级上限，停止通过大量项目试验探索上限外组合。首轮 JDK 27、Gradle 9.8.0、AGP 9.4.1 与 SDK 37.2 选择已取代，下面保留历史证据，不能当成最终配置或验收。

| 项目 | 最终选择 | 官方依据 |
|---|---|---|
| Kotlin / 编译器插件 | 2.4.20 | 最新正式版；Compose 官方兼容说明允许最新 Kotlin |
| Gradle | 9.7.0 | Kotlin 2.4.20 支持范围 7.6.3–9.7.0；AGP 9.3 最低 9.5.0 |
| AGP | 9.3.1 | Kotlin 官方支持上限 9.3.1；取代用户开始时已有的 9.4.1，依据本次新要求 |
| Gradle JDK / toolchain JDK | 已有 JBR 25.0.3 | detekt 最高测试 JDK 25；Gradle 从 9.1 起支持运行 JDK 25 |
| Kotlin / Java 字节码 | 25 | 不高于采用的 JDK，相关任务一致；D8/R8 必要验证另列 |
| Android SDK | compile 37.0、target 37、Build Tools 37.0.0、min 28 | AGP 9.3 支持 API 37，官方 API 工具表明确列出 37.0；未找到 37.2 的明确支持条目，不再探索 |
| Xcode | 26.4 要求，环境缺失 | Kotlin 2.4.20 官方表列出 26.4；本机只有 Xcode 27，不安装或切换全局工具 |
| Swift | 6 模式 | 保留 Swift 6；需在 Xcode 26.4 环境验证，不采用本机 27 的结果替代 |

来源：[Kotlin KMP 兼容表](https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html)、[detekt 表及字段语义](https://detekt.dev/docs/introduction/compatibility/)、[Gradle Java 兼容表](https://docs.gradle.org/current/userguide/compatibility.html)、[AGP 9.3](https://developer.android.com/build/releases/agp-9-3-0-release-notes)、[Android API 工具要求](https://developer.android.com/build/releases/about-agp#api-level-support)、[Compose 兼容说明](https://kotlinlang.org/docs/multiplatform/compose-compatibility-and-versioning.html)。

detekt 明确说明 Gradle/Kotlin/AGP 列是插件自身编译版本，compileOnly 不强制用户版本；这些列不能机械当作最高支持范围。JDK 列则明确是最高测试版本，按 Xander 要求作为上限 25。保留 Kotlin 2.4.20，不仅为与 detekt 编译版本一致而降到 2.4.10。

最终组合验证已执行（日志 `/private/tmp/notes-upgrade-official-validation.log`）：单次串行 Gradle 批次耗时 3m3s，1059 个任务，950 执行、100 缓存、9 up-to-date。批次 exit 1，失败任务仅为 composeApp 既有导航透明度断言、profile 和 about detekt；不能宣称全量通过。

| 验证 | 本次官方范围内组合结果 |
|---|---|
| `help` / 完整 Wrapper 生成 | 通过；Wrapper 生成批次 56s |
| 四个 Android Debug flavor / DevFull Release R8 | 通过 |
| 8 个 Android 宿主测试模块 | 219 通过、1 既有失败、537 跳过 |
| Android lint | 通过 |
| profile / about detekt | 失败；仍为既有 16 / 35 条问题 |
| Fused Library / Baseline Profile 模块 assemble | 通过 |
| 字节码检查 | theme class major 69，即 Java 25 |
| iOS 编译、模拟器测试、Xcode 整包 | 本次未执行：缺少官方表所列 Xcode 26.4；首轮 Xcode 27 结果不能替代 |
| `git diff --check` | 通过 |

完整 Wrapper JAR SHA-256 为 `7a9ce74cff467ca1bf60a4fcd9f05185acceda4d0f382434d393e17864262c5d`，发行包 SHA-256 为 `84fbba45c7f4c64abc77460e1c00f541e9f960e3c7ed2538f1ede19eacd873ae`，分别与官方 [JAR 校验](https://services.gradle.org/distributions/gradle-9.7.0-wrapper.jar.sha256)、[发行包校验](https://services.gradle.org/distributions/gradle-9.7.0-bin.zip.sha256) 一致。Gradle 9.7.0 自身的 [兼容说明](https://docs.gradle.org/9.7.0/userguide/compatibility.html) 也列出了 JDK 25 支持。

本次实际命令（单批次 `--continue` 收集独立结果，不忽略失败）：

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
  ./gradlew help :androidApp:assembleDebug :androidApp:assembleDevFullRelease \
  :composeApp:testAndroidHostTest :core:data:testAndroidHostTest \
  :core:database:testAndroidHostTest :core:theme:testAndroidHostTest \
  :feature:main:testAndroidHostTest :feature:login:testAndroidHostTest \
  :feature:email:testAndroidHostTest :feature:profile:testAndroidHostTest \
  :androidApp:lintDevFullDebug :feature:profile:detekt :feature:about:detekt \
  :android:output:login:assemble :android:baselineprofile:assemble \
  -Dkotlin.daemon.jvmargs=-Xmx8g \
  '-Dorg.gradle.jvmargs=-Xmx8g -Dfile.encoding=UTF-8 -XX:+UseParallelGC' \
  --max-workers=4 --continue
```

项目内此前下载的 JDK 27 仅留作历史缓存，最终构建不使用它；没有删除目录、git 回滚或改变系统配置。Xcode 26.4 环境准备需另行处理，本次未安装全局工具。


## 范围与来源

核对版本目录中全部显式版本的库和插件 marker，共 71 个去重坐标，来源为 Google Maven、Maven Central 和 Gradle Plugin Portal 的 `maven-metadata.xml`。同一坐标对应多个别名时合并记录；BOM 管理的无版本工件继续跟随 BOM，未人为加版本。KSP 的 Google Maven marker 元数据停留在旧版本，因此额外核对 Maven Central、Plugin Portal 和官方发布说明。

既有未提交改动包括 AGP 9.3.2 → 9.4.1、Roborazzi 1.75.0 → 1.76.0、DataStore alpha10 → alpha11、Baseline Profile rc02 → 1.5.0、Macrobenchmark 1.4.1 → 1.5.0。除按正式版优先重新选择 DataStore、按官方支持上限重新选择 AGP 外，保留这些版本。

## 首轮工具链选择（已由上方收敛方案取代）

| 项目 | 升级前 | 采用版本 | 原因 / 限制 |
|---|---|---|---|
| Gradle | 9.7.1 | 9.8.0 | 官方当前正式版；Wrapper 镜像来源沿用现有配置 |
| Gradle / toolchain JDK | 21 | 27（项目内 Oracle JDK 27+35） | 官方正式版；使用忽略的构建目录验证，不安装全局 JDK。detekt 官方最高测试 JDK 25，27 需项目实测 |
| Kotlin / Java 字节码目标 | 21 | 26 | Kotlin 当前支持最高 target 26；与 Java 任务一致，D8/R8 已通过，宿主测试见下方结果；已拆分 jdk 和 jvm-target |
| Kotlin | 2.4.20 | 2.4.20 | 最新正式版；不采用 2.5.0-Beta1 |
| AGP | 工作区 9.4.1 | 9.4.1 | 最新正式版；不采用 9.5 alpha |
| Android SDK | compile 37.0、target 37、Build Tools 37.0.0、min 28 | compile 37.2，其余保留 | Google SDK 正式发布清单与已安装平台确认；minor 显式配置，min 为支持范围 |
| Xcode / Swift | 本机 Xcode 27.0，Swift 5 模式 | 使用现有 Xcode，Swift 6 模式 | 本机编译器为 Swift 6.4；iOS deployment target 保留支持范围，整包构建验证语言模式 |

Kotlin 2.4.20 官方兼容表列出的上限是 Gradle 9.7.0、AGP 9.3.1，Xcode 为 26.4。采用组合超出该表；下方项目实测不能视为官方已认证。detekt 表中的 JDK 25 是最高测试版本，不是已证明 JDK 27 不能运行。

官方来源：[Gradle 版本](https://docs.gradle.org/current/release-notes.html)、[Gradle Java 兼容表](https://docs.gradle.org/current/userguide/compatibility.html)、[Kotlin KMP 兼容表](https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html)、[detekt 兼容表](https://detekt.dev/docs/introduction/compatibility/)、[AGP 9.4](https://developer.android.com/build/releases/agp-9-4-0-release-notes)。

## 全量版本决策

| 坐标 / 别名 | 开始时版本 | 最新正式版 | 采用版本 | 选择理由 |
|---|---|---|---|---|
| [`org.robolectric:robolectric`](https://repo.maven.apache.org/maven2/org/robolectric/robolectric/maven-metadata.xml)<br>robolectric | 4.17 | 4.17 | 4.17 | 最新正式版，保留 |
| [`io.github.takahirom.roborazzi:roborazzi`](https://repo.maven.apache.org/maven2/io/github/takahirom/roborazzi/roborazzi/maven-metadata.xml)<br>roborazzi | 1.76.0 | 1.76.0 | 1.76.0 | 最新正式版，保留 |
| [`org.jetbrains.compose.ui:ui-test-junit4`](https://repo.maven.apache.org/maven2/org/jetbrains/compose/ui/ui-test-junit4/maven-metadata.xml)<br>jetbrains-compose-ui-test-junit4 | 1.12.1 | 1.12.1 | 1.12.1 | 最新正式版，保留 |
| [`org.jetbrains.kotlin:kotlin-test`](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-test/maven-metadata.xml)<br>kotlin-test | 2.4.20 | 2.4.20 | 2.4.20 | 最新正式版，保留 |
| [`androidx.test.uiautomator:uiautomator`](https://dl.google.com/dl/android/maven2/androidx/test/uiautomator/uiautomator/maven-metadata.xml)<br>androidx-test-uiautomator | 2.4.0 | 2.4.0 | 2.4.0 | 最新正式版，保留 |
| [`androidx.benchmark:benchmark-macro-junit4`](https://dl.google.com/dl/android/maven2/androidx/benchmark/benchmark-macro-junit4/maven-metadata.xml)<br>androidx-benchmark-macro-junit4 | 1.5.0 | 1.5.0 | 1.5.0 | 最新正式版，保留 |
| [`junit:junit`](https://repo.maven.apache.org/maven2/junit/junit/maven-metadata.xml)<br>junit | 4.13.2 | 4.13.2 | 4.13.2 | 最新正式版，保留 |
| [`androidx.test.ext:junit`](https://dl.google.com/dl/android/maven2/androidx/test/ext/junit/maven-metadata.xml)<br>androidx-test-ext-junit | 1.3.0 | 1.3.0 | 1.3.0 | 最新正式版，保留 |
| [`androidx.test.espresso:espresso-core`](https://dl.google.com/dl/android/maven2/androidx/test/espresso/espresso-core/maven-metadata.xml)<br>androidx-test-espresso | 3.7.0 | 3.7.0 | 3.7.0 | 最新正式版，保留 |
| [`com.squareup.leakcanary:leakcanary-android`](https://repo.maven.apache.org/maven2/com/squareup/leakcanary/leakcanary-android/maven-metadata.xml)<br>squareup-leakCanary | 2.14 | 2.14 | 2.14 | 最新正式版，保留 |
| [`androidx.activity:activity-compose`](https://dl.google.com/dl/android/maven2/androidx/activity/activity-compose/maven-metadata.xml)<br>androidx-activity-compose | 1.13.0 | 1.13.0 | 1.13.0 | 最新正式版，保留 |
| [`androidx.lifecycle:lifecycle-runtime-compose`](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-runtime-compose/maven-metadata.xml)<br>androidx-lifecycle-compose | 2.11.0 | 2.11.0 | 2.11.0 | 最新正式版，保留 |
| [`androidx.lifecycle:lifecycle-viewmodel-compose`](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-viewmodel-compose/maven-metadata.xml)<br>androidx-viewmodel-compose | 2.11.0 | 2.11.0 | 2.11.0 | 最新正式版，保留 |
| [`androidx.profileinstaller:profileinstaller`](https://dl.google.com/dl/android/maven2/androidx/profileinstaller/profileinstaller/maven-metadata.xml)<br>androidx-profileinstaller | 1.4.1 | 1.4.1 | 1.4.1 | 最新正式版，保留 |
| [`androidx.appcompat:appcompat`](https://dl.google.com/dl/android/maven2/androidx/appcompat/appcompat/maven-metadata.xml)<br>androidx-appcompat | 1.8.0 | 1.8.0 | 1.8.0 | 最新正式版，保留 |
| [`androidx.core:core`](https://dl.google.com/dl/android/maven2/androidx/core/core/maven-metadata.xml)<br>androidx-core | 1.19.1 | 1.19.1 | 1.19.1 | 最新正式版，保留 |
| [`androidx.navigationevent:navigationevent-compose`](https://dl.google.com/dl/android/maven2/androidx/navigationevent/navigationevent-compose/maven-metadata.xml)<br>androidx-navigationevent-compose | 1.1.2 | 1.1.2 | 1.1.2 | 最新正式版，保留 |
| [`org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose`](https://repo.maven.apache.org/maven2/org/jetbrains/androidx/lifecycle/lifecycle-viewmodel-compose/maven-metadata.xml)<br>androidx-lifecycle-viewmodel-compose | 2.11.0 | 2.11.0 | 2.11.0 | 最新正式版，保留 |
| [`org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose`](https://repo.maven.apache.org/maven2/org/jetbrains/androidx/lifecycle/lifecycle-runtime-compose/maven-metadata.xml)<br>androidx-lifecycle-runtime-compose | 2.11.0 | 2.11.0 | 2.11.0 | 最新正式版，保留 |
| [`org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-navigation3`](https://repo.maven.apache.org/maven2/org/jetbrains/androidx/lifecycle/lifecycle-viewmodel-navigation3/maven-metadata.xml)<br>androidx-lifecycle-viewmodel-navigation3 | 2.11.0 | 2.11.0 | 2.11.0 | 最新正式版，保留 |
| [`org.jetbrains.androidx.window:window-core`](https://repo.maven.apache.org/maven2/org/jetbrains/androidx/window/window-core/maven-metadata.xml)<br>androidx-window-core | 1.5.1 | 1.5.1 | 1.5.1 | 最新正式版，保留 |
| [`org.jetbrains.androidx.navigation3:navigation3-ui`](https://repo.maven.apache.org/maven2/org/jetbrains/androidx/navigation3/navigation3-ui/maven-metadata.xml)<br>jetbrains-navigation3-ui | 1.1.2 | 1.1.2 | 1.1.2 | 最新正式版，保留 |
| [`androidx.room3:room3-runtime`](https://dl.google.com/dl/android/maven2/androidx/room3/room3-runtime/maven-metadata.xml)<br>androidx-room-runtime | 3.0.3 | 3.0.3 | 3.0.3 | 最新正式版，保留 |
| [`androidx.room3:room3-compiler`](https://dl.google.com/dl/android/maven2/androidx/room3/room3-compiler/maven-metadata.xml)<br>androidx-room-compiler | 3.0.3 | 3.0.3 | 3.0.3 | 最新正式版，保留 |
| [`androidx.room3:room3-paging`](https://dl.google.com/dl/android/maven2/androidx/room3/room3-paging/maven-metadata.xml)<br>androidx-room-paging | 3.0.3 | 3.0.3 | 3.0.3 | 最新正式版，保留 |
| [`androidx.sqlite:sqlite-bundled`](https://dl.google.com/dl/android/maven2/androidx/sqlite/sqlite-bundled/maven-metadata.xml)<br>androidx-sqlite-bundled | 2.7.1 | 2.7.1 | 2.7.1 | 最新正式版，保留 |
| [`androidx.datastore:datastore-preferences`](https://dl.google.com/dl/android/maven2/androidx/datastore/datastore-preferences/maven-metadata.xml)<br>androidx-datastore-preferences | 1.3.0-alpha11 | 1.2.1 | 1.2.1 | 切换正式版；项目未使用 1.3 Builder、Web 或加密 API，数据回归已通过 |
| [`androidx.paging:paging-compose`](https://dl.google.com/dl/android/maven2/androidx/paging/paging-compose/maven-metadata.xml)<br>androidx-paging-compose | 3.5.1 | 3.5.1 | 3.5.1 | 最新正式版，保留 |
| [`androidx.paging:paging-common`](https://dl.google.com/dl/android/maven2/androidx/paging/paging-common/maven-metadata.xml)<br>androidx-paging-common | 3.5.1 | 3.5.1 | 3.5.1 | 最新正式版，保留 |
| [`org.jetbrains.compose.material3:material3`](https://repo.maven.apache.org/maven2/org/jetbrains/compose/material3/material3/maven-metadata.xml)<br>jetbrains-compose-material3 | 1.12.0-alpha03 | 1.9.0 | 1.12.0-alpha03 | 保留预览版：现有 DropdownMenuPopup 等 API（正式版 1.9.0 源码不存在）；遵循 Compose 1.12.1 发布清单，不追 1.13 alpha |
| [`org.jetbrains.compose.material3:material3-window-size-class`](https://repo.maven.apache.org/maven2/org/jetbrains/compose/material3/material3-window-size-class/maven-metadata.xml)<br>jetbrains-compose-material3-window-size | 1.12.0-alpha03 | 1.9.0 | 1.12.0-alpha03 | 保留预览版：现有 DropdownMenuPopup 等 API（正式版 1.9.0 源码不存在）；遵循 Compose 1.12.1 发布清单，不追 1.13 alpha |
| [`org.jetbrains.compose.material3:material3-adaptive-navigation-suite`](https://repo.maven.apache.org/maven2/org/jetbrains/compose/material3/material3-adaptive-navigation-suite/maven-metadata.xml)<br>jetbrains-compose-material3-adaptive-navigation-suite | 1.12.0-alpha03 | 1.9.0 | 1.12.0-alpha03 | 跟随当前 Material 3 预览系列，避免混用不同发布系列 |
| [`org.jetbrains.compose.material3.adaptive:adaptive`](https://repo.maven.apache.org/maven2/org/jetbrains/compose/material3/adaptive/adaptive/maven-metadata.xml)<br>jetbrains-compose-material3-adaptive | 1.3.0-rc01 | 1.3.0 | 1.3.0 | 更新为最新正式版，回归结果见下方 |
| [`org.jetbrains.compose.material3.adaptive:adaptive-layout`](https://repo.maven.apache.org/maven2/org/jetbrains/compose/material3/adaptive/adaptive-layout/maven-metadata.xml)<br>jetbrains-compose-material3-adaptive-layout | 1.3.0-rc01 | 1.3.0 | 1.3.0 | 更新为最新正式版，回归结果见下方 |
| [`org.jetbrains.compose.material3.adaptive:adaptive-navigation`](https://repo.maven.apache.org/maven2/org/jetbrains/compose/material3/adaptive/adaptive-navigation/maven-metadata.xml)<br>jetbrains-compose-material3-adaptive-navigation | 1.3.0-rc01 | 1.3.0 | 1.3.0 | 更新为最新正式版，回归结果见下方 |
| [`org.jetbrains.compose.material3.adaptive:adaptive-navigation3`](https://repo.maven.apache.org/maven2/org/jetbrains/compose/material3/adaptive/adaptive-navigation3/maven-metadata.xml)<br>jetbrains-compose-material3-adaptive-navigation3 | 1.3.0-rc01 | 1.3.0 | 1.3.0 | 更新为最新正式版，回归结果见下方 |
| [`org.jetbrains.compose.runtime:runtime`](https://repo.maven.apache.org/maven2/org/jetbrains/compose/runtime/runtime/maven-metadata.xml)<br>jetbrains-compose-runtime | 1.12.1 | 1.12.1 | 1.12.1 | 最新正式版，保留 |
| [`org.jetbrains.compose.foundation:foundation`](https://repo.maven.apache.org/maven2/org/jetbrains/compose/foundation/foundation/maven-metadata.xml)<br>jetbrains-compose-foundation | 1.12.1 | 1.12.1 | 1.12.1 | 最新正式版，保留 |
| [`org.jetbrains.compose.components:components-resources`](https://repo.maven.apache.org/maven2/org/jetbrains/compose/components/components-resources/maven-metadata.xml)<br>jetbrains-compose-components-resources | 1.12.1 | 1.12.1 | 1.12.1 | 最新正式版，保留 |
| [`org.jetbrains.compose.ui:ui`](https://repo.maven.apache.org/maven2/org/jetbrains/compose/ui/ui/maven-metadata.xml)<br>jetbrains-compose-ui | 1.12.1 | 1.12.1 | 1.12.1 | 最新正式版，保留 |
| [`org.jetbrains.compose.ui:ui-tooling-preview`](https://repo.maven.apache.org/maven2/org/jetbrains/compose/ui/ui-tooling-preview/maven-metadata.xml)<br>jetbrains-compose-ui-tooling-preview | 1.12.1 | 1.12.1 | 1.12.1 | 最新正式版，保留 |
| [`org.jetbrains.kotlinx:kotlinx-coroutines-core`](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-core/maven-metadata.xml)<br>jetbrains-kotlinx-coroutines-core | 1.11.0 | 1.11.0 | 1.11.0 | 最新正式版，保留 |
| [`org.jetbrains.kotlinx:kotlinx-coroutines-test`](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-test/maven-metadata.xml)<br>jetbrains-kotlinx-coroutines-test | 1.11.0 | 1.11.0 | 1.11.0 | 最新正式版，保留 |
| [`org.jetbrains.kotlinx:kotlinx-coroutines-android`](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-android/maven-metadata.xml)<br>jetbrains-kotlinx-coroutines-android | 1.11.0 | 1.11.0 | 1.11.0 | 最新正式版，保留 |
| [`org.jetbrains.kotlinx:kotlinx-collections-immutable`](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-collections-immutable/maven-metadata.xml)<br>jetbrains-kotlinx-collections-immutable | 0.5.2 | 0.5.2 | 0.5.2 | 最新正式版，保留 |
| [`org.jetbrains.kotlinx:kotlinx-serialization-core`](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-serialization-core/maven-metadata.xml)<br>jetbrains-kotlinx-serialization-core | 1.11.0 | 1.11.0 | 1.11.0 | 最新正式版，保留 |
| [`org.jetbrains.kotlinx:kotlinx-serialization-json`](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-serialization-json/maven-metadata.xml)<br>jetbrains-kotlinx-serialization-json | 1.11.0 | 1.11.0 | 1.11.0 | 最新正式版，保留 |
| [`io.coil-kt.coil3:coil-compose`](https://repo.maven.apache.org/maven2/io/coil-kt/coil3/coil-compose/maven-metadata.xml)<br>coil-compose | 3.6.3 | 3.6.3 | 3.6.3 | 最新正式版，保留 |
| [`io.coil-kt.coil3:coil-network-ktor3`](https://repo.maven.apache.org/maven2/io/coil-kt/coil3/coil-network-ktor3/maven-metadata.xml)<br>coil-network-ktor | 3.6.3 | 3.6.3 | 3.6.3 | 最新正式版，保留 |
| [`io.ktor:ktor-bom`](https://repo.maven.apache.org/maven2/io/ktor/ktor-bom/maven-metadata.xml)<br>ktor-bom | 3.6.0 | 3.6.0 | 3.6.0 | 最新正式版，保留 |
| [`io.insert-koin:koin-bom`](https://repo.maven.apache.org/maven2/io/insert-koin/koin-bom/maven-metadata.xml)<br>koin-bom | 4.2.2 | 4.2.2 | 4.2.2 | 最新正式版，保留 |
| [`me.zhanghai.compose.preference:preference`](https://repo.maven.apache.org/maven2/me/zhanghai/compose/preference/preference/maven-metadata.xml)<br>compose-preference | 2.2.0 | 2.2.0 | 2.2.0 | 最新正式版，保留 |
| [`com.mohamedrejeb.calf:calf-file-picker`](https://repo.maven.apache.org/maven2/com/mohamedrejeb/calf/calf-file-picker/maven-metadata.xml)<br>calf-file-picker | 0.14.0 | 0.14.0 | 0.14.0 | 最新正式版，保留 |
| [`dev.detekt:detekt-rules-ktlint-wrapper`](https://repo.maven.apache.org/maven2/dev/detekt/detekt-rules-ktlint-wrapper/maven-metadata.xml)<br>detekt-formatting | 2.0.0-alpha.6 | 无（当前坐标） | 2.0.0-alpha.6 | 保留 2.x 预览版：当前 dev.detekt 插件/API 与 Kotlin 2.4 构建链；1.23.8 的官方编译/测试链较旧，不回迁插件和规则配置 |
| [`org.jetbrains.kotlin.multiplatform:org.jetbrains.kotlin.multiplatform.gradle.plugin`](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/multiplatform/org.jetbrains.kotlin.multiplatform.gradle.plugin/maven-metadata.xml)<br>kotlin-gradle-plugin, jetbrains-kotlin-multiplatform | 2.4.20 | 2.4.20 | 2.4.20 | 最新正式版，保留 |
| [`org.jetbrains.kotlin.plugin.serialization:org.jetbrains.kotlin.plugin.serialization.gradle.plugin`](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/plugin/serialization/org.jetbrains.kotlin.plugin.serialization.gradle.plugin/maven-metadata.xml)<br>kotlin-serialization-plugin, jetbrains-kotlin-serialization | 2.4.20 | 2.4.20 | 2.4.20 | 最新正式版，保留 |
| [`org.jetbrains.kotlin.plugin.compose:org.jetbrains.kotlin.plugin.compose.gradle.plugin`](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/plugin/compose/org.jetbrains.kotlin.plugin.compose.gradle.plugin/maven-metadata.xml)<br>kotlin-compose-plugin, jetbrains-kotlin-compose-compiler | 2.4.20 | 2.4.20 | 2.4.20 | 最新正式版，保留 |
| [`com.android.kotlin.multiplatform.library:com.android.kotlin.multiplatform.library.gradle.plugin`](https://dl.google.com/dl/android/maven2/com/android/kotlin/multiplatform/library/com.android.kotlin.multiplatform.library.gradle.plugin/maven-metadata.xml)<br>android-gradle-plugin, android-kmp-library | 9.4.1 | 9.4.1 | 9.3.1 | 按 Kotlin 官方支持上限收敛 |
| [`org.jetbrains.compose:org.jetbrains.compose.gradle.plugin`](https://repo.maven.apache.org/maven2/org/jetbrains/compose/org.jetbrains.compose.gradle.plugin/maven-metadata.xml)<br>compose-gradle-plugin, jetbrains-compose | 1.12.1 | 1.12.1 | 1.12.1 | 最新正式版，保留 |
| [`com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin`](https://repo.maven.apache.org/maven2/com/google/devtools/ksp/com.google.devtools.ksp.gradle.plugin/maven-metadata.xml)<br>ksp-gradle-plugin, google-ksp | 2.3.11 | 2.3.12 | 2.3.12 | 更新为最新正式版，回归结果见下方 |
| [`io.insert-koin.compiler.plugin:io.insert-koin.compiler.plugin.gradle.plugin`](https://repo.maven.apache.org/maven2/io/insert-koin/compiler/plugin/io.insert-koin.compiler.plugin.gradle.plugin/maven-metadata.xml)<br>koin-compiler-gradle-plugin, koin-compiler | 1.2.1 | 1.2.1 | 1.2.1 | 最新正式版，保留 |
| [`dev.detekt:dev.detekt.gradle.plugin`](https://repo.maven.apache.org/maven2/dev/detekt/dev.detekt.gradle.plugin/maven-metadata.xml)<br>detekt-gradle-plugin, detekt | 2.0.0-alpha.6 | 无（当前坐标） | 2.0.0-alpha.6 | 保留 2.x 预览版：当前 dev.detekt 插件/API 与 Kotlin 2.4 构建链；1.23.8 的官方编译/测试链较旧，不回迁插件和规则配置 |
| [`io.github.takahirom.roborazzi:io.github.takahirom.roborazzi.gradle.plugin`](https://repo.maven.apache.org/maven2/io/github/takahirom/roborazzi/io.github.takahirom.roborazzi.gradle.plugin/maven-metadata.xml)<br>roborazzi | 1.76.0 | 1.76.0 | 1.76.0 | 最新正式版，保留 |
| [`com.android.application:com.android.application.gradle.plugin`](https://dl.google.com/dl/android/maven2/com/android/application/com.android.application.gradle.plugin/maven-metadata.xml)<br>android-application | 9.4.1 | 9.4.1 | 9.3.1 | 按 Kotlin 官方支持上限收敛 |
| [`com.android.lint:com.android.lint.gradle.plugin`](https://dl.google.com/dl/android/maven2/com/android/lint/com.android.lint.gradle.plugin/maven-metadata.xml)<br>android-lint | 9.4.1 | 9.4.1 | 9.3.1 | 按 Kotlin 官方支持上限收敛 |
| [`com.android.library:com.android.library.gradle.plugin`](https://dl.google.com/dl/android/maven2/com/android/library/com.android.library.gradle.plugin/maven-metadata.xml)<br>android-library | 9.4.1 | 9.4.1 | 9.3.1 | 按 Kotlin 官方支持上限收敛 |
| [`com.android.fused-library:com.android.fused-library.gradle.plugin`](https://dl.google.com/dl/android/maven2/com/android/fused-library/com.android.fused-library.gradle.plugin/maven-metadata.xml)<br>android-fusedlibrary | 9.4.1 | 9.4.1 | 9.3.1 | 按 Kotlin 官方支持上限收敛 |
| [`com.android.test:com.android.test.gradle.plugin`](https://dl.google.com/dl/android/maven2/com/android/test/com.android.test.gradle.plugin/maven-metadata.xml)<br>android-test | 9.4.1 | 9.4.1 | 9.3.1 | 按 Kotlin 官方支持上限收敛 |
| [`androidx.room3:androidx.room3.gradle.plugin`](https://dl.google.com/dl/android/maven2/androidx/room3/androidx.room3.gradle.plugin/maven-metadata.xml)<br>androidx-room | 3.0.3 | 3.0.3 | 3.0.3 | 最新正式版，保留 |
| [`androidx.baselineprofile:androidx.baselineprofile.gradle.plugin`](https://dl.google.com/dl/android/maven2/androidx/baselineprofile/androidx.baselineprofile.gradle.plugin/maven-metadata.xml)<br>androidx-baselineprofile | 1.5.0 | 1.5.0 | 1.5.0 | 最新正式版，保留 |
| [`com.codingfeline.buildkonfig:com.codingfeline.buildkonfig.gradle.plugin`](https://repo.maven.apache.org/maven2/com/codingfeline/buildkonfig/com.codingfeline.buildkonfig.gradle.plugin/maven-metadata.xml)<br>buildKonfig | 0.23.0 | 0.23.0 | 0.23.0 | 最新正式版，保留 |

Material 3 / Adaptive 发布清单：[Compose 官方发布页](https://github.com/JetBrains/compose-multiplatform/releases)。detekt 1.23.8 与 2.x 是不同插件/API，不以删掉分析规则或绕过检查换取正式版后缀。

## 首轮验证（历史证据，不代表最终组合）

升级前 `./gradlew help`：通过（Gradle 9.7.1，JBR 21）。沙箱内首次失败在 Wrapper 缓存锁权限；获得命令沙箱授权后配置检查通过，属于执行环境限制。

首轮 JBR 25 / target 25 的 Android+iOS 全量构建失败：11 个任务失败，KSP class-structure transform 和 D8 报 `Java heap space`，不能视为版本不兼容。按仓库规定使用 8GB 堆并限制 4 个 worker，开始 JDK 27 / target 26 验证。

项目内 JDK：`.gradle/jdks/jdk-27.jdk/Contents/Home`，Oracle JDK 27+35-2325。下载的 macOS aarch64 archive SHA-256 与 [官方校验文件](https://download.oracle.com/java/27/latest/jdk-27_macos-aarch64_bin.tar.gz.sha256) 一致：`6cb6d94d1bacb75d31357212cdd28e601fe15588fcb00c15cbe8b7c2be68990c`。

JDK 27 / target 26 的 `help :androidApp:assembleDebug :composeApp:compileKotlinIosSimulatorArm64` 已通过（51s，648 个任务：171 执行、6 缓存、471 up-to-date）。抽查数据、主题和邮件 class 文件的 major version 为 70（Java 26）。

Android 扩展验证：Release R8、lint、Baseline Profile 模块和 7 个其他宿主测试模块通过；composeApp 全量为 694 个用例，156 通过、1 失败、537 跳过。其他 7 模块共 63 个用例通过。首次 Fused 与两个 detekt 任务失败，整批 exit 1，不能写成全量通过。

对照副本 `/private/tmp/notes-upgrade-baseline-20261007` 使用相同已提交源码与任务开始时的工作区依赖版本（Gradle 9.7.1、JDK/target 21、DataStore alpha11、Adaptive rc01、KSP 2.3.11、AGP 9.4.1）。原仓库没有 git 回滚。基线独立复现 `DeviceCornerOpacityTest.ordinaryPushAndPopSwitchWithoutMotionOrBlending` 的 `Page stays fully opaque (push)` 断言；profile 16 条、about 35 条 detekt 问题与升级后逐条一致。保留现有检查与断言，不修改无关源码或正式截图基线。

Fused 校验在升级前同样失败：缺少 `:feature:login-api` 的 include；补入后校验进一步报告缺少 `:core:preference`（它依赖已包含的 theme）。对照实际模块依赖与 [官方 Fused 文档](https://developer.android.com/build/publish-library/fused-library) 补齐这两项，未关闭校验。最终 `:android:output:login:assemble :android:output:login:report` 通过（3s，172 个任务：20 执行、152 up-to-date）。

iOS 模拟器回归已通过（26s，274 个任务：150 执行、124 up-to-date）：composeApp 29、core:data 8、core:database 6、core:framework 6 个用例通过，core:data 1 个既有 Keychain 用例跳过。Entry 实际调用和 Preference 初始化包含在 composeApp 回归中。

进一步核对 [Google SDK 发布清单](https://dl.google.com/android/repository/repository2-3.xml)，采用正式平台 `platforms;android-37.2` 和正式 Build Tools 37.0.0（过滤 beta/rc 包名）。KMP/Android 宿主/Baseline Profile 统一使用 `release(37) { minorApiLevel = 2 }`。首次命名 `android-compileSdk-minor` 与现有 `android-compileSdk` 形成 accessor 层级碰撞，配置检查失败；改为 `android-compileSdkMinor` 后重新验证，没有绕过检查。

最终 Android 验证使用完整 Wrapper、SDK 37.2、JDK 27 / target 26：Debug 四个 flavor、DevFull Release R8、lint、Baseline Profile 模块和 7 个其他宿主模块通过；composeApp 同样为 156 通过、1 失败、537 跳过，合计 219 通过、1 失败、537 跳过。批次耗时 1m19s，1055 个任务：935 执行、103 缓存、17 up-to-date；批次 exit 1，包括上述既有测试与后续已修复的 Fused 校验失败。Fused 修改仅影响独立输出配置，随后单独打包通过。

完整 Gradle Wrapper 已由 `wrapper` 任务生成；发行包 SHA-256 为 `bafd5ce9cfaea0fbccfdc8439a1ac42fbd4cd9c89dc9a988228d8a2639a58e6c`，Wrapper JAR SHA-256 为 `238e777fcddd7e34f9708186085def2abd6e08e658505b38718d79d74c21abd5`，分别与 [官方发行包校验](https://services.gradle.org/distributions/gradle-9.8.0-bin.zip.sha256)、[官方 Wrapper 校验](https://services.gradle.org/distributions/gradle-9.8.0-wrapper.jar.sha256) 一致。

Xcode 27 / Swift 6 模拟器整包构建通过（`** BUILD SUCCEEDED **`），其中 Gradle 框架编译/链接也通过（28s）。使用 `generic/platform=iOS Simulator`，验证包含 arm64 Swift 编译及共享框架，不需要签名。iOS deployment target 保留原值。

| 验证 | 最终状态 |
|---|---|
| Gradle 配置 / Kotlin 生成代码 | 通过 |
| Android 四个 Debug flavor / DevFull Release R8 | 通过 |
| Android 宿主测试 | 219 通过、1 既有失败、537 跳过 |
| iOS Simulator 测试 | 49 通过、1 既有 Keychain 跳过 |
| iOS Kotlin 编译 / Xcode Swift 6 整包 | 通过 |
| Android lint | 通过 |
| profile / about detekt | 失败：16 / 35 条既有问题，基线逐条一致 |
| Fused Library assemble / report | 补齐 include 后通过 |
| Baseline Profile 模块 assemble | 通过；未生成设备 Profile |
| `git diff --check` / JDK 忽略目录 | 通过；本地 JDK 不进入版本控制 |

Gradle 提示现有插件/配置使用了将在 Gradle 10 移除的 API；Robolectric 在 JDK 27 上有 native-access 提示。当前检查均保留，未添加绕过标记，这些提示不是本次编译失败。

### 可复用命令

下列 Gradle 命令均使用项目内 JDK，并按 OOM 规则设置 Gradle/Kotlin daemon 为 8GB 堆、4 个 worker；各批次串行执行：

```bash
export JAVA_HOME="$PWD/.gradle/jdks/jdk-27.jdk/Contents/Home"
./gradlew :androidApp:assembleDebug :androidApp:assembleDevFullRelease \
  :composeApp:testAndroidHostTest :core:data:testAndroidHostTest \
  :core:database:testAndroidHostTest :core:theme:testAndroidHostTest \
  :feature:main:testAndroidHostTest :feature:login:testAndroidHostTest \
  :feature:email:testAndroidHostTest :feature:profile:testAndroidHostTest \
  :androidApp:lintDevFullDebug :android:output:login:assemble \
  :android:baselineprofile:assemble --continue --max-workers=4 \
  -Dkotlin.daemon.jvmargs=-Xmx8g \
  '-Dorg.gradle.jvmargs=-Xmx8g -Dfile.encoding=UTF-8 -XX:+UseParallelGC'
./gradlew :composeApp:iosSimulatorArm64Test :core:data:iosSimulatorArm64Test \
  :core:database:iosSimulatorArm64Test :core:framework:iosSimulatorArm64Test \
  --max-workers=4 -Dkotlin.daemon.jvmargs=-Xmx8g \
  '-Dorg.gradle.jvmargs=-Xmx8g -Dfile.encoding=UTF-8 -XX:+UseParallelGC'
./gradlew :feature:profile:detekt :feature:about:detekt --continue
./gradlew :android:output:login:assemble :android:output:login:report
GRADLE_OPTS='-Dorg.gradle.jvmargs="-Xmx8g -Dfile.encoding=UTF-8 -XX:+UseParallelGC" -Dkotlin.daemon.jvmargs=-Xmx8g' \
  xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -configuration Debug -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' \
  -derivedDataPath /private/tmp/notes-upgrade-xcode-derived-data \
  CODE_SIGNING_ALLOWED=NO build
```

本机原始日志在 `/private/tmp/notes-upgrade-final-android.log`、`notes-upgrade-ios-validation.log`、`notes-upgrade-xcode-build.log`、`notes-upgrade-fused-final.log`、`notes-upgrade-baseline-comparison.log`。临时目录不保证长期保留，交付结论以上述记录为准。

## 未验收范围

- JVM target 27：Kotlin 当前最高 target 26，不采用。JDK 27 / target 26 已完成上述项目验证。
- 真机运行、正式截图基线、Baseline Profile 设备生成、真实 Keychain 宿主验收单独记录，不由编译替代。
