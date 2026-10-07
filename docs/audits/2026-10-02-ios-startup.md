# iOS 启动耗时调查

日期：2026-10-02

状态：已完成源码检查、当前源码 Debug/Release 构建、Debug dylib 对照、iOS 27.0/18.5 模拟器对照、临时分段计时、PhotosUI 最小链接实验，以及实际 Notes 的头像选择器 A/B/C 对照；真机未连接，未验证全新安装。未保留应用实现或构建配置改动。

## 结论

当前复现的明显耗时集中在应用入口之前的 `dyld` 依赖加载。Koin 初始化与 `initData()` 的同步依赖解析确实位于启动主线程，但本次采样中占比很小，不能将它们认定为主要原因。等待登录状态时根导航不绘制内容，会使后续等待也呈现白屏。

后续执行确认：在 iOS 27.0 模拟器上，Release 与关闭 Debug dylib 均未产生可确认的首屏改善；同一 Release 包在 iOS 18.5 模拟器上显著更快。Instruments 的 `dyld_sim` 入口前区间由 iOS 27.0 的约 1.60 秒缩短到 iOS 18.5 的约 0.216 秒。

按 Apple 官方流程继续做单变量链接实验后，确认 **PhotosUI 及其传递依赖在当前 iOS 27.0 模拟器有明显加载成本**。一个约 53 KB、无 Kotlin/Compose/Koin/数据库的最小 UIKit 可执行文件，仅额外强制链接 PhotosUI，就将启动到 `main()` 的中位数从 0.382 秒增到 1.686 秒。Notes 通过 Calf 的头像照片选择器引入 PhotosUI，最终 Release 二进制确实强链接该系统框架。

实际 Notes 的 A/B/C 对照进一步限定了这个结论：**停用头像选择器，乃至移除 PhotosUI 直接链接，都没有明显提前首页出现时间**。移除依赖使 `main()` 前 dyld 区间从约 1.91 秒降到 0.384 秒，但进入应用后，UIKit 无障碍组件加载出现约 1.35 秒的 dlopen，运行日志仍记录 PhotosUI 映像。当前环境下加载成本转移到了后续阶段，不能把最小探针的收益直接当作 Notes 的首屏收益，也不能据此删除头像选择功能。没有真机证据。

## 复现环境和方法

- 仓库 HEAD：`9aebb606`；调查开始时工作区干净。
- 设备：iPhone 18 Pro，iOS 27.0，模拟器 ID `6339160B-A0ED-4AD3-B1F9-F0B99921100F`。
- 应用：`me.zhangls.notes.Notes`，版本 1.0 (1)，已安装、已登录、已有样例数据。
- 使用已安装包，未重新构建，因此没有证明其与当前 HEAD 完全一致；包文件修改时间为当天 03:37。
- 每次 `simctl terminate` 后 `simctl launch`，计时起点在 launch 命令之前；未连接 Xcode/LLDB。
- 连续截图判断首页出现，截图自身约耗时 0.4 秒，所以时间是区间而非精确首帧时间。
- 另用 `sample` 以 1 ms 采样间隔读取调用栈；采样会扰动启动，单独分析，不混用为性能基线。
- 属于进程重新启动、文件缓存可能已预热；按 Apple 分类应称 warm launch，而非重启设备后或依赖被逐出内存后的 cold launch。没有重启模拟器或清除用户数据。

## 运行证据

两次未启用采样或额外诊断环境变量的启动：

| 轮次 | launch 命令返回 | 最后一次白屏截图完成 | 首次首页截图完成 |
| --- | --- | --- | --- |
| 1 | 0.131 s | 1.991 s | 2.471 s |
| 2 | 0.135 s | 1.993 s | 2.498 s |

以上是截图完成时间，不是截图内部精确的抓帧时间。首页在约 2.0–2.5 秒的观察窗口内出现，精度受截图耗时限制。

另一次设 `SIMCTL_CHILD_DYLD_PRINT_STATISTICS=1` 的运行也在 1.987 秒截图仍白屏、2.490 秒截图显示首页。该轮没有在宿主预期路径取得 stderr，未获得可用 dyld 时间统计，不能作为分阶段精确耗时依据。

启动后立即采样 2 秒的调用图中，主线程的全部 1536 个样本处于 `dyld4::prepare`，其中 1398 个处于 `JustInTimeLoader::loadDependents`。此时尚未进入 `iOSApp.init()`。

另一次完整 8 秒采样中，主线程共 5623 个样本：

- 1389 个仍在 `dyld4::prepare`，其中 1247 个在依赖加载。
- `iOSApp.init()` 调用 `initKoin` 的分支仅 3 个样本，调用 `initData` 的分支仅 5 个样本；同步数据库 provider 出现在后者调用栈中。
- 后续可见 SwiftUI/UIKit 场景创建、Compose 组合与 Metal 绘制，最终主线程进入事件循环等待。
- 同轮截图在 3.481 秒仍白屏，4.490 秒已显示首页，说明采样明显影响绝对耗时。

样本数不是精确毫秒计时，不应按 1 ms 间隔直接转换为函数耗时。

`otool -L` 检查显示应用 Debug 逻辑位于约 86 MB 的 `Notes.debug.dylib`，入口可执行文件约 40 KB。直接链接包括 SwiftUI、UIKit、PhotosUI、Metal、DeveloperToolsSupport 等系统框架与 Swift 库。Kotlin framework 的约定配置已是 `isStatic = true`，没有发现各业务模块作为独立动态 framework 嵌入的证据。二进制大小与依赖列表仅是后续对照线索，不能据此断言某个库就是根因。

原始临时证据：`/private/tmp/notes-immediate-startup-sample.txt`、`/private/tmp/notes-full-startup-sample.txt` 和 `/private/tmp/notes-unprofiled-{1,2}-{0..4}.png`。临时文件可能被系统清理。

## 源码发现

### 主线程提前创建初始化任务依赖

`iosApp/iosApp/iOSApp.swift:10` 在 SwiftUI App 初始化时依次执行 Koin 和数据初始化。

`composeApp/src/commonMain/kotlin/me/zhangls/entry/NotesModule.kt:50` 先在调用线程执行 `get<InitData>()`，之后才 `startupScope.launch`。解析链为 `InitData → EmailsRepositoryImpl → AppDatabase / DAO`，数据库 builder/build 与文件路径准备因此会发生在首屏之前。Room 查询上下文虽为 IO，并不代表对象创建也自动切到 IO。

可用单变量实验将任务解析移入 IO 协程，比较启动前后分段耗时。但本次同步解析只占很少样本，预计无法单独解释入口前的长时间白屏。

### 登录状态读取前完全不绘制 UI

`AppViewModel` 的初始 `isLogin` 为 null，协程收集 DataStore 用户数据后才更新。`AppNavHost.kt:52` 在 null 时直接 return，故登录状态等待期间没有页面内容。应通过埋点量化首次用户数据发射与首次内容绘制的间隔；加载态只能改善这一段的反馈，不能缩短 `main()` 之前的等待。

### 样例数据与网络

`InitData.run()` 的样例插入只在 `launchCount == 0` 时执行，实际插入在 IO 协程中。本次使用存量数据，未测首装插入开销。启动入口不等待该任务完成，不能从调用关系推断首屏必须等待全部插入。

当前启动链没有发现等待登录网络接口的代码；登录也是本地模拟登录，没有证据支持“10 秒网络超时拖慢启动”的解释。

## 用户授权后的执行结果

用户随后要求开始执行。重新构建当前 HEAD 的 Debug 和 Release，排除“已安装包可能不是当前源码”的限制。除临时 Swift 时间日志外，所有对照使用同一业务源码；日志改动完成后已还原。

### 构建

- 当前源码 Debug Xcode 构建通过。
- Release 首次在 Kotlin/Native `DevirtualizationAnalysis` 阶段因 Java heap space 失败；使用仓库的 8 GB Kotlin/Gradle 参数后 `:composeApp:linkReleaseFrameworkIosSimulatorArm64` 通过，耗时 3 分 46 秒，随后 Release Xcode 构建通过。
- Debug 主体二进制约 87 MB，Release 约 55 MB。文件体积降低没有在本次截图精度下带来首屏时间改善。

### 单变量与运行环境对照

下表均为首次观察到首页的截图完成时间，不能当作精确首帧时间。每轮都终止进程再启动，未连接调试器或采样；每个有效包均人工检查最终截图，避免把应用退出后的 SpringBoard 当作首页。

| 包与环境 | 第 1 轮 | 第 2 轮 | 第 3 轮 |
| --- | --- | --- | --- |
| 当前 Debug / iOS 27.0 | 2.455 s | 2.449 s | 2.465 s |
| Debug，`ENABLE_DEBUG_DYLIB=NO` / iOS 27.0 | 2.462 s | 2.500 s | 2.471 s |
| Release / iOS 27.0 | 2.466 s | 2.519 s | 2.485 s |
| 同一 Release / iOS 18.5 | 2.122 s | 1.106 s | 1.081 s |

iOS 18.5 使用已有 iPhone 16 模拟器（ID `7573B90B-DE16-4B9E-AF69-DA738B738CC9`），复制 Notes 的 Application Support 数据保持用户与邮件一致；设备型号、系统主题和屏幕几何与 iOS 27.0 不同，所以端到端差异不是严格的单变量系统版本实验。入口前 `dyld_sim` 差异不依赖页面布局，是更直接的证据。iOS 18.5 第一次运行明显更慢，需将缓存预热状态纳入解释。

Debug dylib 对照仅使用命令行构建参数，未修改 pbxproj。为了复用同一 Kotlin framework 而跳过 Gradle 的第一版对照包漏打包 Compose 资源，运行时退出；其截图和时间全部作废。补齐基线的同一份 `compose-resources` 后重新安装测量，得出上表有效结果。因此没有保留关闭 Debug dylib 的配置。

### Instruments 分段证据

使用 App Launch 模板，导出 `dyld-activity-interval` 表。取 `Launch Executable` 中 containment level 2、调用栈属于 `dyld_sim` 的区间：

| 包与环境 | 入口前区间 |
| --- | ---: |
| 当前 Debug / iOS 27.0 | 1.911004 s |
| Release / iOS 27.0 | 1.602339 s |
| 同一 Release / iOS 18.5 | 0.216482 s |

外层 containment level 1 的 Launch Executable 区间持续到轨迹结束，不可把它误认为完整入口前耗时。生命周期表没有提供可用数据，未声称获得 Instruments 精确首帧指标。轨迹有表输入源与采样周期警告；dyld 表包含完整起点、时长与调用栈，用于定位加载阶段，绝对性能仍以脱离 Instruments 的重复运行作交叉验证。

### Swift 临时计时

在 Swift App 初始化和 `makeUIViewController` 中临时加入 `ProcessInfo.systemUptime` / NSLog，三轮结果：

| 阶段 | 第 1 轮 | 第 2 轮 | 第 3 轮 |
| --- | ---: | ---: | ---: |
| Koin 初始化（第 1 轮包含第一条 NSLog 成本） | 13.952 ms | 3.602 ms | 3.965 ms |
| `initData()` 返回前的同步依赖解析与调度 | 6.945 ms | 4.176 ms | 4.126 ms |
| Compose 控制器创建 | 29.442 ms | 0.923 ms | 0.902 ms |

这些时间不包含完整 Compose 首次组合或帧呈现，也不包含 IO 协程完成时间。它们进一步排除了“秒级 Koin 或同步数据库依赖创建”作为本次主要耗时的解释。`InitData` 的后台解析优化仍可以单独实施，但本次没有证据支持把它当作主要问题的修复，故未保留该代码改动。

### 结果与后续范围

本轮只保留调查报告与索引，临时 Swift 日志已还原。恢复 iOS 27.0 上默认 Debug 包，关闭本轮临时启动的 iOS 18.5 模拟器；没有修改模拟器分辨率、密度。未连接可用 iPhone，因此真机验证无法执行。未更改主线历史或创建提交。

可复查的临时证据：

- `/private/tmp/notes-current-debug.trace`、`notes-current-release.trace`、`notes-release-ios18.trace`。
- `/private/tmp/notes-debug-dyld.xml`、`notes-release-dyld.xml`、`notes-ios18-dyld.xml`。
- `/private/tmp/notes-{current-debug,no-dylib-valid,release,release-ios18}-timings.json` 与对应 PNG。
- `/private/tmp/notes-initialization-{0,1,2}.log`。
- `/private/tmp/notes-startup-{debug,release,restored}-build.log`、`notes-release-framework-build.log`。

后续优先在真机脱离调试器验证 Release。若真机也慢，再沿对应轨迹排查；当前应将 iOS 27 模拟器的启动测量与生产性能分开。登录状态加载反馈属于体验改进，不能处理 `main()` 前的系统加载等待。

## 按 Apple 官方文档继续定位：PhotosUI 链接实验

### 方法依据

Apple 的 [Reducing your app's launch time](https://developer.apple.com/documentation/xcode/reducing-your-app-s-launch-time) 将动态库加载、静态初始化、应用初始化和首帧工作区分开。其 [WWDC19 Optimizing App Launch](https://developer.apple.com/videos/play/wwdc2019/423/) 强调检查依赖的隐性启动成本、在 Release 和一致数据条件下分析，以及区分 profiling 的开销与性能测量。依照 [Logging Dynamic Loader Events](https://developer.apple.com/library/archive/documentation/DeveloperTools/Conceptual/DynamicLibraries/100-Articles/LoggingDynamicLoaderEvents.html) 使用 `DYLD_PRINT_LIBRARIES` 查看 main 前的映像范围。

Apple 对 cold/warm launch 的分类依据是设备、依赖和缓存状态，不仅是进程是否存活。报告中原先“进程冷启动”的表述已改正；这些重复终止/启动实验属于缓存可能已预热的 warm launch。

### 排除静态初始化作为主要原因

重新解析已有 dyld XML，**只保留 `dyld_sim` 的入口前区间**，排除进入 main 后的 dlopen 事件：

| 阶段事件 | Debug / iOS 27.0 | Release / iOS 27.0 |
| --- | ---: | ---: |
| Static Initializer 的事件时长合计 | 35.736 ms | 29.988 ms |
| Apply Fixups | 22.816 ms | 8.738 ms |
| Objc Image Init | 2.888 ms | 3.010 ms |

事件时长可能存在嵌套，不能简单把所有类别相加当作独占 CPU 时间。这里用于判断量级：这些已记录事件无法解释约 1.6–1.9 秒入口前区间；即时采样所显示的主要栈是 `JustInTimeLoader::loadDependents → matchesPath → mach_o::Image::makeFromDyldMapped`，更符合加载器遍历与匹配依赖的工作。未声称确认 dyld 内部算法缺陷或共享缓存损坏。

### 纯原生最小应用的单变量实验

用同一份 Objective-C 源码、同一个 clang 优化参数与 iOS 27.0 模拟器生成四个可执行文件，均约 53 KB。源程序进入 main 时立即写出固定标记；不同包只改变额外链接的系统框架，使用 `-needed_framework` 确保未调用 API 时也保留该 load command。程序不含业务逻辑，不调用 PhotosUI 或 SwiftUI API。

每组测量三次，交错运行；计时起点在 `simctl launch --console` 前，终点为宿主收到 main 标记。包含 simctl/日志传输的固定成本，**不是精确入口前 dyld 时长，也不是首帧时长**。另以无缓冲管道重复确认结果，降低日志缓冲影响。

| 链接组合 | 第 1 轮 | 第 2 轮 | 第 3 轮 | 中位数 |
| --- | ---: | ---: | ---: | ---: |
| UIKit + Foundation | 0.382113 s | 0.363945 s | 0.382195 s | 0.382113 s |
| 上述组合 + SwiftUI | 0.416200 s | 0.417132 s | 0.417013 s | 0.417013 s |
| 上述组合 + PhotosUI | 1.686306 s | 1.699975 s | 1.679057 s | 1.686306 s |
| UIKit + Foundation + SwiftUI + PhotosUI | 1.672907 s | 1.668217 s | 1.686553 s | 1.672907 s |

在该实验中，仅加入 PhotosUI 就增加约 **1.30 秒**，与 Notes 主要入口前等待处于同一量级。SwiftUI 本身没有复现这个增量，Kotlin、数据库和应用业务代码也不是最小复现的必要条件。

独立的 PhotosUI 探针 App Launch 轨迹中 `dyld_sim` level 2 区间为 **1.781423 秒**。探针只用于分析 main 之前，main 后退出的轨迹不用于评价 UI 或首帧性能。第一次轨迹采集与日志实验控制同一个探针，可能受干扰，已作废并在日志实验结束后重新独立采集。

### 加载范围和项目依赖来源

开启 `DYLD_PRINT_LIBRARIES`，截取 main 标记前日志中带 UUID 的映像记录：

- UIKit 基线：527 条唯一映像记录。
- 额外 PhotosUI：1224 条唯一映像记录，比基线增加 697 条系统映像记录。
- PhotosUI 组另有 423 条 `move loaded to delayed` 记录，基线为 23 条。因此这些数字是加载器报告的映像范围，**不代表所有映像都在 main 前执行了初始化**。

新增范围涉及 PhotosUI、PhotosUICore 和大量图像、媒体与系统私有框架。这是依赖图增大的证据，不能把新增范围中的某一个私有框架直接认定为独立根因。

调查当日的来源链已核对；以下路径属于 2026-10-02 的历史源码，头像选择器现位于 `feature:profile`，不作为当前文件定位入口：

1. `feature/email/build.gradle.kts:45` 依赖 `calf-file-picker`。
2. `AvatarPicker.kt:34` 用 `FilePickerFileType.Image` 创建选择器。
3. 本地缓存的 Calf 0.14.0 iOS 源码导入 `platform.PhotosUI.PHPickerConfiguration`、`PHPickerViewController` 等；其 klib manifest 声明依赖 `org.jetbrains.kotlin.native.platform.PhotosUI`。
4. `dyld_info -linked_dylibs` 确认 Notes Release 包中强链接 `/System/Library/Frameworks/PhotosUI.framework/PhotosUI`。

PhotosUI 是头像选择功能的实际依赖，并非可以直接删除的无用库。仅将 `rememberFilePickerLauncher` 或 PHPicker 对象创建延后到用户点击，不会移除二进制的 PhotosUI load command，也无法处理本实验复现的 main 前加载。

## 实际 Notes：头像选择功能的启动影响验证

### 对照设计

在同一 iPhone 18 Pro / iOS 27.0 模拟器、同一登录状态和用户数据下，顺序构建并安装三个 Debug 诊断包。每组终止/启动三轮；计时期间不运行 Instruments、Gradle 或额外日志采集。未清除数据、重启设备或改变无障碍设置，属于 warm launch 对照。

- **A 原版**：保留 `rememberFilePickerLauncher`、头像点击与 `calf-file-picker`。
- **B 只停用选择器**：移除选择器对象创建和头像点击，保留头像显示、回调签名与 `calf-file-picker`。二进制仍直接链接 PhotosUI。
- **C 移除选择依赖**：沿用 B 的头像显示代码，将 `calf-file-picker` 临时替换为同版本 `calf-io`，仅保留其他接口所需的 `KmpFile` 类型。`dyld_info` 确认 PhotosUI 直接 load command 消失。已安装包与 C 备份的 `Notes.debug.dylib` SHA-256 相同，排除装错包。

C 临时停用了头像选择，目的是验证依赖的启动影响，并不是保留功能的替代实现。测试后完整恢复源码、依赖和原始 Debug 包。

### 未启用 Instruments 的首页测量

| 方案 | 第 1 轮 | 第 2 轮 | 第 3 轮 | 中位数 |
| --- | ---: | ---: | ---: | ---: |
| A 原版 | 3.456 s | 2.499 s | 2.500 s | 2.500 s |
| B 停用选择器，保留库 | 2.497 s | 2.484 s | 2.490 s | 2.490 s |
| C 停用选择器，移除库 | 2.484 s | 2.469 s | 2.506 s | 2.484 s |
| A 恢复原版后复测 | 2.453 s | 2.517 s | 2.479 s | 2.479 s |

时间为启动命令开始到首次含首页内容的截图完成；已检查 B/C 截图，确认为相同邮件首页。每次截图本身约 0.4 秒、轮询步长约 0.5 秒，不是精确首帧时间。A 第一次更慢如实保留；其后两轮与恢复后的原版用于检查重复性。中位数仅差 10–16 ms，远小于测量分辨率，**未观察到明显首屏加速**，不能声称百分比优化。

### 阶段变化及原因

C 在独立 App Launch 轨迹中的 `dyld_sim` level 2 入口前区间为 **384.259 ms**。恢复原版后重新采集为 **1.632247 s**（先前同设备原版 Debug 为 **1.911004 s**）。移除 PhotosUI 直接依赖确实减少了入口前加载，而不仅是改变 Kotlin 对象创建。

但 C 在进入 main 后又出现 **1.348445 s** 的 dlopen：

```text
UIApplicationMain
 → -[UIApplication _run]
 → -[UIApplication(UIKitApplicationAccessibility) _accessibilityInit]
 → _updateApplicationAccessibility / _accessibilityBundlePrincipalClass
 → NSBundle loadAndReturnError
 → _CFBundleDlfcnLoadBundle
 → dyld4::APIs::dlopen_from
 → System/Library/AccessibilityBundles/UIKit.axbundle/UIKit
```

恢复原版的新轨迹中同一路径的实际加载区间为 **97.090 ms**（原版先前轨迹约 **137.92 ms**）。C 的独立 `DYLD_PRINT_LIBRARIES` 日志仍记录 PhotosUI、PhotosUICore 等映像，以及 PhotosUI 的无障碍 bundle；因此“删除直接链接”不等于“应用整个启动期不再加载 PhotosUI”。这里的日志用于确认运行时加载范围，不用日志采集时的启动时间作性能基线。

| 本轮独立轨迹 | 入口前 dyld 区间 | main 后 UIKit 无障碍 bundle 实际 dlopen |
| --- | ---: | ---: |
| 恢复原版 A | 1.632247 s | 0.097090 s |
| 移除选择依赖 C | 0.384259 s | 1.348445 s |

综合轨迹和首页测量，当前环境下依赖加载成本从入口前转移到 UIKit 的无障碍加载阶段，抵消了大部分可见收益。这是当前模拟器的实测解释，未证明开启了 VoiceOver、未确认某个无障碍开关是根因，也没有证据认定应用头像业务代码主动加载该无障碍 bundle。

### 结论与范围

头像选择库影响启动的**阶段分布**；选择器对象创建本身没有表现出秒级成本。在当前 iOS 27.0 模拟器中，停用或移除头像选择并没有明显改善首页出现时间。前述最小原生探针只验证到 main，不能替代实际应用到首页的验证。

本轮为 Debug 模拟器、已登录 warm launch 实验。真机 Release、真正 cold launch 和不同系统无障碍环境尚未验证。当前证据不支持为了启动速度删除头像功能或直接替换照片选择体验。后续应优先在真机 Release 的完整启动轨迹中确认系统加载是否同样占主导。

原版恢复构建 `BUILD SUCCEEDED`；三个临时修改文件均与备份逐字节一致，恢复包重新包含 PhotosUI 直接链接。恢复后的三次启动均显示正常邮件首页；仓库仅保留报告和文档索引改动。未提交或修改 Git 历史。

临时证据位于 `/private/tmp/notes-avatar-ab/`（三组应用、源码备份和构建日志）、`/private/tmp/notes-avatar-{C,A-restored}.trace`、`/private/tmp/notes-avatar-{C,A-restored}-dyld.xml`、`/private/tmp/notes-avatar-C-dyld.log`，以及 `/private/tmp/notes-avatar-{A,B,C,A-restored}-timings.json` 和对应截图。这些临时文件不属于仓库长期归档。
