# Navigation 显示模块

本模块遵循根 AGENTS.md；职责与依赖边界见 [架构文档](../../docs/architecture.md#navigation-3)。

- `src/commonMain/kotlin/me/zhangls/navigation/` 放导航显示、转场与窗格安全区实现；文件使用所声明类型或 Composable 的英文名称。
- `src/androidMain/`、`src/iosMain/` 只放设备圆角的平台实现。
- `src/androidHostTest/kotlin/me/zhangls/navigation/` 放显示、几何与逐帧像素回归；宿主集成测试保留在 composeApp。测试辅助文件只服务本模块，不从应用模块引用。
- 不依赖 Feature、core:framework 或应用组合根，不拥有返回栈、登录规则、Tab 归属或窗口事实。
- 只公开宿主实际需要的显示入口、只读进度、动画时长与安全区包装；事件分发、圆角值和几何计算保持 internal/private。
- 保持 85% 缩放、220ms 缩放与 120ms 淡出、48dp/48pt 默认圆角及既有手势锚点，不变更组件尺寸、留白、交互与正式截图基线。
- 验证：`./gradlew :core:navigation:testAndroidHostTest`；Native 像素用例的分批命令见 [自适应 UI 回归](../../docs/testing/adaptive-ui.md)。编译：`./gradlew :core:navigation:compileKotlinIosSimulatorArm64`。
- 不存放生成截图或一次性报告；临时产物使用系统临时目录，正式基线变更和文件清理须遵守根授权规则。
