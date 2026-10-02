# iOS Toast 无障碍验证

更新：2026-10-02。

## 自动验证

```bash
./gradlew :core:framework:iosSimulatorArm64Test \
  :composeApp:compileKotlinIosSimulatorArm64
```

当前 6 项原生测试通过：4 项既有 ToastLayoutTest 与 2 项 ToastAccessibilityTest。新增测试直接检查生产 ToastViewController 的 UILabel：系统 subheadline 文本样式、多行和 Dynamic Type 自动更新配置；改变 UILabel 的实际 trait 内容字号后，字体 pointSize 增大。traitOverrides 用例使用 iOS 17+ API，仅用于测试，修复的生产 API 支持更早的 iOS。

旧固定字体实现上的配置测试失败，修复后通过。iOS 编译验证了 announcement 与字号通知的 Kotlin/Native 绑定。独立 Kotlin/Native 测试没有 UIKit 应用场景中的完整 VoiceOver 验收能力，不能把编译通过当作已听到播报。

## 设备验收

1. 记录原始文字大小和 VoiceOver 设置。通过 Xcode 运行 Notes，开启 VoiceOver，触发登录成功或其他已有 Toast。应读出完整提示文本，且保留当前界面的读屏焦点，不把焦点移动到屏幕根节点。
2. 将系统文字大小调到无障碍大字号，触发提示，确认字体增大、文本可多行显示。提示显示期间通过控制中心修改文字大小，确认标签重新测量并留在当前窗口安全区域；旋转或缩放窗口后继续检查布局。
3. 在 250ms 淡入阶段替换提示或关闭宿主，确认旧提示不会随后播报。正常显示仍保持短提示约 2 秒、长提示约 3.5 秒；文本 announcement 的实际播报调度由 VoiceOver 处理。
4. 在带弹窗／底部面板的场景触发提示，确认触摸仍穿透、底层控制仍可使用。宿主在后台、未附着到场景或场景断开时，不应发送新的 announcement。
5. 结束后恢复原始文字大小与 VoiceOver 设置。

当前未执行以上端到端设备步骤；已执行的是原生控件测试、既有布局回归及 iOS 编译。重要业务结果如果需要长期查阅，应另行设计持久化界面反馈。

## 官方参考

- [UIKit announcement](https://developer.apple.com/documentation/uikit/uiaccessibility/notification/announcement)
- [自动字体缩放](https://developer.apple.com/documentation/uikit/scaling-fonts-automatically)
