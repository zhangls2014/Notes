@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package me.zhangls.about

import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import platform.Foundation.base64EncodedStringWithOptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WebAssetBridgeTest {
  @Test
  fun readsTheCompletedCacheFileForTheBridge() {
    val origin = "https://website.example.test"
    val url = "$origin/0123456789abcdef0123.wasm"
    val asset = checkNotNull(cachedWebAsset(origin, url))
    val bytes = byteArrayOf(9, 8, 7, 6)
    val path = Path(checkNotNull(IosWebAssetDisk().path(asset)))
    SystemFileSystem.createDirectories(checkNotNull(path.parent))
    SystemFileSystem.sink(path).buffered().use { it.write(bytes) }

    val afterRestart = IosWebAssetDisk()
    val listed = listCacheDirectory("$WEB_ASSET_CACHE_DIRECTORY/${asset.directory}")
    assertEquals(bytes.size.toLong(), listed.single { it.name == asset.filename }.bytes)
    assertEquals("CQgHBg==", afterRestart.data(asset)?.base64EncodedStringWithOptions(0UL))
    val wrongVersion = asset.copy(filename = "abcdef0123456789abcd.wasm")
    assertNull(afterRestart.data(wrongVersion))
  }
}
