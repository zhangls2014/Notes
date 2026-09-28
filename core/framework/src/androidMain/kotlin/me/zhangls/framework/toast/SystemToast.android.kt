package me.zhangls.framework.toast

import android.content.Context
import android.widget.Toast
import org.koin.core.annotation.Singleton

@Singleton(binds = [SystemToast::class])
class AndroidSystemToast(context: Context) : SystemToast {
  private val applicationContext = context.applicationContext

  override fun show(message: String, longDuration: Boolean) {
    Toast.makeText(
      applicationContext,
      message,
      if (longDuration) Toast.LENGTH_LONG else Toast.LENGTH_SHORT,
    ).show()
  }
}
