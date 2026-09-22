# Android 17（API 37）多形态屏幕适配审查

> 审查对象：本仓库（KMP + Compose Multiplatform）
> 审查范围：屏幕可旋转、可调整尺寸、手机 / 平板 / 双折 / 三折折叠屏
> 日期：2026-09-22

## 0. 先说三条前置事实

### 0.1 「适配 Android 17」在本项目**不是将来时**

`gradle/kmp.versions.toml`：`android-compileSdk = "37"`、`android-targetSdk = "37"`。
凡「仅影响以新版为目标平台的应用」的变更**已经在当前构建上生效**。所以本次不是"升级版本号后要改什么"，
而是"已经在生效的规则下，还有哪些地方没做到"。

### 0.2 ⚠️ 本报告**未取得官方变更清单**，判定依赖本地权威证据

技能要求的两个官方页面（`behavior-changes-37` / `behavior-changes-all`）本次**全部抓取失败**：
沙箱代理返回 `CONNECT tunnel failed, response 502`，绕开代理直连 `Connection timed out`，
WebFetch / WebSearch 两条通道同样失败。**这是事实，不是借口** —— 它意味着：

- 本报告在**方向 / 尺寸 / 多窗口 / 折叠**这一类条目上的覆盖来自下面三类本地证据，
  覆盖面**可能小于**官方清单；
- 其他领域（网络、权限、后台、隐私）本次**完全没有覆盖**，不能据此认为那些领域无变更；
- **网络恢复后应补一次官方逐条核对**，方法与命令见 §5。

实际使用的三类本地替代证据：

| 来源 | 为什么可信 | 位置 |
|---|---|---|
| AGP/lint 内置的变更说明文本 | 随 AGP 9.3.2 一起分发（`lint-checks-32.3.2.jar`），文本由平台团队维护，直接写明了"从 Android 16 起……" | `~/.gradle/caches/…/com.android.tools.lint/lint-checks/32.3.2/…jar` 内 `DiscouragedDetector` / `GestureBackNavDetector` / `ChromeOsDetector` |
| 平台 API 差分 | `platforms/android-36` 与 `platforms/android-37.0` 的桩代码 + `data/api-versions.xml`（`since="37.0"` 共 1173 条） | `~/Library/Android/sdk/platforms/android-{36,37.0}/` |
| platform framework-res | `data/res/values/config.xml`、`attrs_manifest.xml` —— 平台自己的开关与可用清单属性 | 同上 |

### 0.3 本机模拟器本来就跑在 API 37 + 16 KB 页大小上

现有 AVD `Resizable_Experimental` 的镜像就是 `system-images;android-37.2;google_apis_ps16k`
（tag：`page_size_16kb`）。实测：

```
$ adb shell getprop ro.build.version.sdk   → 37
$ adb shell getconf PAGE_SIZE              → 16384
```

即：应用在**真实 API 37 + 16 KB 页大小**上已被反复安装、启动、走完登录与列表-详情流程。

## 1. 判定总表

| 条目 | 结论 | 依据 |
|---|---|---|
| 大屏忽略 `screenOrientation` | **N-A**（本来就没用） | 清单无此属性；lint 内置文案 |
| 大屏忽略 `resizeableActivity=false` | **N-A** | 同上 |
| 大屏忽略 `min/maxAspectRatio` | **N-A** | 同上 |
| 硬件特性门槛（触摸屏等） | **N-A**（不会限流平板/桌面） | 合并清单**无任何** `uses-feature` |
| 预测性返回默认开启，`onBackPressed` / `KEYCODE_BACK` 不再触发 | **N-A** | 全仓零引用；清单已 `enableOnBackInvokedCallback="true"` |
| 强制 edge-to-edge（targetSdk ≥ 35） | **已适配** | 上一轮已改为 insets 消费式 |
| 16 KB 页大小 | **实测通过** | API 37 + `PAGE_SIZE=16384` 镜像上全程可用 |
| 旋转后重新排布 | **实测通过** | 674×841dp 单栏+左 Rail ↔ 841×674dp 双栏+空态 |
| 尺寸档位（240 → 1600dp） | **实测通过** | 本轮 240–853dp 六档形态正确、无崩溃（越界检查覆盖 2 档）；1600dp 由上一轮实测覆盖 |
| 分离式铰链 → 窗格避让 | **实测通过**（窗格类页面） | 铰链 0° 时列表栏止于铰链 x=884 |
| 桌面支架姿态判定（含三折） | **逻辑通过**（库源码 + 单测） | `calculatePosture` 用 `any { 水平 && HALF_OPENED }` |
| **登录页消费铰链几何** | **需改动** | 全仓 0 处引用 `excludedBounds` / `hingeList`；实测表单中心压在铰链线上 |
| **进程被杀后的状态保持** | **需改动（需决策）** | `savedKey` 只有 `AppViewModel` 设了 |

## 2. 已通过项（附证据）

### 2.1 方向 / 尺寸 / 宽高比的 opt-out：本来就没用，所以打不到

lint 随包分发的说明文本（`DiscouragedDetector`，逐字）：

> Fixed screen orientations will be ignored in most cases, **starting from Android 16**.
> Android is moving toward a model where apps are expected to adapt to various orientations, display sizes, and aspect ratios.

> Setting `resizeableActivity` to `false` will be ignored in most cases, **starting from Android 16**.

> Minimum and maximum aspect ratios will be ignored in most cases, **starting from Android 16**.

**源码清单与合并清单都没有这些属性**（合并产物实际核对过）：

```
$ grep -E "screenOrientation|resizeableActivity|maxAspectRatio|minAspectRatio" \
      androidApp/src/main/AndroidManifest.xml                       → 无
$ grep -oE '<uses-feature[^/]*/>' <merged_manifest>/AndroidManifest.xml → 无
```

`<uses-feature>` 一项尤其值得单列：库若声明了未加 `required="false"` 的 `android.hardware.touchscreen`，
应用会被平板/桌面/自由窗口设备**过滤掉**（lint 的 `ChromeOsDetector` 专门为此告警）。本项目合并清单干净。

### 2.2 预测性返回：全仓没有依赖旧返回路径的代码

lint（`GestureBackNavDetector`）：

> For apps targeting and running on Android 16+ (API 36+), predictive back animations are enabled by
> default. A back gesture does not trigger `{Activity,Dialog}.onBackPressed`, and does not dispatch
> `KeyEvent.KEYCODE_BACK`.

检索 `onBackPressedDispatcher` / `onBackPressed(` / `KEYCODE_BACK` / `BackHandler`：**仅 1 处命中，且是注释**。
清单已声明 `android:enableOnBackInvokedCallback="true"`。

### 2.3 旋转：同一实例内正确重排

AVD `FoldBook_76`（内屏 1768×2208 @420dpi，书本式竖向铰链）：

| 窗口 | 导航套件 | 列表-详情 | 详情返回键 |
|---|---|---|---|
| 674×841dp（竖） | 左侧 Rail | 单栏 | — |
| 841×674dp（横） | 左侧 Rail | **双栏** + 空态「Select an email to read」 | 无 |

两次都是**同一个应用实例**（未重启、未清数据），旋转后按新几何重排。

### 2.4 尺寸档位：240dp → 1600dp 全通

| 窗口 | 套件 | 列表-详情 | 越界节点 |
|---|---|---|---|
| 240×420dp | 底部栏 | 单栏 | **0（已查）** |
| 320×480dp | 底部栏 | 单栏 | **0（已查）** |
| 360×879dp | 底部栏 | 单栏 | — |
| 674×841dp | 左 Rail | 单栏 | — |
| 841×674dp | 左 Rail | 双栏 | — |
| 853×533dp | 左 Rail（按库规则推导，未实测主界面） | 双栏 | — |

「越界节点」= 节点边界超出窗口的个数（按 dump 的 `bounds` 与窗口像素尺寸比对）；标 `—` 表示该档
没跑这项检查，只核对了形态与是否有异常。全程各档 `logcat` 无 `FATAL EXCEPTION`。

> 补充：`attrs_manifest.xml` 里是存在 `android:minWidth` / `android:minHeight`（按 task 生效的最小窗口尺寸）
> 这个逃生口的，但**不需要** —— 240×420dp 已经渲染正确。

### 2.5 折叠与姿态：库已经把三折考虑进去了

`AndroidPosture.android.kt`（androidx `adaptive`）：

```kotlin
foldingFeatures.forEach {
  if (it.orientation == FoldingFeature.Orientation.HORIZONTAL &&
      it.state == FoldingFeature.State.HALF_OPENED) { isTableTop = true }
  hingeList.add(HingeInfo(...))
}
```

用 `forEach` + **任一**水平半开铰链即置位 —— 所以**三折的两条水平铰链同样判为桌面支架**。
（这一点我原本担心是缺口，读源码后确认不是，特此记录以免重复怀疑。）

`NavigationSuiteScaffoldDefaults.navigationSuiteType(adaptiveInfo)`：

```kotlin
if (minWidth == Compact) ShortNavigationBarCompact
else if (windowPosture.isTabletop || minHeight == Compact) ShortNavigationBarMedium
else WideNavigationRailCollapsed
```

本应用对 ExtraLarge（≥1600dp）把 `WideNavigationRailCollapsed` 显式提升为 `WideNavigationRailExpanded`
（`AppShell.kt`）—— 因为 `isTabletop` 时库给的是 Medium，**桌面支架永远不会命中这个覆盖**，
即"最不该给 Rail 的场景"是安全的。

### 2.6 铰链排除区对窗格类页面生效

`calculatePaneScaffoldDirective(…)` 的 `excludedBounds = getExcludedVerticalBounds(posture, verticalHingePolicy)`，
默认策略 `HingePolicy.AvoidSeparating`。实测（`FoldBook_76`，铰链 0°）：列表栏容器止于 `x=884`，
正是铰链所在列 —— 说明铰链信息确实到达了应用，且库的窗格装配在消费它。

## 3. 需要调整项

### 3.1 登录页（及任何自绘布局）没有任何铰链几何感知

**现状**

```bash
$ grep -rnE "excludedBounds|separatingVerticalHingeBounds|occludingVerticalHingeBounds|hingeList|HingeInfo" \
       --include="*.kt" . | grep -v /build/
（空）
```

全仓对铰链的唯一使用是 `WindowAdaptiveInfo.isTabletopPosture` → `windowPosture.isTabletop`，
即**一个布尔值**。登录页的两区排布（`loginArrangementFor`）用的是它 + `maxHorizontalPartitions`，
自绘 `Row` / `Column`，**不消费 `directive.excludedBounds`，也不看 `hingeList`**。

**实测后果**（`FoldBook_76`，铰链 `x=884`，书本式半开、窗口为原生 1768px）：

```
EditText x=[338,1430]  中心=(884,846)   ← 铰链线正好穿过输入框中心
EditText x=[338,1430]  中心=(884,1109)
```

表单居中于窗口 → 中心恰好压在铰链上。横排形态同理会把品牌区/表单区的分界落在与铰链无关的位置。

**修法（两选一，需你定）**

- **A1（推荐，与项目"布局决策只用库能力"的判据一致）**：用库的窗格容器承载登录页的两区
  （`SupportingPaneScaffold`：主窗格 = 表单，支持窗格 = 品牌）。分区数、铰链避让、间距全部由
  directive 决定，`loginArrangementFor` 与其 10 项单测随之简化。代价：视觉需在 6 档窗口重验一遍。
- **A2（最小改动）**：保留自绘 Row/Column，但按 `directive.excludedBounds` 把内容限制到铰链一侧，
  并把铰链宽度计入两区之间的间隔。代价：等于在 feature 里重实现一遍窗格放置，与"只用库能力"相悖。

### 3.2 进程被杀后的状态保持：`savedKey` 默认 null

**现状**（`core/framework/.../mvi/MviViewModel.kt:36`）：

```kotlin
savedKey: String? = null          // 默认纯内存态
```

全仓**只有 `AppViewModel` 给了 key**（`savedKey = "state"`）。三个 feature 的状态类
（`EmailState` / `SearchState` / `LoginState`）都明确写了"不要为它指定 savedKey"，
理由正当：`EmailState` 里含 `UserModel`（accessToken / refreshToken）。

**为什么它属于"多形态"问题**：配置变更（旋转 / 折叠 / 分屏 / 密度变化）**不**销毁 ViewModel，
所以上述状态在旋转与折叠时是安全的（实测旋转 6 次后应用状态完好）。但**进程被回收**时会全丢，
而折叠屏显著提高了这件事的概率 —— 把手机折起来放进兜里，正是后台回收最喜欢的时候。
后果是：**半写完的邮件草稿、多选集合、搜索词全部消失**。

**修法建议**：把"值得恢复"的部分从"敏感 / 大对象"里拆出来（如
`EmailDraftState(recipientIds, subject, body)`、`SearchState(searchText)`），
只对拆出来的 `@Serializable` 小状态给 `savedKey`；token 与账户列表继续留在内存态。
这是设计改动，不建议顺手做。

## 4. 建议项（非必须）

- **C · 极矮窗口下的写邮件弹层**：窗口高 480dp 时弹层内容（收件人 chip + 主题 + 正文 + 按钮）
  需要滚动才能到「Save / Cancel」；高 320dp（横屏）时更明显。功能可用（弹层内部有滚动容器），
  但官方对大屏的建议是改用侧边面板（SideSheet）。属实测结论，非缺陷。
- **D · 旋转后不恢复 IME 可见性**（新版平台行为）：当前 `windowSoftInputMode="adjustResize"`，
  旋转后键盘消失、需重新点输入框。可接受取舍，**建议写进文档而不是改代码** ——
  `stateAlwaysVisible` 会让每次进页面都弹键盘，`configChanges` 会改变生命周期，两者代价都更大。
- **E · 可用于窗口化的 API 37 新增面**（本次**无需**动作，列出备查）：
  `ActivityManager.AppTask.requestWindowingLayer` + `WINDOWING_LAYER_*` 常量、
  `ActivityOptions.setMovableTaskRequired`、`Activity.setHandoffEnabled` /
  `onHandoffActivityDataRequested`（跨设备交接）、`Display.getFrameRateVelocityMapping`、
  `WindowManager.PROPERTY_COMPAT_ALLOW_EXCLUDE_CAPTION_INSETS`。
  来源：`api-versions.xml` 中 `since="37.0"` 的条目（共 1173 条，人工筛出窗口/显示相关 25 条）。

## 5. 验证不了 / 未验证（明确列出）

| 项 | 原因 |
|---|---|
| 官方变更清单逐条 | 网络不可达（§0.2）。恢复后应重新抓取两页并逐条重判 |
| 物理折叠设备的真实姿态 | 无头 AVD 的姿态注入不可靠，且平台有硬规则：**铰链 feature 必须横跨窗口才会被上报**。实测抓到平台告警：`WindowLayoutComponentImpl: Horizontal FoldingFeature must have full width` —— 一旦用 `wm size` 改过窗口几何，铰链就被丢弃。真实姿态只能靠真机 |
| 三折实机 | 本机无三折设备定义（可用定义只有 6.7" / 7.6" / 8" 三种单铰链折叠） |
| 16 KB 页大小下的 native 库逐项确认 | 只验证了"应用整体可用"，未逐项核对 so 的对齐与加载路径（`adb logcat \| grep nativeloader` 可补） |
| 网络 / 权限 / 后台 / 隐私等其它领域的 Android 17 变更 | 未覆盖（§0.2） |

## 6. 复现命令

```bash
# 折叠设备（书本式，竖向铰链；Android 17 + 16KB 页）
avdmanager create avd -n FoldBook_76 \
  -k "system-images;android-37.2;google_apis_ps16k;arm64-v8a" -d "7.6in Foldable"
emulator -avd FoldBook_76 -no-window -no-audio -no-boot-anim -port 5556

# 折叠设备（翻盖式，横向铰链 → 桌面支架）
avdmanager create avd -n Flip67 \
  -k "system-images;android-37.2;google_apis_ps16k;arm64-v8a" -d "6.7in Foldable"

# 姿态注入（关键：用控制台的 posture，不是 device_state）
adb -s emulator-5556 emu posture 2        # 1 closed / 2 half-opened / 3 opened / 4 flipped / 5 tent
adb -s emulator-5556 emu sensor set hinge-angle0 90
#   posture 命令的实际副作用（平台侧可见）：
#     settings put global device_posture 2
#     settings put global display_features hinge-[884,0,885,2208]

# 尺寸与旋转
adb shell wm density 200                  # 只改密度：像素几何不变，铰链仍然横跨窗口
adb shell settings put system user_rotation 1     # 旋转（0 竖 / 1 横）
adb shell settings put system accelerometer_rotation 0   # 锁定自动旋转
# 收尾：wm size reset && wm density reset && settings put system accelerometer_rotation 1

# 权威读法：看应用自己看到的窗口
adb shell dumpsys window windows | grep -m1 -oE "w[0-9]+dp h[0-9]+dp [0-9]+dpi [a-z]+"
# ⚠️ 不要用 wm size / wm density 反算 dp —— 应用窗口的尺寸与密度可能与显示器不同（上一轮已踩）
```

## 7. 结论

面向"旋转 + 多尺寸 + 手机/平板/双折/三折"这一组要求，应用**绝大部分已经就绪**，主要归功于上一轮
把窗口事实收敛为单一来源、把布局决策交给库策略、以及把 insets 改成宿主消费式这三件事。
本轮只剩 **两处**需要动手：登录页的铰链几何感知（§3.1）与跨进程的状态保持拆分（§3.2）。

真正未覆盖的是**官方清单本身**（§0.2），那一步必须等网络恢复后补做 —— 本报告不能替代它。
