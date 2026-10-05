# AGENTS.md

本文件是 AI 在本仓库工作的入口，只包含可执行命令和全局硬约束。详细架构见 [docs/architecture.md](docs/architecture.md)，文档索引见 [docs/README.md](docs/README.md)。

## 项目概况

Notes 是 Kotlin Multiplatform 应用，使用 Compose Multiplatform、Navigation 3、Koin 注解、Room、Ktor 和模块化架构。共享逻辑与 UI 位于 `commonMain`，供 Android 与 iOS 复用。

## 构建与验证

```bash
# Android 整包构建（:composeApp 是 library，没有 assembleDebug）
./gradlew :androidApp:assembleDebug

# 安装 Android 应用
./gradlew :androidApp:installDebug

# iOS 编译验证
./gradlew :composeApp:compileKotlinIosSimulatorArm64

# 共享逻辑与 Android 宿主回归（按改动选择模块）
./gradlew :composeApp:testAndroidHostTest :core:data:testAndroidHostTest
./gradlew :feature:profile:testAndroidHostTest :feature:email:testAndroidHostTest

# iOS 平台与 commonTest 通过模拟器执行
./gradlew :composeApp:iosSimulatorArm64Test
./gradlew :core:data:iosSimulatorArm64Test

# 自适应 UI 宿主测试；截图候选与基线命令见 docs/testing/adaptive-ui.md
./gradlew :composeApp:testAndroidHostTest --tests '*AdaptiveUiTest*'

# Android lint；根 detekt 无源集，不作为有效检查；按模块运行 detekt
./gradlew :androidApp:lintDevFullDebug
./gradlew :feature:profile:detekt

# 实验性 Fused Library
./gradlew :android:output:login:assemble
```

全量冷构建或 dex 合并发生 Java heap space OOM 时：

```bash
./gradlew :androidApp:assembleDebug \
  -Dkotlin.daemon.jvmargs=-Xmx8g \
  -Dorg.gradle.jvmargs="-Xmx8g -Dfile.encoding=UTF-8 -XX:+UseParallelGC"
```

按改动范围选择测试与平台检查，源集及专项回归见 [测试指南](docs/testing/README.md)。

iOS 应用使用 Xcode 打开 `iosApp/iosApp.xcodeproj` 运行。Android lint 报告位于 `androidApp/build/reports/lint-results-*.html` / `.sarif`。

## 模块与依赖硬规则

- Feature 使用同级 API/实现模块：`:feature:<name>-api` 与 `:feature:<name>`。实现必须依赖自身 API。
- Feature 实现不得依赖其他 Feature 实现；允许依赖对方 `-api`。Feature API 不得依赖任何 Feature 实现。
- 只有 `:composeApp` 依赖全部 Feature 实现并完成 Koin、导航和应用装配；Core 不得依赖 Feature。
- 跨 Feature 共享的静态 UI 数据放到合适的 Core 模块，不挂在某个 Feature API 下。
- Gradle 默认使用 `implementation`；只有依赖类型出现在当前模块公开 API 时才使用 `api`。
- 新增 `-api` 模块使用 `me.zhangls.kmp-compose-api`，不要手工重复配置 Compose 插件。
- 当前 Native 编译链下，跨模块 `@Composable` 抽象 Entry 方法不得声明默认参数，调用方显式传参；新增或修改 Entry 必须运行 iOS 真实实现调用回归，不能只验证编译。升级编译链后须经回归验证再决定是否解除限制。

### 数据边界

- `core:data` 对 `core:database` 必须使用 `implementation`，不得向 Repository 接口或公开模型泄漏 Room entity、DAO、查询载体。
- `core:data` 的公开面是 `model/`、`repository/`、`util/`、`DataModule.kt`；实现放在 `impl/` 且保持 `internal`。
- Room 字段变化必须配置 AutoMigration 并更新 `core/database/schemas/`。移动 `AppDatabase` 包名时同步迁移以全限定名命名的 schema 目录。
- `core:model` 只放跨数据层和 UI 基础模块使用的稳定值类型，不含 Compose。

### UI 与设置边界

- 应用内功能图标必须来自 Google Fonts 的 Material Symbols，统一使用 Rounded，默认未填充（`FILL=0`）；明确的填充变体（如 `StarFill`，`FILL=1`）允许保留并用于状态表达。共享图标由 `core:theme` 提供。转换去掉原始注释，import 与正文之间保留两行空行，使用原有缓存属性和 `path { ... }` 指令，不使用 `addPathNodes`，填充颜色统一为 `Color.Black`。品牌图标（登录页、关于应用页及桌面启动图标）保留现有设计，不受此功能图标约束限制。
- 窗口事实只在 `composeApp` 调用 `rememberWindowAdaptiveInfo()` 读取，通过 `LocalWindowAdaptiveInfo` 下发。
- `PaneScaffoldDirective` 只在组合根从同一窗口事实计算，通过 `LocalPaneScaffoldDirective` 下发，并统一使用 `HingePolicy.AlwaysAvoid`。Feature 不直接调用 `currentWindowAdaptiveInfo*()`，不维护第二套断点。
- 铰链避让只属于实际分栏的窗格布局：登录页 `SupportingPaneScaffold` 与邮件列表—详情 SceneStrategy 消费 `LocalPaneScaffoldDirective`。单栏页面、应用导航、搜索、设置和浮层不得读取铰链或增加安全区包装。同一铰链几何在半开和全开之间必须保持窗格稳定。
- 登录页保留左右分栏，不在窗格内避让横向铰链，不因横向折痕缩减内容高度。
- Feature 不用导航方位、设备名称等派生值反推窗口宽度或姿态。
- 内容宽度统一使用 `core:theme` 的 `ContentWidth` 和 `Modifier.contentWidth()`，不要在 Feature 中复制裸 `widthIn` 常量。
- 列表—详情由 Navigation 3 `SceneStrategy` 在根导航装配；邮件 Feature 不自建 pane scaffold。
- `core:preference` 只拥有设置元数据、渲染契约和通用控件，不持有当前值、Feature Intent、清单或顺序；底层 Compose Preference 依赖保持 `implementation`。
- `ProvidePreferenceLocals` 必须显式提供空的内存数据源，设置值与更新由 Repository 和 Feature 行为提供；不得使用控件库默认的平台偏好存储。修改包装器或升级控件库时运行 iOS 初始化回归。

## Navigation 3 与 MVI

- `core:framework` 定义 `Destination`、`NavEffect`、`RequireLogin`、Deep Link 和导航贡献；具体目的地与 Entry 契约位于 Feature API。
- 登录拦截、根返回栈、Deep Link 恢复和 SceneStrategy 装配属于 `composeApp/AppNavHost.kt`。
- Feature 的 MVI 类型统一放在 `mvi/`：`Intent`、`Action`、`State`、`Reducer`、`ViewModel`。
- `MviViewModel.savedKey` 默认 `null` 是安全约束。只有小型、可序列化且不含密码、token、大对象的状态才可显式启用跨进程恢复。

## 工作约束

- 每个独立需求合入 `master` 前，必须把设计、计划、实现、测试和文档等过程提交整理为一个最终提交。若 `master` 已前进，先将该最终提交 rebase 到最新 `master`，再仅以 fast-forward 方式合入；不得在 `master` 保留需求分支的多个过程提交或 merge commit，主线历史必须保持线性、干净。
- Compose Resources 格式化占位符必须带位置，例如 `%1$s`；不要使用 `%s`。
- 依赖版本只在 `gradle/kmp.versions.toml` 维护。
- 同一仓库目录下不要并发执行两个 Gradle 构建，避免守护进程地址注册表锁冲突。
- 修改架构时同步更新 [docs/architecture.md](docs/architecture.md)；一次性调查报告放入 `docs/audits/` 并注明日期和状态。
- 修复 UI 布局前明确需要保持的组件、尺寸、留白与交互；超出修复范围的视觉变化及正式截图基线更新须单独说明并取得用户确认。验收清单见 [自适应 UI 回归](docs/testing/adaptive-ui.md)。
- 交付时区分实际执行、通过、跳过、失败和未验收项；编译、行为测试、候选截图与设备验收分别报告，不互相替代。性能结论须注明测量条件与精度，详见 [验证证据与交付记录](docs/testing/README.md#验证证据与交付记录)。

### Android 模拟器显示设置

AI 为测试或调试修改模拟器分辨率后，必须在结束本次模拟器操作前对同一设备执行：

```bash
adb -s <serial> shell wm size reset
adb -s <serial> shell wm density reset   # 修改过密度时
```

随后查询 `wm size` / `wm density`，确认没有 `Override`。不得硬编码某机型的默认值。自动化脚本应通过 `trap` 或 `finally` 保证失败和中断时也还原；设备断连导致无法恢复时必须明确告知用户。完整流程见 [docs/testing/android-emulator.md](docs/testing/android-emulator.md)。
