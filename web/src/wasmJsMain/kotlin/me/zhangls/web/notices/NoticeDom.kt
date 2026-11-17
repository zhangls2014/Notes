@file:OptIn(ExperimentalWasmJsInterop::class)

package me.zhangls.web.notices

import kotlin.js.ExperimentalWasmJsInterop

// Preserve an anchor navigation event for installed WebView delegates that distinguish link clicks.
internal fun openNoticeLink(url: String): Unit = js("""{
  const link = document.createElement('a');
  link.href = url;
  link.hidden = true;
  document.body.append(link);
  link.click();
  link.remove();
}""")

internal fun readNoticePosition(): String? = js("history.state?.notesNoticePosition ?? null")

internal fun saveNoticePosition(index: Int, offset: Int): Unit = js("""{
  history.replaceState({...history.state, notesNoticePosition: index + ':' + offset}, '');
}""")
