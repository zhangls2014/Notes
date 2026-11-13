package me.zhangls.entry

import android.util.Log

internal actual fun logAuthFailure(message: String) {
  Log.w("NotesAuth", message)
}
