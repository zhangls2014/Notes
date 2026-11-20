# 更新日志自适应与网页视口协议设计

日期：2026-11-20。状态：已实施；验证与设备缺口见 `docs/testing/pending-acceptance.md` 的 2026-11-20 条目。过程文件尚未删除。

## 目标与边界

按已评审原型调整更新日志内容区。手机、平板、折叠设备仅显示自身平台的已发布记录；窗口尺寸只决定布局，不决定平台。导航栏与设置 UI 不改。桌面浏览器保留已有的平台入口。

同时定义可供其他网页使用的视口几何协议，交付规范、独立浏览器接收脚本，以及当前 Android/iOS WebView 的适配。协议不包含更新日志、版本选择、导航、主题、账户或设备标识，不新增第三方运行时依赖。

## 已检查的现状

- `web/src/wasmJsMain/kotlin/me/zhangls/web/Website.kt` 已使用 Navigation 3 列表—详情场景；当前以 760dp 乘字号比例判断宽窗。
- 同文件在窄窗使用站点侧栏；本需求保留该站点 UI，只基于侧栏扣除后的真实内容宽度计算更新日志布局。
- `Pages.kt` 的 ReleaseCard 固定内边距 24dp，需要区分窄窗与宽窗。
- `ReleaseHistoryScreen.kt` 已传入 `embed=1` 和安装平台。纯浏览器入口目前允许 URL platform 覆盖识别结果，需要按移动端规则收敛。
- `NoticesWebView.android.kt` 与 `.ios.kt` 已有受控页面脚本执行、加载代次和就绪逻辑，可复用，不另开通用原生方法调用桥。
- 应用窗口事实来自组合根；WebView 不重新收集 WindowManager 折叠信息。
- `docs/app-releases.md` 目前仍写平台可选，实施时先更新该约束。

## 内容设计

1. 独立网页显示更新日志标题与只读平台标识；嵌入应用时保留既有隐藏网页标题的约定，避免与原生标题重复。
2. 单栏按版本倒序连续阅读完整内容；版本、日期、最新发布标记、分类和条目保持统一顺序。只有存在对应平台条目的版本才显示。
3. 双栏显示版本目录及单个版本详情，默认最新版本；不使用多列版本卡片网格。
4. 无铰链时先以内容宽度 840 CSS px 等价布局空间作为双栏候选门槛，再检查目录、详情、间距及字体缩放后的最小空间。目录基准 220、详情至少 360；卡片正文宽度上限复用现有 ContentWidth。具体数值经候选截图确认，不直接替换共享宽度定义。
5. 单栏页边距与卡片内边距均为 16；宽窗卡片内边距为 24。安全区留在可滚动内容内部，视口两侧留白可发起滚动。
6. 竖向铰链将实际双栏放在物理分段内，允许非对称两侧。相同几何下半开与全开保持窗格位置一致；信息完整时半开优先保持物理分栏，空间不足时单栏。横向折痕不增加单栏安全区或折痕占位。
7. 保存当前版本与版本内阅读锚点。单双栏互换后恢复该版本及条目位置；字号变化导致重排时按内容锚点恢复，不能只复用旧像素偏移。显式版本链接与浏览器返回继续有效。
8. 移动端平台优先使用受信宿主的安装平台；独立浏览器识别 Android / iOS / iPadOS，覆盖 URL 中不相符的 platform 参数。平台未知时不得按窗口宽度猜测为移动设备，沿用桌面入口行为。协议本身不包含平台字段。

## 选择通用 JSON 快照的理由

仅 URL 参数不能实时响应折叠、旋转和 WebView 位置变化。仅依赖浏览器标准 API 无法覆盖所有内嵌运行环境。因此使用薄的原生几何适配加标准浏览器适配，统一输出快照。保留现有消息/脚本执行能力，不引入通用 RPC、业务命令总线或全局事件框架。

标准浏览器 API 与宿主只能有一个当前权威来源；已绑定宿主时采用宿主信息，否则采用浏览器标准信息，均不可用才降级。

## Device Viewport Protocol v1 草案

这是项目定义的开放复用协议，不声称是浏览器标准。命名不带 Notes 或业务名称。

### 宿主载荷

宿主向当前受信主文档发布以下完整状态，接收端以快照替换旧值，不做增量拼接：

```json
{
  "protocol": "device-viewport",
  "version": 1,
  "documentId": "document-session-example",
  "sequence": 12,
  "type": "snapshot",
  "coordinateSpace": "webview-normalized",
  "availability": "available",
  "features": [
    {
      "kind": "fold",
      "bounds": {"left": 0.49, "top": 0, "right": 0.51, "bottom": 1},
      "orientation": "vertical",
      "state": "half-opened",
      "separating": true,
      "occlusion": "none"
    }
  ]
}
```

- `version` 为整数主版本；未知主版本拒绝，未知附加字段忽略。
- `documentId` 是当前主文档加载会话的临时关联值，不持久化，不是设备 ID；刷新重新生成。接收端只接受当前会话。
- `sequence` 在会话内递增；重复、乱序及过期文档结果丢弃。
- `coordinateSpace` 固定为 `webview-normalized`：当前 WebView 内容可视矩形左上角为 `(0,0)`，右下角为 `(1,1)`，横纵轴分别归一化。不是整屏坐标、文档滚动坐标、Android dp 或 UIKit point。
- `availability` 为 `available` / `unavailable`。`available` 且空数组表示确定没有与此 WebView 相交的折痕；`unavailable` 表示没有可靠传感信息。两者都清除旧几何，但保留能力差异。
- `features` 为数组，不能限制为一个铰链。`kind` 为 `fold` / `hinge` / `unknown`；无法从当前公共窗口事实区分时报告 `unknown`，不猜测。
- `orientation` 为 `vertical` / `horizontal`；`state` 为 `flat` / `half-opened` / `unknown`；`occlusion` 为 `none` / `full` / `unknown`。
- `separating`、姿态和遮挡分别表达，网页不能把 `flat` 等同于铰链消失，也不能把半开等同于有遮挡。
- 坐标必须有限、在 `[0,1]` 内且边界有序；接受宽或高为零的折线，拒绝宽高同时为零的点。未知枚举忽略对应 feature，不能让网页崩溃。

### 坐标转换

宿主从组合根既有窗口事实取得折痕几何，结合原生视图在同一窗口坐标系的实际位置，变换到 WebView 局部坐标并裁剪。完全不相交的 feature 排除；零宽/零高折线采用显式相交判断，不能用会丢失零面积矩形的通用判断。

局部 x 坐标除以 WebView 内容宽度，局部 y 坐标除以内容高度。WebView 零尺寸时不发布有效几何。原生安全区已通过视图位置反映，不能再减一次状态栏或标题栏。

接收脚本把归一化边界乘以网页当前 layout viewport 的 CSS 宽高；页面再扣除自身内容根位置，得到 pane 布局坐标。不使用 `devicePixelRatio` 直接替代上述转换。页面滚动、视口缩放、窗口重排时重新计算映射；视觉视口偏移与缩放单独读取，不混入 layout 坐标。

原生视图有缩放或平移时，使用实际视图变换统一映射窗口几何；不能只减一个布局原点。动画期间映射无法保持一致时暂不采用旧几何，回到按可用空间布局。旋转中等待同一布局批次的窗口事实与视图尺寸，再发布快照，避免把旧方向几何套到新视图。

### 网页接口与生命周期

浏览器接收脚本独立于 Kotlin/Wasm、Compose 和业务，建立 `window.deviceViewport`：

```js
const snapshot = window.deviceViewport.getSnapshot();
const unsubscribe = window.deviceViewport.subscribe(next => {
  // next 是已验证、已映射为 layout viewport CSS px 的最新快照。
});
unsubscribe();
```

- `getSnapshot()` 返回当前快照，包含来源 `host` / `browser` / `fallback`、能力状态、CSS viewport 尺寸和 features；subscribe 立即交付当前状态，返回取消订阅函数。
- 规范同时给出传输载荷与网页 CSS px 输出的 JSON Schema、空快照、零宽折线、双铰链、横向折痕示例。
- 原生传输为单向只读数据投递，网页不获得调用文件系统、原生导航或设备控制的权限。受控入口接收版本化 JSON，禁止拼接未转义的可执行字符串。
- 首次页面完成加载时投递并保存最近快照；网页迟于宿主初始化时仍能读取。窗口事实、视图尺寸或位置变化后去重发布，快速变化按帧合并。
- 关闭、解绑、页面离开受信来源时停止发布；刷新建立新会话并清除旧状态；重新绑定保留的 WebView 时重发当前快照。
- 仅配置的 HTTPS 官网来源、当前主文档可接收宿主数据；第三方项目页与 iframe 不自动获得桥接。
- 标准浏览器支持时适配 `window.viewport.segments` 和可用的姿态信息；标准分段只能证明分区，无法推断的 feature 属性保持 unknown。宿主卸载或数据失效时清除缓存，再选择标准来源或 fallback。
- fallback 使用普通窗口尺寸布局，不伪造铰链、机型、设备姿态。

## 实施顺序与职责

先完成规则，再实现；全过程在当前工作区串行执行 Gradle。

1. 更新 `docs/app-releases.md` 的平台限制及展示约定；在 `docs/architecture.md` 明确通用视口信息边界。在 `docs/README.md` 为新增协议规范和 JSON Schema 增加归属与索引，先确定目录命名规则。
2. 新增 `docs/web-device-viewport.md` 与对应 schema，补齐字段校验、坐标映射、会话、能力降级和接入样例。长期规范不放过程计划目录。
3. 网页新增独立 `web/device-viewport.js` 及 Node 测试，由 `web/build.mjs` 复制并在自有页面启动入口引入。其他网页可直接复用该脚本与协议，不依赖本应用打包。
4. `web/.../Browser.kt` 接收统一快照；网页组合根装配窗口事实和 PaneScaffoldDirective，复用 HingePolicy.AlwaysAvoid，更新日志内部不另建窗口观测源。
5. 在 `Website.kt` 与 `Pages.kt` 实施内容区及平台约束；必要时将更新日志专属组合函数提取为 `ReleasesPage.kt`，不重写导航或设置。
6. About 现有 common/android/ios 源集中增加业务无关命名的载荷序列化与宿主投递辅助文件，复用已有 WebView、来源校验及生命周期。通用几何转换纯函数放在现有主题布局包，避免把业务协议塞入 core:model 或新建模块。后续网页复用该适配入口，不复制更新日志逻辑。
7. 更新未发布 `releases.json`、相关测试指南与未验收清单。设计和过程记录按文档保留规则整理，未经授权不删除。

## 验证与成功标准

- JS：解析、版本/字段拒绝、空与 unavailable 重置、重复与乱序、旧文档丢弃、取消订阅、宿主/标准/fallback 优先级、主交互快照更新；验证构建输出包含独立脚本。
- 几何：窗口到 WebView 的偏移及变换、局部裁剪、零宽折线、非对称两侧、双铰链、横向折痕、旋转后的尺寸更新、无交集与零尺寸。
- 页面：320/360/390/430 窄窗，600 分屏，840/960/1200 宽窗；Android/iOS 平台过滤、错误 URL 平台覆盖、iPadOS 识别、大字体、英文、深浅主题、加载失败。
- 状态：单双栏切换与折叠展开保留版本及阅读锚点；浏览器历史返回正常；同几何半开全开稳定；仅实际双栏消费铰链。
- 宿主：Android 加载及保留 WebView 回归，刷新会话隔离；iOS 编译与现有实际 Entry 调用回归；双端均检查第三方页面不接收协议。
- 执行 `node --test web/build.test.mjs web/device-viewport.test.mjs`；串行运行 `:web:buildSite`、About 的宿主测试及 detekt、相关 AboutUiTest、iOS 编译和模拟器回归。先核对本机 JBR/JDK，不改系统配置。
- 生成候选截图供维护者评审，不更新正式截图基线。真机铰链精度、原生动画坐标、iOS WebView 与普通移动浏览器检测分别记录验收结果，编译不替代设备验收。
- 不部署、不修改 CI/CD、不安装全局依赖、不删除文件。

## 官方依据

- [Android FoldingFeature](https://developer.android.com/reference/androidx/window/layout/FoldingFeature)：几何位于应用窗口坐标系。
- [Chrome Viewport Segments API](https://developer.chrome.com/blog/viewport-segments-api-shipped)：当前 JavaScript 分段入口为 window.viewport.segments；避免采用旧的 visualViewport.segments 名称。
- [Android JavaScript bridge](https://developer.android.com/develop/ui/views/layout/webapps/native-api-access-jsbridge)：主框架、来源限制及消息传递边界。
- [Apple WKUserScript](https://developer.apple.com/documentation/webkit/wkuserscript)：脚本注入与主框架范围。
