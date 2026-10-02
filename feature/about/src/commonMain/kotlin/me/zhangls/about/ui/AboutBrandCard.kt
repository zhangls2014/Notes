package me.zhangls.about.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.zhangls.theme.component.AppIcon
import me.zhangls.theme.component.appName
import notes.feature.about.generated.resources.Res
import notes.feature.about.generated.resources.about_brand_description
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AboutBrandCard() {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.extraLarge,
    color = MaterialTheme.colorScheme.primaryContainer,
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      AppIcon(
        modifier = Modifier.size(80.dp).clip(MaterialTheme.shapes.large),
      )
      Text(appName(), style = MaterialTheme.typography.headlineLarge,
        textAlign = TextAlign.Center)
      Text(stringResource(Res.string.about_brand_description), style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center)
    }
  }
}
