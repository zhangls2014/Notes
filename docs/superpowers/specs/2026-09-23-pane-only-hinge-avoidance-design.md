# 仅分栏 UI 规避铰链设计

状态：已实现（2026-09-23）

日期：2026-09-23

## 背景

当前实现把“窗口存在铰链”作为通用 UI 切换条件。设置页、搜索、普通对话框、设置选择器、新建邮件弹窗和应用导航外壳会因此换用自定义安全区或对话框实现。它们并不形成需要跨越铰链的多窗格结构，这种处理会改变普通组件的尺寸、滚动、弹出位置和视觉样式。

铰链排除区域应属于分栏布局决策，而不是整个应用的通用安全区。

## 目标

- 仅在 UI 实际采用多窗格布局时规避铰链。
- 登录页的主窗格／辅助窗格继续避开铰链。
- 邮件列表／详情窗格继续避开铰链。
- 单栏页面、浮层、导航栏和普通组件不因铰链存在而切换实现或改变样式。
- 半开与全开状态使用同一铰链几何规则，保持分栏稳定。

## 非目标

- 不为所有单栏内容提供“最大安全区域”容器。
- 不让 Feature 自行读取设备名称、姿态名称或维护第二套断点。
- 不改变现有窗口尺寸分档、导航返回栈或邮件场景结构。
- 不重新设计设置页、搜索页或弹窗视觉。

## 方案

窗口事实仍只在组合根读取，并生成一份 `PaneScaffoldDirective`。该 directive 使用 `HingePolicy.AlwaysAvoid`，通过 CompositionLocal 下发，但只允许真正的窗格消费者使用：

1. 登录页的 `SupportingPaneScaffold`。
2. 邮件列表／详情的 Navigation 3 `ListDetailSceneStrategy`。

所有其他 UI 恢复标准组件路径，不读取铰链状态，也不使用通用铰链安全区包装器。

## 组件边界

### 保留铰链感知

- `composeApp` 继续从同一份 `WindowAdaptiveInfo` 计算 `PaneScaffoldDirective`。
- `LoginScreen` 将 directive 原样交给 `SupportingPaneScaffold`。
- `AppNavHost` 将 directive 原样交给 `rememberListDetailSceneStrategy`。
- directive 的纯几何测试继续覆盖真实铰链、半开／全开稳定性和多个排除区域。

### 移除铰链感知

- `AppShell` 始终使用标准 `NavigationSuiteScaffold`。
- `AppNavHost` 不再用 `HingeSafeContent` 包裹整个导航内容。
- 设置页不再使用 `HingeSafeContent`。
- 搜索展开态使用原有 Material SearchBar 实现。
- 普通对话框使用原有 Compose `Dialog`。
- 设置列表选择器使用原有 `ListPreference`。
- 设置图标选择器使用原有 `DropdownMenuPopup`。
- 新建邮件继续使用原有 `ModalBottomSheet`。
- 登录页不再额外使用手工 `HingeSafeColumn`；分栏位置完全由 `SupportingPaneScaffold` 与 directive 决定。

### 删除无用抽象

移除不再有调用方的通用安全区实现，包括 `HingeSafeContent`、`HingeSafeDialog`、`HingeSafeColumn`、`safeRegions`、`largestSafeRegion` 以及仅为这些组件服务的铰链布尔辅助方法。保留计算 `PaneScaffoldDirective` 所必需的稳定姿态和排除区域逻辑。

## 数据流

```text
rememberWindowAdaptiveInfo()
        |
        v
calculateAppPaneScaffoldDirective(HingePolicy.AlwaysAvoid)
        |
        +--> Login SupportingPaneScaffold
        |
        +--> Email ListDetailSceneStrategy

其他页面和组件：不读取铰链信息
```

## 测试与验证

- 先增加或调整架构守卫测试，使单栏调用点仍引用铰链 API 时失败。
- 保留 directive 的几何测试，确认真实铰链产生排除区域，半开／全开结果稳定。
- 运行 `core:theme`、`feature:login`、`feature:main` 相关测试。
- 运行 Android 整包构建、iOS 编译、受影响模块 Detekt 和 Android lint。
- 真机或折叠模拟器检查：
  - 普通／折叠状态下设置页、搜索、弹窗和导航栏使用相同组件样式。
  - 登录双栏和邮件列表／详情双栏不覆盖铰链。

## 成功标准

- 代码中只有登录分栏和邮件列表／详情分栏消费带铰链排除区域的 directive。
- 单栏 UI 不出现 `hasHinges`、`HingeSafeContent` 或 `HingeSafeDialog` 分支。
- 无铰链与有铰链窗口中的单栏组件保持同一组件树和交互语义。
- Android、iOS 与相关测试均通过。
