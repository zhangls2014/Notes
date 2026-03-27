package me.zhangls.entry.util

import android.content.Context

/**
 * @author zhangls
 */
fun Context.packageInfo() = packageManager.getPackageInfo(packageName, 0)

fun Context.appVersionCode() = packageInfo().longVersionCode