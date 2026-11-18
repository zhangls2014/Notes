package me.zhangls.download

data class DownloadCheckpoint(val filename: String, val expectedBytes: Long, val savedBytes: Long)

private val checkpointName = Regex("""\.(.+)\.([1-9][0-9]*)\.([a-f0-9]{64})\.part(?:\.(full|[0-9]+))?""")

fun downloadCheckpointFilename(name: String): String? = checkpointName.matchEntire(name)?.groupValues?.get(1)

internal fun checkpointName(filename: String, size: Long, sha256: String): String =
  ".$filename.$size.$sha256.part"

/** Reads recoverable byte counts from the file sizes in one download directory. */
fun downloadCheckpoints(files: Map<String, Long>): List<DownloadCheckpoint> {
  val checkpoints = mutableMapOf<String, MutableMap<String, Long>>()
  for ((name, bytes) in files) {
    val match = checkpointName.matchEntire(name) ?: continue
    val suffix = match.groupValues[4]
    val base = if (suffix.isEmpty()) name else name.removeSuffix(".$suffix")
    checkpoints.getOrPut(base) { mutableMapOf() }[suffix] = bytes
  }
  return checkpoints.mapNotNull { (base, parts) ->
    val (filename, size) = checkNotNull(checkpointName.matchEntire(base)).destructured
    val expected = size.toLongOrNull() ?: return@mapNotNull null
    val prefix = checkpointOffset(parts[""] ?: 0, expected)
    val saved = if (parts["full"] == expected) expected else prefix + parts.entries.sumOf { (offset, bytes) ->
      val start = offset.toLongOrNull()
      if (start == null || start !in prefix until expected || start % FRAGMENT_BYTES != 0L ||
        offset != start.toString() || bytes != minOf(FRAGMENT_BYTES, expected - start)
      ) 0L else bytes
    }
    if (saved > 0) DownloadCheckpoint(filename, expected, saved) else null
  }
}

internal fun checkpointOffset(size: Long, expected: Long): Long = when {
  size == expected -> size
  size !in 0 until expected -> 0
  else -> size - size % FRAGMENT_BYTES
}
