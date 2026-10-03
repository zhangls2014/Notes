# 注释与实现一致性审计

日期：2026-10-03。状态：全部 10 项注释已修订，静态验证通过；未修改功能代码。检查基准：`e8701e71`。

## 范围与方法

静态扫描 Git 跟踪的 Kotlin、Gradle Kotlin DSL、Swift、XML、TOML、properties 与 xcconfig，共 386 个源码/配置文件，其中 229 个包含注释。排除 Room schema 与 detekt baseline 中保存的历史源码片段；不将历史设计、计划和审计中的旧描述视为当前源码注释。

对模块归属、导航、MVI 持久化、登录窗格、共享控件、文件处理和构建命名的候选项逐一读取上下文与调用方。Koin 索引规则另核对本机缓存的 4.2.2 sources jar 中 `BeanDefinition.kt#indexKey`。本次为静态审计，不运行应用或完整测试，不据此宣称运行时功能存在故障。

检查时确认 8 项明确偏差，另有 2 项表述精度建议，现已全部修订。以下问题描述与行号保留检查基准时的证据；当前源码行号可能因修订移动。

## 明确偏差

### 1. 导航贡献把函数名唯一误写成绑定隔离条件

位置：[NavigationContribution.kt](../../core/framework/src/commonMain/kotlin/me/zhangls/framework/nav/NavigationContribution.kt)，第 9–14 行。

注释示例只有 `@Single`，并要求“登记函数名在各 feature 之间必须唯一”。实际各 Feature 使用 `@Named` 登记唯一 qualifier；Koin 的 `indexKey` 由类型、qualifier 与 scope 构成，不包含 provider 函数名。仅给函数取不同名字不能保证这些同接口绑定被分别保留。

证据：[EmailModule.kt](../../feature/email/src/commonMain/kotlin/me/zhangls/email/EmailModule.kt) 第 22–28 行已经正确解释 qualifier；main、login、profile、about 的导航贡献也都有独立 `@Named`。

建议：示例同时列出 `@Single` 与 `@Named("<feature>NavigationContribution")`，要求 qualifier 唯一，删除将函数名当作 Koin 索引依据的说明。

影响：按公共契约注释新增绑定时，可能遗漏 qualifier，使导航贡献覆盖或注册失败。现有绑定已配置 qualifier，本次未发现它们因此丢失。

### 2. 自动收集导航贡献被描述为整个组合根零改动

位置：

- [AppNavHost.kt](../../composeApp/src/commonMain/kotlin/me/zhangls/entry/AppNavHost.kt)，第 63–64 行。
- [NavigationRegistry.kt](../../composeApp/src/commonMain/kotlin/me/zhangls/entry/NavigationRegistry.kt)，第 14–15 行。
- [NavigationContribution.kt](../../core/framework/src/commonMain/kotlin/me/zhangls/framework/nav/NavigationContribution.kt)，第 21 行。

这些注释声称组合根不持有任何 Feature 路由知识，新增 Feature 时零改动。实际自动聚合的范围是目的地序列化注册表与 Deep Link 匹配器；根导航仍显式调用 `profileNavEntry` / `aboutNavEntry` 等 Entry 装配函数，[NavHandler.kt](../../composeApp/src/commonMain/kotlin/me/zhangls/entry/NavHandler.kt) 的 `rootFor` 还显式决定个人信息、关于页面与邮件详情的 Tab 归属。[NotesModule.kt](../../composeApp/src/commonMain/kotlin/me/zhangls/entry/NotesModule.kt) 与共享模块依赖也需接入新实现。

建议：改为“新增序列化与 Deep Link 贡献不需要修改 Registry 的收集逻辑；Feature 实现、Nav Entry 与必要的根导航策略仍由 composeApp 显式装配”。

影响：容易让开发者漏掉 Entry、Koin 模块或 Tab 归属的装配步骤。

### 3. AppState 注释否认了实际保存的设置快照

位置：[AppViewModel.kt](../../composeApp/src/commonMain/kotlin/me/zhangls/entry/AppViewModel.kt)，第 30 行。

注释说主题/语言的“真实取值来自 DataStore，不在本 State 里”。[AppState.kt](../../composeApp/src/commonMain/kotlin/me/zhangls/entry/AppState.kt) 明确包含 `dynamicColor`、`darkTheme`、`fontSize` 与 `appLanguage`；ViewModel 第 48–54 行将 Repository 发出的值复制到 State。`savedKey = "state"` 配合 `MviViewModel` 保存整个 AppState，而非仅保存登录标志。

建议：写明“DataStore 是设置的持久数据源；AppState 保存用于 UI 的设置快照，并随 savedKey 恢复；Repository 流随后更新快照”。保留非敏感、小型状态的限制。

影响：误导对跨进程恢复范围和持久化数据边界的判断。

### 4. 登录表单与品牌窗格的角色说明相反

位置：[LoginScreen.kt](../../feature/login/src/commonMain/kotlin/me/zhangls/login/LoginScreen.kt)，第 193、298 行。

注释分别将 FormPane 称为“主窗格”、BrandPane 称为“支持窗格”。同文件第 165–175 行实际把 BrandPane 放入 `mainPane`，FormPane 放入 `supportingPane`。紧凑窗口由 `loginPanePlan` 指定 Supporting 为当前目的地，使表单保留。

建议：FormPane 改称支持窗格，BrandPane 改称主窗格；补充窄窗口优先显示表单的策略，避免把视觉重要性与 Adaptive 的角色名混为一谈。

影响：调整隐藏策略、目的地或窗格宽度时容易选错角色。

### 5. 居中标题栏仍描述已删除的右侧占位按钮

位置：[CenteredTopAppBar.kt](../../core/theme/src/commonMain/kotlin/me/zhangls/theme/component/CenteredTopAppBar.kt)，第 22 行。

注释说通过右侧空白不可用 IconButton 实现居中。实际直接使用 `CenterAlignedTopAppBar`，仅按需提供 navigationIcon，并原样传入调用方的 actions；没有空白按钮。

建议：说明标题对齐交给 Material 组件，包装器提供本地化返回按钮和 actions 插槽，删除占位按钮描述。

### 6. iOS framework 命名示例大小写不符

位置：[KmpConventions.kt](../../build-logic/src/main/kotlin/me/zhangls/convention/KmpConventions.kt)，第 19–20 行。

注释示例为 `dataKit` 与 `loginApiKit`。函数第 26–29 行对每段首字母大写后拼接，实际结果是 `DataKit` 与 `LoginApiKit`；`ComposeApp` 示例正确。

建议：只修正文档示例，保持现有产物命名不变。

### 7. 分组行参数被描述为仅控制外角

位置：[PreferenceRow.kt](../../core/preference/src/commonMain/kotlin/me/zhangls/preference/ui/PreferenceRow.kt)，第 52 行。

注释说 `isGroupStart` / `isGroupEnd` 仅控制外角。实际第 61–67 行按这些标志在 `extraLarge` 外角与 `extraSmall` 内角之间选择；组内连接边也由它们决定。

建议：改为“根据分组首尾选择上下边的外角或内角；分组清单与顺序仍由消费方拥有”。

### 8. 发送中的 FAB 仅阻止业务操作，没有禁用控件语义

位置：[EmailFab.kt](../../feature/email/src/commonMain/kotlin/me/zhangls/email/component/EmailFab.kt)，第 27 行。

注释称发送中“禁用点击”。实际第 74 行仅在 onClick 内通过 `if (!isSending)` 忽略打开草稿操作；按钮本身仍挂载点击处理，未按发送状态切换 disabled 语义。

建议：按现有行为将注释改为“发送中展示进度并忽略打开草稿操作”。若产品要求向辅助技术表达不可用，应另行调整交互与语义，并验证发送期间的可达性；本次不将注释检查扩展为行为修复。

## 表述精度建议

### 9. 文件名回退说明与扩展名回退说明重复

位置：[AppFileManager.kt](../../core/data/src/androidMain/kotlin/me/zhangls/data/util/AppFileManager.kt)，第 100、111 行。

两处都写“如果没有扩展名，用 MIME 补”。第 100 行属于 `getFileNameNoExtension`：它使用 MIME 的主类型（例如 `image`）作为文件名回退；第 111 行属于 `getFileExtension`：它使用 MIME 子类型（例如 `jpeg`）。当前文字没有区分两个函数输出，容易读成同一种补扩展名行为。

建议：分别描述文件名与扩展名的回退规则。这是注释精度建议，不据此判定回退算法错误。

### 10. Tooltip 使用场景仍举设置页顶部入口

位置：[TooltipIconButton.kt](../../core/theme/src/commonMain/kotlin/me/zhangls/theme/component/TooltipIconButton.kt)，第 20 行。

说明举例为“设置页顶部的深色模式 / 语言入口”。当前这两个图标入口位于登录页 FormPane，由 `SelectIconButton` 复用 TooltipIconButton；设置页顶栏只有标题，主题和语言以列表行展示。

建议：把当前使用场景改为登录页的设置入口；若保留旧场景，应明确它是历史背景，避免让读者在错误的 Feature 查找调用方。

## 未计为不一致的情况

- 使用“早先”“原先”明确标识的历史背景，当前实现已在后续说明清楚时，不算过时注释。
- 关于页面的更新检查、邮件回复/回复全部和模拟登录的 TODO 与当前尚未实现的功能一致。
- 保留旧 `ProfileDestination` 序列化名称的说明与显式 `@SerialName` 一致。
- 数据层凭据迁移、Keychain 测试限制和 MVI 默认不保存的说明，与本次读取到的实现一致。
- 性能耗时记录、第三方 API 原理说明未做新的运行时测量或全面外部验证，不作为本次一致性结论。

## 修订与验证（2026-10-03）

- 第 1–2 项：明确唯一 qualifier 的登记条件，以及 Registry 聚合与 composeApp 显式装配的职责范围。
- 第 3–4 项：说明 AppState 保存整个小型 UI 快照，并修正表单/品牌窗格的实际角色与窄窗口选择。
- 第 5–7 项：删除标题栏占位按钮描述，修正 framework 名称大小写，并说明分组首尾选择外角/内角。
- 第 8–10 项：按现有行为说明 FAB 发送期间忽略操作但保留点击语义，区分 MIME 主/子类型回退，更新 Tooltip 使用场景。

共修改 11 个 Kotlin 源码/构建文件的注释。对每个文件读取 HEAD 原文与修改后内容，按词法跳过行注释、支持嵌套的块注释和注释外空白，并保留字符串、字符及原始字符串后比较代码标记，全部一致；`git diff --check` 通过。审计报告及索引的本地链接已检查。

本次仅修订注释与审计文档，未执行完整构建或运行时测试。FAB 的 disabled 语义仍保持原行为；如需要改变，应作为独立行为变更验证。
