# API 迁移实施计划

依据：已批准的 [过时 API 审计方案](../../audits/2026-10-02-outdated-api.md)。

目标：按顺序移除 AGP 内部接口、将 iOS Toast 绑定到实际 Compose 宿主窗口、升级已确认的稳定补丁依赖。可选文本框改造留到独立需求。

架构：Android 用公开 VariantOutput Provider 设置 APK 名称；iOS 每个 App 宿主拥有自己的 SystemToast presenter，在 App 组合根通过内部平台函数装配，取消全局原生 Toast 单例。Android 实现仍由 Koin 注入，iOS 实现只由宿主创建，保持编译期依赖检查开启。显示时从宿主取得 windowScene，使用该场景创建窗口；根视图布局回调基于 bounds 和安全区重新计算标签位置；宿主销毁时取消任务并关闭窗口。共享业务契约 SystemToast 保持不变。

- [x] 1. 修改 `androidApp/build.gradle.kts`，删除内部导入，用 `outputFileName.set(output.versionName.zip(output.versionCode) { ... })` 生成原有名称；执行 `:androidApp:assembleDebug`，检查四个 flavor 的输出元数据与 APK。
- [x] 2. 在 `core:framework` 添加 iOS Toast 几何测试：窄窗口、窗口原点、安全区、缩放布局；先运行测试确认未实现，再实现几何计算。
- [x] 3. `IosSystemToast` 改为宿主创建的对象，窗口用 `UIWindow(windowScene:)`，在 UIViewController 布局回调中计算标签位置，取消替换与 dispose 时清理窗口。
- [x] 4. `App.kt` 内部通过平台 rememberSystemToast 装配，保持 App 公共签名不变；`MainViewController.kt` 通过 LocalUIViewController 为每个 Compose 宿主创建 presenter，并在 DisposableEffect dispose 时关闭。
- [x] 5. 执行 framework iOS 测试、composeApp iOS 编译和 Xcode simulator 构建，修复编译问题；更新架构中的 Toast 生命周期说明。
- [x] 6. 分组更新版本目录：CMP 1.12.1 / JetBrains Navigation3 1.1.2 / Window 1.5.1，然后 AndroidX NavigationEvent 1.1.2 和 Room 3.0.3；核实后补入 Core 1.19.1（core 坐标）与 SQLite 2.7.1。保留 Material3、Adaptive、AGP、Gradle 与预发布依赖版本。
- [x] 7. 验证 Android 构建与 lint、iOS 编译、数据层测试及自适应 UI 测试；更新审计报告实施状态和证据，检查 diff。

本次工作在 `codex/api-modernization` 分支实施；用户已要求提交合并，整理为一个最终提交后以 fast-forward 合入本地 master。构建串行运行，模拟器显示设置保持原状。

完成证据：四个 Android debug flavor 构建、lint（0 错误 / 10 警告）、iOS 编译与 Xcode simulator 整包通过；数据库 6 项测试与 Android 宿主 110 项实际执行全部通过，Toast 几何 4 项通过。详细日志和 UIKit 人工验收边界见审计报告实施记录。
