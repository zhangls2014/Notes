@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package me.zhangls.about

import io.ktor.http.Url
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.Foundation.base64EncodedStringWithOptions
import platform.WebKit.WKContentWorld
import platform.WebKit.WKScriptMessage
import platform.WebKit.WKScriptMessageHandlerWithReplyProtocol
import platform.WebKit.WKUserContentController
import platform.WebKit.WKUserScript
import platform.WebKit.WKUserScriptInjectionTime
import platform.darwin.NSObject

private const val HANDLER_NAME = "notesWebAssets"

internal class WebAssetBridge(siteUrl: String) : NSObject(), WKScriptMessageHandlerWithReplyProtocol {
  private val origin = Url(siteUrl)
  private val disk = IosWebAssetDisk()
  private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

  fun install(controller: WKUserContentController) {
    controller.addScriptMessageHandlerWithReply(this, WKContentWorld.pageWorld, HANDLER_NAME)
    controller.addUserScript(
      WKUserScript(
        source = webAssetFetchScript,
        injectionTime = WKUserScriptInjectionTime.WKUserScriptInjectionTimeAtDocumentStart,
        forMainFrameOnly = true,
      ),
    )
  }

  override fun userContentController(
    userContentController: WKUserContentController,
    didReceiveScriptMessage: WKScriptMessage,
    replyHandler: (Any?, String?) -> Unit,
  ) {
    val message = didReceiveScriptMessage
    val frame = message.frameInfo
    val securityOrigin = frame.securityOrigin
    val port = securityOrigin.port.toInt().takeUnless { it == 0 } ?: 443
    val asset = (message.body as? String)?.let { cachedWebAsset(origin.toString(), it) }
    val sameOrigin = securityOrigin.protocol == "https" &&
      securityOrigin.host.equals(origin.host, ignoreCase = true) && port == origin.port
    if (!frame.isMainFrame() || !sameOrigin || asset == null) {
      replyHandler(null, null)
      return
    }
    scope.launch {
      val contents = withContext(Dispatchers.Default) {
        runCatching { disk.data(asset)?.base64EncodedStringWithOptions(0UL) }.getOrNull()
      }
      replyHandler(contents, null)
    }
  }

  fun close(controller: WKUserContentController) {
    controller.removeScriptMessageHandlerForName(HANDLER_NAME, WKContentWorld.pageWorld)
    scope.cancel()
  }
}

private val webAssetFetchScript = """
  (() => {
    const fetchOnline = window.fetch.bind(window);
    const handler = window.webkit.messageHandlers.$HANDLER_NAME;
    window.fetch = async (input, options) => {
      const request = input instanceof Request ? input : null;
      if ((options?.method ?? request?.method ?? "GET").toUpperCase() !== "GET") {
        return fetchOnline(input, options);
      }
      const url = new URL(request?.url ?? input, document.baseURI);
      const wasm = /^\/[a-f0-9]{20}\.wasm$/.test(url.pathname);
      const font = /^\/web-assets\/misans-[a-f0-9]{64}\.otf$/.test(url.pathname);
      const signal = options?.signal ?? request?.signal;
      if (url.origin === location.origin && !url.search && !url.hash && !signal?.aborted && (wasm || font)) {
        try {
          const data = await handler.postMessage(url.href);
          if (data && !signal?.aborted) {
            const binary = atob(data);
            const bytes = new Uint8Array(binary.length);
            for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
            const mime = wasm ? "application/wasm" : "font/otf";
            return new Response(bytes, { headers: { "Content-Type": mime } });
          }
        } catch (_) {
          // A cache read failure must fall back to the original HTTPS request.
        }
      }
      return fetchOnline(input, options);
    };
  })();
""".trimIndent()
