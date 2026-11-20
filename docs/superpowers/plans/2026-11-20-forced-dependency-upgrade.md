# 一次性强制依赖升级实施计划

状态：维护者已于 2026-11-20 明确回复“实施”；实现与自动验证已执行，截图、静态门禁及设备验收仍有未完成项，详见升级审计。

**Goal:** 将 Android/KMP 应用及构建、测试依赖升级到核对当天的最新版本，以实测记录兼容性与缺口。

**Architecture:** 保持现有 Feature API/实现、Core 和组合根边界，仅适配升级导致的配置与 API 变化。本次允许越过官方兼容范围；实际验证只能证明已测试范围，不推断未测试平台或设备兼容。

**Tech Stack:** Kotlin Multiplatform、Compose Multiplatform、AndroidX、Gradle、Koin、KSP、Room、Ktor、Xcode。

执行方式：在当前隔离 worktree 内串行实施，复用现有测试和工具；不额外派发子代理。长期升级规范保持原样，只有本次审计记录例外。

## 范围与版本口径

- 延续本轮 Android/KMP 检查范围：版本目录全部显式插件/库、BOM、Gradle Wrapper、构建约定、Android SDK 配置，以及影响这些构建的运行/编译工具链；独立 server/auth 的 npm 依赖不在本次范围。
- 最新优先指正式版。已使用预览版的组件，若正式版无法维持现有功能，允许评估最新预览版并记录原因；不主动将全部正式版换成 RC/EAP。
- 不受官方兼容上限限制，也不跳过弃用诊断、禁用安全检查、注释功能或修改测试断言来制造通过。
- BOM 受管版本继续交由最新 BOM 管理；不为每个传递依赖单独钉死最大版本。
- AndroidX Compose 1.13.0 / Material 3 1.5.0 纳入目标。JetBrains 工件尚无对应正式版时，先评估最新 CMP 预览组合；如仍无法覆盖，再评估 Android 专属解析覆盖及其双端回归，不将 AndroidX 工件强塞入 iOS 源集。
- 不自动提高 minSdk 或 iOS deployment target，不为字节码版本数字追新。JDK 若需新增，仅使用项目内安装，不改系统配置。
- 若最新版本确实无法兼容，保留证据与未完成状态，提出最小修复或版本选择方案；不静默降级后宣称“全部最新且通过”。

## 1. 固定目标与基线

Files: `docs/audits/2026-11-20-dependency-upgrade.md`、`gradle/kmp.versions.toml`（读取）。

- [x] 记录开始时 Git 状态，保留此前新增审计与文档索引。
- [x] 重读官方仓库实际发布元数据，固定精确目标版本和来源。已确认目标包括 Kotlin 2.4.21、AGP 9.5.1、Gradle 9.9.1、KSP 2.3.14、BuildKonfig 0.24.0、Robolectric 4.18.1、Roborazzi 1.79.0；其余坐标重新核对。
- [x] 核对最新 Material 3/CMP 预览组合、Android 与 JetBrains Lifecycle 独立版本及 Native 工件存在性。
- [x] 在当前版本运行核心宿主与 Native 基线，记录执行/缓存/失败/跳过；历史报告仅作失败定位线索。

## 2. 工具链升级与最小修复

Files: `gradle/kmp.versions.toml`、`gradle/wrapper/gradle-wrapper.properties`、必要时 Wrapper 脚本/JAR、`build-logic/`、Gradle 构建文件。

- [x] 在审计中写明本次一次性授权及核定版本，再修改配置。
- [x] 同步 Kotlin 编译插件与 kotlin-test，升级 AGP、Gradle、KSP、Koin 编译插件、BuildKonfig 和分析工具。核对 Wrapper 官方校验和。
- [x] Gradle 调用使用命令级 JAVA_HOME；首先运行 `./gradlew help` 验证配置，然后运行 `./gradlew :androidApp:assembleDebug :composeApp:compileKotlinIosSimulatorArm64 --continue`。
- [x] 按错误对应的源码、发行说明和参考实现定位 API 变化；两轮未解决换策略，不靠加绕过标记维持旧接口。

## 3. 全部应用与测试库升级

Files: `gradle/kmp.versions.toml`、必要的 `core/`、`feature/`、`composeApp/` 源码与已有测试。

- [x] 更新所有剩余显式依赖、BOM 和内联版本；检查 Android 解析图与 Native 工件。
- [x] 按工具链、UI、数据/网络、测试/分析顺序分批验证，但最终交付同一套最新组合。
- [x] API 适配保持原有功能和交互；只有产生行为修复时增加针对性回归测试。
- [x] Compose 重点覆盖草稿 BottomSheet、SearchBar、顶栏、表单、导航、主题/边框和真实 Entry；不顺带将 derivedStateOf 替换为 computedStateOf。

## 4. 验证矩阵

所有 Gradle 命令串行执行，遇任务重命名先发现实际任务，不关闭检查。

- [x] Android Debug：`./gradlew :androidApp:assembleDebug`。
- [x] Release/R8 与 lint：`./gradlew :androidApp:assembleDevGlobalRelease :androidApp:lintDevGlobalDebug --continue`。
- [x] 所有已配置共享/宿主模块：`./gradlew :composeApp:testAndroidHostTest :core:navigation:testAndroidHostTest :core:data:testAndroidHostTest :core:database:testAndroidHostTest :core:theme:testAndroidHostTest :core:framework:testAndroidHostTest :core:network:testAndroidHostTest :feature:main:testAndroidHostTest :feature:login:testAndroidHostTest :feature:email:testAndroidHostTest :feature:profile:testAndroidHostTest :feature:about:testAndroidHostTest :feature:settings:testAndroidHostTest --continue`。
- [x] Android 语义检查：`./gradlew detektMainAndroid detektHostTestAndroid :androidApp:detektDevGlobalDebug :androidApp:detektDevGlobalDebugUnitTest :androidApp:detektDevGlobalDebugAndroidTest --continue`；普通模块 detekt 另外覆盖手写 Native 源码。
- [x] Native：`./gradlew :composeApp:compileKotlinIosArm64 :composeApp:compileKotlinIosSimulatorArm64 :composeApp:iosSimulatorArm64Test :core:data:iosSimulatorArm64Test :core:database:iosSimulatorArm64Test --continue`；其他已配置 iOS 测试模块按测试指南补齐。必须包含真实 Entry、Preference 初始化和 Room 回归。
- [x] Fused 与 Baseline：`./gradlew :android:output:login:assemble :android:baselineprofile:assemble --continue`。
- [x] 截图按 `docs/testing/adaptive-ui.md` 运行现有 verify；失败与原组合对照，正式基线不自动更新。
- [x] 使用现有 Xcode 27.2 执行模拟器整包构建；未进行 Android/iOS 设备交互验收，未修改模拟器显示设置。

## 5. 声明、文档与交付

Files: `web/open-source/`、`feature/about/src/commonMain/composeResources/files/releases.json`、根 `README.md`、本次审计与必要架构文档。

- [x] 按 `tools/open-source/README.md` 导出 Android/iOS 解析依赖并刷新声明；运行 Python 生成器测试、网站 Node 测试和 `:web:buildSite`，保留原始 LICENSE/NOTICE。
- [x] 同步 README 版本/环境信息、未发布更新记录；只有实际架构变更才更新架构文档。
- [x] 清点每个显式版本是否达到冻结目标，记录采用/例外/阻碍。交付分别报告编译、行为、截图、设备及 Xcode 整包结果，并区分执行、缓存、通过、失败、跳过。
- [x] 文档归位、引用检查与 `git diff --check`；合入前按仓库要求整理为单一最终提交，未经指示不自动合入 master。

## 授权边界

维护者确认本方案后执行。删除文件/目录、git 回滚、CI/CD 或密钥配置、数据库 schema/数据迁移、全局依赖/系统修改、公开发布仍须另行授权。若升级要求这些操作，先准备可审查差异再单独询问，继续不受影响的工作。

执行说明：勾选表示已实施或实际执行，不能据此推断所有门禁通过。完整双端行为回归最终无失败，截图 verify 与 detekt 仍失败；不自动更新基线、不合入 master。设备验收与正式视觉审阅继续在集中清单跟踪。计划保留以保存未验收范围，未授权额外删除。
