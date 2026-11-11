# iOS 启动图标与应用名称

## 构建与命名规则

- Xcode Release 使用现有 `AppIcon` 蓝底黄星图标，Debug 使用 `AppIconDebug` 无文字紫底黄星图标。通过各构建配置的 `ASSETCATALOG_COMPILER_APPICON_NAME` 选择主图标。系统启动画面继续由 [UILaunchScreen](testing/ios-launch-screen.md) 单独管理。 [Apple App Icon 配置](https://developer.apple.com/documentation/xcode/configuring-your-app-icon)
- 需要与原应用并装时，显式为专项构建指定 `PRODUCT_BUNDLE_IDENTIFIER`。默认 Bundle ID 沿用 [Config.xcconfig](../iosApp/Configuration/Config.xcconfig) 的配置，切换 Debug / Release 本身无需新 ID。
- 每次新增独立 Bundle ID，同次打包必须设置独立的桌面名称。沿用 Android 的「品牌 · 用途」命名，例如 `Notes · Local Test` / `摘星 · 本地联调`。同用途多个并装包用实际区别补充名称。重打同一专项包时沿用该包的 ID 和名称。
- 在同一份专项构建配置或隔离构建副本中指定 Bundle ID 和名称。随包提供 [英文](../iosApp/iosApp/en.lproj/InfoPlist.strings) 与 [简体中文](../iosApp/iosApp/zh-Hans.lproj/InfoPlist.strings) 的 `CFBundleDisplayName` 和 `CFBundleName`，并给 `Info.plist` 设置默认显示名。只覆盖 Xcode 的默认显示名会继续被现有本地化文件中的值覆盖。 [Apple 显示名本地化说明](https://developer.apple.com/library/archive/qa/qa1823/_index.html)

## 图标生成与接入

1. 使用 [Debug SVG](../androidApp/artwork/launcher-debug.svg) 的配色，逐档以 iOS 原版 PNG 为几何基准。替换背景、对话框和折角的颜色，保留画布尺寸、满底、抗锯齿边缘、黄星及对话框的位置和四周留白。沿用 iOS 自身的图形比例，禁止套用 Android 视口裁切或重新缩放、居中。输出不透明的正方形 PNG，圆角由系统处理。
2. 在 `iosApp/iosApp/Assets.xcassets/AppIconDebug.appiconset/` 生成 PNG 与 `Contents.json`。以 [现有 AppIcon 清单](../iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/Contents.json) 的槽位和像素尺寸为准。当前清单 35 个槽位共用 25 张方形 PNG，边长为 `16 / 20 / 29 / 32 / 40 / 50 / 57 / 58 / 60 / 64 / 72 / 76 / 80 / 87 / 100 / 114 / 120 / 128 / 144 / 152 / 167 / 180 / 256 / 512 / 1024 px`。保留现有槽位的 `idiom`、`size` 和 `scale`，逐档比对画布和图形边界。
3. Debug 选择 `AppIconDebug`，Release 选择 `AppIcon`。保持按配置打包主图标，避免把两套图标作为可切换的备用资源一同打包。
4. 分别构建 Debug 和 Release 模拟器应用，检查产物 `Info.plist` 的 Bundle ID、默认显示名和主图标，以及两个 `.lproj/InfoPlist.strings` 的实际内容。用英文和简体中文检查 iPhone、iPad 主屏幕图标及标签，验证小尺寸辨识度和名称截断。需要并装时用两个不同的 Bundle ID 验收。

Debug 图标已导出并接入 Xcode Debug 配置。设备验收待执行。
