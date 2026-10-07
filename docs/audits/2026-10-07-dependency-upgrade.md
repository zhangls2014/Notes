# 2026-10-07 依赖与工具链升级记录

状态：已按官方上限修正并完成 Android 验证；iOS 缺少 Xcode 26.4，既有检查失败仍保留。最终配置见下方“按官方支持范围收敛”。长期规则见 [升级规范](../dependency-upgrades.md)；本页只记录本次核对，不代表未来最新版本。

## 按官方支持范围收敛（最终决策）

Xander 明确要求以官方支持范围为升级上限，停止通过大量项目试验探索上限外组合。首轮 JDK 27、Gradle 9.8.0、AGP 9.4.1 与 SDK 37.2 选择已取代；过程配置和命令通过 Git 追溯，不作为当前配置或验收。

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

## 依赖变更与预览版例外

以下仅列出版本发生变化或需要解释的例外；未变化的正式版以版本目录为准，不重复维护完整坐标清单。“最新正式版”是 2026-10-07 核对结果，不代表之后的最新版本。

| 坐标 / 别名 | 开始时版本 | 当日最新正式版 | 采用版本 | 选择理由 |
|---|---|---|---|---|
| [`androidx.datastore:datastore-preferences`](https://dl.google.com/dl/android/maven2/androidx/datastore/datastore-preferences/maven-metadata.xml)<br>androidx-datastore-preferences | 1.3.0-alpha11 | 1.2.1 | 1.2.1 | 切换正式版；项目未使用 1.3 Builder、Web 或加密 API，数据回归已通过 |
| [`org.jetbrains.compose.material3:material3`](https://repo.maven.apache.org/maven2/org/jetbrains/compose/material3/material3/maven-metadata.xml)<br>jetbrains-compose-material3 | 1.12.0-alpha03 | 1.9.0 | 1.12.0-alpha03 | 保留预览版：现有 DropdownMenuPopup 等 API（正式版 1.9.0 源码不存在）；遵循 Compose 1.12.1 发布清单，不追 1.13 alpha |
| [`org.jetbrains.compose.material3:material3-window-size-class`](https://repo.maven.apache.org/maven2/org/jetbrains/compose/material3/material3-window-size-class/maven-metadata.xml)<br>jetbrains-compose-material3-window-size | 1.12.0-alpha03 | 1.9.0 | 1.12.0-alpha03 | 保留预览版：现有 DropdownMenuPopup 等 API（正式版 1.9.0 源码不存在）；遵循 Compose 1.12.1 发布清单，不追 1.13 alpha |
| [`org.jetbrains.compose.material3:material3-adaptive-navigation-suite`](https://repo.maven.apache.org/maven2/org/jetbrains/compose/material3/material3-adaptive-navigation-suite/maven-metadata.xml)<br>jetbrains-compose-material3-adaptive-navigation-suite | 1.12.0-alpha03 | 1.9.0 | 1.12.0-alpha03 | 跟随当前 Material 3 预览系列，避免混用不同发布系列 |
| [`org.jetbrains.compose.material3.adaptive:adaptive`](https://repo.maven.apache.org/maven2/org/jetbrains/compose/material3/adaptive/adaptive/maven-metadata.xml)<br>jetbrains-compose-material3-adaptive | 1.3.0-rc01 | 1.3.0 | 1.3.0 | 更新为当日最新正式版，回归结果见上方验证表 |
| [`org.jetbrains.compose.material3.adaptive:adaptive-layout`](https://repo.maven.apache.org/maven2/org/jetbrains/compose/material3/adaptive/adaptive-layout/maven-metadata.xml)<br>jetbrains-compose-material3-adaptive-layout | 1.3.0-rc01 | 1.3.0 | 1.3.0 | 更新为当日最新正式版，回归结果见上方验证表 |
| [`org.jetbrains.compose.material3.adaptive:adaptive-navigation`](https://repo.maven.apache.org/maven2/org/jetbrains/compose/material3/adaptive/adaptive-navigation/maven-metadata.xml)<br>jetbrains-compose-material3-adaptive-navigation | 1.3.0-rc01 | 1.3.0 | 1.3.0 | 更新为当日最新正式版，回归结果见上方验证表 |
| [`org.jetbrains.compose.material3.adaptive:adaptive-navigation3`](https://repo.maven.apache.org/maven2/org/jetbrains/compose/material3/adaptive/adaptive-navigation3/maven-metadata.xml)<br>jetbrains-compose-material3-adaptive-navigation3 | 1.3.0-rc01 | 1.3.0 | 1.3.0 | 更新为当日最新正式版，回归结果见上方验证表 |
| [`dev.detekt:detekt-rules-ktlint-wrapper`](https://repo.maven.apache.org/maven2/dev/detekt/detekt-rules-ktlint-wrapper/maven-metadata.xml)<br>detekt-formatting | 2.0.0-alpha.6 | 无（当前坐标） | 2.0.0-alpha.6 | 保留 2.x 预览版：当前 dev.detekt 插件/API 与 Kotlin 2.4 构建链；1.23.8 的官方编译/测试链较旧，不回迁插件和规则配置 |
| [`com.android.kotlin.multiplatform.library:com.android.kotlin.multiplatform.library.gradle.plugin`](https://dl.google.com/dl/android/maven2/com/android/kotlin/multiplatform/library/com.android.kotlin.multiplatform.library.gradle.plugin/maven-metadata.xml)<br>android-gradle-plugin, android-kmp-library | 9.4.1 | 9.4.1 | 9.3.1 | 按 Kotlin 官方支持上限收敛 |
| [`com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin`](https://repo.maven.apache.org/maven2/com/google/devtools/ksp/com.google.devtools.ksp.gradle.plugin/maven-metadata.xml)<br>ksp-gradle-plugin, google-ksp | 2.3.11 | 2.3.12 | 2.3.12 | 更新为当日最新正式版，回归结果见上方验证表 |
| [`dev.detekt:dev.detekt.gradle.plugin`](https://repo.maven.apache.org/maven2/dev/detekt/dev.detekt.gradle.plugin/maven-metadata.xml)<br>detekt-gradle-plugin, detekt | 2.0.0-alpha.6 | 无（当前坐标） | 2.0.0-alpha.6 | 保留 2.x 预览版：当前 dev.detekt 插件/API 与 Kotlin 2.4 构建链；1.23.8 的官方编译/测试链较旧，不回迁插件和规则配置 |
| [`com.android.application:com.android.application.gradle.plugin`](https://dl.google.com/dl/android/maven2/com/android/application/com.android.application.gradle.plugin/maven-metadata.xml)<br>android-application | 9.4.1 | 9.4.1 | 9.3.1 | 按 Kotlin 官方支持上限收敛 |
| [`com.android.lint:com.android.lint.gradle.plugin`](https://dl.google.com/dl/android/maven2/com/android/lint/com.android.lint.gradle.plugin/maven-metadata.xml)<br>android-lint | 9.4.1 | 9.4.1 | 9.3.1 | 按 Kotlin 官方支持上限收敛 |
| [`com.android.library:com.android.library.gradle.plugin`](https://dl.google.com/dl/android/maven2/com/android/library/com.android.library.gradle.plugin/maven-metadata.xml)<br>android-library | 9.4.1 | 9.4.1 | 9.3.1 | 按 Kotlin 官方支持上限收敛 |
| [`com.android.fused-library:com.android.fused-library.gradle.plugin`](https://dl.google.com/dl/android/maven2/com/android/fused-library/com.android.fused-library.gradle.plugin/maven-metadata.xml)<br>android-fusedlibrary | 9.4.1 | 9.4.1 | 9.3.1 | 按 Kotlin 官方支持上限收敛 |
| [`com.android.test:com.android.test.gradle.plugin`](https://dl.google.com/dl/android/maven2/com/android/test/com.android.test.gradle.plugin/maven-metadata.xml)<br>android-test | 9.4.1 | 9.4.1 | 9.3.1 | 按 Kotlin 官方支持上限收敛 |

Material 3 / Adaptive 发布清单：[Compose 官方发布页](https://github.com/JetBrains/compose-multiplatform/releases)。detekt 1.23.8 与 2.x 是不同插件/API，不以删掉分析规则或绕过检查换取正式版后缀。

## 对照证据与未验收范围

升级前的独立对照副本使用开始时的工作区版本，复现 `DeviceCornerOpacityTest.ordinaryPushAndPopSwitchWithoutMotionOrBlending` 的 `Page stays fully opaque (push)` 断言失败；profile 16 条、about 35 条 detekt 问题与升级后逐条一致。因此这些问题保留为既有失败，未通过修改断言或关闭检查使其通过。

Fused 输出在对照版本中也缺少 `:feature:login-api` 与 `:core:preference` 的 include；补齐实际依赖后 assemble/report 通过，未关闭校验。具体构建入口见 [Fused Library 指南](../../android/output/README.md)。

首轮超出最终官方范围的工具链曾通过 iOS 模拟器测试及 Xcode 27 / Swift 6 整包构建，这些结果不能替代最终组合在受支持 Xcode 26.4 环境中的验收。旧配置、流水日志、校验值和重复命令保存在 Git 提交 `bc6f0f67e62080501b563323dd279477e43a7178` 的本文中，可用 `git show <commit>:docs/audits/2026-10-07-dependency-upgrade.md` 查询，不作为当前操作指南。

真机运行、正式截图基线、Baseline Profile 设备生成及平台安全存储验收独立跟踪。未完成事项和关闭条件见 [待验收清单](../testing/pending-acceptance.md)，长期验证入口见 [测试指南](../testing/README.md)。
