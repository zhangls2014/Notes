package me.zhangls.about

import kotlinx.coroutines.test.runTest
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.SystemTemporaryDirectory
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WebAssetManifestDiskTest {
  @Test
  fun pruningIsLimitedToManagedObsoleteFilesInTheSelectedSite() = runTest {
    val root = Path(SystemTemporaryDirectory, "notes-manifest-test-${Random.nextLong()}")
    val site = Path(root, "one.example.test-443")
    val other = Path(root, "two.example.test-443")
    SystemFileSystem.createDirectories(site)
    SystemFileSystem.createDirectories(other)
    val current = "0123456789abcdef0123.wasm"
    val old = "abcdef0123456789abcd.wasm"
    val font = "misans-${"a".repeat(20)}.otf"
    val manifest = WebAssetManifest(
      WebAsset("/$current", 1, "b".repeat(64)),
      WebAsset("/web-assets/$font", 1, "a".repeat(64)),
    )
    val currentPart = ".$current.1.${"b".repeat(64)}.part"
    val oldPart = ".$old.1.${"c".repeat(64)}.part.full"
    fun write(directory: Path, name: String) {
      SystemFileSystem.sink(Path(directory, name)).buffered().use { it.write(byteArrayOf(1)) }
    }
    try {
      for (name in listOf(current, old, font, currentPart, oldPart, "unrelated.txt", "unknown.wasm")) write(site, name)
      write(other, old)
      val nested = Path(site, "11111111111111111111.wasm")
      SystemFileSystem.createDirectories(nested)
      write(nested, old)
      val disk = WebAssetManifestDisk(Path(site, ".manifest-v2.json"))
      disk.write(manifest)
      assertEquals(manifest, disk.read())
      assertEquals(WebAssetKind.Skiko, disk.read()?.kind(current))
      assertEquals(WebAssetKind.MiSans, disk.read()?.kind(font))
      assertNull(disk.read()?.kind(old))
      assertTrue(disk.prune(manifest) { false })
      assertFalse(SystemFileSystem.exists(Path(site, old)))
      assertFalse(SystemFileSystem.exists(Path(site, oldPart)))
      for (name in listOf(current, font, currentPart, "unrelated.txt", "unknown.wasm")) {
        assertTrue(SystemFileSystem.exists(Path(site, name)), name)
      }
      assertTrue(SystemFileSystem.exists(Path(other, old)))
      assertTrue(SystemFileSystem.exists(Path(nested, old)))
      SystemFileSystem.sink(Path(site, ".manifest-v2.json")).buffered().use { it.write(byteArrayOf(0)) }
      assertNull(disk.read())
    } finally {
      deleteTestTree(root)
    }
  }
}

private fun deleteTestTree(path: Path) {
  if (SystemFileSystem.metadataOrNull(path)?.isDirectory == true) SystemFileSystem.list(path).forEach(::deleteTestTree)
  SystemFileSystem.delete(path, mustExist = false)
}
