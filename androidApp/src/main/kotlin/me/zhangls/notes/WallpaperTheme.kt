package me.zhangls.notes

import android.app.WallpaperManager
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

@Composable
internal fun rememberWallpaperColor(): State<Color?> {
  val context = LocalContext.current.applicationContext
  val lifecycle = LocalLifecycleOwner.current.lifecycle
  return produceState(null, context, lifecycle) {
    val manager = WallpaperManager.getInstance(context)
    lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
      callbackFlow {
        val listener = WallpaperManager.OnColorsChangedListener { _, which ->
          if (which and WallpaperManager.FLAG_SYSTEM != 0) trySend(Unit)
        }
        manager.addOnColorsChangedListener(listener, Handler(Looper.getMainLooper()))
        trySend(Unit)
        awaitClose { manager.removeOnColorsChangedListener(listener) }
      }
        .buffer(Channel.CONFLATED)
        .map {
          try {
            manager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)?.primaryColor?.toArgb()?.let(::Color)
          } catch (_: SecurityException) {
            null
          }
        }
        .flowOn(Dispatchers.IO)
        .collect { color -> if (color != null) value = color }
    }
  }
}
