@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package me.zhangls.about

import platform.Foundation.base64EncodedStringWithOptions
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WebAssetBridgeTest {
  @Test
  fun readsAnAtomicallyStoredAssetForTheBridge() {
    val origin = "https://website.example.test"
    val url = "$origin/0123456789abcdef0123.wasm"
    val asset = checkNotNull(cachedWebAsset(origin, url))
    val bytes = byteArrayOf(9, 8, 7, 6)
    IosWebAssetDisk().write(asset, bytes)

    val afterRestart = IosWebAssetDisk()
    assertContentEquals(bytes, afterRestart.read(asset))
    assertEquals("CQgHBg==", afterRestart.data(asset)?.base64EncodedStringWithOptions(0UL))
    val wrongVersion = asset.copy(filename = "abcdef0123456789abcd.wasm")
    assertNull(afterRestart.data(wrongVersion))
  }
}
