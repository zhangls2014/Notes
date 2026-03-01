package me.zhangls.framework.deeplink

import android.net.Uri

/**
 * @author zhangls
 */
class DeepLinkRequest(val uri: Uri) {
  val scheme: String? = uri.scheme
  val host: String? = uri.host
  val path: String? = uri.path
  val pathSegments: List<String> = uri.pathSegments
  val queries: Map<String, String> = uri.queryParameterNames.associateWith { uri.getQueryParameter(it) ?: "" }
}