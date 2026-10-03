# 设置项映射优化计划

目标：统一 settings 内的类型安全构造辅助函数，使平台能力可注入，保持清单顺序、值映射和 Intent 行为。

架构：清单与行为继续归 feature:settings，core:preference 只提供元数据和渲染契约。保留 SettingsScreen 的 remember 缓存与现有调用方式。

- [x] 在 feature/settings/src/commonTest 中添加清单、唯一 key、当前值、操作与修改回调测试，验证缺少能力参数时失败。
- [x] 为 SettingsModel.toPreferenceUiModels 添加默认取平台值的 dynamicColorSupported 参数。
- [x] 使用 private actionPreference 和 selectPreference<T> 统一重复构造；单独的 Toggle 直接在清单构造。
- [x] 更新 PreferenceSpec 注释和架构文档，明确 common 清单与 expect/actual 能力位职责。
- [x] 运行 settings 宿主测试、iOS 测试源编译和模块 detekt，检查差异。

验证命令：`./gradlew :feature:settings:testAndroidHostTest :feature:settings:compileTestKotlinIosSimulatorArm64 :feature:settings:detekt`。

当前工作区已有其他修改，本次不创建过程提交，不改动无关文件。

验证结果：5 项 Android 宿主测试通过，iOS 测试源编译通过，git diff --check 通过。模块 detekt 仍报告 SettingsScreen.kt 和 SettingsViewModel.kt 中原有的 8 项告警；本次 mapper 与测试无剩余告警。函数签名格式沿用现有基线，清理被移除辅助函数的过期条目。
