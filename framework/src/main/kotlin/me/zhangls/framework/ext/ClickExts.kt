package me.zhangls.framework.ext

/**
 * 点击事件防抖
 *
 * @author zhangls
 */
fun (() -> Unit).withDebounce(
  debounceTime: Long = 800L
): () -> Unit {
  var lastClickTime = 0L
  return {
    val now = System.currentTimeMillis()
    if (now - lastClickTime > debounceTime) {
      lastClickTime = now
      this()
    }
  }
}