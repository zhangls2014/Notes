package me.zhangls.preference.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import me.zhangls.preference.PreferenceSpec
import me.zhangls.theme.component.HingeSafeDialog
import org.jetbrains.compose.resources.stringResource

/** Shared selection surface for controls whose platform popup can cross a physical hinge. */
@Composable
@Suppress("FunctionSignature") // Ktlint assumes four-space continuation; this project uses two.
internal fun <T> PreferenceSelectionDialog(
  spec: PreferenceSpec.Select<T>,
  value: T,
  onValueChange: (T) -> Unit,
  onDismissRequest: () -> Unit,
) {
  HingeSafeDialog(onDismissRequest = onDismissRequest) {
    Surface(
      shape = MaterialTheme.shapes.extraLarge,
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
      Column(
        Modifier
          .verticalScroll(rememberScrollState())
          .padding(vertical = 16.dp)
          .selectableGroup(),
      ) {
        Text(
          text = stringResource(spec.title),
          modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
          style = MaterialTheme.typography.titleLarge,
        )
        spec.options.forEach { option ->
          Row(
            modifier = Modifier.fillMaxWidth().selectable(
              selected = option.value == value,
              role = Role.RadioButton,
              onClick = {
                onValueChange(option.value)
                onDismissRequest()
              },
            ).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            RadioButton(selected = option.value == value, onClick = null)
            Text(stringResource(option.label), Modifier.padding(start = 16.dp))
          }
        }
      }
    }
  }
}
