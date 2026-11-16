@file:OptIn(ExperimentalWasmJsInterop::class)

package me.zhangls.web

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsString
import kotlin.js.Promise
import kotlinx.coroutines.await

private fun fetchReleaseJson(): Promise<JsString?> = js("""fetch('/releases/published.json', {cache: 'no-cache'})
  .then(response => response.ok ? response.text() : null)
  .catch(() => null)""")

internal suspend fun releaseJson(): String =
  checkNotNull(fetchReleaseJson().await<JsString?>()) { "Could not fetch release history" }.toString()

internal fun siteData(): String = js("document.getElementById('notes-data').textContent")
internal fun path(): String = js("location.pathname")
internal fun fragment(): String = js("location.hash")
internal fun parameter(name: String): String? = js("new URLSearchParams(location.search).get(name)")
internal fun browserLanguage(): String = js("navigator.language")
internal fun browserDark(): Boolean = js("matchMedia('(prefers-color-scheme: dark)').matches")
internal fun browserPlatform(): String =
  js("/iPhone|iPad|iPod/.test(navigator.userAgent) || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1) ? 'ios' : 'android'")

internal fun preference(name: String): String? =
  js("(() => { try { return localStorage.getItem('notes.' + name) } catch { return null } })()")

internal fun storePreference(name: String, value: String): Unit =
  js("(() => { try { localStorage.setItem('notes.' + name, value) } catch {} })()")

internal fun displayLanguage(language: String): Unit = js("{ document.documentElement.lang = language; }")
internal fun openPage(url: String): Unit = js("{ location.assign(url); }")
internal fun pushPage(url: String): Unit = js("""{
  const target = new URL(url, location.href);
  const current = new URLSearchParams(location.search);
  for (const [name, value] of current) {
    if (name !== 'platform') target.searchParams.set(name, value);
  }
  history.pushState(null, '', target);
}""")

internal fun clearParameter(name: String): Unit = js("""{
  const url = new URL(location.href);
  url.searchParams.delete(name);
  history.replaceState(null, '', url);
}""")
internal fun onHistoryChange(callback: () -> Unit): Unit =
  js("{ addEventListener('popstate', () => callback()); }")

internal fun onThemeChange(callback: () -> Unit): Unit =
  js("{ matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => callback()); }")

internal fun hideFallback(): Unit =
  js("{ document.getElementById('notes-app').style.visibility = 'visible'; document.getElementById('notes-fallback').hidden = true; }")
