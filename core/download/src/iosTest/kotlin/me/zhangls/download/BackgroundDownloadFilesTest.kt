package me.zhangls.download

import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.SystemTemporaryDirectory
import kotlinx.io.readByteArray
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BackgroundDownloadFilesTest {
  @Test
  fun queuesOneFragmentPerFileBeforeQueueingTheirNextFragments() {
    val files = List(3) { index ->
      DownloadRequest("https://download.example.test/$index", "/$index", 3 * FRAGMENT_BYTES, "0".repeat(64))
    }
    val rounds = interleaveRanges(files.map { file ->
      listOf(RangeTask(file, 0), RangeTask(file, FRAGMENT_BYTES))
    })
    assertEquals(files + files, rounds.map { it.request })
    assertEquals(List(3) { 0L } + List(3) { FRAGMENT_BYTES }, rounds.map { it.start })
  }

  @Test
  fun publishesOutOfOrderBackgroundFragmentsWithAnExistingCheckpoint() {
    val root = Path(SystemTemporaryDirectory, "notes-background-${Random.nextLong()}")
    SystemFileSystem.createDirectories(root)
    val destination = Path(root, "resource")
    val payload = ByteArray(2 * FRAGMENT_BYTES.toInt() + 5) { (it % 251).toByte() }
    val fixture = Path(root, "fixture")
    fun write(path: Path, start: Int, end: Int) =
      SystemFileSystem.sink(path).buffered().use { it.write(payload, start, end) }
    try {
      write(fixture, 0, payload.size)
      val request = DownloadRequest(
        "https://download.example.test/resource",
        destination.toString(),
        payload.size.toLong(),
        downloadSha256(fixture.toString()),
      )
      val files = DownloadFiles(request)
      val unit = FRAGMENT_BYTES.toInt()
      write(files.partial, 0, unit)
      val parts = files.ranges()
      write(files.fragment(parts.last().start), 2 * unit, payload.size)
      assertFalse(files.contains(parts[1], files.prefixSize()))
      assertTrue(files.contains(parts[0], files.prefixSize()))
      write(files.fragment(parts[1].start), unit, 2 * unit)

      DownloadFiles(request).publish(fromFullResponse = false)

      assertContentEquals(payload, SystemFileSystem.source(destination).buffered().use { it.readByteArray() })
      assertFalse(SystemFileSystem.exists(files.partial))
      assertFalse(SystemFileSystem.exists(files.fragment(parts.last().start)))
    } finally {
      SystemFileSystem.list(root).forEach { SystemFileSystem.delete(it, mustExist = false) }
      SystemFileSystem.delete(root)
    }
  }
}
