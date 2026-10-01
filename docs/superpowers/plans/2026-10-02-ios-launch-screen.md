# iOS Launch Screen Implementation Plan

**Goal:** 显示与 Android 图标一致的 iOS 系统静态启动页。

**Architecture:** iOS asset catalog 提供品牌背景色和透明 PDF 图形，系统启动配置引用资产。用户已要求执行，直接在当前工作区实施；不创建过程提交，不改已有启动调查文件。

**Tech Stack:** Xcode asset catalog、Info.plist、原生系统 Launch Screen。

- [x] 将 Android 图标的三条矢量路径等比例转换为 240 × 240 pt PDF，创建 `iosApp/iosApp/Assets.xcassets/LaunchLogo.imageset/`，启用矢量保留。
- [x] 创建 `LaunchBackground.colorset`，背景为 Android `#0F2A44`。
- [x] 在 `iosApp/iosApp/Info.plist` 配置 UILaunchScreen 的 UIColorName、UIImageName，删除 Debug/Release 自动空启动页生成设置。若截图显示图片尺寸不合适，切换 storyboard 显式居中与尺寸约束。
- [x] 执行 xcodebuild Debug（现有模拟器、无签名），检查最终打包 plist。
- [x] 安装并捕获启动截图、首页截图，验证浅色和深色；恢复原有模拟器外观。运行 git diff --check，更新计划与文档索引。

## 执行结果

- 采用 Info.plist 配置，无需 storyboard。图形为透明 PDF，三条路径与 Android 原资源一致，整体画布为 240 pt。
- Debug 构建成功：`/private/tmp/notes-launch-screen-build.log`；最终包 UILaunchScreen 包含 LaunchBackground 与 LaunchLogo。
- iPhone 18 Pro / iOS 27.0 已登录模拟器的浅色、深色截图均显示启动图，后续截图显示正常邮件首页。没有据截图宣称精确首帧时长。
- 原有外观为 light，finally 恢复并查询确认为 light。截图位于 `/private/tmp/notes-launch-screen-{light,dark}-{0..6}.png`。
- 未新增依赖、未更改共享业务逻辑；保留此前调查文档的未提交改动，没有创建提交。
