# 项目文档

这里的文档按用途分为当前规范、操作指南和历史审计。修改代码时，应以当前规范和源码为准；历史审计只用于理解问题背景和验证证据。

## 当前规范

- [项目架构](architecture.md)：模块职责、依赖边界、导航、MVI、自适应布局和变更检查清单。它是架构信息的唯一当前来源。
- [根 AGENTS.md](../AGENTS.md)：供 AI 使用的构建命令、全局硬约束和验证要求。
- [根 README.md](../README.md)：面向开发者的项目概览、环境要求和快速开始。
- [依赖与工具链升级规范](dependency-upgrades.md)：官方范围上限、正式版优先、版本兼容选择、验证和授权边界。

## 文档目录

```text
docs/
├── architecture.md       # 当前模块职责、依赖边界与运行时架构
├── dependency-upgrades.md # 长期依赖与工具链升级规范
├── testing/              # 测试入口与平台/专项操作指南
│   └── README.md         # 源集、模块与回归命令索引
├── audits/               # 带日期和后续状态的一次性审计
└── superpowers/
    ├── specs/            # 设计方案与已确认约束
    └── plans/            # 实施步骤及当次验证记录
```

新增或拆分模块时同步根 README 的结构图与 `architecture.md`；移动测试或改变任务时同步测试指南。历史记录保留当时的路径、版本和验证结果，后续状态可补充，不能把旧结果改写成当前验证结论。

## 操作指南

- [测试指南](testing/README.md)：共享/平台源集分布、按模块选择回归与静态检查命令。
- [Android 模拟器测试](testing/android-emulator.md)：多尺寸、旋转、折叠姿态测试，以及显示设置的强制还原流程。
- [自适应 UI 回归](testing/adaptive-ui.md)：宿主行为测试、候选截图与人工批准视觉基线流程。
- [登录凭据存储验证](testing/secure-token-storage.md)：迁移回归、平台安全存储验收和 Keychain 测试环境限制。
- [iOS Launch Screen](testing/ios-launch-screen.md)：Android 图标资源映射、系统启动页配置与验证。
- [iOS Toast 无障碍验证](testing/ios-toast-accessibility.md)：原生字体回归、播报与 Dynamic Type 设备验收。

## 历史审计

以下文档记录特定日期的代码状态，正文中的“现状”和“待调整”可能已经过期。每份报告顶部列出了后续状态。

- [2026-10-07 依赖与工具链升级](audits/2026-10-07-dependency-upgrade.md)：全量版本决策、预览版例外与本次验证状态。

- [2026-09-21 多形态屏幕适配审计](audits/2026-09-21-adaptive-layout.md)
- [2026-09-22 Android 17 多形态适配审计](audits/2026-09-22-android-17-multiform-factor.md)
- [2026-09-23 折叠屏铰链避让与姿态稳定性审计](audits/2026-09-23-foldable-hinge.md)
- [2026-09-25 Koin 依赖注入边界审查与优化建议](audits/2026-09-25-koin-dependency-injection.md)
- [2026-09-25 自适应 UI 与架构审计](audits/2026-09-25-adaptive-ui-review.md)
- [2026-09-28 输入页面 IME 遮挡排查与修复](audits/2026-09-28-ime-input-visibility.md)
- [2026-10-01 Material 3 Expressive 迁移可行性](audits/2026-10-01-material3-expressive-feasibility.md)
- [2026-10-02 iOS 启动耗时调查](audits/2026-10-02-ios-startup.md)
- [2026-10-02 全项目过时 API 审计与更新方案](audits/2026-10-02-outdated-api.md)
- [2026-10-02 无障碍适配审计](audits/2026-10-02-accessibility.md)
- [2026-10-03 注释与实现一致性审计](audits/2026-10-03-comment-consistency.md)：8 项明确偏差与 2 项表述精度建议已全部修订，含静态验证记录。

## 设计与实施记录

`superpowers/` 保存功能开发、模块拆分与文档调整的一次性记录，不作为项目架构规范。[设计目录](superpowers/specs/) 保存设计方案，[实施目录](superpowers/plans/) 保存计划与当次验证。最近涉及结构变化的记录：

- [2026-10-03 个人信息设计](superpowers/specs/2026-10-03-personal-information-design.md)：入口、头像保存与无障碍交互约束。
- [2026-10-03 个人信息模块拆分](superpowers/plans/2026-10-03-profile-module-split.md)：已实施；profile API/实现归属、旧目的地序列化兼容与拆分验证。
- [2026-10-03 关于应用页面开发方案](superpowers/plans/2026-10-03-about-app.md)：已实施；品牌卡片设计、About 模块边界与当次验证记录。
