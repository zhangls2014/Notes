# Inspect Code 分析与优化清单

日期：2026-10-09（Asia/Shanghai）
状态：已按确认范围实施检查配置、Room 定点处理与小范围清理；升级评估完成。回归存在失败及设备验收缺口，详见执行结果。

## 已确认执行范围

Xander 已确认按检查配置 → Room 误报 → 小范围清理 → 升级评估的顺序执行。闲置能力 C1–C5 全部保留，不修复或删除；升级只做评估，不更改版本目录、Wrapper 或平台配置。

- [ ] 1. 共享 Scope 与 profile 已落地，生成路径排除已通过重扫验证；额外入口配置已提供，但已知入口仍有误报，修正 profile ID 和 Single 注解后的再次 IDE 验收待完成。
- [x] 2. Room 构造器：添加官方推荐的定点 suppression 和原因；验证 Android/两个 iOS 目标。
- [x] 3. B1–B6（实施及复核完成，失败/设备验收另记）：精确清理空判断/import/测试变量，等价迁移时间参数与类型过滤，复核 converter suppression；运行对应宿主和 iOS 回归。保留 IME 与搜索栏既有差异。
- [x] 4. 评估扫描中的 Kotlin/AGP/Gradle/NavigationEvent/Paging 候选：核对官方元数据、兼容表和 Native 工件；输出可升级、应保留和证据缺口。

工作分支为 `codex/inspect-code-cleanup`，保留原工作区修改；本次整理源码、检查配置与审计证据为一个提交，不自动合入或发布。

提交范围补充：Xander 随后明确授权将开始时已有的 IME 第一处 Duration 转换和 EmailSearchBar 清理一起纳入同一个最终提交。这些差异在本次构建/回归时已存在，后续只改变暂存和提交归属，没有再改源码。

## 初次审计结论与范围

当前扫描不能直接转化为数千项代码整改。XML 导出记录中，7,667 条位于生成目录，1,484 条属于 IDE 模块依赖图；合计占 94.5%。其余 537 条仍包含测试入口、注入入口、平台契约、拼写和文档格式提示，并不等于 537 个缺陷。

唯一的 ERROR 是 Room 自动生成构造器的 IDE 识别问题。三个目标均已有对应 `actual`；本次 iOS 真机目标编译实际执行成功，Android 宿主测试和 iOS 模拟器编译为 `UP-TO-DATE`。没有证据表明缺少平台实现，也没有证据支持批量删除依赖或“未使用”声明。

初次审计建议先改善检查范围和入口识别，再做小范围源码清理，最后单独评估依赖升级和闲置 API。此清单按处理顺序排序，不将代码风格提示描述为运行时故障，也不从告警数量推导性能收益。

初次审计依据：Android Studio 中现有 `Inspections on Project 'Notes'`、`Project Default` profile 的 XML 导出，以及当前工作区源码。初次审计阶段没有重新执行完整 Inspect Code；实施后的重扫见执行结果。

- 扫描界面：1 ERROR、7,327 WARNING、1,353 WEAK WARNING、26 GRAMMAR_ERROR、795 TYPO，合计 9,502。
- 命名规则 XML：9,505 条；额外存在文件名为 `.xml`、规则 ID 为空的 unused declaration 导出文件，183 条。全部 XML 共 9,688 条。
- 界面与导出的统计并不完全一致：命名规则文件比界面多 3 条，另有 183 条未命名规则记录；尚未确认差异成因。以下表格明确使用导出记录数，不能当作界面去重后的问题总数。
- 同规则、文件、行号、描述完全相同的命名规则记录合并后为 9,063 条。这只是整理口径，同一行可以有多个不同位置的合法告警，不能因此断言扫描器重复执行。
- 代码基点：`5b0dd51c14cf738d45075c5fc9adde76732bcdf6`。开始时已有 `ImeHandoffInsets.android.kt` 和 `EmailSearchBar.kt` 两处未提交修改；初次审计没有修改源码；后续实施仅追加 IME 第二处 Duration 转换，搜索栏原有改动完整保留。
- 原始 XML：`/private/tmp/notes-inspect-2026-10-09/`；临时数据不会永久保存，长期证据以本页的规则统计、源码位置、判断理由和验证限制为准。

## 第一批：提高检查结果的可信度

| 编号 | 优化项 | 证据与判断 | 建议处理 | 验收条件 |
|---|---|---|---|---|
| A1 | 排除生成源码的整改噪声 | 生成目录 7,667 条，其中无用 import 4,515 条、冗余 suppression 573 条、局部变量命名 655 条；主要来自 Compose Resources、Room/KSP 等产物 | 建立只包含手写源码、构建脚本和项目文档的检查 Scope；保留业务源码的 correctness 检查，生成器问题按生成器单独分析 | 重扫的整改报告不包含 `**/build/**`、`.gradle/**` 等产物；不得手改生成文件 |
| A2 | 定点处理 Room ERROR | [AppDatabase.kt](../../core/database/src/commonMain/kotlin/me/zhangls/database/AppDatabase.kt) 第 38 行；Room 编译器负责生成构造器，三个目标已有生成实现 | 优先检查 Gradle 同步及 KSP 源集识别；确认构造器生成正常后，可按官方说明仅在此 `expect object` 添加 `@Suppress("KotlinNoActualForExpect")` 和原因注释 | 此 ERROR 消失；Android 与两个 iOS 目标保持编译可用；不手写第二份 `actual`、不改 schema |
| A3 | 修正“未使用”入口识别 | Koin 的 `@Singleton`、`@Factory`、模块 provider，Swift 调用的 `MainViewController`，JUnit 的参数工厂、测试方法，Manifest 的 Activity/Application，Compose Preview 均被不同 unused 规则报告 | 按具体规则配置 entry points；跨语言和编译器插件入口无法识别时定点登记，不全局关闭 unused | 已知运行入口不再被列为可删除项；真正孤立的私有函数仍可被报告 |
| A4 | 独立核查 IDE 模块依赖提示 | `UnnecessaryModuleDependencyInspection` 1,484 条，路径是 `Notes.*.commonMain/iosMain/...` 等 IDE 模块；部分描述明确表示 exported dependencies 未被检查 | 以 Gradle 声明、公开 API 和平台源集依赖为准逐条核对；先抽查代表模块，不根据 IDE 源集模块批量删依赖 | 每个候选依赖都能说明用途或证明冗余，并通过对应平台验证；遵守 Feature/API/Core 边界 |

Room 的生成模式和定点 suppression 有[官方说明](https://developer.android.com/kotlin/multiplatform/room)。检查 Scope 和规则级 profile 可分别参考 [JetBrains Scope 文档](https://www.jetbrains.com/help/idea/configuring-scopes-and-file-colors.html)和 [Inspection profiles](https://www.jetbrains.com/help/idea/customizing-profiles.html)。入口识别方案参考 [JetBrains unused declaration 入口配置说明](https://youtrack.jetbrains.com/articles/SUPPORT-A-3972/How-to-suppress-Unused-declaration-warnings-for-methods-called-by-JVM-agents)，具体是否支持 Koin 注解仍需在当前 IDE 的相应规则中验证。

## 第二批：可直接规划的小范围清理

本批以维护性为目的，未发现这些告警已造成当前用户流程故障。删除文件仍须单独授权；源码实施前先说明具体范围。

| 编号 | 优化项 | 已核对的位置 | 建议与限制 | 验收 |
|---|---|---|---|---|
| B1 | 清理空的版本升级分支 | [InitData.kt](../../composeApp/src/commonMain/kotlin/me/zhangls/entry/data/InitData.kt) 第 24 行，分支内只有 TODO | 移除空判断，将未来意图保留为紧邻版本记录的注释；保持首次插入邮件、增加启动次数和记录版本的顺序，不借机实现升级业务 | `:composeApp:testAndroidHostTest`；核对启动初始化顺序不变 |
| B2 | 清理 1 个无用 import | [EmailPagedList.kt](../../feature/email/src/commonMain/kotlin/me/zhangls/email/component/EmailPagedList.kt) 第 6 行 `fillMaxWidth` | 仅移除该 import；4,515 条生成目录同类提示不参与源码整改 | `:feature:email:testAndroidHostTest` 和模块 detekt |
| B3 | 清理 2 个测试无用解构变量 | [LoginPanePlanTest.kt](../../feature/login/src/commonTest/kotlin/me/zhangls/login/LoginPanePlanTest.kt) 第 102、219 行 `adaptiveInfo` | 将未使用分量替换为 `_`，保留 directive 的断言和窗格策略 | `:feature:login:testAndroidHostTest` |
| B4 | 将时间参数改为 Duration | [UserRepositoryImpl.kt](../../core/data/src/commonMain/kotlin/me/zhangls/data/impl/repository/UserRepositoryImpl.kt) 第 50 行 1 处；[SystemToast.ios.kt](../../core/framework/src/iosMain/kotlin/me/zhangls/framework/toast/SystemToast.ios.kt) 第 55、57、59 行 3 处；[ProfileViewModelTest.kt](../../feature/profile/src/androidHostTest/kotlin/me/zhangls/profile/ProfileViewModelTest.kt) 第 61、65、74、78 行 4 处 | 保持退避上限、Toast 总时长和 5 秒测试超时完全一致。扫描还包含 IME 文件 2 条；当前第 35 行已使用 `.milliseconds`，只剩第 44 行仍使用 Long。故扫描的 10 条当前只剩 9 处，不覆盖用户既有修改 | data/profile 宿主测试；framework iOS 编译及 Toast 回归；IME 项如实施另做键盘切换行为验收 |
| B5 | 简化 iOS 语言标签过滤 | [LanguageManager.kt](../../composeApp/src/iosMain/kotlin/me/zhangls/entry/LanguageManager.kt) 第 31 行 `mapNotNull { it as? String }` | 可改 `filterIsInstance<String>()`；这是等价表达优化，不改变语言策略 | iOS 编译；语言切换与跟随系统行为验收 |
| B6 | 复核单个文件级 suppression | [Converters.kt](../../core/database/src/commonMain/kotlin/me/zhangls/database/Converters.kt) 第 1 行 `@file:Suppress("unused")` | 当前生成 DAO 调用 converter，IDE 认为 suppression 冗余；可评估移除，但必须核对 Android/Native 生成与检查结果，不把问题扩大到其他 suppression | database 宿主测试、两个 iOS 目标编译；无新增有效 unused 告警 |

## 第三批：已决定保留的闲置能力

| 编号 | 候选 | 调查结果 | 建议与验证 |
|---|---|---|---|
| C1 | 闲置网络包装及错误类型 | [SafeRequest.kt](../../core/network/src/commonMain/kotlin/me/zhangls/network/SafeRequest.kt) 只有定义、无手写调用；`NetworkError.Timeout/NoConnection/Unauthorized/TokenExpired` 同样无引用 | 先决定当前网络层是否采用这一套结果协议。删除闲置能力或让调用链采用它是两种不同需求，不在本次审计实施。保留并启用前补错误映射与取消回归 |
| C2 | `safeRequest` 的取消传播缺口 | 上述函数 `catch (Exception)` 会把 `CancellationException` 转为 `Unknown`；这是阅读 unused 候选时发现的附加问题，不是 Inspect Code 直接报出的运行时错误 | 若保留并启用，先单独捕获取消异常并重新抛出。当前无调用，不能声称现有网络请求已经受影响；用取消场景测试证明传播。依据[协程异常处理文档](https://kotlinlang.org/docs/exception-handling.html) |
| C3 | 未使用的文件工具 | Android `AppFileManager.createCacheImageFile/getFileNameNoExtension`；iOS `documentDir()`；全仓手写源码查询未发现调用 | 私有 `documentDir` 可列为清理候选；公开平台工具要确认有无外部使用。`getFileNameNoExtension` 的 MIME 回退使用 `substringBeforeLast("/")`，对 `image/jpeg` 会得到 `image`，不能当扩展名或真实文件名；若保留并使用，应先明确回退契约 |
| C4 | 未使用的 `DialogState` | [DialogState.kt](../../core/framework/src/commonMain/kotlin/me/zhangls/framework/mvi/DialogState.kt) 中此模型无调用；同文件 `DialogResult` 被设置页面和行为处理使用 | 只评估 `DialogState`，不得整文件删除；设置层已有自己的 `DialogUiModel`，不为去告警新增整合抽象 |
| C5 | 实际闲置的版本目录别名 | `jetbrains-compose-material3-adaptive-navigation` 与 `koin-ktor` 未找到 accessor 或字符串查找引用 | 可移除精确别名；不要误伤同前缀的 navigation-suite/navigation3；不能因此删掉整个 adaptive/Koin 版本组。核对 Gradle 配置和受影响目标编译 |

2026-10-09 Xander 明确决定 C1–C5 全部保留，不执行删除、启用或附带修复。以下建议仅保留调查证据，不是本次待执行任务。

C3 的公开工具和 C5 的别名属于“仓库内无调用”的证据，不代表未来需求已被否决。任何文件删除均在提出具体名单后单独取得 Xander 授权。

## 应保留代码或暂缓处理的提示

| 类别 | 判断与证据 |
|---|---|
| 14 条版本目录 unused | SDK/JDK/JVM 7 个版本被应用/测试脚本及 `build-logic` 使用；runtime/foundation/ui/koin-annotations/detekt-formatting 5 个别名由约定插件 `findLibrary()` 获取。只有 C5 的 2 个别名仍为闲置候选，不能按 14 条全部清理 |
| 23 条图标 unused receiver | `Icons.Rounded.*` 接收者用于命名空间，符合现有图标 API；删除 receiver 会改变调用契约，保留 |
| 29 条未使用参数 | 多数来自 expect 声明、override/委托签名、测试 fake。common 的 `DraftModalBottomSheet` 参数在 iOS actual 中使用；Android 的 viewport 无需使用也不等于应从公共契约删除。只有不受契约约束的本地参数才是进一步候选 |
| 8 条返回值恒定 | 3 条脱敏 `toString()` 必须保持；`resetInvalidCredentials()` 的 null 表示凭据失效；其余 4 条来自测试 fake，不能为了消警改成暴露凭据或改变返回语义 |
| 非 ASCII 测试名称 | 导航序列化测试包含中文场景名，符合默认中文沟通，不是编码损坏。14 条扫描记录包含同位置的不同提示与重复项 |
| iOS `MainViewController` 命名 | Swift 在 `ContentView.swift` 调用该入口；不为两条命名告警更改跨语言 API |
| Unstable API 44 条 | 31 条在生成目录，13 条来自 Gradle/AGP 构建脚本。`@Incubating` 是升级兼容风险标识，不能用旧 API 或删掉 APK 命名、仓库约束来“修复”。在下一次工具链升级中定点核对 |
| 27 条 Markdown 表格格式 | 当前表格有表头与分隔行，检查主要涉及源码列宽对齐，不构成已确认的渲染故障。先检查链接和内容；不批量重排历史调查文档 |
| 语法与拼写 | 技术名、作者名、代码、中文测试与生成资源占相当部分；只修明确错误的用户可见文本，词典按具体术语维护，不全局禁用拼写检查 |
| 测试样板/Preview 被报告 unused | `.xml` 中测试类、`@Test/@Before/@After` 和 Preview 有大量“无调用”提示；它们由框架运行。只有完成入口核对后仍无用途的声明才可进入清理名单 |

## 一次升级评估（2026-10-09）

后续全项目检查补充了明确列出 Kotlin 2.4.21 的 KMP 兼容表，该版本已调整为候选；本节保留初次评估口径，当前建议见[全项目检查](2026-10-09-dependency-upgrade.md)。

按[升级规范](../dependency-upgrades.md)核对扫描中的五组候选。只读取官方发布说明、兼容表、Maven XML/POM/Gradle module 元数据，不修改版本目录或 Wrapper，不运行候选版本构建，因此下列“候选”不代表项目实测已通过。

| 组件 | 当前 | 当天最新正式版 | 评估结论 |
|---|---|---|---|
| Kotlin | 2.4.20 | 2.4.21 | Maven 工件确实发布；当前官方兼容表仍只列 2.4.20，2.4.21 与 Compose/Koin compiler/KSP/detekt/Native 的完整组合证据未齐，暂保留 2.4.20 |
| AGP | 9.3.1 | 9.4.1（同系列还有 9.3.3） | Kotlin 2.4.20 明确上限为 9.3.1；9.3.3 和 9.4.1 都越界，保留当前版本 |
| Gradle | 9.7.0 | 9.8.1 | Kotlin 2.4.20 明确上限为 9.7.0，保留当前版本 |
| NavigationEvent | 1.1.2 | 1.2.0 | 正式发布，Compose 工件的 module 元数据含 Android、iosArm64、iosSimulatorArm64；可列为后续独立升级候选。依赖 Compose runtime 1.11.2，尚未完成本项目 CMP/Navigation 3 解析和 Native 行为回归 |
| Paging | 3.5.1 | 3.5.2 | common/compose 工件的 module 元数据均含上述三个目标；修复首次 refresh 前 append 抛异常的问题，优先列为后续独立升级候选。尚未跑候选版本的分页/Room/Native 回归 |

版本与约束证据：

- [Kotlin 官方兼容表](https://kotlinlang.org/docs/gradle-configure-project.html)：当前表列 2.4.20 支持 Gradle 7.6.3–9.7.0、AGP 8.5.2–9.3.1。不能把“更新版本也可尝试”解释为扩大项目升级上限。
- [Kotlin Maven 元数据](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-gradle-plugin/maven-metadata.xml)、[2.4.21 POM](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-gradle-plugin/2.4.21/kotlin-gradle-plugin-2.4.21.pom)、[AGP Maven 元数据](https://dl.google.com/dl/android/maven2/com/android/tools/build/gradle/maven-metadata.xml)、[Gradle 当前正式版 API](https://services.gradle.org/versions/current)：均实际读取成功，不从 IDE 提示直接推导版本可用。
- [NavigationEvent 发布说明](https://developer.android.com/jetpack/androidx/releases/navigationevent)、[1.2.0 module 元数据](https://dl.google.com/dl/android/maven2/androidx/navigationevent/navigationevent-compose/1.2.0/navigationevent-compose-1.2.0.module)：2026-10-07 正式发布，新增 enabled 状态/title/url 等 API；发布工件自身的构建 Gradle 版本不是消费方的最低或最高要求。
- [Paging 发布说明](https://developer.android.com/jetpack/androidx/releases/paging)、[common 3.5.2 module 元数据](https://dl.google.com/dl/android/maven2/androidx/paging/paging-common/3.5.2/paging-common-3.5.2.module)、[compose 3.5.2 module 元数据](https://dl.google.com/dl/android/maven2/androidx/paging/paging-compose/3.5.2/paging-compose-3.5.2.module)：2026-10-07 正式发布。

24 条库/插件提示去重后为 12 条，另有一条 Wrapper 提示；此次按已确认范围评估五组，不扩展为全项目依赖升级。证据摘要暂存 `/private/tmp/notes-inspect-upgrade-evaluation.json`，原始元数据暂存同目录 `notes-upgrade-*.txt`。

## 初次建议与成功标准（后续以确认范围为准）

1. 完成 A1–A3，重新扫描手写源码 → 验证生成目录、已知入口和 Room ERROR 的噪声消失，有效 correctness 检查仍启用。
2. 实施 B1–B3，以及经确认的 B4–B6 → 验证精确源码差异与所属模块回归；不改 UI 尺寸、留白和交互，不更新截图基线。
3. 对 C1–C5 决定保留或清理 → 验证无调用证据、公开契约和平台编译；文件删除单独授权。
4. 独立核对依赖升级 → 验证官方支持上限和项目回归，不使用自动 `Update versions` 一键升级整个目录。

目标是让扫描结果可以驱动维护，而不是把所有检查项清零。性能优化只有经相同条件测量才能成立，本次没有性能测量。

## 确认后执行结果

### 检查配置与重扫

- 共享 `Notes Handwritten` Scope、`Notes` profile 及项目 profile 选择配置；保持 unused/correctness 开启。入口样本记录 Koin 的 `Singleton/Single/Factory/Module/KoinApplication`、JUnit、Preview 及 Manifest/Swift 的精确入口，并只向本机 `misc.xml` 合并该组件。
- 实际重开 Notes 项目并运行 Inspect Code，勾选测试源。导出 `/private/tmp/notes-inspect-final-2026-10-09/` 的 25 个 XML（24 个命名规则文件及 `.xml`），合计 918 条：296 条 IDE 模块依赖、291 条未命名 unused declaration、其余 331 条命名规则。所有文件路径均不含 `/build/`；共涉及 193 个有路径文件（另有一个无路径统计桶）。存在业务源码、common/Android/iOS 源集和测试文件，首轮仅根文件的错误 Scope 未作为有效验收。
- 该轮界面为 0 ERROR、436 WARNING、54 WEAK WARNING、26 GRAMMAR_ERROR、111 TYPO，合计 627。未命名 unused 导出仍未进入界面同一统计口径，不能把 918 与 627 混用，不能以数量差异证明重复扫描或性能收益。
- Room 构造器 ERROR、空分支、无用 import、测试解构变量和原 Duration 参数提示均未出现在该轮导出。converter 的两个局部 suppression 仍被 RedundantSuppression 报告：去掉它们时 unused 又误报生成 DAO 的真实调用，两个检查器结论冲突；保留定点 suppression，不追求清零。
- 第一次 profile 使用了错误的 Java unused 规则 ID；核对当前 IDE bytecode 后已改为 `unused`，保留 `test_entries=true` 和非 Java 引用检查，并补充 Koin `Single`。最终上述扫描在这一轮修正前执行，因此不能宣称入口规则已全部生效。Swift `MainViewController`、JUnit 参数工厂、部分 Koin provider/Native class、Manifest 入口仍被报告；它们不进入删除名单。后续重新加载修正配置并重扫这些入口是未验收项。
- 入口验收曾因 IDE 中已有的 SDK 安装流程暂停；本任务未发起或更改安装。随后实际重新打开 Notes 项目，扫描对话框仍选中 IDE 级旧 `Notes` profile；尝试选择/导入时菜单点击未生效，未启动修正配置的扫描。因此项目级 profile 自动加载及修正入口规则仍未验收，不能沿用上一轮结果证明生效。Scope 已验证结果与尚未验证的入口配置分别交付。
- 抽查 `core:data → core:database` 为 implementation，邮件实现依赖自身 API，装配仍在 composeApp；未删除 IDE 提示中的任何 Gradle 依赖。

### 精确源码变化

- `InitData` 只移除空判断、保留版本更新 TODO；首次邮件插入、启动计数、版本记录顺序不变。
- 删除邮件列表一个未用 import；两处测试解构分量改 `_`。
- 九处时间调用等价转为 Duration：data 一处、iOS Toast 三处、profile 测试四处、IME 一处；秒/毫秒与退避上限不变。IME 另一处已有用户 Duration 改动保持原样。
- iOS 语言标签用 `filterIsInstance<String>()` 保持过滤顺序与空列表语义。
- Room 构造器按官方模式添加 `KotlinNoActualForExpect` 定点 suppression 与原因。converter 文件级 suppression 收窄到两个由生成 DAO 调用的方法，记录 IDE 引用解析原因；没有修改 schema。
- `EmailSearchBar` 原有 diff 与执行前备份完全一致。C1–C5 功能、类型、工具和版本别名全部保留。版本目录和 Wrapper 无差异。

### 构建、行为与静态检查

实际使用已有 Android Studio JBR 25.0.3，Gradle 9.7.0；所有 Gradle 调用串行。第一批命令覆盖 composeApp/data/database/email/login/profile 宿主测试、database iOS 真机目标编译、composeApp iOS 模拟器编译、framework iOS 测试及 email detekt，使用 `--continue` 收集独立结果。

| 验证 | 实际结果 |
|---|---|
| core:data Android 宿主测试 | 8 通过、0 失败/跳过，任务实际执行 |
| core:database Android 宿主测试 | 3 通过、0 失败/跳过，任务实际执行 |
| feature:email Android 宿主测试 | 7 通过、0 失败/跳过，任务实际执行 |
| feature:login Android 宿主测试 | 13 通过、0 失败/跳过，任务实际执行 |
| feature:profile Android 宿主测试 | 14 通过、0 失败/跳过，任务实际执行 |
| composeApp Android 宿主测试 | 163 通过、3 失败、617 跳过；不是全通过 |
| core:framework iOS 模拟器测试 | 6 通过、0 失败/跳过；不等于设备 Toast/读屏验收 |
| database 两个 iOS 目标编译、composeApp iOS 模拟器编译 | 修改后任务实际执行并通过；没有 Xcode/Swift 整包验收 |
| feature:email detekt | 失败，75 条格式/复杂度发现；没有批量整改或新增 baseline |

第一批整体 `BUILD FAILED`，50 秒，551 个任务中 112 实际执行、439 up-to-date。日志 `/private/tmp/notes-inspect-cleanup-validation-2026-10-09.log`。

converter 改为定点 suppression 后再次串行执行：

```bash
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
  ./gradlew :core:database:testAndroidHostTest \
  :core:database:compileKotlinIosArm64 \
  :core:database:compileKotlinIosSimulatorArm64 --console=plain
```

本轮 `BUILD SUCCESSFUL`，18 秒；42 个任务，12 实际执行、30 up-to-date。宿主测试及两个 Native 编译任务实际执行；三个目标均存在生成的 `actual object AppDatabaseConstructor`。日志 `/private/tmp/notes-inspect-converters-validation-2026-10-09.log`。

应用三项失败为 `DeviceCornerNavigationTest.predictiveExitClipsPhysicalCornersAndCancellationRestoresPage`、`reportedSquareCornersKeepSquareMotion`（Previous page has a gray backdrop），以及 `DeviceCornerOpacityTest.ordinaryPushAndPopSwitchWithoutMotionOrBlending`（Page stays fully opaque (push)）。已按 systematic-debugging 复核断言、调用路径与当前差异：测试直接装配 DeviceCornerNavDisplay，未调用 InitData、Repository、IME 交接或 Toast；相关导航实现、测试和依赖版本均无本次差异。但未做实施前基线对照，根因未确定，不能直接归为既有或环境故障，也不在本次扩大导航修改。

邮件 detekt 的 75 条均为格式或复杂度问题；当前 `EmailPagedList` 对应剩余项是未修改的 toggleSelection 表达式，搜索栏 diff 保持用户原样。静态检查仍按失败交付，不通过 suppression/baseline 绕过。

XML 解析、`git diff --check`、文档本地链接和用户搜索栏 diff 保留检查完成。未执行全应用打包、候选升级构建、正式截图更新或设备行为验收。未验收项集中在[待验收清单](../testing/pending-acceptance.md#inspect-code-清理验证缺口2026-10-09)。

## 初次审计验证（实施前）

- 完整读取导出的 45 个 XML 文件（包含 `.xml`），按规则及路径汇总；逐类核对手写源码，重点查询 unused 入口、版本目录动态查找和平台实现。未逐行审阅全部生成源码。
- 第一次 Gradle 调用配置失败：默认环境没有可自动发现的 JDK 25；未进入目标编译/测试阶段。
- 按 README 使用已有 Android Studio JBR `25.0.3`。首次在沙箱内启动时因 Gradle Wrapper 缓存锁写权限失败；获准使用现有缓存后同一批命令成功。未安装全局依赖或改系统配置。

```bash
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
  ./gradlew :core:database:testAndroidHostTest \
  :core:database:compileKotlinIosArm64 \
  :core:database:compileKotlinIosSimulatorArm64 --console=plain
```

- 最终 `BUILD SUCCESSFUL`，8 秒；42 个任务，7 个实际执行、2 个缓存、33 个 up-to-date。
- `compileKotlinIosArm64` 与对应 KSP 实际执行成功；`compileKotlinIosSimulatorArm64`、Android 数据库编译和 `testAndroidHostTest` 为 `UP-TO-DATE`。已有宿主测试报告为 3 个用例、0 失败、0 错误、0 跳过，时间戳是 2026-10-06 UTC；不能写成本次重新跑过 3 个测试。
- 三个平台生成目录均存在 `actual object AppDatabaseConstructor`；支持唯一 ERROR 为 IDE 识别误报。此次构建仍报告 expect/actual 类处于 Beta、部分编译没有 Koin entry point 等编译警告，与本条 ERROR 不同。
- 最终日志：`/private/tmp/notes-inspect-room-validation-2026-10-09.log`。
- 未执行：完整重新 Inspect Code、全应用 Android/iOS 打包、iOS 模拟器行为测试、全模块 lint/detekt、UI 截图与设备验收。它们不是本次审计已通过项。
- 初次审计阶段只新增调查文档并更新文档索引；当时未实施优化。整个任务未删除文件、修改 CI/CD、迁移数据库或发布。

## 初次导出规则统计

“手写/配置/文档”按路径是否包含 `/build/` 区分，不意味着该列每一条都是真缺陷；“IDE 模块”是没有文件路径的源集依赖提示。数字包含重复记录和未命名规则，以便后续复查原始扫描规模。

| 规则 | 导出数 | 生成目录 | IDE 模块 | 手写/配置/文档 |
|---|---:|---:|---:|---:|
| KotlinUnusedImport | 4516 | 4515 | 0 | 1 |
| UnnecessaryModuleDependencyInspection | 1484 | 0 | 1484 | 0 |
| SpellCheckingInspection | 795 | 686 | 0 | 109 |
| LocalVariableName | 655 | 655 | 0 | 0 |
| RedundantSuppression | 574 | 573 | 0 | 1 |
| RemoveRedundantQualifierName | 261 | 261 | 0 | 0 |
| 未命名 unused declaration（`.xml`） | 183 | 3 | 0 | 180 |
| JoinDeclarationAndAssignment | 182 | 182 | 0 | 0 |
| UnusedSymbol | 173 | 143 | 0 | 30 |
| ReplacePutWithAssignment | 145 | 145 | 0 | 0 |
| TrailingComma | 90 | 90 | 0 | 0 |
| RedundantVisibilityModifier | 72 | 72 | 0 | 0 |
| RedundantExplicitType | 67 | 67 | 0 | 0 |
| BooleanLiteralArgument | 66 | 54 | 0 | 12 |
| ClassName | 62 | 62 | 0 | 0 |
| FunctionName | 54 | 52 | 0 | 2 |
| UnstableApiUsage | 44 | 31 | 0 | 13 |
| unused | 29 | 0 | 0 | 29 |
| MarkdownIncorrectTableFormatting | 27 | 0 | 0 | 27 |
| GrazieInspection | 26 | 0 | 0 | 26 |
| UnusedReceiverParameter | 23 | 0 | 0 | 23 |
| ConvertToStringTemplate | 18 | 18 | 0 | 0 |
| NonAsciiCharacters | 14 | 0 | 0 | 14 |
| UnusedVersionCatalogEntry | 14 | 0 | 0 | 14 |
| LiftReturnOrAssignment | 13 | 13 | 0 | 0 |
| RemoveExplicitTypeArguments | 13 | 9 | 0 | 4 |
| AndroidLintAndroidGradlePluginVersion | 12 | 0 | 0 | 12 |
| PrivatePropertyName | 12 | 12 | 0 | 0 |
| ConvertLongToDuration | 10 | 0 | 0 | 10 |
| ReplaceGetOrSet | 10 | 10 | 0 | 0 |
| CanBeVal | 9 | 9 | 0 | 0 |
| SameReturnValue | 8 | 0 | 0 | 8 |
| AndroidLintGradleDependency | 6 | 0 | 0 | 6 |
| AndroidLintNewerVersionAvailable | 6 | 0 | 0 | 6 |
| ReplaceCallWithBinaryOperator | 4 | 4 | 0 | 0 |
| RemoveRedundantBackticks | 2 | 0 | 0 | 2 |
| UnusedVariable | 2 | 0 | 0 | 2 |
| AndroidLintUnusedAttribute | 1 | 0 | 0 | 1 |
| ControlFlowWithEmptyBody | 1 | 0 | 0 | 1 |
| KotlinNoActualForExpect | 1 | 0 | 0 | 1 |
| LatestMinorVersion | 1 | 0 | 0 | 1 |
| MayBeConstant | 1 | 1 | 0 | 0 |
| RedundantSamConstructor | 1 | 0 | 0 | 1 |
| SimplifiableCall | 1 | 0 | 0 | 1 |
| **合计** | **9,688** | **7,667** | **1,484** | **537** |
