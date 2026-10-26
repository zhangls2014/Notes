# detekt 类型解析与 Compose Rules 接入验证

日期：2026-10-27。状态：接入和覆盖验证完成，静态检查门禁未通过，新增覆盖内的告警待整改。本文保留版本选择、源集漏扫原因与验证证据；日常命令以[测试指南](../testing/README.md#类型解析与-compose-检查)为准。

## 版本与执行条件

| 项目 | 本次使用 |
|---|---|
| detekt / ktlint wrapper | 2.0.0-alpha.6，保持项目原版本 |
| Compose Rules | 0.6.8，版本目录管理，仅进入 detektPlugins |
| 项目 Kotlin | 2.4.20，未升级 |
| Gradle / AGP | 9.7.0 / 9.3.1，未升级 |
| 执行 JDK | Azul Zulu 25.0.5+8，aarch64，校验官方 SHA-256 |
| 构建内存 | Gradle 与 Kotlin daemon 均为 8GB；最多 2 个 worker |

Compose Rules 的网站使用说明仍展示较旧矩阵；核对[0.6.8 发布说明](https://github.com/mrmans0n/compose-rules/releases/tag/v0.6.8)、[Maven Central 元数据](https://repo.maven.apache.org/maven2/io/nlopez/compose/rules/detekt/maven-metadata.xml)及[POM](https://repo.maven.apache.org/maven2/io/nlopez/compose/rules/detekt/0.6.8/detekt-0.6.8.pom)，确认正式工件存在并精确依赖 detekt 2.0.0-alpha.6。该分析引擎使用 Kotlin 2.4.10；没有将规则库编译版本误读为项目 Kotlin 的最高支持版本，也没有替换项目编译器。

JDK 保存在已忽略目录 `.gradle/jdks/zulu25.38.107-ca-jdk25.0.5-macosx_aarch64/Contents/Home`，每次命令使用 `JAVA_HOME` 前缀，不改变全局安装或系统配置。发行包通过 [Azul 官方元数据](https://api.azul.com/metadata/v1/zulu/packages/3026a246-7e93-4e2f-b86d-f92bac5c8f04)提供的 SHA-256 校验：`80e23f202d1815e989c980102e9b9f34cbcd40df9a5c98ac8f0a0aa6f4a45776`。安装包和临时验证样本保存在系统临时目录，未提交二进制。

## 源集边界与修正理由

- 原普通任务在约定插件应用时立即快照 `kotlin.sourceSets`，遗漏之后创建的 `androidHostTest` 和部分 Native 源集。改为 Provider 延迟读取，不以目录当时是否存在决定永久排除。
- 修改前实测 `core:data:detektMainAndroid` 的 iosMain 输入数为 0，未复现平台源码混入；收窄全局 `setSource` 是明确配置边界，不将其表述为已复现的混入缺陷。只有普通 `detekt` 和 `detektBaseline` 使用全平台目录，compilation 任务保留插件提供的 source sets、classpath、friend paths 及编译选项。
- 对生成文件使用 `**/build/**` 相对 glob 会在独立源码树根下漏掉资源生成目录。已改为按文件实际路径排除模块 build 目录，同时应用于检查与 baseline 任务；生成类仍在编译 classpath 中。
- 最终输入断言：`core:data:detektMainAndroid` 为 23 个文件、iosMain 为 0、生成目录为 0；普通扫描为 28 个文件、含 2 个 iosMain 文件，并确认导航普通扫描包含 androidHostTest。
- 类型解析仍只覆盖插件支持的 JVM/Android compilation。Native 源码的普通扫描不能替代 Native 编译、运行和平台 API 验收。

## 规则选择与精确适配

初次接入时，Compose Rules 发布配置共 43 条，项目逐条显式配置，先启用 13 条行为检查：ContentEmitterReturningValues、ContentSlotReused、InvalidReadOnlyComposable、LambdaParameterInRestartableEffect、ModifierClickableOrder、ModifierNotUsedAtRoot、ModifierReused、ModifierShouldBeForwardedToEveryBranch、MutableStateAutoboxing、RememberContentMissing、RememberMissing、StaleRememberUpdatedStateInRemember、VarsWithoutStateBacking。

在现有默认协程规则上增加 GlobalCoroutineUsage、CoroutineLaunchedInTestWithoutRunTest、SuspendFunSwallowedCancellation。初次接入未启用全部规则，后续按用户要求全量启用，见下节；没有新增 baseline 或自动修改产品源码。

经用户单独确认后，只增加两项适配：UnusedPrivateFunction 忽略 Preview 注解入口；MagicNumber 完整保留 detekt 默认的九项测试排除，再补入 androidHostTest。逐条对照语义报告，只移除 31 条实际 Preview 入口提示、609 条 androidHostTest 魔法数字，无其他移除或新增告警；普通扫描只移除后者。其他测试规则及生产数值规则保留。

## 验证证据

| 验证 | 实际结果 |
|---|---|
| 同一个 `value!!.length` 样本 | 普通任务 classpath=0，规则未运行；Android compilation classpath=7，检出 1 条 UnsafeCallOnNullableType |
| 未 remember 的 Compose state 样本 | 检出 1 条 RememberMissing，证明规则库实际加载 |
| 默认 configuration cache | 普通与类型解析任务成功存储，再次执行明确复用；零告警模块验证通过 |
| Android 构建 | DevFull、DevMainland、PrdFull、PrdMainland 四个 Debug 变体均构建成功；部分依赖任务使用缓存 |
| Android 宿主回归 | data、framework、login 与组合根定向用例实际执行，43 通过、0 跳过、0 失败 |
| iOS | 组合根主源码编译完成，组合根、data、framework、login 测试实际执行，66 通过、1 个既有 Keychain 应用宿主用例跳过、0 失败；部分编译任务为 UP-TO-DATE |
| 只读审查与文档检查 | 源集、版本依赖、显式规则清单及两项适配复核通过；git diff --check 通过 |

临时样本任务按“预期检出告警”的测试方式读取报告断言；正式扫描维持 failOnSeverity=Info，不忽略失败。没有设备、截图或完整 Android UI 宿主回归，不将构建或分析器可执行等同于行为验收。

## 初次接入报告与待整改边界（13 条 Compose 规则）

| 范围 | 告警条数 | 状态 |
|---|---:|---|
| 23 个模块普通扫描，不应用 baseline | 1,134 | 全部已注册 KMP 源集；包含 Native 和宿主测试 |
| 普通门禁，使用原 baseline | 1,124 | 11 个模块因告警失败 |
| Android compilation 门禁，使用原 baseline | 1,110 | 46 个任务：36 个实际分析，10 个 NO-SOURCE；17 个因告警失败、19 个零告警完成 |
| 上述 compilation 中生产代码 | 94 | 包含普通规则及新增类型解析、Compose 规则 |
| 上述 compilation 中测试代码 | 1,016 | 大量为此前漏扫的格式问题，含 2 条 Android 仪器测试告警 |

普通与 compilation 扫描范围重叠，数字不能相加，也不能都当成独立语义缺陷。原“18 条剩余”只对应旧源集快照及无类型解析的范围，不能代表当前完整检查。修复源集覆盖后，普通扫描在适配前新增的范围包括 1,603 条 androidHostTest 和 114 条 Native 提示；用户确认的测试数值适配消除了其中 609 条，未批量格式化或隐藏其他结果。

新的生产代码报告包括 48 条 UnsafeCallOnNullableType、11 条 InjectDispatcher、4 条 SuspendFunSwallowedCancellation、5 条 Effect 回调捕获、3 条内容插槽复用，以及不可达 catch、忽略返回值等；需逐条确认和分批整改，不能直接认定为运行缺陷。正式门禁仍未通过，baseline 未修改。

完整报告和探针日志本次位于系统临时目录；可通过测试指南的命令重新生成。持久的未验收事项以[集中清单](../testing/pending-acceptance.md)为准。

## 后续全量启用 Compose Rules

同日按用户要求，将 0.6.8 的全部 43 条规则显式设为 `active: true`，与实际 JAR 的 `config/config.yml` 逐条核对一致；不启用 detekt 全局 allRules，其他选项沿用规则库默认值。保留原有 Preview、测试数值、文件命名及格式约定，没有修改应用源码或 baseline。

| 范围 | 全量启用后 | 相比初次接入新增 | 消除 |
|---|---:|---:|---:|
| 普通全量扫描，不应用 baseline | 1,191 | 57 | 0 |
| 普通门禁，原 baseline | 1,181 | 57 | 0 |
| Android compilation 门禁，原 baseline | 1,174 | 64 | 0 |

范围重叠，不能相加。类型解析结果包含生产代码 158 条、测试代码 1,016 条，新增 64 条均位于生产代码。普通任务 23 个模块全部重新分析，门禁 16 个模块因告警失败；类型解析选择 46 个任务，其中 36 个重新分析、10 个无源码，21 个因告警失败。未复用分析结果缓存；依赖编译可以为 UP-TO-DATE。未发现分析器执行错误或生成目录告警。

类型解析新增分布：ParameterNaming 16、UnstableCollections 14、CompositionLocalAllowlist 7、ComposableParamOrder 7、UnnecessaryLaunchedEffect 6、ModifierMissing 5、LambdaParameterEventTrailing 3、ComposableNestingDepth 3、MissingReadOnlyComposable 2、ViewModelForwarding 1。普通扫描另外覆盖 Native 源码，规则能力也有差异，因此新增数不相等。

本次仅修改检查配置及文档，未重新运行应用构建、行为测试、设备和截图验收。逐条报告、执行日志和前后对比保存在系统临时目录 `notes-detekt-compose-all-20261027/`；日常配置与命令以测试指南为准。门禁仍未通过，告警须后续分批确认整改。

### 默认配置去重

随后按用户要求移除与规则库默认值一致的 Compose 配置，只保留默认关闭的 ComposableNestingDepth、Material2、PreviewNaming、UnstableCollections 四项 `active: true`，规则集也沿用默认启用状态。项目仍启用全部 43 条规则。

普通全量、原 baseline 门禁与 Android compilation 门禁全部重新分析，分别为 1,191、1,181、1,174 条；与全部显式启用时逐条对照一致，新增 0、消除 0。没有分析器执行错误；门禁仍因既有告警失败。日志和对比结果在系统临时目录 `notes-detekt-compose-defaults-20261027/`。

## 前五批修复与回归（2026-10-28）

按用户授权完成格式、局部简化与命名、Compose 接口与集合稳定性、状态与副作用、空安全与协程异常五批。第一至四批规则清零；第五批取消传播已修复，用户明确接受暂留 SafeRequest 的 1 条 TooGenericExceptionCaught，以保留将未知普通异常映射 NetworkError.Unknown 的边界契约。没有添加 suppression、规则例外或 baseline，没有修改依赖版本、schema 或迁移行为。

| 范围 | 修复前 | 修复后 |
|---|---:|---:|
| 普通全量，无 baseline | 1,191 | 39 |
| 普通原 baseline 门禁 | 1,181 | 31 |
| Android compilation 原 baseline 门禁 | 1,174 | 30 |

三种范围重叠，不能相加。普通与 compilation 按文件位置、规则和消息精确去重后，告警从 1,292 减至 40；前五批原 1,255 条中，除用户接受暂留的 1 条外已消除。剩余为第六批复杂度 31 条、CompositionLocal 适配 8 条、暂留异常边界 1 条；格式换行额外暴露两个 AdaptiveUiTest 方法的 LongMethod，未扩大本轮范围拆分。普通扫描 23 个模块均重新分析；compilation 46 个任务中 37 个实际分析、9 个无源码。新增 network commonTest 使一个原无源码任务开始分析。未发现分析器错误或生成目录告警；正式门禁仍因剩余问题失败。

保留以下能避免重复踩坑的证据：

- 网络取消与头像提交后清理取消用例在旧实现上出现预期断言失败；修复后通过。普通未知异常仍保留原始 cause，文件复制失败清理与凭据原子写失败恢复采用 finally，历史 SQL fixture 的 24 条字符串逐条比对完全一致。
- IO dispatcher 由 DataModule 构造参数提供默认值，provider 明确放入模块；数据库与消费者使用注入。顶层 provider 的返回类型 CoroutineDispatcher 位于包外，在 Native 跨模块图中未被发现；[Koin 编译器说明](https://github.com/InsertKoinIO/koin-compiler-plugin/blob/main/docs/COMPILE_TIME_SAFETY.md)记录了按返回类型包过滤函数 hint 的限制。改为模块成员后，保持 compileSafety 开启并通过 Native 编译和回归。
- 内容槽首次改为双层 movable content 后，完整组合根 801 项中 3 个退出登录场景失败，异常栈为 DetermineMovableContentNodeIndex/applyLateChanges。移除 Tab 单层仍复现；将 Tab 与 AppShell 都改为单一内容调用点后，定向场景和完整回归通过。不能将中间失败轮计作通过或归为普通等待超时。
- AppShell 无 Tab 分支现在使用透明 Surface、继承内容颜色，原约束和自然尺寸由独立测试覆盖。Surface 的矩形裁剪、语义容器和命中节点不是任意调用下透明的包装；当前生产单根 NavDisplay 的登录、退出、点击与布局回归通过，设备视觉和复杂父子交互仍不由编译代替。
- Keyed SideEffect 保留首次及 key 变化触发，挂起 Flow 收集仍保留 LaunchedEffect 并读取最新回调；同步副作用调度点确有变化，设备键盘与语言切换需继续验收。

验证结果：Android 四个 Debug 变体构建成功，完整组合根 172 通过、629 条件跳过；受影响模块共 237 通过，最终汇总轮 220 实际执行、17 复用结果。最后仅提取 AppShell 测量逻辑以消除本次新增复杂度后，再次完整执行组合根，仍为 172/629/0。iOS framework 与模拟器回归完成，共 74 通过、1 个既有 Keychain 用例跳过，汇总轮实际通过 63、复用 11；最后组合根 31 项再次执行通过。profile Native 任务无测试源码，不能计作行为通过。Swift ContentView 对生成 framework 的类型检查通过。图标生成器核对 23 个输出通过，文档链接及 git diff --check 通过。

JDK、版本与内存条件沿用前文。本轮没有设备或正式截图基线验收；自动测试产生的候选不替代人工批准，当前版本保存恢复回归也不能外推为跨编译版本的保存状态兼容。完整逐条剩余报告、前后失败日志和最终执行命令位于系统临时目录 `notes-detekt-fix-five-20261027/`，持久待验收状态已归入集中清单。
