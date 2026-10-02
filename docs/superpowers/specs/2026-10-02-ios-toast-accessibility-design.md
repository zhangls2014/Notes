# iOS Toast 无障碍修复

用户授权修复无障碍审计第三项。沿用每个 Compose 宿主持有一个 Toast presenter 的边界，不改变 Android 或全局事件模型。

- 标签使用 UIKit subheadline 文本样式和 adjustsFontForContentSizeCategory；保持多行。
- 每个 Toast 窗口观察系统内容字号变化，请求根视图重新布局，关闭窗口时移除观察者。
- 显示协程等待现有 250ms 淡入后发送 UIAccessibilityAnnouncementNotification；仅对仍可见且属于前台场景的窗口播报。不使用 screenChanged／layoutChanged，不主动移动读屏焦点。
- 保持原有 2／3.5 秒显示时长，淡入耗时计入总时长。被替换、关闭或取消的显示任务不会延迟播报；窗口消失后文本 announcement 仍由系统处理。
- 使用 iOS 原生测试验证文本样式、自动字号更新配置和标签内容，回归既有布局测试并编译应用。Kotlin/Native 独立测试不是 VoiceOver 宿主，实际声音、动态字号修改和焦点顺序另需设备验收。
