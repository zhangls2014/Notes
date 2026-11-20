package me.zhangls.about

import io.ktor.http.URLProtocol
import io.ktor.http.Url
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

internal class DeviceViewportPublisher(initialUrl: String) {
  private val origin = Url(initialUrl).let { "${it.protocol.name}://${it.host}:${it.port}" }
  private var sequence = 0L
  private var lastGeometry: String? = null
  private var documentId: String? = null

  fun trusted(target: String): Boolean = runCatching {
    val url = Url(target)
    val noCredentials = url.user == null && url.password == null
    url.protocol == URLProtocol.HTTPS && "${url.protocol.name}://${url.host}:${url.port}" == origin && noCredentials
  }.getOrDefault(false)

  fun documentScript(): String = """
    (() => {
      const trustedOrigin = new URL(${JsonPrimitive(origin)}).origin;
      if (window.top !== window || location.origin !== trustedOrigin) return null;
      return window.deviceViewport?.documentId ?? null;
    })();
  """.trimIndent()

  fun bindDocument(id: String) {
    if (id.isBlank()) return
    documentId = id
    reset()
  }

  fun reset() {
    lastGeometry = null
  }

  fun unbind() {
    documentId = null
    reset()
  }

  fun publish(
    target: String,
    features: String,
    availability: String = "available",
    geometryKey: String = "",
  ): String? {
    val id = documentId
    val fingerprint = "$availability:$features:$geometryKey"
    if (id == null || !trusted(target) || lastGeometry == fingerprint) return null
    lastGeometry = fingerprint
    val packet = buildJsonObject {
      put("protocol", "device-viewport")
      put("version", 1)
      put("type", "snapshot")
      put("sequence", ++sequence)
      put("coordinateSpace", "webview-normalized")
      put("availability", availability)
      put("features", Json.parseToJsonElement(features))
    }
    return """
      requestAnimationFrame(() => {
        if (window.top !== window || location.origin !== new URL(${JsonPrimitive(origin)}).origin) return;
        const bridge = window.deviceViewport;
        if (!bridge || bridge.documentId !== ${JsonPrimitive(id)}) return;
        document.documentElement.dataset.hostPlatform = ${JsonPrimitive(releasePlatform)};
        bridge.receive({...$packet,documentId:bridge.documentId});
      });
    """.trimIndent()
  }

  fun detachScript(): String? = documentId?.let { id ->
    "window.deviceViewport?.documentId === ${JsonPrimitive(id)} && window.deviceViewport.detach(${JsonPrimitive(id)});"
  }
}

internal fun viewportDocumentId(result: String?): String? = result?.let {
  runCatching { Json.parseToJsonElement(it).jsonPrimitive.contentOrNull }.getOrNull()
}
