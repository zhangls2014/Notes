@file:OptIn(ExperimentalForeignApi::class)

package me.zhangls.framework.toast

import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.NSTextAlignmentCenter
import platform.UIKit.UIView
import platform.UIKit.UIViewAutoresizingFlexibleBottomMargin
import platform.UIKit.UIViewAutoresizingFlexibleLeftMargin
import platform.UIKit.UIViewAutoresizingFlexibleRightMargin
import platform.UIKit.UIViewAutoresizingFlexibleTopMargin
import platform.UIKit.UIColor
import platform.UIKit.UIEvent
import platform.UIKit.UIFont
import platform.UIKit.UILabel
import platform.UIKit.UIScreen
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowLevelAlert

actual fun showSystemToast(message: String, longDuration: Boolean) {
  NativeToast.show(message, if (longDuration) 3500L else 2000L)
}

/**
 * iOS 没有系统级 Toast，这里用独立 UIWindow 模拟系统 Toast：
 * windowLevel 高于弹窗（不会被 Dialog / BottomSheet 遮挡），且触摸穿透（不拦截任何手势）。
 */
private object NativeToast {
  private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

  // 当前显示的 Toast，串行替换：新 Toast 到来时立即移除旧的
  private var current: ToastWindow? = null

  fun show(message: String, durationMs: Long) {
    scope.launch {
      current?.dismiss()
      current = null

      val toast = ToastWindow(message)
      current = toast
      toast.show()

      delay(durationMs)
      toast.hide()
      if (current === toast) current = null
    }
  }
}

private class ToastWindow(message: String) {
  private val window = PassthroughWindow(frame = UIScreen.mainScreen.bounds)
  private var label: UILabel? = null

  init {
    window.backgroundColor = UIColor.clearColor
    window.rootViewController = UIViewController()
    window.windowLevel = UIWindowLevelAlert + 1.0

    val toastLabel = buildLabel(message)
    label = toastLabel
    window.addSubview(toastLabel)
  }

  fun show() {
    window.hidden = false
    UIView.animateWithDuration(ANIMATION_DURATION) {
      label?.alpha = 1.0
    }
  }

  fun hide() {
    UIView.animateWithDuration(
      ANIMATION_DURATION,
      animations = { label?.alpha = 0.0 },
      completion = { _ -> window.hidden = true },
    )
  }

  fun dismiss() {
    window.hidden = true
  }

  private fun buildLabel(message: String): UILabel {
    val screenBounds = UIScreen.mainScreen.bounds
    val screenWidth = screenBounds.useContents { size.width }
    val screenHeight = screenBounds.useContents { size.height }

    val font = UIFont.systemFontOfSize(14.0)
    val maxWidth = screenWidth - 60.0

    // 用临时 label 测量文本尺寸（sizeThatFits 与当前 frame 无关）
    val measuring = UILabel(frame = CGRectMake(0.0, 0.0, 0.0, 0.0))
    measuring.text = message
    measuring.numberOfLines = 0
    measuring.font = font
    val fitted = measuring.sizeThatFits(CGSizeMake(maxWidth, screenHeight / 2.0))
    val width = fitted.useContents { width } + 40.0
    val height = fitted.useContents { height } + 28.0

    // frame 是 val 不可赋值，直接在构造器中给定最终尺寸与位置（底部居中）
    val toastLabel = UILabel(
      frame = CGRectMake((screenWidth - width) / 2.0, screenHeight - height - 100.0, width, height),
    )
    toastLabel.text = message
    toastLabel.numberOfLines = 0
    toastLabel.textAlignment = NSTextAlignmentCenter
    toastLabel.textColor = UIColor.whiteColor
    toastLabel.backgroundColor = UIColor(white = 0.0, alpha = 0.78)
    toastLabel.font = font
    toastLabel.layer.cornerRadius = 12.0
    toastLabel.clipsToBounds = true
    toastLabel.autoresizingMask =
      UIViewAutoresizingFlexibleLeftMargin or
      UIViewAutoresizingFlexibleRightMargin or
      UIViewAutoresizingFlexibleTopMargin or
      UIViewAutoresizingFlexibleBottomMargin
    toastLabel.alpha = 0.0

    return toastLabel
  }

  private companion object {
    private const val ANIMATION_DURATION = 0.25
  }
}

/**
 * 触摸穿透的透明 UIWindow：hitTest 永远返回 null，事件继续派发给下层 UI。
 */
private class PassthroughWindow(frame: CValue<CGRect>) : UIWindow(frame) {
  override fun hitTest(point: CValue<CGPoint>, withEvent: UIEvent?): UIView? = null
}
