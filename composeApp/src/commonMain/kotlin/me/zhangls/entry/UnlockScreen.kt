package me.zhangls.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.zhangls.theme.layout.ContentWidth
import me.zhangls.theme.layout.contentWidth
import notes.composeapp.generated.resources.Res
import notes.composeapp.generated.resources.unlock_action
import notes.composeapp.generated.resources.unlock_error_device_credential
import notes.composeapp.generated.resources.unlock_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun UnlockScreen(verifying: Boolean, missingCredential: Boolean, onUnlock: () -> Unit) {
  Surface {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Column(
        modifier = Modifier
          .contentWidth(ContentWidth.Form)
          .verticalScroll(rememberScrollState())
          .safeDrawingPadding()
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Text(
          text = stringResource(Res.string.unlock_title),
          style = MaterialTheme.typography.headlineSmall,
          textAlign = TextAlign.Center,
        )
        if (missingCredential) {
          Text(
            text = stringResource(Res.string.unlock_error_device_credential),
            textAlign = TextAlign.Center,
          )
        }
        Button(onClick = onUnlock, enabled = !verifying) {
          Text(stringResource(Res.string.unlock_action))
        }
      }
    }
  }
}
