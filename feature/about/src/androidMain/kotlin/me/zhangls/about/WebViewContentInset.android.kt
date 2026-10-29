package me.zhangls.about

import android.webkit.WebView

/** Add scrollable clearance without reducing the WebView's drawing area. */
internal fun WebView.applyBottomContentInset(bottom: Float) {
  evaluateJavascript(
    """
    (() => {
      const root = document.documentElement;
      if (!root) return;
      root.dataset.notesBottomPadding ??= getComputedStyle(root).paddingBottom;
      root.style.paddingBottom = `calc(${ '$' }{root.dataset.notesBottomPadding} + ${bottom}px)`;
    })();
    """.trimIndent(),
    null,
  )
}
