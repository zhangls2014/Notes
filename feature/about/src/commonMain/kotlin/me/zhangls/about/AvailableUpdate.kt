package me.zhangls.about

import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.zhangls.framework.AppInfo
import me.zhangls.network.httpClient

internal const val NOTES_SITE = "https://notes.zhangls.me"
private const val IOS_CHANNEL = "ios-app-store"
private const val UPDATE_TIMEOUT_MILLIS = 10_000L
private val androidChannels = setOf("android-prd-full", "android-prd-mainland")
private val versionPattern = Regex("""(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)""")
private val buildPattern = Regex("""[1-9]\d*(\.\d+){0,2}""")
private val appStoreUrl = Regex("""https://apps\.apple\.com/(?:[a-z]{2}/)?app/(?:[\w-]+/)?id\d+""")
private val updateJson = Json { ignoreUnknownKeys = true }

@Serializable
internal data class UpdateManifest(val channel: String, val version: String, val build: String, val updateUrl: String)

internal suspend fun checkAvailableUpdate(appInfo: AppInfo, channel: String): UpdateManifest? {
  require(channel == IOS_CHANNEL || channel in androidChannels)
  return httpClient {
    followRedirects = false
    install(HttpTimeout) { requestTimeoutMillis = UPDATE_TIMEOUT_MILLIS }
  }.use { client ->
    val response = client.get("$NOTES_SITE/updates/v1/$channel.json")
    check(response.status.isSuccess())
    availableUpdate(
      updateJson.decodeFromString<UpdateManifest>(response.bodyAsText()),
      channel,
      appInfo.getVersionName(),
      requireNotNull(appInfo.getBuildNumber()),
    )
  }
}

internal fun availableUpdate(
  manifest: UpdateManifest,
  channel: String,
  currentVersion: String,
  currentBuild: String,
): UpdateManifest? {
  require(manifest.channel == channel && versionPattern.matches(manifest.version))
  val newer = when (channel) {
    in androidChannels -> {
      val expectedUrl = "$NOTES_SITE/downloads/android/$channel/${manifest.build}/"
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

internal fun AppInfo.onlineNoticesPath(): String? {
  val channel = getUpdateChannel() ?: return null
  val version = getVersionName()
  val build = getBuildNumber()
  return if (build != null && versionPattern.matches(version) && buildPattern.matches(build)) {
    "/open-source/$channel/$version/$build/$releasePlatform.html"
  } else {
    null
  }
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
