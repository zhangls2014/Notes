# iOS Launch Screen

日期：2026-10-02。状态：按用户“实现 Launch Screen，图片资源参考 Android”的要求实施；采用上一轮已提出并获用户执行指令的系统静态启动页方案。

## 设计

使用系统 Launch Screen 覆盖应用首个界面呈现前的等待。背景使用 Android `app_launcher_background` 的 `#0F2A44`；图片复用 `androidApp/src/main/res/drawable/app_ic_launcher_foreground.xml` 的三条路径、颜色与内部变换，转换为透明背景 PDF 矢量资源。启动图居中，浅色和深色使用同一品牌色和图形。

优先用 Info.plist 的 `UILaunchScreen` 指定颜色与图片。若运行时不能获得合适的居中尺寸，使用原生 LaunchScreen.storyboard 显式约束；两者只保留一个有效配置。资源放在 iOS 资产目录，不新增依赖，不改 Android 图标，不改共享导航、登录状态或头像选择逻辑，不添加人为延时。

## 验证

构建 iOS Debug，检查最终 Info.plist 和资源打包；在现有已登录模拟器上检查启动页及进入首页。截图验证启动背景、图形、位置，以及深浅外观；测试后恢复原有外观设置。启动页改善展示体验，不宣称缩短系统加载时间。
