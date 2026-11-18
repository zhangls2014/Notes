@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package me.zhangls.about

import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSData
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.dataWithContentsOfFile

internal actual fun webAssetDownloadPath(asset: CachedWebAsset): String? = IosWebAssetDisk().path(asset)

internal class IosWebAssetDisk {
  fun path(asset: CachedWebAsset): String? =
    NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
      .filterIsInstance<String>().firstOrNull()
      ?.let { "$it/$WEB_ASSET_CACHE_DIRECTORY/${asset.directory}/${asset.filename}" }

  fun data(asset: CachedWebAsset): NSData? = path(asset)?.let { NSData.dataWithContentsOfFile(it) }
}
