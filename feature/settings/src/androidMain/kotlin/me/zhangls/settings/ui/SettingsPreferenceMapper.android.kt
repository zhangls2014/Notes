package me.zhangls.settings.ui

import android.annotation.SuppressLint
import android.os.Build

internal actual val supportsDynamicColor: Boolean
  @SuppressLint("ObsoleteSdkInt")
  get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1
