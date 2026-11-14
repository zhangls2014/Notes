package me.zhangls.about

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.io.IOException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.zhangls.about.api.NotesWebsite
import me.zhangls.framework.AppInfo

internal const val MAINLAND_UPDATE_CHANNEL = "android-prd-mainland"
internal const val GLOBAL_UPDATE_CHANNEL = "android-prd-global"
internal val androidUpdateChannels = setOf(GLOBAL_UPDATE_CHANNEL, MAINLAND_UPDATE_CHANNEL)

internal fun publishedAndroidChannel(region: String?): String? = when (region) {
  "mainland", MAINLAND_UPDATE_CHANNEL -> MAINLAND_UPDATE_CHANNEL
  "global", GLOBAL_UPDATE_CHANNEL -> GLOBAL_UPDATE_CHANNEL
  else -> null
}

private val versionPattern = Regex("""(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)""")
private val buildPattern = Regex("""[1-9]\d*""")
private val updateJson = Json { ignoreUnknownKeys = true }

@Serializable
internal data class UpdateManifest(val channel: String, val version: String, val build: String, val updateUrl: String)

internal enum class UpdateFailure { Transport, Http, Manifest }

internal class UpdateCheckException(val reason: UpdateFailure, val status: Int? = null) :
  Exception("Update check failed: $reason")

internal suspend fun checkAvailableUpdate(
  client: HttpClient,
  appInfo: AppInfo,
  channel: String,
  site: NotesWebsite,
): UpdateManifest? {
  require(channel in androidUpdateChannels)
  val body = fetchManifest(client, "${site.origin}/updates/v1/$channel.json")
  return try {
    availableUpdate(
      updateJson.decodeFromString<UpdateManifest>(body),
      channel,
      requireNotNull(appInfo.getBuildNumber()),
      site,
    )
  } catch (_: IllegalArgumentException) {
    throw UpdateCheckException(UpdateFailure.Manifest)
  }
}

private suspend fun fetchManifest(client: HttpClient, url: String): String = try {
  val response = client.get(url) { header(HttpHeaders.CacheControl, "no-cache") }
  if (!response.status.isSuccess()) throw UpdateCheckException(UpdateFailure.Http, response.status.value)
  response.bodyAsText()
} catch (_: IOException) {
  throw UpdateCheckException(UpdateFailure.Transport)
}

internal fun availableUpdate(
  manifest: UpdateManifest,
  channel: String,
  currentBuild: String,
  site: NotesWebsite,
): UpdateManifest? {
  require(channel in androidUpdateChannels && manifest.channel == channel)
  require(versionPattern.matches(manifest.version))
  require(buildPattern.matches(manifest.build) && buildPattern.matches(currentBuild))
  val expectedUrl = if (channel == MAINLAND_UPDATE_CHANNEL) {
    "${site.origin}/downloads/android/$channel/${manifest.build}/"
  } else {
    "${site.origin}/releases/#v${manifest.version}"
  }
  require(manifest.updateUrl == expectedUrl)
  return manifest.takeIf { requireNotNull(it.build.toIntOrNull()) > requireNotNull(currentBuild.toIntOrNull()) }
}

internal suspend fun loadPublishedUpdateNotes(client: HttpClient, site: NotesWebsite, version: String): AppRelease? =
  try {
    val response = client.get("${site.origin}/releases/published.json")
    if (response.status.isSuccess()) {
      parseReleases(response.bodyAsText()).forPlatform("android").find { it.version == version && it.date != null }
    } else {
      null
    }
  } catch (_: IOException) {
    null
  } catch (_: IllegalArgumentException) {
    null
  }
