package me.zhangls.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.zhanghai.compose.preference.ListPreference
import me.zhanghai.compose.preference.Preference
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.SwitchPreference
import me.zhangls.framework.mvi.DialogResult
import me.zhangls.settings.SettingsIntent.ClickSettings
import me.zhangls.settings.SettingsIntent.DialogCallback
import me.zhangls.settings.SettingsIntent.UpdateSettings
import me.zhangls.settings.domain.Preference
import me.zhangls.theme.component.CenteredTopAppBar
import me.zhangls.theme.component.SimpleDialog
import me.zhangls.theme.toColor
import notes.feature.settings.generated.resources.Res
import notes.feature.settings.generated.resources.settings_label_settings
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * @author zhangls
 */
@Composable
fun SettingsScreen(
  isBottomNavigationBar: Boolean,
  viewmodel: SettingsViewModel = koinViewModel(),
  onResult: (SettingsResult) -> Unit = {}
) {
  val state by viewmodel.state.collectAsStateWithLifecycle()

  LaunchedEffect(viewmodel) {
    viewmodel.effect.collect { effect ->
      when (effect) {
        is SettingsResult -> onResult(effect)
      }
    }
  }

  Scaffold(
    topBar = {
      CenteredTopAppBar(title = stringResource(Res.string.settings_label_settings))
    }
  ) { padding ->
    val contentPadding = if (isBottomNavigationBar) {
      padding
    } else {
      PaddingValues(
        top = padding.calculateTopPadding(),
        bottom = padding.calculateBottomPadding(),
        end = padding.calculateEndPadding(LayoutDirection.Ltr)
      )
    }

    ProvidePreferenceLocals {
      LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = contentPadding) {
        items(
          count = state.preferences.size,
          key = { index -> state.preferences[index].key },
          contentType = { index -> state.preferences[index] }
        ) {
          when (val preference = state.preferences[it]) {
            is Preference.Switch -> {
              SwitchItem(preference = preference) { preference, result ->
                viewmodel.sendIntent(UpdateSettings(preference, result))
              }
            }

            is Preference.Alert<*> -> {
              AlertItem(preference = preference) { preference, result ->
                viewmodel.sendIntent(UpdateSettings(preference, result))
              }
            }

            is Preference.Text -> {
              TextItem(preference = preference) { preference ->
                viewmodel.sendIntent(ClickSettings(preference))
              }
            }
          }
        }
      }
    }
  }

  state.dialog?.let { dialog ->
    SimpleDialog(
      title = dialog.title,
      content = dialog.message,
      confirmText = dialog.confirm,
      confirm = {
        val result = DialogResult.Confirm(dialog.dialogId)
        viewmodel.sendIntent(DialogCallback(result))
      },
      dismissText = dialog.dismiss,
      dismiss = {
        val result = DialogResult.Dismiss(dialog.dialogId)
        viewmodel.sendIntent(DialogCallback(result))
      }
    )
  }
}

@Composable
private fun SwitchItem(
  modifier: Modifier = Modifier,
  preference: Preference.Switch,
  onValueChange: (String, Boolean) -> Unit
) {
  SwitchPreference(
    value = preference.value,
    onValueChange = { onValueChange(preference.key, it) },
    modifier = modifier,
    title = { Text(text = stringResource(preference.title)) },
    summary = preference.summary?.let {
      { Text(text = stringResource(it)) }
    },
    icon = {
      preference.icon?.let {
        Icon(imageVector = it, contentDescription = null)
      }
    }
  )
}

@Composable
private fun <T> AlertItem(
  modifier: Modifier = Modifier,
  preference: Preference.Alert<T>,
  onValueChange: (String, T) -> Unit
) {
  val optionTextMap = preference.options.associate {
    it.value to stringResource(it.label)
  }

  ListPreference(
    value = preference.value,
    onValueChange = { onValueChange(preference.key, it) },
    values = preference.options.map { it.value },
    modifier = modifier,
    valueToText = { AnnotatedString(optionTextMap[it] ?: "") },
    title = { Text(text = stringResource(preference.title)) },
    summary = preference.summary?.let {
      { Text(text = stringResource(it)) }
    },
    icon = {
      preference.icon?.let {
        Icon(imageVector = it, contentDescription = null)
      }
    }
  )
}

@Composable
private fun TextItem(
  modifier: Modifier = Modifier,
  preference: Preference.Text,
  onClick: (String) -> Unit
) {
  Preference(
    title = {
      Text(
        text = stringResource(preference.title),
        color = preference.tint?.toColor() ?: Color.Unspecified
      )
    },
    modifier = modifier,
    onClick = { onClick(preference.key) },
    summary = preference.summary?.let {
      { Text(text = stringResource(it)) }
    },
    icon = {
      preference.icon?.let {
        Icon(imageVector = it, contentDescription = null, tint = preference.tint?.toColor() ?: Color.Unspecified)
      }
    }
  )
}

@Preview
@Composable
fun SettingsScreenPreview() {
  SettingsScreen(true, onResult = {})
}
