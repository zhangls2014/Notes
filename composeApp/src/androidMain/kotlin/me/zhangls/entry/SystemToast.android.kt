package me.zhangls.entry

import androidx.compose.runtime.Composable
import me.zhangls.framework.toast.SystemToast
import org.koin.compose.koinInject

@Composable
internal actual fun rememberSystemToast(): SystemToast = koinInject()
