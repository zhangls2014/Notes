package me.zhangls.notes

import android.app.WallpaperManager
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.json.JSONException
import org.json.JSONObject

private const val THEME_SETTING = "theme_customization_overlay_packages"

internal data class ThemeColorSource(val source: String, val wallpaperColor: Color?)

internal fun readThemeColorSource(sdk: Int, readSetting: () -> String?): String {
  if (sdk < Build.VERSION_CODES.S) return "home_wallpaper"
  return try {
    readSetting()?.let { JSONObject(it).optString("android.theme.customization.color_source", "home_wallpaper") }
      ?: "home_wallpaper"
  } catch (_: SecurityException) {
    "home_wallpaper"
  } catch (_: JSONException) {
    "home_wallpaper"
  }
}

internal fun readThemeWallpaperColor(source: String, readColor: (Int) -> Int?): Int? {
  fun color(which: Int): Int? = try {
    readColor(which)
  } catch (_: SecurityException) {
    null
  }
  return if (source == "lock_wallpaper") {
    color(WallpaperManager.FLAG_LOCK) ?: color(WallpaperManager.FLAG_SYSTEM)
  } else {
    color(WallpaperManager.FLAG_SYSTEM)
  }
}

@Composable
internal fun rememberThemeColorSource(ioDispatcher: CoroutineDispatcher = Dispatchers.IO): State<ThemeColorSource> {
  val context = LocalContext.current.applicationContext
  val lifecycle = LocalLifecycleOwner.current.lifecycle
  return produceState(ThemeColorSource("home_wallpaper", null), context, lifecycle, ioDispatcher) {
    val manager = WallpaperManager.getInstance(context)
    lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
      callbackFlow {
        val listener = WallpaperManager.OnColorsChangedListener { _, which ->
          if (which and (WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK) != 0) trySend(Unit)
        }
        manager.addOnColorsChangedListener(listener, Handler(Looper.getMainLooper()))
        val resolver = context.contentResolver
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
          override fun onChange(selfChange: Boolean) {
            trySend(Unit)
          }
        }
        var observing = false
        try {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
              resolver.registerContentObserver(Settings.Secure.getUriFor(THEME_SETTING), false, observer)
              observing = true
            } catch (_: SecurityException) {
              // Some ROMs restrict access; returning to the foreground still refreshes the source.
            }
          }
          trySend(Unit)
          awaitClose { }
        } finally {
          manager.removeOnColorsChangedListener(listener)
          if (observing) resolver.unregisterContentObserver(observer)
        }
      }
        .buffer(Channel.CONFLATED)
        .map {
          val source = readThemeColorSource(Build.VERSION.SDK_INT) {
            Settings.Secure.getString(context.contentResolver, THEME_SETTING)
          }
          val color = if (source == "preset") {
            null
          } else {
            readThemeWallpaperColor(source) { which ->
              manager.getWallpaperColors(which)?.primaryColor?.toArgb()
            }
          }
          ThemeColorSource(source, color?.let(::Color))
        }
        .flowOn(ioDispatcher)
        .collect { selection ->
          value = selection.copy(wallpaperColor = selection.wallpaperColor ?: value.wallpaperColor)
        }
    }
  }
}
