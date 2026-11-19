package me.zhangls.email.component

import androidx.compose.runtime.Composable
import notes.feature.email.generated.resources.Res
import notes.feature.email.generated.resources.email_time_days
import notes.feature.email.generated.resources.email_time_hours
import notes.feature.email.generated.resources.email_time_just_now
import notes.feature.email.generated.resources.email_time_minutes
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.Instant

@Composable
internal fun mailTime(value: String): String {
  val instant = try {
    Instant.parse(value)
  } catch (_: IllegalArgumentException) {
    return value
  }
  val elapsed = Clock.System.now() - instant
  return when {
    elapsed.inWholeMinutes < 1 -> stringResource(Res.string.email_time_just_now)
    elapsed.inWholeHours < 1 -> stringResource(Res.string.email_time_minutes, elapsed.inWholeMinutes.toInt())
    elapsed.inWholeDays < 1 -> stringResource(Res.string.email_time_hours, elapsed.inWholeHours.toInt())
    else -> stringResource(Res.string.email_time_days, elapsed.inWholeDays.toInt())
  }
}
