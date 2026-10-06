# Latest Compatible Dependencies Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans after Xander approves the scope and release-channel policy. Execute serially in this chat.

**Goal:** 将 Notes 的依赖和构建配置更新为通过 Android、iOS 和宿主回归的最新兼容组合。

**Architecture:** 保持现有模块、数据与 UI 边界；先确定版本规则，再调整依赖和必要的 API 调用。JDK toolchain 与 JVM 字节码目标分开评估，不把提高最低系统版本等同于升级工具链。

**Tech Stack:** Kotlin Multiplatform、Compose Multiplatform、AGP、Gradle、JDK、Xcode、KSP、Koin、Room、Ktor、AndroidX、Robolectric、Roborazzi。

---

状态：Xander 已授权整理长期文档并执行升级，已实施并完成验证记录；既有测试和 detekt 失败另列，不宣称全量通过。长期规则见 [升级规范](../../dependency-upgrades.md)，全量清单与当次结果见 [升级记录](../../audits/2026-10-07-dependency-upgrade.md)。超出官方兼容表的组合须区分官方支持与项目实测。

## 2026-10-07 范围修正

Xander 要求官方支持/最高测试范围作为上限；此修正优先于以下首轮计划与完成勾选。

- [x] 先更新长期规范：取官方明确范围的交集，不以实测扩大上限。
- [x] 调整到 JDK/target 25、Gradle 9.7.0、AGP 9.3.1、compile SDK 37.0；保留范围内的正式依赖升级与已修复 Fused include。
- [x] 完整生成 Wrapper，执行配置、Android Debug/Release R8、受影响共享宿主测试、lint、detekt、Fused 必要回归；保留既有失败证据，不重复探索范围外组合。
- [x] 更新最终记录与 README。iOS 需要 Xcode 26.4，本机缺失；不使用 Xcode 27 冒充验收，也不安装全局工具。

## 首轮计划（历史记录）

## 版本选择规则

- 优先选择满足现有功能和平台要求的最新正式版，不主动采用 EAP、alpha、beta、RC 或 snapshot。
- 当前已使用预览版的组件，先核对是否已有正式版；如正式版缺少项目所需 API，保留当前预览版并记录具体原因，不以追求全正式版为由移除功能。
- 只在不存在满足要求的正式版时评估预览版；不自动启用新的实验性语言特性。
- 最新指核对时实际发布且能解析的版本；兼容结论需同时记录官方约束和本项目验证证据。

## 已检查事实（2026-10-07）

- 当前 Kotlin 2.4.20、Gradle 9.7.1、AGP 9.4.1、Compose 1.12.1、JVM target 21。
- Kotlin 官方兼容表为 2.4.20 列出 Gradle 7.6.3–9.7.0、AGP 8.5.2–9.3.1、Xcode 26.4。当前组合不能描述为完全位于该表范围内。
- Gradle 当前发布说明为 9.8.0，其 Java 兼容表支持 JDK 27；JDK 25 不是唯一可选升级候选。
- Kotlin 官方 EAP 为 2.5.0-Beta1。作为候选评估，不预先认定其与全部编译器插件兼容。
- `java_home` 列出 JBR 21.0.10、Xcode 为 27.0；实施时额外检查发现 Android Studio 内置 JBR 25.0.3，使用它验证，无需安装全局 JDK。随后按用户最新兼容目标在忽略目录内下载 JDK 27 候选验证。
- 版本目录已有用户改动：AGP、Roborazzi、DataStore、Baseline Profile、Macrobenchmark。以当前工作区为起点，保留这些改动的来源。
- README 的 AGP 表仍为 9.3.2，与工作区版本目录不一致。

官方来源：

- https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html
- https://kotlinlang.org/docs/eap.html
- https://docs.gradle.org/current/release-notes.html
- https://docs.gradle.org/current/userguide/compatibility.html
- https://kotlinlang.org/docs/multiplatform/compose-compatibility-and-versioning.html
- https://developer.android.com/build/releases/agp-9-4-0-release-notes

## Task 1: 确认并记录版本规则

Files: `AGENTS.md`、`README.md`、本计划。

- [x] 确认正式版优先的选择规则，并记录在本计划；实施前将规则写入项目规范。
- [x] 版本使用精确值；核对官方发布页和仓库元数据，记录核对日期及来源。
- [x] 审计版本目录所有显式库与插件版本，共 71 个去重坐标，以及 Wrapper、BOM 和 Xcode 工程配置。清单见升级记录。
- [x] 系统安装/配置、数据库 schema/迁移、文件删除和 git 回滚、公开发布仍分别遵守用户红线。

## Task 2: 验证工具链组合

Files: `gradle/kmp.versions.toml`、`gradle/wrapper/gradle-wrapper.properties`、`build-logic/src/main/kotlin/me.zhangls.kmp-library.gradle.kts`、`androidApp/build.gradle.kts`、`android/baselineprofile/build.gradle.kts`、必要时 `gradle.properties`。

- [x] 优先评估 Gradle 9.8.0 与 Kotlin 2.4.20 等正式版组合；Kotlin 2.5.0-Beta1 不作为默认升级目标。逐项对照 KSP、Koin compiler、Compose compiler、detekt、AGP 的官方要求。
- [x] 先尝试 JBR/toolchain 25，首轮全量构建 OOM；采用项目内 JDK 27、Kotlin/Java target 26，编译与行为回归结果见升级记录。已拆分 JDK 与 JVM target 值，三个消费处一致使用。
- [x] 在用户批准安装方式前，不安装新全局 JDK，不修改 IDE 或系统配置；先检查可用环境并给出明确安装范围。
- [x] 串行执行 `./gradlew help` 和 `./gradlew :androidApp:assembleDebug :composeApp:compileKotlinIosSimulatorArm64`。记录配置、生成代码、编译和 dex 阶段结果。

## Task 3: 更新其余依赖与配置

Files: `gradle/kmp.versions.toml`、受升级影响的模块 build 文件和 API 调用、`iosApp/iosApp.xcodeproj/project.pbxproj`（仅必要兼容修正）。

- [x] 按 Compose/AndroidX、数据与网络、工具与测试分组更新；BOM 管理的工件继续由 BOM 管理，KMP 工件核对 Android/iOS 目标。
- [x] 只迁移升级要求的 API，不修改功能与布局；不因追新提高 Android minSdk 或 iOS deployment target，除非依赖提出硬要求并在方案中说明。
- [x] Room 若要求 schema/迁移，暂停该部分并给出具体迁移方案请求批准；其他不依赖它的升级继续执行。
- [x] 遇到不兼容，先对照官方文档、参考实现和已知问题。不得跳过验证或添加绕过标记；记录限制并采用最近通过版本。任何需要撤销工作区改动的操作先遵守回滚红线。

## Task 4: 回归和交付

Files: `README.md`、本计划；架构受影响时更新 `docs/architecture.md`。

- [x] 串行运行 Android 构建与 Release R8：`./gradlew :androidApp:assembleDebug :androidApp:assembleDevFullRelease`。
- [x] 宿主回归：`./gradlew :composeApp:testAndroidHostTest :core:data:testAndroidHostTest :core:database:testAndroidHostTest :core:theme:testAndroidHostTest :feature:main:testAndroidHostTest :feature:login:testAndroidHostTest :feature:email:testAndroidHostTest :feature:profile:testAndroidHostTest`。
- [x] iOS 回归：`./gradlew :composeApp:compileKotlinIosSimulatorArm64 :composeApp:iosSimulatorArm64Test :core:data:iosSimulatorArm64Test :core:database:iosSimulatorArm64Test :core:framework:iosSimulatorArm64Test`。Entry 实际调用和 Preference 初始化测试必须包含在结果核对中。
- [x] 静态检查与实验性输出：`./gradlew :androidApp:lintDevFullDebug :feature:profile:detekt :feature:about:detekt :android:output:login:assemble`；编译 Baseline Profile 测试模块，生成/设备执行另列验收状态。
- [x] 确认 Xcode 工程 scheme 和模拟器 destination 后执行不需要签名的模拟器整包构建；记录 Kotlin 编译与 Xcode 整包结果，不能互相替代。
- [x] 同步 README 的最终版本和环境要求；交付每项升级、保留原因、官方支持范围和实测范围。
- [x] 分别报告通过、失败、跳过、缓存复用和未验收项；截图基线与设备设置遵循既有专项规范，不自动批准或替换基线。

验收标准：所有实际使用依赖都有版本决策记录；采用组合完成上述可执行回归，未完成的设备、签名与平台验收明确列出；没有遗失既有工作区改动或触碰未授权红线。
