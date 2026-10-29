package me.zhangls.about

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import notes.feature.about.generated.resources.Res
import org.jetbrains.compose.resources.MissingResourceException

@Serializable
internal data class AppRelease(val version: String, val date: String?, val changes: List<ReleaseChange>)

@Serializable
internal data class ReleaseChange(val type: String, val platforms: List<String>, val text: Map<String, String>) {
  fun localizedText(language: String): String = text.getValue(if (language == "zh") "zh" else "en")
}

internal expect val releasePlatform: String

private val versionPattern = Regex("(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)")
private val datePattern = Regex("[0-9]{4}-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])")
private val versionOrder = compareBy<AppRelease> { it.version.split('.')[0].toInt() }
  .thenBy { it.version.split('.')[1].toInt() }
  .thenBy { it.version.split('.')[2].toInt() }

internal fun parseReleases(content: String): List<AppRelease> =
  Json.decodeFromString<List<AppRelease>>(content).also(::validateReleases)

internal fun validateReleases(releases: List<AppRelease>) {
  require(releases.map { it.version }.distinct().size == releases.size) { "Duplicate release version" }
  require(releases.count { it.date == null } <= 1) { "Multiple unreleased versions" }
  releases.forEach { release ->
    require(versionPattern.matches(release.version) && release.version.split('.').all { it.toIntOrNull() != null })
    require(release.date == null || datePattern.matches(release.date))
    require(release.changes.isNotEmpty())
    release.changes.forEach { change ->
      require(change.type in setOf("added", "improved", "fixed"))
      require(change.platforms.isNotEmpty() && change.platforms.distinct().size == change.platforms.size)
      require(change.platforms.all { it in setOf("android", "ios") })
      require(!change.text["en"].isNullOrBlank() && !change.text["zh"].isNullOrBlank())
    }
  }
  val newest = releases.maxWithOrNull(versionOrder)
  require(releases.none { it.date == null && it != newest }) { "Unreleased version must be newest" }
}

internal fun List<AppRelease>.forPlatform(platform: String): List<AppRelease> =
  map { release -> release.copy(changes = release.changes.filter { platform in it.platforms }) }
    .filter { it.changes.isNotEmpty() }
    .sortedWith(versionOrder.reversed())

@Composable
internal fun rememberReleaseHistory(): State<Result<List<AppRelease>>?> =
  produceState<Result<List<AppRelease>>?>(null) {
    value = loadReleaseHistory()
  }

internal suspend fun loadReleaseHistory(): Result<List<AppRelease>> = try {
  Result.success(parseReleases(Res.readBytes("files/releases.json").decodeToString()).forPlatform(releasePlatform))
} catch (exception: MissingResourceException) {
  Result.failure(exception)
} catch (exception: IllegalArgumentException) {
  Result.failure(exception)
}
