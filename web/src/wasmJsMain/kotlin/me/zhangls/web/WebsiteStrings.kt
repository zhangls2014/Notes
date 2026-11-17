package me.zhangls.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import me.zhangls.web.resources.Res
import me.zhangls.web.resources.allStringResources
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.getSystemResourceEnvironment

internal class WebsiteStrings private constructor(val chinese: Boolean, private val strings: Map<String, String>) {
  operator fun get(resource: StringResource): String = strings.getValue(resource.key)

  companion object {
    suspend fun load(chinese: Boolean): WebsiteStrings {
      val environment = withResourceLanguage(if (chinese) "zh" else "en", ::getSystemResourceEnvironment)
      return WebsiteStrings(chinese, Res.allStringResources.mapValues { (_, resource) -> getString(environment, resource) })
    }
  }
}

internal val LocalWebsiteStrings = staticCompositionLocalOf<WebsiteStrings> { error("Website strings are not loaded") }

@Composable
internal fun websiteString(resource: StringResource): String = LocalWebsiteStrings.current[resource]
