# 网页视口协议

Device Viewport Protocol v1 是本项目定义的可复用协议，不是浏览器标准。它只报告几何事实，不包含业务、导航、主题、平台选择或设备标识。任意 HTML 页面都可使用独立的 `web/device-viewport.js`，不依赖 Compose 或 Kotlin/Wasm。机器约束见 [JSON Schema](web-device-viewport.schema.json)。

## 数据与坐标

宿主向受信主文档的 `window.deviceViewport.receive()` 投递完整 JSON 对象：

```json
{
  "protocol": "device-viewport",
  "version": 1,
  "documentId": "current-document-session",
  "sequence": 1,
  "type": "snapshot",
  "coordinateSpace": "webview-normalized",
  "availability": "available",
  "features": [{
    "kind": "unknown",
    "bounds": {"left": 0.49, "top": 0, "right": 0.51, "bottom": 1},
    "orientation": "vertical",
    "state": "half-opened",
    "separating": true,
    "occlusion": "none"
  }]
}
```

| 字段 | 约束 |
|---|---|
| protocol / version / type | 固定 `device-viewport` / `1` / `snapshot`；未知主版本拒绝，未知附加字段忽略 |
| documentId | 读取当前主文档的 `deviceViewport.documentId`，投递脚本必须再次核对该值；临时关联值，不落盘 |
| sequence | 正的安全整数，同一 WebView 发布器递增；重复、乱序和旧会话不接受 |
| coordinateSpace | 固定 `webview-normalized`；WebView 内容矩形左上 `(0,0)`、右下 `(1,1)`，两轴分别归一化 |
| availability | `available` 表示可知，空数组表示确定没有相交特征；`unavailable` 表示缺少可靠信息，必须发送空数组 |
| features | 所有相交特征的完整数组（最多 64 项），每次替换旧快照；支持多个铰链，不传布局列数 |
| kind | `fold` / `hinge` / `unknown`；上游只能报告几何时保持 unknown |
| orientation | `vertical` / `horizontal` |
| state | `flat` / `half-opened` / `unknown`；flat 不表示铰链消失 |
| separating | 是否分隔物理区域，与遮挡和姿态独立 |
| occlusion | `none` / `full` / `unknown` |

边界必须是有限数，满足 `0 ≤ left ≤ right ≤ 1`、`0 ≤ top ≤ bottom ≤ 1`。保留零宽或零高折线，拒绝点。未知枚举忽略该 feature，畸形数值或错误数据结构拒绝整个快照，避免部分错误覆盖有效状态。不得从 `half-opened` 推断遮挡，不得从 `flat` 推断没有分段。

### 原生到网页

1. 窗口事实只由组合根读取。宿主使用该事实与 WebView 的实际位置、尺寸，将窗口坐标变换为 WebView 内容局部坐标，裁剪到视图矩形；不再次扣减系统栏、标题栏和安全区。
2. 不相交的特征排除，零宽或零高折线单独判定。局部边界分别除以 WebView 内容宽、高；零尺寸或不可信映射报告 unavailable。
3. 接收脚本乘以网页当前 layout viewport 的 CSS 宽高，生成网页快照。页面若不占整个 viewport，必须再扣除内容根在 viewport 的位置。
4. 原生像素、dp、UIKit point 均不可直接当作 CSS px，也不能只除以 devicePixelRatio。视图存在平移/缩放时必须使用实际变换；无法可靠处理的变换应报告 unavailable。
5. 旋转和尺寸变化后，应在本次窗口事实与视图测量完成后再投递。网页在 resize 时清除失配映射并等待新快照；单纯滚动或 visual viewport 变化重新映射，不修改原生事实。

## 网页接入

先引入独立脚本，再启动业务 UI。网站生成器会提供带内容指纹的脚本地址；其他网站可以自行托管同一文件。

```js
const viewport = window.deviceViewport;
const initial = viewport.getSnapshot();
const unsubscribe = viewport.subscribe(snapshot => {
  // 根据实际内容空间决定布局，不能把 features 非空直接当作必须分栏。
  console.log(snapshot.source, snapshot.viewport, snapshot.features);
});
// 页面组件卸载时：
unsubscribe();
```

`subscribe()` 立即交付当前状态，后续变化按动画帧合并。输出为不可变快照：

```json
{
  "protocol": "device-viewport",
  "version": 1,
  "documentId": "current-document-session",
  "sequence": 1,
  "coordinateSpace": "layout-viewport-css-px",
  "source": "host",
  "availability": "available",
  "viewport": {"width": 1000, "height": 800},
  "features": [{
    "kind": "unknown",
    "bounds": {"left": 490, "top": 0, "right": 510, "bottom": 800},
    "orientation": "vertical",
    "state": "half-opened",
    "separating": true,
    "occlusion": "none"
  }]
}
```

来源优先级为 host → browser → fallback。host 绑定期间，不合并浏览器分段，防止出现两套不一致的几何。标准浏览器适配使用 `window.viewport.segments`，在窗口尺寸、屏幕方向或姿态事件后重新读取；不可得的姿态、种类和遮挡保持 unknown。fallback 返回 unavailable 和空 features，网页仍可根据窗口空间布局。

## 实时更新与生命周期

- 原生首次加载完成、恢复保留视图、窗口事实或视图几何变化时投递。连续变化按帧取最新值，去重后序号递增，不为每个中间传感事件排队。
- 每次消息是完整替换。折痕离开 WebView 或展开后没有折痕时发送 available＋空数组；能力不可用发送 unavailable＋空数组，两者都清除旧几何。
- `receive(payload)` 返回是否接受；主文档切换创建新的 documentId。所有注入均校验当前 HTTPS 来源和加载代次，不把已排队的旧窗口快照投递到新页面。
- 解绑时调用 `detach(currentDocumentId)`，停止原生监听，回到 browser/fallback；保留该会话已接受的最大序号，防止旧消息重新绑定。恢复视图后以更高序号发送当前状态。
- 快照在业务订阅前也可接收；迟到的 Wasm 启动只需读取最近状态，无须轮询或重新请求硬件。
- 接收接口是页面内只读几何入口，不是跨来源鉴权机制。宿主只向已配置的 HTTPS 官网主文档发布，不向第三方项目页或 iframe 暴露原生能力，不使用 `*` 来源或全局原生方法调用接口。

## 其他几何示例

以下为快照中的 features 字段节选，外层字段沿用完整消息。

```json
[]
```

零宽竖向折线：

```json
[{"kind":"fold","bounds":{"left":0.5,"top":0,"right":0.5,"bottom":1},"orientation":"vertical","state":"flat","separating":false,"occlusion":"none"}]
```

两个竖向铰链：

```json
[
  {"kind":"hinge","bounds":{"left":0.32,"top":0,"right":0.34,"bottom":1},"orientation":"vertical","state":"unknown","separating":true,"occlusion":"full"},
  {"kind":"hinge","bounds":{"left":0.66,"top":0,"right":0.68,"bottom":1},"orientation":"vertical","state":"unknown","separating":true,"occlusion":"full"}
]
```

横向折线：

```json
[{"kind":"fold","bounds":{"left":0,"top":0.5,"right":1,"bottom":0.5},"orientation":"horizontal","state":"half-opened","separating":true,"occlusion":"none"}]
```

协议只报告事实，不要求每个网页避让每条折线。Notes 更新日志仅在实际左右双栏时避让竖向铰链，单栏和站点导航不添加铰链包装。

## 验证与依据

运行 `node --test web/device-viewport.test.mjs web/build.test.mjs`。双端投递、映射和设备验收入口见[测试指南](testing/README.md)；未经验证的设备行为记录到[待验收清单](testing/pending-acceptance.md)。

- [Android FoldingFeature](https://developer.android.com/reference/androidx/window/layout/FoldingFeature)：应用窗口坐标系。
- [CSS Viewport 草案](https://drafts.csswg.org/css-viewport/#the-viewport-interface)：分段快照与方向、窗口、姿态事件。
- [Chrome Viewport Segments API](https://developer.chrome.com/blog/viewport-segments-api-shipped)：标准分段入口与浏览器适配。
- [Android JavaScript bridge](https://developer.android.com/develop/ui/views/layout/webapps/native-api-access-jsbridge)：主框架与来源限制。
- [Apple WKUserScript](https://developer.apple.com/documentation/webkit/wkuserscript)：脚本注入和主框架范围。
