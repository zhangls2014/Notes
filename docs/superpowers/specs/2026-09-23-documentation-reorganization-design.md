# 文档体系整理设计

## 目标

把仓库文档划分为“当前规范”“操作指南”和“历史审计”三类，使维护者和 AI 能快速找到有效信息，不再从历史过程里推断当前架构。同时同步根 `README.md`，修正已经过期的模块、版本和功能描述。

## 信息架构

整理后的目录如下：

```text
AGENTS.md
README.md
docs/
├── README.md
├── architecture.md
├── testing/
│   └── android-emulator.md
└── audits/
    ├── 2026-09-21-adaptive-layout.md
    └── 2026-09-22-android-17-multiform-factor.md
```

`AGENTS.md` 是 AI 的入口，只保留可执行命令、全局硬约束、验证要求和文档索引。`README.md` 面向项目读者，提供准确的项目概览和常用入口。`docs/architecture.md` 是当前架构的唯一说明。`docs/testing/` 保存可重复的操作流程。`docs/audits/` 保存带日期的历史证据，不作为当前状态来源。

## 文件处理

### `AGENTS.md`

压缩项目介绍和模块清单，保留以下内容：

- Android/iOS 构建、测试和 lint 命令；
- Feature API/实现模块边界、Core 依赖方向、`implementation` 隔离；
- Navigation 3、窗口信息、MVI 持久化等全局硬规则；
- Compose Resources、Room schema、Gradle 并发和模拟器还原约束；
- 指向详细架构、模拟器操作和历史审计的链接。

删除具体组件枚举、迁移历史、事故叙述和已能从代码直接读取的实现细节。

### `docs/architecture.md`

以当前代码和现有 `AGENTS.md` 为事实来源，将新增的通用 KMP 文档改写成本项目规范。文档采用实际名称和技术：`:composeApp`、Koin、`Destination`/`NavEffect`、Feature Entry 接口、`commonMain`、`core:data`/`core:database` 隔离、Material 3 Adaptive。

通用文档中与本项目不符的 `:app`、Hilt、`Route` 后缀、`EntryProviderInstaller`、Feature 内 Repository 实现等示例不保留。仍有价值的模块边界、Contract 判断方法和验收清单将按本项目实现重写。

### 历史审计

两份审计报告移动到 `docs/audits/` 并保留原始主体，避免丢失证据。每份文档顶部增加状态说明：报告描述的是审查当时的状态，当前架构以 `docs/architecture.md` 为准。

对已经明确过期、会误导读者的结论增加“后续状态”说明。例如 Android 17 报告中的登录页铰链问题已经由 `SupportingPaneScaffold` 和相关测试解决；跨进程状态持久化仍是有意的 opt-in 设计。

### Android 模拟器指南

从审计文档提炼模拟器多尺寸、旋转和姿态测试方法。所有修改分辨率、密度或旋转状态的流程都必须有清理步骤；推荐脚本使用 `trap`，退出时执行 `wm size reset`、`wm density reset` 并恢复自动旋转。验证时查询 `wm size` 和 `wm density`，确认不存在 `Override`。

### `README.md` 与文档索引

根 README 修正当前模块结构、版本、Feature 归属和 MVI 状态持久化描述，并链接 `docs/README.md`。文档索引说明每份文档的用途、权威程度和更新时间，避免历史审计被误当成当前规范。

## 内容原则

- 当前事实只维护一份，其他文档通过链接引用。
- 硬约束说明“必须做什么”；设计理由放在架构文档。
- 审计报告不持续改写成架构规范，只追加状态说明。
- 命令必须可以直接执行，并与仓库当前 Gradle task 对齐。
- 不删除仍有价值的审查证据，不改业务代码和构建配置。

## 验证

整理完成后执行以下检查：

1. 检查所有 Markdown 相对链接均指向存在的文件。
2. 搜索旧路径和旧模块描述，确保没有悬空引用。
3. 对照 `settings.gradle.kts`、version catalog 和关键源码核对模块、版本与架构名称。
4. 运行 `git diff --check`，确认无空白和补丁格式问题。
5. 检查工作区状态，确认未覆盖任务开始前的用户改动。
