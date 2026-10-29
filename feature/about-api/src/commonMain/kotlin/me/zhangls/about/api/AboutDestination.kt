package me.zhangls.about.api

import kotlinx.serialization.Serializable
import me.zhangls.framework.nav.Destination

/** Public application information, available without authentication. */
@Serializable
data object AboutDestination : Destination

@Serializable
enum class OpenSourcePage { LIBRARIES, LICENSES }

@Serializable
data class OpenSourceNoticesDestination(
  val page: OpenSourcePage = OpenSourcePage.LIBRARIES,
  val anchor: String? = null,
  val projectUrl: String? = null,
) : Destination
