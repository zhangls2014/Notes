# 全项目过时 API 审计与更新方案

日期：2026-10-02

状态：本轮两项 API 修复与稳定补丁升级已完成并通过构建、lint 和自动化测试；UIKit 多窗口交互验收待补。以下原始调查记录保留升级前版本及证据，实施结果见文末。

## 范围与结论

静态检查覆盖 settings 中的 19 个模块、build-logic、Kotlin 源集、Swift 入口、版本目录、Gradle Wrapper 和 Android 清单，共 282 个 Kotlin / Swift / Gradle Kotlin DSL 文件。重点检查平台 API、Compose / Material 3、Navigation 3、窗口适配、Room、DataStore、Ktor、Koin，以及测试和构建脚本。

确认存在两组优先更新事项：

1. iOS Toast 使用已弃用的 `UIScreen.main`（Kotlin 为 `mainScreen`）及 `UIWindow.init(frame:)`。
2. APK 重命名使用 AGP 内部实现类和内部辅助函数；项目当前 AGP 9.3.2 已提供公开替代接口。

没有发现项目范围内普遍使用旧导航、旧搜索栏、旧底部弹层等问题。重新编译 Android / iOS 主代码后，没有出现 Kotlin API 弃用警告。这不能替代 Apple API 文档核查，也不代表第三方库内部没有弃用调用。

## 原始调查验证

按顺序执行，未并发运行两个 Gradle 构建：

```bash
./gradlew :androidApp:lintDevFullDebug \
  :composeApp:compileKotlinIosSimulatorArm64 --offline --warning-mode all

./gradlew :androidApp:compileDevFullDebugKotlin \
  :composeApp:compileKotlinIosSimulatorArm64 \
  --offline --warning-mode all --rerun-tasks
```

- 第一次构建成功，25 秒；lint 为 0 个错误、26 个警告。
- 第二次构建成功，23 秒；262 个任务全部执行，其中 Android / iOS 主代码编译任务 33 个，没有弃用警告。
- lint 警告分类：依赖更新提示 25 个，清单 `UnusedAttribute` 1 个。多个坐标引用同一版本，所以 25 个提示不代表 25 个独立升级事项。
- `expect` / `actual` 类的 Beta 提示、Koin 子模块没有入口的提示、Fused Library 的 Publication Only Mode 提示，不是 API 弃用。
- lint 报告：`androidApp/build/reports/lint-results-devFullDebug.{html,xml,txt,sarif}`。
- 编译日志：`/private/tmp/notes-api-audit-build.log`、`/private/tmp/notes-api-audit-recompile.log`，属于临时验证文件。

未执行 Xcode Swift 整包构建、真机运行、UI 测试、Baseline Profile 生成、Fused Library 打包及其他 Android flavor 构建。iOS Kotlin 编译覆盖共享代码和 iosMain，但不等于 Swift / Xcode 整包验证。测试模块和实验性输出模块的独立入口以静态检查为主。

## 一、优先更新

### 1. iOS Toast 改为绑定当前窗口场景

位置：`core/framework/src/iosMain/kotlin/me/zhangls/framework/toast/SystemToast.ios.kt`。

- 第 67 行：`PassthroughWindow(frame = UIScreen.mainScreen.bounds)`。
- 第 100 行：以 `UIScreen.mainScreen.bounds` 计算标签尺寸和位置。
- 第 146 行：子类通过 `UIWindow(frame)` 初始化窗口。

Apple 文档将 [UIScreen.main](https://developer.apple.com/documentation/uikit/uiscreen/main) 和 [UIWindow.init(frame:)](https://developer.apple.com/documentation/uikit/uiwindow/init(frame:)) 标为 Deprecated。建议统一迁移到场景绑定的窗口，并基于实际承载视图的窗口几何布局。

当前实现假设屏幕尺寸等于应用窗口尺寸，也没有给额外窗口指定 `UIWindowScene`。由代码可推断，在 iPad 多窗口、窗口缩放或外接显示器场景中可能出现 Toast 布局位置不正确或窗口归属不明确；本次没有运行复现，不将这些情况描述为已经验证的故障。

更新方案：

1. 从触发 Toast 的宿主视图或 ViewController 获取对应 `window.windowScene`，在 iOS 宿主建立场景上下文 / presenter 注册；需要多个窗口时按场景管理。
2. 使用 `UIWindow(windowScene:)` 构造触摸穿透窗口。不能简单取 `connectedScenes.first()`，否则多窗口时仍可能选择错误场景。
3. 标签通过 Toast 根视图的 `bounds` 和 `safeAreaInsets` 布局；在尺寸变化后重新布局。只有确实需要显示器信息时才读取 `windowScene.screen`。
4. 保留现有触摸穿透、显示时长、淡入淡出和较高 windowLevel；场景解绑时关闭窗口。
5. 验证 iPhone、iPad 分屏 / 窗口缩放、旋转、两个窗口、弹层上方显示与触摸穿透。

预计涉及 `SystemToast.ios.kt` 与 iOS 宿主上下文的装配；不需要更改 Android Toast。若引入新的跨模块宿主契约，同步更新架构文档。

### 2. APK 命名改用 AGP 公开 API

位置：`androidApp/build.gradle.kts` 第 1–2 行和第 140–148 行。

当前导入并使用：

```kotlin
com.android.build.api.variant.impl.VariantOutputImpl
com.android.build.api.variant.impl.capitalizeFirstChar
```

这属于内部 API 依赖，不应误称为已经收到 `@Deprecated` 警告。问题是升级 AGP 时内部实现没有公开契约保障，而且当前版本已经能消除这项依赖。

[AGP VariantOutput 官方接口](https://developer.android.com/reference/tools/gradle-api/9.3/com/android/build/api/variant/VariantOutput?authuser=2) 明确列出 `outputFileName: Property<String>`，自 9.3.2 加入，恰好是项目当前版本。

更新方案：删除两个内部导入和 `is VariantOutputImpl` 判断，直接对每个公开 output 调用 `outputFileName.set(...)`。名称通过 `versionName.zip(versionCode)` 的 Provider 组合延迟生成；首字母处理使用 Kotlin 标准库 `replaceFirstChar` 并明确大小写语义。

迁移方向示例（方案片段，未修改源码）：

```kotlin
androidComponents {
  onVariants { variant ->
    val flavorLabel = variant.flavorName
      ?.replaceFirstChar { it.titlecase() }
    variant.outputs.forEach { output ->
      output.outputFileName.set(
        output.versionName.zip(output.versionCode) { name, code ->
          "Notes_v${name}(${code})_${flavorLabel}_${variant.buildType}.apk"
        }
      )
    }
  }
}
```

验证所有 flavor 的 APK 文件名及输出元数据，至少执行 `:androidApp:assembleDebug`；确认安装任务仍然能定位 APK。无需为此先升级 AGP。

## 二、可以现代化，但不是弃用修复

### 3. 登录和邮件编辑器改为状态式文本输入

位置：

- `feature/login/.../LoginScreen.kt` 第 336、379 行。
- `feature/email/.../component/NewEmailSheet.kt` 第 160、169 行。

这些输入框仍使用 `value` / `onValueChange`；密码使用 `PasswordVisualTransformation`。当前版本源码没有将这些重载标为弃用，重新编译也没有对应警告。

[官方迁移指南](https://developer.android.com/develop/ui/compose/text/migrate-state-based) 提供迁移到 `TextFieldState` 的方案，适用于改善输入法组合文本、选区和编辑状态管理。

可选实施：账户、主题、正文使用状态式 `OutlinedTextField`；密码考虑 `OutlinedSecureTextField` 并显式配置遮挡策略，保持当前默认隐藏行为。搜索栏已有 `rememberTextFieldState`，无需重复迁移。

UI 内维护编辑状态，将业务文本和验证结果通过 Intent 同步；明确草稿加载、清空、登录失败和页面重建时的同步规则，避免两个状态来源互相覆盖。密码不得新增跨进程保存，不启用 MVI `savedKey` 存储密码，也不盲目使用会保存密码的状态恢复方式。

验证中文输入法组合文本、光标 / 选区、清空账户、密码显隐、Go / Next 动作和草稿恢复。

### 4. 导航外壳的新重载可按需求采用

位置：`feature/main/.../AppShell.kt` 第 91 行。

当前 `NavigationSuiteScaffold(navigationSuiteItems = ..., layoutType = ...)` 解析到带默认 `state` 参数的非弃用重载。核对项目实际解析的 `material3-adaptive-navigation-suite:1.12.0-alpha03` sources.jar，不能因为另一个同名旧重载已经 HIDDEN，就把当前调用判断为弃用。

库还提供 `navigationItems` / `navigationSuiteType` 新重载，适合更灵活的内容和 primary action。只有需要这些功能时再迁移；本次不建议把它纳入必修项。迁移时必须回归 ShortNavigationBar、WideNavigationRail、RTL 和现有系统 Insets 消费策略，保留窗口事实从组合根下发的约束。

## 三、依赖更新方案

版本落后与 API 弃用分开处理。建议先修上述代码，再分组升级并验证，不一次升级全部依赖。

| 分组 | 当前 | 更新建议 | 依据 / 条件 |
| --- | --- | --- | --- |
| Compose Multiplatform 基础组件 | 1.12.0 | 1.12.1 | [官方发行说明](https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.12.1)有文本选择和 iOS 修复；统一更新版本目录引用 |
| JetBrains Navigation 3 | 1.1.1 | 1.1.2 | 上述 CMP 1.12.1 组件表列出的版本 |
| JetBrains Window Core | 1.5.0 | 1.5.1 | 上述 CMP 1.12.1 组件表列出的版本 |
| AndroidX NavigationEvent | 1.1.1 | 1.1.2 | [官方发行说明](https://developer.android.com/jetpack/androidx/releases/navigationevent)确认已发布；回归预测返回 |
| Room 3 | 3.0.2 | 3.0.3 | [官方发行说明](https://developer.android.com/jetpack/androidx/releases/room3)确认版本；runtime / compiler / paging / 插件统一更新，验证历史 schema 迁移 |
| Core / SQLite | 1.19.0 / 2.7.0 | 1.19.1 / 2.7.1 | [Core 发行说明](https://developer.android.com/jetpack/androidx/releases/core)与 [SQLite 发行说明](https://developer.android.com/jetpack/androidx/releases/sqlite)已核实；Core Kotlin 扩展已合并到 core，移除空 core-ktx 兼容坐标 |
| Baseline Profile / Benchmark | 1.5.0-rc02 / 1.4.1 | 候选 1.5.0 / 1.5.0 | lint 提示；独立升级并验证生成流程，官方页面的不同缓存结果不一致，不仅凭 lint 宣称已兼容 |
| DataStore | 1.3.0-alpha10 | 先保持；候选 alpha11 | 仍为预发布分支，lint 有提示；只有修复或功能收益明确时更新 |
| AGP | 9.3.2 | 先保持；候选 9.3.3 / 9.4.1 | lint 有提示；先去除内部 API，再核对 Kotlin / Gradle / AGP 联合兼容性 |

Material 3 保持 `1.12.0-alpha03`，Adaptive 保持 `1.3.0-rc01`：它们正是 CMP 1.12.1 官方组件表列出的对应版本，不能跟随基础 Compose 版本机械改成 `1.12.1`。当前 Expressive 用法不是“旧 API”，也不应为了消除预发布标签而直接降级。

工具链需单独记录兼容性例外：[Kotlin 官方兼容表](https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html)列出 Kotlin 2.4.20 的 Gradle 上限 9.7.0、AGP 上限 9.3.1；仓库为 Gradle 9.7.1、AGP 9.3.2，略超该文档范围。本次编译通过是实测证据，但不等于官方已认证该组合；不据此直接升 AGP 9.4.1，也不为了匹配表格盲目降级当前工具链。

所有依赖版本仍只在 `gradle/kmp.versions.toml` 维护；Wrapper 分发版本在其配置文件中管理。

## 四、已经采用较新 API 的部分

- KMP Android：使用 `com.android.kotlin.multiplatform.library` 和 `android {}`，不是传统 `androidTarget` + Android Library 插件组合。
- Kotlin 构建：采用 `compilerOptions` 与显式 JVM toolchain，没有发现 `kotlinOptions`。
- 导航：Navigation 3、SceneStrategy、`rememberSceneState`、NavigationEvent 预测返回；不需要迁回 Navigation 2。
- 窗口适配：`currentWindowAdaptiveInfoV2`、`WindowSizeClass.BREAKPOINTS_V2`；无旧无参接口的实际调用。
- 搜索：`AppBarWithSearch`、`rememberSearchBarState`、`ExpandedFullScreenSearchBar` / `ExpandedDockedSearchBar`、状态式输入。
- Bottom Sheet：`rememberBottomSheetState`；旧 `rememberModalBottomSheetState` 只出现在迁移说明注释里。
- Room：`androidx.room3`、`RoomDatabaseConstructor`、BundledSQLiteDriver、查询 CoroutineContext。
- Koin：编译器插件、注解和 `koinViewModel`；本次没有发现需要淘汰的旧调用。
- 文本链接与 Preview：`LinkAnnotation` 和 `androidx.compose.ui.tooling.preview.Preview`。
- 时间：共享 Toast 使用 `kotlin.time.Clock`。iOS `NSDate` 本身不应视为弃用 API。
- Android 包信息：`getPackageInfo(String, int)` [官方文档](https://developer.android.com/reference/android/content/pm/PackageManager#getPackageInfo(java.lang.String,int))未标弃用；`PackageInfoFlags` 重载自 API 33 加入，minSdk 28 下不是必需迁移。版本号已使用 `longVersionCode`。
- 本仓库没有发现为了隐藏弃用调用而设置的 `Suppress("DEPRECATION")`。

## 五、建议实施顺序与验收

1. **先移除 AGP 内部 API**：范围小，不需要变更版本。验收多 flavor APK 命名、assemble 和安装定位。
2. **修正 iOS Toast 场景窗口**：同时替换两类弃用 API，验收旋转、窗口缩放、多窗口和触摸穿透；执行 Kotlin iOS 编译和 Xcode 整包验证。
3. **升级稳定补丁依赖**：优先 CMP / Navigation 3 / Window、NavigationEvent、Room，分组验证；工具链和预发布依赖单独决策。
4. **按输入体验需要迁移文本框**：不是阻塞性弃用修复，重点验证 IME、密码保存边界和草稿同步。
5. **建立回归检查**：编译与 lint 保留弃用提示；对项目引入的新弃用调用和内部 API 建立检查，避免把 Koin 日志 / expect-actual Beta 等非弃用提示一起升级成阻塞错误。

更新后的基础验收命令：`:androidApp:assembleDebug`、`:androidApp:lintDevFullDebug`、`:composeApp:compileKotlinIosSimulatorArm64`。涉及导航 / UI 时再执行 `:composeApp:testAndroidHostTest --tests '*AdaptiveUiTest*'` 及相关输入 / 导航测试；Room / DataStore 更新执行数据层测试和迁移验证。模拟器测试如更改显示尺寸或密度，按仓库约束在结束前 reset 并确认无 Override。

每个独立需求的过程提交在合入 master 前整理为一个最终提交；最新 master 上 rebase 后仅 fast-forward 合入。本次实现按用户要求整理为一个最终提交，并仅以 fast-forward 合入本地 master。


## 六、2026-10-02 实施记录

实施分支：`codex/api-modernization`；按用户要求采用单个最终提交、fast-forward 合入本地 master 的方式交付。

已完成的代码与依赖修改：

- APK 命名只使用 AGP 公开 `VariantOutput.outputFileName` 与版本 Provider；四个 debug flavor 产物名称与原规则一致，输出元数据指向的 APK 均存在。
- iOS Toast 用当前 Compose 宿主的 `windowScene` 创建额外窗口，以根视图 bounds / safeAreaInsets 重新布局。替换、宿主 dispose、场景 disconnect 时关闭并解绑窗口；窗口触摸穿透，不成为 key window。`App` 公共签名与 `SystemToast` 业务接口保持不变，Android 仍通过 Koin 装配。
- 版本目录更新 CMP 1.12.1、JetBrains Navigation 3 1.1.2、Window 1.5.1、NavigationEvent 1.1.2、Room 3.0.3、Core 1.19.1、SQLite 2.7.1。Core 使用已包含 Kotlin 扩展的 `core` 坐标；Material 3、Adaptive、工具链与预发布依赖保持原版本。
- 新增 4 项 iOS Toast 几何测试；新增从历史 v1/v2/v3 到当前 v4 的实际数据库迁移测试，使用冻结的历史 SQL、索引与 identity hash，检查账户、邮件、收件人和默认账户字段保留。
- 架构文档同步 Toast 平台装配与生命周期。独立代码审查未发现阻塞问题，并核对了 UIKit 场景初始化和 Compose 宿主 dispose 的本地源码契约。

阶段验证已确认：

- API 修复阶段 `:androidApp:assembleDebug` 成功，四个 debug flavor 的 APK 名称及输出元数据均通过核对。
- `:core:framework:iosSimulatorArm64Test` 的 4 项 Toast 几何测试通过；新增 helper 实现前先运行并确认失败，实现后转绿。
- 第一组 CMP / Navigation 3 / Window 升级后，`:composeApp:compileKotlinIosSimulatorArm64` 和自适应 UI 测试通过。AdaptiveUiTest 为 544 个矩阵组合，其中 80 项执行、464 项因场景不适用按测试内 Assume 跳过，0 失败。
- API 修复阶段 Xcode simulator 整包构建成功，并在 iPad Pro 13 iOS 18.5 模拟器安装、启动，检查登录页面正常呈现；模拟器已恢复到启动前的关闭状态。未修改分辨率或密度。

验证边界：Toast 的窗口归属、几何及清理实现已完成；仍未逐项执行双窗口、外接显示器、旋转中显示、弹层触摸穿透等 UIKit 交互验收。全局 SharedFlow Toast 事件没有目标窗口标识，多个活跃宿主会各自在自身窗口呈现事件；本次不扩大为窗口路由改造。状态式登录 / 编辑文本框与导航外壳新重载属于可选体验改造，未纳入本次必修项。


最终依赖升级验证：

- `:androidApp:assembleDebug :androidApp:lintDevFullDebug :composeApp:compileKotlinIosSimulatorArm64 :core:database:iosSimulatorArm64Test :core:data:iosSimulatorArm64Test` 成功，43 秒，787 个任务；Room KSP 重新生成后，已提交的 schema 无差异。
- 数据库测试实际执行 6 项（3 条历史迁移 + 3 项编解码），全部通过。`core:data` 当前没有测试源，任务跳过，不能将它记为有数据层测试执行。
- 最终 lint 为 0 错误、10 警告：6 个 AGP 版本提示、3 个依赖更新提示、1 个清单 UnusedAttribute；未用 suppression 隐藏这些提示。
- `:composeApp:testAndroidHostTest :core:framework:iosSimulatorArm64Test` 成功，31 秒。完整 Android 宿主测试共 574 个矩阵组合，执行 110 项、场景不适用跳过 464 项，0 失败 / 0 错误；覆盖导航、预测返回、页面过渡、返回栈序列化、主题及自适应 UI。framework 测试命中此前已通过的同一输入缓存。
- 首次冷组合构建在 dex / lint / Native 测试编译发生 Java heap / GC overhead OOM；按 AGENTS.md 使用 Kotlin / Gradle 8GB 堆并设置 `--max-workers=4` 后成功。仅命令行设置，不改变项目默认构建配置。
- 官方仓库部分下载停滞时，以模块元数据的 size / SHA-256 / SHA-1 校验分段下载，再由 Gradle 注册缓存；没有替换依赖来源或修改项目仓库配置。

成功的命令使用 `--offline --max-workers=4 -Dkotlin.daemon.jvmargs=-Xmx8g -Dorg.gradle.jvmargs="-Xmx8g -Dfile.encoding=UTF-8 -XX:+UseParallelGC"`。最终日志：`/private/tmp/notes-api-final-8g.log`、`/private/tmp/notes-api-final-ui.log`；这些是临时本地证据，持久测试报告仍位于各模块 build/test-results 及 build/reports。

- 最终 Xcode simulator 整包构建 `CODE_SIGNING_ALLOWED=NO build` 成功，包含升级后的 Kotlin framework 链接与 Swift 编译；日志 `/private/tmp/notes-api-final-xcode.log`。
- 最终整包在 iPad Pro 13 iOS 18.5 模拟器安装并启动成功，截图确认登录左右分栏及输入区域正常；设备恢复原始 Shutdown 状态，未修改显示参数。临时截图 `/private/tmp/notes-api-final-ipad.png`。
