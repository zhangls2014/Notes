# 项目文档

这里保留当前规范、可重复的操作指南、必要的决策理由和仍有独立价值的调查证据。修改代码时，以当前规范和源码为准；历史调查不能作为当前实现或本次验证的依据。

## 当前规范

- [项目架构](architecture.md)：模块职责、依赖边界、导航、MVI、自适应布局和变更检查清单。它是架构信息的唯一当前来源。
- [根 AGENTS.md](../AGENTS.md)：供 AI 使用的构建命令、全局硬约束和验证要求。
- [根 README.md](../README.md)：面向开发者的项目概览、环境要求和快速开始。
- [依赖与工具链升级规范](dependency-upgrades.md)：官方范围上限、正式版优先、版本兼容选择、验证和授权边界。

## 文档目录

```text
docs/
├── README.md              # 文档入口与保留规则
├── architecture.md        # 当前模块职责、依赖边界与运行时架构
├── dependency-upgrades.md # 长期依赖与工具链升级规范
├── decisions.md           # 重要决策的理由与历史追溯
├── testing/               # 测试入口与平台/专项操作指南
│   ├── README.md          # 源集、模块与回归命令索引
│   └── pending-acceptance.md # 未解决问题与未完成验收
└── audits/                # 有独立保留价值的调查与验证证据
```

新增或拆分模块时同步根 README 的结构图与 `architecture.md`；移动测试或改变任务时同步测试指南。历史记录保留当时的路径、版本和验证结果，后续状态可补充，不能把旧结果改写成当前验证结论。

## 保留与清理规则

- 当前规则只在对应规范中维护；其他文档通过链接引用，不另存一套现行架构。
- 文档必须至少提供一种价值：当前开发规则、可重复操作、未完成事项，或无法从源码直接恢复的重要决策与实验依据。
- 临时设计和计划仅在需求进行期间保留，可使用 `superpowers/specs/YYYY-MM-DD-<topic>-design.md` 与 `superpowers/plans/YYYY-MM-DD-<topic>.md`。结束或被取代后，将最终约束写入对应规范，必要理由写入 `decisions.md`，未完成事项写入 `testing/pending-acceptance.md`，再清理过程文件。
- 一次性调查放入 `audits/YYYY-MM-DD-<topic>.md`，注明日期、状态、测量条件和验证限制。完成后只保留能避免重复调查的实验、重大版本决策或仍需跟进的证据；普通已完成审计在提炼后清理。
- 新增文档时先判断能否更新现有文件；不为每次小修改永久新增设计、计划和审计三套记录，也不把已完成过程文件全部搬入归档目录。
- 清理前确认原文件已纳入 Git；未提交的独有内容先保存。检查剩余文档的引用和待办没有丢失，再执行删除。删除须遵守 Xander 的授权边界。
- 每个需求结束时检查文档状态；验收完成后更新集中清单中的证据与日期，不把历史通过记录当作当前版本已经通过。

## 操作指南

- [测试指南](testing/README.md)：共享/平台源集分布、按模块选择回归与静态检查命令。
- [Android 模拟器测试](testing/android-emulator.md)：多尺寸、旋转、折叠姿态测试，以及显示设置的强制还原流程。
- [自适应 UI 回归](testing/adaptive-ui.md)：宿主行为测试、候选截图与人工批准视觉基线流程。
- [登录凭据存储验证](testing/secure-token-storage.md)：迁移回归、平台安全存储验收和 Keychain 测试环境限制。
- [iOS Launch Screen](testing/ios-launch-screen.md)：Android 图标资源映射、系统启动页配置与验证。
- [iOS Toast 无障碍验证](testing/ios-toast-accessibility.md)：原生字体回归、播报与 Dynamic Type 设备验收。
- [待验收与已知限制](testing/pending-acceptance.md)：集中跟踪设备验收、截图采集问题和当前升级验证缺口。

## 决策与调查证据

以下调查记录特定日期的代码、环境与测量结果，不能直接外推为当前状态。

- [重要决策记录](decisions.md)：铰链范围、Tab 状态、IME、主题、凭据与 Feature 边界的决策理由。
- [2026-10-07 依赖与工具链升级](audits/2026-10-07-dependency-upgrade.md)：全量版本决策、预览版例外与本次验证状态。
- [2026-10-07 iOS 持续白屏调查与修复](audits/2026-10-07-ios-white-screen.md)：Keychain 布尔参数桥接错误的单字段对照、Native 回归及模拟器首屏验收。
- [2026-10-02 iOS 启动耗时调查](audits/2026-10-02-ios-startup.md)：启动阶段、PhotosUI 最小链接实验与头像选择器对照；保留测量条件及精度。

## 历史追溯

2026-10-07 精简文档时，保留了当前规范和专项指南，将已完成设计、实施计划及普通审计的有效内容提炼到决策记录与待验收清单。清理前版本为 `4c33e11d07ca9c233442608ad833382440aeb6cd`，原始记录通过 Git 查询，不作为当前工作指令。

```bash
# 查询清理前的文档清单
git ls-tree -r --name-only 4c33e11d07ca9c233442608ad833382440aeb6cd docs

# 阅读原始审计；历史方案可能已被取代
git show 4c33e11d07ca9c233442608ad833382440aeb6cd:docs/audits/2026-10-02-accessibility.md
```
