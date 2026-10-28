# 测试指南

本页按当前源码和 Gradle 配置列出测试入口。历史执行次数与设备验收结果保留在专项指南和实施记录中，不代表本次修改已运行这些测试。构建命令与环境要求见 [项目 README](../../README.md)，全局硬约束见 [AGENTS.md](../../AGENTS.md)。

## 源集与模块

`me.zhangls.kmp-library` 为共享模块统一启用 `withHostTest {}`。`commonTest` 可以通过 Android 宿主 JVM 或 iOS 模拟器执行；`androidHostTest` 只在宿主执行，`iosTest` 只在 iOS target 执行。UI 宿主测试需要对应模块的 Robolectric、Compose 测试依赖与 Android 资源配置。

| 模块 | 当前测试归属 | 重点 |
|---|---|---|
| `composeApp` | `commonTest`、`androidHostTest`、`iosTest` | 返回栈序列化、Tab/页面导航、场景策略、主题、自适应布局、个人信息导航、关于页面、跨模块 Entry 调用与网页打开 |
| `core:data` | `commonTest`、`iosTest` | 用户资料/登录凭据协调、明文迁移与真实 Keychain 集成边界 |
| `core:database` | `commonTest`、`iosTest` | 收件人编码与历史 schema 迁移 |
| `core:theme` | `commonTest` | 铰链几何 |
| `core:navigation` | `androidHostTest` | 页面几何、设备圆角、预测返回与遮罩逐帧回归 |
| `core:framework` | `commonTest`、`iosTest` | Deep Link 字符串解析；原生 Toast 布局、Dynamic Type 与播报调度 |
| `feature:main` | `commonTest` | 导航套件尺寸策略 |
| `feature:login` | `commonTest` | 登录输入校验、窗格与表单布局策略；登录业务/UI 回归另在组合根宿主测试中 |
| `feature:email` | `androidHostTest` | 邮件列表无障碍操作与新建邮件表单 |
| `feature:profile` | `androidHostTest` | 头像复制/清理、原子更新、ViewModel 与页面交互 |
| `feature:about` | 由组合根测试覆盖 | `AboutAppInfoTest`、`AboutUiTest` 与自适应导航回归 |

导航显示机制的测试位于 `core/navigation/src/androidHostTest/kotlin/me/zhangls/navigation/`，只依赖本模块与测试库；`LayeredNavDisplayTest`、`AppSceneStrategiesTest`、Tab 和序列化测试继续在组合根验证。Native 像素用例分批命令见 [自适应 UI 回归](adaptive-ui.md)。

`core/framework/src/commonTest/kotlin/` 按生产代码包结构镜像放置纯逻辑测试，文件使用 `<Subject>Test.kt`；Deep Link 解析用例放在 `me/zhangls/framework/deeplink/`，平台 Toast 用例继续保留在 `iosTest`。

API 模块不承载页面测试；跨 Feature 导航和真实 Entry 装配在 `composeApp` 验证。模块目录对应 Gradle 路径，例如 `feature/profile` 对应 `:feature:profile`。

## 按改动选择回归

共享逻辑和 Android 宿主 UI：

```bash
./gradlew :composeApp:testAndroidHostTest :core:data:testAndroidHostTest \
  :core:database:testAndroidHostTest :core:theme:testAndroidHostTest \
  :core:navigation:testAndroidHostTest \
  :feature:main:testAndroidHostTest :feature:login:testAndroidHostTest \
  :feature:email:testAndroidHostTest :feature:profile:testAndroidHostTest
```

个人信息或关于应用修改时，先运行所属模块测试，再验证组合根的入口、返回与目的地兼容：

```bash
./gradlew :feature:profile:testAndroidHostTest \
  :composeApp:testAndroidHostTest \
  --tests '*ProfileNavigationTest*' --tests '*NavBackStackSerializationTest*' \
  --tests '*AboutAppInfoTest*' --tests '*AboutUiTest*' \
  --tests '*AdaptiveUiTest.aboutPageOpensFromSettingsAndReturnsToItsTab*'
```

`--tests` 过滤器跟随对应的测试任务；不要将组合根用例的过滤器用于 profile 模块。自适应布局变更运行完整 `AdaptiveUiTest`；截图候选、人工批准基线和既有 verify 限制见 [自适应 UI 回归](adaptive-ui.md)。

iOS 平台代码、Native 跨模块契约或数据迁移变更：

```bash
./gradlew :composeApp:compileKotlinIosSimulatorArm64 \
  :composeApp:iosSimulatorArm64Test :core:data:iosSimulatorArm64Test \
  :core:database:iosSimulatorArm64Test :core:framework:iosSimulatorArm64Test
```

`core:data` 的真实 Keychain 集成用例当前标记 `@Ignore`，需要可访问 Keychain 的应用宿主验证；宿主仓库测试或 iOS 编译成功不能替代该验收。步骤见 [登录凭据存储验证](secure-token-storage.md)。

### Entry 与设置初始化回归

新增或修改跨模块 `@Composable` Entry 时，更新 `composeApp/src/iosTest/kotlin/me/zhangls/entry/EmailEntryDispatchTest.kt`，通过 API 接口调用真实 Feature 实现，显式提供参数，验证 Native 分派。这个测试负责入口链接，不替代页面交互与设备验收。

修改 `core:preference/ProvidePreferenceLocals` 或升级底层控件库时，运行同目录的 `PreferenceLocalsTest.kt`，验证没有应用偏好域时仍能提供 locals；同时检查包装器继续显式传入空的内存数据源。相关回归通过现有 `./gradlew :composeApp:iosSimulatorArm64Test` 执行，不能用 Android 宿主测试或 iOS 编译代替。

### 网页打开能力回归

运行 `./gradlew :composeApp:testAndroidHostTest --tests '*BrowserUriHandlerTest*'` 验证 HTTP/HTTPS 分流、其他协议委托、无效 URL、失败异常契约与 Android Custom Tabs Intent。`./gradlew :composeApp:iosSimulatorArm64Test` 同时覆盖共享分流与 iOS 未附着窗口时拒绝展示的行为。自动测试不访问网络，不证明真实浏览器界面的展示、关闭或浏览历史；设备关闭条件见 [待验收清单](pending-acceptance.md#系统应用内浏览器)。

## 验证证据与交付记录

- 记录本次实际执行的命令与结果，分别列出实际执行、通过、跳过、失败／错误数量和未完成的设备验收。参数化配置总数包含跳过项时，不把总数当作通过数；任务为 `UP-TO-DATE` 或使用缓存时注明，没有重新执行测试用例。
- 编译成功只证明编译阶段；行为测试只证明其断言范围；候选录制只证明生成了图像。截图 verify、人工视觉审阅、真实 IME、动效与平台存储验收分别报告，历史验证结果不得写成本次已验证。
- 截图 verify 失败或采集出现空白时，记录受影响场景和限制，不宣称全量视觉验证通过。基线更新需先审阅并取得用户确认，流程与既有采集限制见 [自适应 UI 回归](adaptive-ui.md)。
- 性能对照记录构建版本与类型、设备和系统、登录状态、冷／热启动、采集工具、计时起止点、轮次及测量精度。对照保持条件一致，不把采集工具附加开销或不同阶段的指标混作同一结论。差异低于测量分辨率时，不宣称优化有效。

[iOS 启动调查](../audits/2026-10-02-ios-startup.md) 记录了照片选择依赖改变启动阶段分布但未观察到明显首页加速的对照；其中的 Debug 模拟器与 warm launch 结果不能外推为真机 Release 或 cold launch 结论。

## 构建与静态检查

```bash
./gradlew :androidApp:assembleDebug :androidApp:lintDevFullDebug

# 按修改模块运行 detekt，例如个人信息和关于应用
./gradlew :feature:profile:detekt :feature:about:detekt
```

`config/detekt/detekt.yml` 中各启用的 ktlint 规则独立读取 `indentSize`，签名、参数、表达式和换行规则均统一为 `2`，避免自动修正时重新引入四空格缩进。图标 import 与正文之间遵循 ktlint 的一行空行规则，不设项目格式例外。上述格式规则保持启用，不通过关闭检查或新增 baseline 消除格式差异。

文件命名遵循根 AGENTS.md 的职责命名约定，KMP 平台文件沿用 `<Name>.android.kt`、`<Name>.ios.kt`。`naming.MatchingDeclarationName` 与 `ktlint.Filename` 的严格类型同名要求不作为项目门禁，两条规则统一关闭，不再维护逐文件或平台排除清单。文件名准确性及 PascalCase 由代码审查约束；类型、函数、属性命名及其他格式、异常和复杂度规则继续启用。

### 类型解析与 Compose 检查

KMP 的普通 `:<module>:detekt` 延迟读取全部已注册源集，扫描各平台手写源码，不带类型解析，避免遗漏后创建的宿主测试和 Native 目录。`detektMainAndroid`、`detektHostTestAndroid` 使用 detekt 插件提供的 Android production/host-test 源集、编译 classpath 和 friend paths；不能把 iOS 源码混入这些任务。生成源码按模块实际构建目录路径排除，生成类仍通过编译 classpath 参与符号解析。iOS 源码仍由普通扫描覆盖，Native 类型与平台 API 的正确性继续通过 iOS 编译和测试验证。

在已配置 JDK 25 的环境串行执行：

```bash
# 所有 KMP 模块的生产与宿主测试语义检查
./gradlew detektMainAndroid detektHostTestAndroid --continue

# Android 宿主的代表性 Debug 变体
./gradlew :androidApp:detektDevFullDebug :androidApp:detektDevFullDebugUnitTest --continue
./gradlew :androidApp:detektDevFullDebugAndroidTest --continue

# Baseline Profile 测试模块
./gradlew :android:baselineprofile:detektDevBenchmarkRelease --continue
```

规则使用现有默认配置，并显式启用 `GlobalCoroutineUsage`、`CoroutineLaunchedInTestWithoutRunTest`、`SuspendFunSwallowedCancellation`。空安全和类型检查中要求类型解析的规则只有在上述 compilation 任务中才会运行，普通扫描零告警不能替代语义分析验收。

用户确认的框架入口和测试源集适配：`UnusedPrivateFunction` 只忽略 `@Preview` 入口，其他私有函数继续检查；`MagicNumber` 保留内置测试目录排除列表并补入 AGP 9 的 `androidHostTest`，生产代码的数值检查不变。测试代码的其他格式、空安全、类型和协程规则仍执行。

Compose Rules 使用 `io.nlopez.compose.rules:detekt:0.6.8`。其[发布说明](https://github.com/mrmans0n/compose-rules/releases/tag/v0.6.8)和 Maven POM 均依赖 detekt `2.0.0-alpha.6`，与项目分析引擎一致。按用户要求，当前版本全部 43 条规则启用；默认启用项不重复配置，仅显式启用默认关闭的 `ComposableNestingDepth`、`Material2`、`PreviewNaming`、`UnstableCollections`，覆盖命名、参数、状态、Effect、内容插槽、Modifier、CompositionLocal 和 ViewModel 等检查；规则选项保持库默认值，含类型解析要求的规则随 compilation 任务运行。规则清单以 `config/detekt/detekt.yml` 为准；升级时重新评审，不自动修复 UI 或更新 baseline。

本机缺少全局 JDK 25 时，可按[升级规范](../dependency-upgrades.md)使用项目内发行版，并在命令前显式设置 `JAVA_HOME`；不修改系统注册。2026-10-27 的版本、路径、探针对照和覆盖扩展记录见[接入验证](../audits/2026-10-27-detekt-analysis.md)，该次告警数不代表以后提交的结果。

根 `detekt` 没有源集，不作为有效检查。共享模块的 detekt 使用根配置与各模块 baseline，扫描手写 Kotlin 源集，排除生成目录。所有命令按需串行执行；同一仓库目录不得同时启动两个 Gradle 构建。整包构建出现 Java heap space 时使用 [AGENTS.md](../../AGENTS.md) 中的 8GB 堆参数。

## Android Studio Inspect Code

项目共享的检查 Scope 放在 `.idea/scopes/`，检查 profile 放在 `.idea/inspectionProfiles/`，文件使用英文名称。这里只保存可复用的源码范围和规则；扫描结果导出到系统临时目录，必要结论按文档保留规则写入 `docs/audits/`，不提交生成报告。

框架入口规则的共享样本放在 `config/inspections/entry-points.xml`；当前 IDE 将 `EntryPointsManager` 存在本机 `.idea/misc.xml`，不将整个本机文件纳入 Git。应用样本时只合并该组件的注解和精确入口，保留已有组件、注解和模式。入口清单随真实调用契约维护，不预留整包通配豁免。

手写源码检查使用 `Notes Handwritten` Scope 和 `Notes` profile，勾选 Include test sources。Scope 包含根文件及多层目录，排除构建、缓存与工作树。当前 IDE 对部分 Gradle/KSP/Compose 生成目录创建独立内容根，路径会相对于该根匹配；因此另外排除已确认的生成目录、Gradle DSL、Compose `.cvr` 产物和 Room/BuildKonfig/约定插件生成文件。增加生成器或修改源码命名时复核这些精确生成模式，不能仅凭目录排除表达式宣称没有生成告警。生成文件仍参与编译和引用解析，不手改产物。

Android Studio 没有自动识别新 profile 时，在 Inspections 的齿轮菜单选择 Import Profile，导入 `.idea/inspectionProfiles/Notes.xml`；新增入口组件后重新打开项目，使本机配置生效。随后运行 Inspect Code，选择上述 Scope/profile 并导出 XML，核对真实业务与测试文件覆盖、`/build/` 路径和已知入口。扫描结果数量下降不能替代范围验收。

入口注解覆盖 Koin、JUnit、Compose Preview；Manifest 和 Swift 入口按具体类/方法登记。仅针对已验证入口配置规则，不全局关闭 unused 或 correctness。IDE 的源集模块依赖提示须与 Gradle 和公开 API 核对，不直接作为删除依赖的依据。
新增规则先通过 XML 和当前 IDE 验证，再记录重新扫描的范围、profile、错误及警告数。编译和静态 XML 校验不能替代 IDE 规则生效验证。

## 平台与专项验收

集中状态见 [待验收与已知限制](pending-acceptance.md)，专项指南负责操作步骤；完成验收时同步更新两处记录。

- [Android 模拟器测试](android-emulator.md)：尺寸、密度、旋转与强制还原。
- [自适应 UI 回归](adaptive-ui.md)：导航、输入、分栏、截图候选与基线。
- [登录凭据存储验证](secure-token-storage.md)：迁移、Keystore、Keychain 与备份边界。
- [iOS Launch Screen](ios-launch-screen.md)：系统启动页资源和 Xcode 验证。
- [iOS Toast 无障碍验证](ios-toast-accessibility.md)：Dynamic Type、VoiceOver 与多窗口场景。

个人信息还需设备上的系统图片选择器、保存失败/取消与读屏验收；关于页面需检查实际安装包的版本名称、原始构建号、返回归属与导航套件恢复。选择理由见 [决策记录](../decisions.md)，未完成事项见 [待验收与已知限制](pending-acceptance.md)。
