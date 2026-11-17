package me.zhangls.web

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.zhangls.web.resources.Res
import me.zhangls.web.resources.view_open_source_notices

@Composable
internal fun NoticesPage(
  platform: String,
  onOpen: () -> Unit,
) {
  Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge,
    color = MaterialTheme.colorScheme.surfaceContainerLow) {
    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Text(if (platform == "ios") "iOS" else "Android", style = MaterialTheme.typography.headlineMedium)
      TextButton(onClick = onOpen) {
        Text(websiteString(Res.string.view_open_source_notices))
      }
    }
  }
}
