package me.zhangls.framework.toast

import android.content.Context
import android.widget.Toast
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

actual fun showSystemToast(message: String, longDuration: Boolean) {
  Toast.makeText(
    ToastContextProvider.context,
    message,
    if (longDuration) Toast.LENGTH_LONG else Toast.LENGTH_SHORT,
  ).show()
}

private object ToastContextProvider : KoinComponent {
  val context: Context by inject()
}
