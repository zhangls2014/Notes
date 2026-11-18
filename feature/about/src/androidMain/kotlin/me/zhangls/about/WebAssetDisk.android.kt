package me.zhangls.about

import android.content.Context
import android.webkit.WebResourceResponse
import org.koin.mp.KoinPlatform
import java.io.File
import java.io.FileNotFoundException

internal actual fun webAssetDownloadPath(asset: CachedWebAsset): String? =
  KoinPlatform.getKoinOrNull()?.getOrNull<Context>()?.let { AndroidWebAssetDisk(it).file(asset).absolutePath }

internal class AndroidWebAssetDisk(context: Context) {
  private val root = File(context.cacheDir, WEB_ASSET_CACHE_DIRECTORY)

  fun file(asset: CachedWebAsset): File = File(File(root, asset.directory), asset.filename)

  fun response(asset: CachedWebAsset): WebResourceResponse? = try {
    WebResourceResponse(asset.mimeType, null, file(asset).inputStream())
  } catch (_: FileNotFoundException) {
    null
  }
}
