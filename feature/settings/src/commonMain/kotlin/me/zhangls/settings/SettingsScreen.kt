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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.zhanghai.compose.preference.ListPreference
import me.zhanghai.compose.preference.Preference
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.SwitchPreference
import me.zhangls.framework.mvi.DialogResult
import me.zhangls.settings.api.SettingsResult
import me.zhangls.settings.ui.PreferenceUiModel
import me.zhangls.settings.ui.toDialogUiModel
import me.zhangls.settings.ui.toPreferenceUiModels
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
internal fun SettingsScreen(
  isBottomNavigationBar: Boolean,
  viewModel: SettingsViewModel = koinViewModel(),
  onResult: (SettingsResult) -> Unit = {}
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  // 领域模型在 UI 层映射为表现层模型
  val preferences = remember(state.settings) { state.settings.toPreferenceUiModels() }
  val sendIntent = viewModel::sendIntent

  LaunchedEffect(viewModel) {
    viewModel.effect.collect { effect ->
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
          count = preferences.size,
          key = { index -> preferences[index].key },
          contentType = { index -> preferences[index]::class }
        ) {
          when (val preference = preferences[it]) {
            is PreferenceUiModel.Switch -> {
              SwitchItem(preference = preference, sendIntent = sendIntent)
            }

            is PreferenceUiModel.Alert<*> -> {
              AlertItem(preference = preference, sendIntent = sendIntent)
            }

            is PreferenceUiModel.Text -> {
              TextItem(preference = preference, sendIntent = sendIntent)
            }
          }
        }
      }
    }
  }

  state.dialog?.let { dialog ->
    // 领域对话框在 UI 层映射为表现层模型
    val dialogState = dialog.toDialogUiModel()
    SimpleDialog(
      title = stringResource(dialogState.title),
      content = stringResource(dialogState.message),
      confirmText = stringResource(dialogState.confirm),
      confirm = {
        val result = DialogResult.Confirm(dialogState.dialogId)
        sendIntent(SettingsIntent.DialogCallback(result))
      },
      dismissText = dialogState.dismiss?.let { stringResource(it) },
      dismiss = {
        val result = DialogResult.Dismiss(dialogState.dialogId)
        sendIntent(SettingsIntent.DialogCallback(result))
      }
    )
  }
}

@Composable
private fun SwitchItem(
  modifier: Modifier = Modifier,
  preference: PreferenceUiModel.Switch,
  sendIntent: (SettingsIntent) -> Unit
) {
  SwitchPreference(
    value = preference.value,
    onValueChange = { sendIntent(preference.onValueChange(it)) },
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
  preference: PreferenceUiModel.Alert<T>,
  sendIntent: (SettingsIntent) -> Unit
) {
  val optionTextMap = preference.options.associate {
    it.value to stringResource(it.label)
  }

  ListPreference(
    value = preference.value,
    onValueChange = { sendIntent(preference.onValueChange(it)) },
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
  preference: PreferenceUiModel.Text,
  sendIntent: (SettingsIntent) -> Unit
) {
  Preference(
    title = {
      Text(
        text = stringResource(preference.title),
        color = preference.tint?.toColor() ?: Color.Unspecified
      )
    },
    modifier = modifier,
    onClick = { sendIntent(preference.clickIntent) },
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

