package me.zhangls.entry

import platform.Foundation.NSLog

internal actual fun logAuthFailure(message: String) {
  NSLog("%@", "NotesAuth: $message")
}
