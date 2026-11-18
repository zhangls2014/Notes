package me.zhangls.about

import android.content.Context
import android.util.AtomicFile
import android.webkit.WebResourceResponse
import org.koin.mp.KoinPlatform
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.security.MessageDigest

internal actual fun webAssetDisk(): WebAssetDisk? =
  KoinPlatform.getKoinOrNull()?.getOrNull<Context>()?.let(::AndroidWebAssetDisk)

internal actual fun webAssetSha256(bytes: ByteArray): String =
  MessageDigest.getInstance("SHA-256").digest(bytes).toHexString()

internal class AndroidWebAssetDisk(context: Context) : WebAssetDisk {
  private val root = File(context.cacheDir, "notes-web-assets")

  private fun file(asset: CachedWebAsset): File = File(File(root, asset.directory), asset.filename)

  override fun read(asset: CachedWebAsset): ByteArray? {
    val file = file(asset)
    return if (file.isFile) file.readBytes() else null
  }

  override fun write(asset: CachedWebAsset, bytes: ByteArray) {
    val file = file(asset)
    file.parentFile?.mkdirs()
    val atomicFile = AtomicFile(file)
    val output = atomicFile.startWrite()
    try {
      output.write(bytes)
      atomicFile.finishWrite(output)
    } catch (failure: IOException) {
      atomicFile.failWrite(output)
      throw failure
    }
  }

  fun response(asset: CachedWebAsset): WebResourceResponse? = try {
    WebResourceResponse(asset.mimeType, null, file(asset).inputStream())
  } catch (_: FileNotFoundException) {
    null
  }
}
