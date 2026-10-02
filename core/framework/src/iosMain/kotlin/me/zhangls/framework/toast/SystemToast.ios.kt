@file:OptIn(ExperimentalForeignApi::class)

package me.zhangls.framework.toast

import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.NSTextAlignmentCenter
import platform.UIKit.UIAccessibilityAnnouncementNotification
import platform.UIKit.UIAccessibilityPostNotification
import platform.UIKit.UIContentSizeCategoryDidChangeNotification
import platform.UIKit.UIColor
import platform.UIKit.UIEvent
import platform.UIKit.UIFont
import platform.UIKit.UIFontTextStyleSubheadline
import platform.UIKit.UILabel
import platform.UIKit.UISceneActivationStateForegroundActive
import platform.UIKit.UISceneDidDisconnectNotification
import platform.UIKit.UIView
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowLevelAlert
import platform.UIKit.UIWindowScene

/** One presenter per Compose host; the composition root owns and closes it. */
class IosSystemToast(private val host: UIViewController) : SystemToast {
  private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
  private var current: ToastWindow? = null
  private var displayJob: Job? = null

  override fun show(message: String, longDuration: Boolean) {
    displayJob?.cancel()
    current?.dismiss()
    current = null
    displayJob = scope.launch {
      val scene = host.viewIfLoaded?.window?.windowScene ?: return@launch
      if (scene.activationState != UISceneActivationStateForegroundActive) return@launch
      val toast = ToastWindow(scene, message)
      current = toast
      try {
        toast.show()
        // 等淡入完成后播报；替换或关闭会取消此协程，不让旧提示延迟播报。
        delay(TOAST_ANIMATION_MILLIS)
        toast.announce()
        delay((if (longDuration) 3500L else 2000L) - TOAST_ANIMATION_MILLIS)
        toast.fadeOut()
        delay(TOAST_ANIMATION_MILLIS)
      } finally {
        toast.dismiss()
        if (current === toast) current = null
      }
    }
  }

  fun close() {
    scope.cancel()
    current?.dismiss()
    current = null
  }
}

private const val TOAST_ANIMATION_MILLIS = 250L
private const val TOAST_ANIMATION_SECONDS = 0.25

private class ToastWindow(scene: UIWindowScene, message: String) {
  private val content = ToastViewController(message)
  private val window = PassthroughWindow(scene).apply {
    backgroundColor = UIColor.clearColor
    rootViewController = content
    windowLevel = UIWindowLevelAlert + 1.0
  }
  private val disconnectObserver = NSNotificationCenter.defaultCenter.addObserverForName(
    name = UISceneDidDisconnectNotification,
    `object` = scene,
    queue = null,
  ) { dismiss() }
  private val contentSizeObserver = NSNotificationCenter.defaultCenter.addObserverForName(
    name = UIContentSizeCategoryDidChangeNotification,
    `object` = null,
    queue = NSOperationQueue.mainQueue,
  ) { window.rootViewController?.view?.setNeedsLayout() }

  fun show() {
    window.hidden = false
    window.layoutIfNeeded()
    UIView.animateWithDuration(TOAST_ANIMATION_SECONDS) { content.label.alpha = 1.0 }
  }

  fun fadeOut() {
    UIView.animateWithDuration(TOAST_ANIMATION_SECONDS) { content.label.alpha = 0.0 }
  }

  fun announce() {
    if (!window.hidden && window.windowScene?.activationState == UISceneActivationStateForegroundActive) {
      // announcement 传文本给 VoiceOver，不请求屏幕切换或改变当前读屏焦点。
      UIAccessibilityPostNotification(UIAccessibilityAnnouncementNotification, content.label.text)
    }
  }

  fun dismiss() {
    NSNotificationCenter.defaultCenter.removeObserver(disconnectObserver)
    NSNotificationCenter.defaultCenter.removeObserver(contentSizeObserver)
    window.hidden = true
    window.rootViewController = null
    window.windowScene = null
  }
}

internal class ToastViewController(message: String) : UIViewController(nibName = null, bundle = null) {
  val label = UILabel(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)).apply {
    text = message
    numberOfLines = 0
    font = UIFont.preferredFontForTextStyle(UIFontTextStyleSubheadline)
    adjustsFontForContentSizeCategory = true
    textAlignment = NSTextAlignmentCenter
    textColor = UIColor.whiteColor
    backgroundColor = UIColor(white = 0.0, alpha = 0.78)
    layer.cornerRadius = 12.0
    clipsToBounds = true
    alpha = 0.0
  }

  override fun viewDidLoad() {
    super.viewDidLoad()
    view.backgroundColor = UIColor.clearColor
    view.addSubview(label)
  }

  override fun viewDidLayoutSubviews() {
    super.viewDidLayoutSubviews()
    val width = view.bounds.useContents { size.width }
    val height = view.bounds.useContents { size.height }
    val insets = view.safeAreaInsets.useContents { ToastInsets(top, left, bottom, right) }
    val maxTextWidth = (width - insets.left - insets.right - 100.0).coerceAtLeast(0.0)
    val maxTextHeight = (height - insets.top - insets.bottom - 28.0).coerceAtLeast(0.0)
    val measured = label.sizeThatFits(CGSizeMake(maxTextWidth, maxTextHeight))
    val frame = measured.useContents { toastFrame(width, height, insets, this.width, this.height) }
    label.setFrame(CGRectMake(frame.x, frame.y, frame.width, frame.height))
  }
}

/** The overlay stays above sheets without becoming key or intercepting touches. */
private class PassthroughWindow(scene: UIWindowScene) : UIWindow(windowScene = scene) {
  override fun hitTest(point: CValue<CGPoint>, withEvent: UIEvent?): UIView? = null
}
