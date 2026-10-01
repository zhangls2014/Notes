# 共享输入框键盘切换保护

状态：已实现并通过 Android 宿主回归、Android 构建与 lint、iOS 编译；新版本真机手感待验收。用户已在真机确认新建邮件页主题与正文切换时存在抖动，并授权封装。

## 设计

将登录 Feature 内的焦点交接状态与 Android IME 高度保护移到 `core:theme/layout`。调用方传入每个输入框的稳定标识，在焦点回调中同步记录交接；共用 Android 的 400 ms 保护窗口与 200 ms 高度下降稳定期，iOS 返回普通 IME inset。各容器仍负责消费：登录表单使用 `windowInsetsPadding`，新建邮件弹层通过 `contentWindowInsets` 与 `BottomSheetDefaults.modalWindowInsets` 合并。搜索框只有单个实际输入入口，暂不接入。

## 验证

在 400×500dp Android 宿主中模拟新建邮件主题 → 正文焦点切换及 IME 220 → 0 → 260px；原实现空档第一帧视口从 216px 扩大到 368px，回归测试因此失败。共享方案在空档保持视口，正常关闭的高度下降当帧释放；默认与共享方案在 220px IME 下视口均为 216px，未重复避让。登录与新建邮件 UI 回归、登录单元测试、iOS 编译、Android 构建与 lint 均通过；厂商安全键盘仍需在受影响真机上验证新版。
