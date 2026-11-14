package me.zhangls.about

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.io.IOException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.zhangls.about.api.NotesWebsite
import me.zhangls.framework.AppInfo

private const val IOS_CHANNEL = "ios-app-store"
private val androidChannels = setOf("android-prd-full", "android-prd-mainland")
private val versionPattern = Regex("""(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)""")
private val buildPattern = Regex("""[1-9]\d*(\.\d+){0,2}""")
private val appStoreUrl = Regex("""https://apps\.apple\.com/(?:[a-z]{2}/)?app/(?:[\w-]+/)?id\d+""")
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
  require(channel == IOS_CHANNEL || channel in androidChannels)
  val body = try {
    val response = client.get("${site.origin}/updates/v1/$channel.json")
    if (!response.status.isSuccess()) throw UpdateCheckException(UpdateFailure.Http, response.status.value)
    response.bodyAsText()
  } catch (_: IOException) {
    throw UpdateCheckException(UpdateFailure.Transport)
  }
  return try {
    availableUpdate(
      updateJson.decodeFromString<UpdateManifest>(body),
      channel,
      appInfo.getVersionName(),
      requireNotNull(appInfo.getBuildNumber()),
      site,
    )
  } catch (_: IllegalArgumentException) {
    throw UpdateCheckException(UpdateFailure.Manifest)
  }
}

internal fun availableUpdate(
  manifest: UpdateManifest,
  channel: String,
  currentVersion: String,
  currentBuild: String,
  site: NotesWebsite,
): UpdateManifest? {
  require(manifest.channel == channel && versionPattern.matches(manifest.version))
  val newer = when (channel) {
    in androidChannels -> {
      val expectedUrl = "${site.origin}/downloads/android/$channel/${manifest.build}/"
      require(manifest.updateUrl == expectedUrl)
      require('.' !in manifest.build && '.' !in currentBuild)
      compareBuilds(manifest.build, currentBuild) > 0
    }

    IOS_CHANNEL -> {
      require(appStoreUrl.matches(manifest.updateUrl) && versionPattern.matches(currentVersion))
      val buildOrder = compareBuilds(manifest.build, currentBuild)
      val versionOrder = compareNumbers(manifest.version, currentVersion)
      versionOrder > 0 || (versionOrder == 0 && buildOrder > 0)
    }

    else -> error("Unknown release channel")
  }
  return manifest.takeIf { newer }
}

private fun compareBuilds(left: String, right: String): Int {
  require(buildPattern.matches(left) && buildPattern.matches(right))
  return compareNumbers(left, right)
}

private fun compareNumbers(left: String, right: String): Int {
  val a = left.split('.').map { requireNotNull(it.toIntOrNull()) }
  val b = right.split('.').map { requireNotNull(it.toIntOrNull()) }
  for (index in 0 until maxOf(a.size, b.size)) {
    val order = a.getOrElse(index) { 0 }.compareTo(b.getOrElse(index) { 0 })
    if (order != 0) return order
  }
  return 0
}
