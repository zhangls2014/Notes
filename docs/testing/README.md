# 测试指南

本页按当前源码和 Gradle 配置列出测试入口。历史执行次数与设备验收结果保留在专项指南和实施记录中，不代表本次修改已运行这些测试。构建命令与环境要求见 [项目 README](../../README.md)，全局硬约束见 [AGENTS.md](../../AGENTS.md)。

## 源集与模块

`me.zhangls.kmp-library` 为共享模块统一启用 `withHostTest {}`。`commonTest` 可以通过 Android 宿主 JVM 或 iOS 模拟器执行；`androidHostTest` 只在宿主执行，`iosTest` 只在 iOS target 执行。UI 宿主测试需要对应模块的 Robolectric、Compose 测试依赖与 Android 资源配置。

| 模块 | 当前测试归属 | 重点 |
|---|---|---|
| `composeApp` | `commonTest`、`androidHostTest`、`iosTest` | 返回栈序列化、Tab/页面导航、场景策略、主题、自适应布局、个人信息导航、关于页面与跨模块 Entry 调用 |
| `core:data` | `commonTest`、`iosTest` | 用户资料/登录凭据协调、明文迁移与真实 Keychain 集成边界 |
| `core:database` | `commonTest`、`iosTest` | 收件人编码与历史 schema 迁移 |
| `core:theme` | `commonTest` | 铰链几何 |
| `core:framework` | `iosTest` | 原生 Toast 布局、Dynamic Type 与播报调度 |
| `feature:main` | `commonTest` | 导航套件尺寸策略 |
| `feature:login` | `commonTest` | 登录窗格与表单布局策略；登录业务/UI 回归另在组合根宿主测试中 |
| `feature:email` | `androidHostTest` | 邮件列表无障碍操作与新建邮件表单 |
| `feature:profile` | `androidHostTest` | 头像复制/清理、原子更新、ViewModel 与页面交互 |
| `feature:about` | 由组合根测试覆盖 | `AboutAppInfoTest`、`AboutUiTest` 与自适应导航回归 |

API 模块不承载页面测试；跨 Feature 导航和真实 Entry 装配在 `composeApp` 验证。模块目录对应 Gradle 路径，例如 `feature/profile` 对应 `:feature:profile`。

## 按改动选择回归

共享逻辑和 Android 宿主 UI：

```bash
./gradlew :composeApp:testAndroidHostTest :core:data:testAndroidHostTest \
  :core:database:testAndroidHostTest :core:theme:testAndroidHostTest \
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

根 `detekt` 没有源集，不作为有效检查。共享模块的 detekt 使用根配置与各模块 baseline，扫描手写 Kotlin 源集，排除生成目录。所有命令按需串行执行；同一仓库目录不得同时启动两个 Gradle 构建。整包构建出现 Java heap space 时使用 [AGENTS.md](../../AGENTS.md) 中的 8GB 堆参数。

## 平台与专项验收

- [Android 模拟器测试](android-emulator.md)：尺寸、密度、旋转与强制还原。
- [自适应 UI 回归](adaptive-ui.md)：导航、输入、分栏、截图候选与基线。
- [登录凭据存储验证](secure-token-storage.md)：迁移、Keystore、Keychain 与备份边界。
- [iOS Launch Screen](ios-launch-screen.md)：系统启动页资源和 Xcode 验证。
- [iOS Toast 无障碍验证](ios-toast-accessibility.md)：Dynamic Type、VoiceOver 与多窗口场景。

个人信息还需设备上的系统图片选择器、保存失败/取消与读屏验收；关于页面需检查实际安装包的版本名称、原始构建号、返回归属与导航套件恢复。相关设计及历史验收记录从 [文档索引](../README.md) 进入。
