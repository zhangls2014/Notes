# iOS Launch Screen

系统静态启动页由 `iosApp/iosApp/Info.plist` 的 `UILaunchScreen` 配置：

- `UIColorName = LaunchBackground`：资产目录颜色为 `#0F2A44`，来源于 Android `androidApp/src/main/res/values/colors.xml` 的 `app_launcher_background`。
- `UIImageName = LaunchLogo`：透明 PDF 矢量图，来源于 Android `androidApp/src/main/res/drawable/app_ic_launcher_foreground.xml`。保留三条路径、颜色及 `scale=0.7 / translate=16` 变换，以 240 × 240 pt 画布导出；asset catalog 开启矢量保留。

浅色和深色使用相同品牌背景和图形。Xcode Debug/Release 均使用显式 plist 配置，不再自动生成空 UILaunchScreen。Android 图标后续变化时，应同步更新 iOS 的 PDF 和品牌色。

启动页由 iOS 系统显示，不执行 Compose、业务逻辑或动画，也不增加固定展示时间。应用开始呈现自己的界面后由系统移除；登录状态未知时的应用内等待 UI 不属于本次实现。

## 验证

使用 Xcode 打开 `iosApp/iosApp.xcodeproj` 并运行 `iosApp` scheme；或在命令行选择已启动模拟器：

```bash
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -configuration Debug -sdk iphonesimulator \
  -destination 'platform=iOS Simulator,id=<simulator-udid>' \
  CODE_SIGNING_ALLOWED=NO build
```

检查启动过程中显示深蓝背景和居中图形，并能正常进入应用。浅色与深色均应验证；改变模拟器外观前记录原值，结束时恢复。iOS 会缓存系统启动画面，修改资源后应重新构建和安装；在保存数据的测试设备上避免为刷新缓存卸载应用。

2026-10-02：Debug 构建及 iPhone 18 Pro / iOS 27.0 模拟器浅色、深色启动截图验证通过。未验证真机或 iPad。此功能改善启动展示，不代表系统加载性能优化。

Apple 官方配置说明：[Specifying your app’s launch screen](https://developer.apple.com/documentation/xcode/specifying-your-apps-launch-screen)。设计与执行记录见 [设计](../superpowers/specs/2026-10-02-ios-launch-screen-design.md)、[计划与验证结果](../superpowers/plans/2026-10-02-ios-launch-screen.md)。
