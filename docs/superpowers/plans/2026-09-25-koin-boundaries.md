# Koin 依赖边界优化实施计划

> 本计划依据 [依赖注入审查](../../audits/2026-09-25-koin-dependency-injection.md) 执行；源码事实以本次复查为准。

**目标：** 移除启动任务与平台桥接的全局服务定位器，并验证能否恢复 Koin 编译安全检查。

**结构：** 保留 `NotesModule` 组合根和现有 Feature API 边界。将启动任务和平台能力作为显式构造依赖，Android/iOS 入口仍只启动一次 Koin。导航贡献机制保持不变。

**技术栈：** Kotlin Multiplatform、Koin 注解与 Compiler Plugin、Compose Multiplatform、SwiftUI。

## 任务 1：校正审查事实

- [x] 检查 Android `NotesApp`、SwiftUI `iOSApp.init` 与 `MainViewController` 的调用顺序。
- [x] 更正文档里 iOS 未启动 Koin 的误判。

## 任务 2：启动数据任务

- [x] 在 `composeApp/src/commonTest` 写行为测试：首次启动插入样例数据并更新启动次数/版本；后续启动不重复插入。
- [x] 运行该测试，确认因尚无可构造的启动任务而失败。
- [x] 将 `InitData` 改为构造注入的任务，提供挂起的 `run()`；应用组合根用协程作用域调用，避免任务自身永久持有作用域。
- [x] 运行测试、Android 编译和 iOS 编译。

## 任务 3：平台信息与 Toast

- [x] 将 Android `AppInfo` 对 `Context` 的依赖放入构造函数；保持 iOS 实现无需平台容器。
- [x] 将系统 Toast 改成平台实例，由应用 UI 入口注入，移除 Android `ToastContextProvider : KoinComponent`。
- [x] 编译 Android 与 iOS，检查未增加 ViewModel 对 `Context` 的依赖。

## 任务 4：依赖图检查

- [x] 分别开启 `composeApp` 和 `core:network` 的 `compileSafety`，运行 Android 与 iOS 编译。
- [x] 检查编译输出；本次没有出现缺失绑定或插件限制，无需增加例外。
- [x] 静态检查三个导航贡献的 qualifier 均唯一；未修改 `getAll()` 逻辑。

## 收尾验证

- [x] 串行运行相关 iOS commonTest、`:androidApp:assembleDebug`、`:composeApp:compileKotlinIosSimulatorArm64`。
- [x] 运行 `git diff --check`，更新审查状态与当前架构文档，报告未能验证的运行场景。

## 2026-09-28 提交拆分

按用户要求，每个问题独立提交并验证：

1. 启动数据任务与 AppInfo 构造注入（包含行为测试）。
2. 系统 Toast 平台接口注入。
3. Koin 编译检查与最终审查记录。

每个阶段均验证 Android 与 iOS 编译；第一阶段运行启动任务测试，最后阶段执行完整 Android Debug 构建及 iOS commonTest。本分支不在此步骤合入 master。
