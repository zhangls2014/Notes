# Android 启动图标与应用名称

## 规则

- `release` 使用现有蓝底黄星启动图标。`debug` 使用无文字的紫底黄星图标。包名独立的专项构建也按其实际构建类型选图标。
- 构建类型和包名分别处理。默认 `applicationId` 为 `me.zhangls.notes`，切换构建变体本身不要求新包名。需要与原应用并装时才显式指定独立 `applicationId`。
- 每次新增独立包名时，同次打包必须设置独立的桌面应用名称。覆盖 Android Manifest 的应用标签所引用的 `@string/app_name`，同时提供默认英文和简体中文。名称用「品牌 · 用途」，例如 `Notes · Local Test` / `摘星 · 本地联调`、`Notes · Dialog Check` / `摘星 · 弹窗验收`。同用途下有多个并装包时，在名称上继续加实际区别，例如 `Debug` 或 `Release`。
- 临时 Gradle init 脚本或验收夹具也遵循此规则。打包时将包名和本地化标签放在同一份专项配置中，重打同一专项包沿用原来的包名和名称。

## SVG 和尺寸

启动图标的设计源稿放在 `androidApp/artwork/`，命名为 `launcher-<用途>.svg`。Android 打包用资源放在对应的 `src/<sourceSet>/res/`。有替代稿时更新同一用途的源稿并通过 Git 追溯旧版。[Debug SVG](../androidApp/artwork/launcher-debug.svg) 的背景、对话框和折角采用紫色，黄星及前景的三条路径、缩放与位移沿用 Android 原版。iOS 使用同一配色，沿用 [iOS 原图的几何](ios-app-identity.md#图标生成与接入)。

[原版 SVG](../androidApp/artwork/launcher-original.svg) 原样归档自提交 `9b43957b` 的 `.idea/icon.svg`，同一文件也保留在 [.idea/icon.svg](../.idea/icon.svg) 供 IDE 使用。历史 SVG 带圆角背景，安装包的导出几何以下方各平台现有资源为基准。

| 资源 | 与当前图标一致的尺寸 |
| --- | --- |
| SVG / Adaptive Icon 图层 | `108 × 108` 视口 / `108 × 108 dp` |
| `mipmap-mdpi` 普通 / 圆形 WebP | `48 × 48 px` |
| `mipmap-hdpi` 普通 / 圆形 WebP | `72 × 72 px` |
| `mipmap-xhdpi` 普通 / 圆形 WebP | `96 × 96 px` |
| `mipmap-xxhdpi` 普通 / 圆形 WebP | `144 × 144 px` |
| `mipmap-xxxhdpi` 普通 / 圆形 WebP | `192 × 192 px` |

## 生成和接入方案

1. 用 Android Studio Vector Asset 从 SVG 的前景组导入 `VectorDrawable`。背景转换为纯色资源。沿用原版单色轮廓，并检查圆形遮罩。系统主题图标可能抹平 Debug 与 Release 的颜色区别，独立安装的包通过应用名称区分。 [Android 自适应图标规范](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive)
2. 普通和圆形 WebP 分别以同密度的原文件为基准，按 Debug SVG 配色替换背景、对话框和折角的颜色，保留原画布、遮罩、逐像素透明度、黄星及对话框的边界和位置。普通与圆形图标的透明留白分别保留，禁止重新裁切、缩放或居中。保留抗锯齿边缘，再用 `cwebp -lossless input.png -o output.webp` 编码。逐个对照原版检查画布像素尺寸、透明度及图形边界。
3. 将生成的前景、背景和 WebP 放在 `androidApp/src/debug/res/`，使用与 [现有图标](../androidApp/src/main/res/mipmap-anydpi/app_ic_launcher.xml) 对应的资源名覆盖。普通和圆形 Adaptive Icon 共用前景轮廓作为单色图层。Release 由 `src/main/res/` 提供原版资源。专项包名及中英文标签由对应专项配置一起注入。 [Android 构建变体与资源优先级](https://developer.android.com/build/build-variants)
4. 构建代表性的 Debug 和 Release APK，用 `aapt2 dump badging <apk>` 核对应用 ID、默认/中文标签和启动图标入口。安装到模拟器后分别检查普通、圆形和系统主题图标，确认桌面小尺寸下可读。有并装需求时，用两个不同 ID 的包检查图标和名称。用相同 ID 的 Debug / Release 分别验收，或用独立测试设备，避免覆盖已有安装。

Debug 图标资源已导出并接入 Debug 源集。设备验收待执行。
