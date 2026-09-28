package me.zhangls.framework.toast

/**
 * 显示系统级 Toast：悬浮于所有 UI（含 Dialog / BottomSheet 等弹窗）之上，不会被遮挡。
 *
 * @param message 已解析的提示文本
 * @param longDuration 长时长（约 3.5s），否则短时长（约 2s）
 */
interface SystemToast {
  fun show(message: String, longDuration: Boolean)
}
