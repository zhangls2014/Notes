package me.zhangls.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.zhanghai.compose.preference.ListPreference
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.SwitchPreference
import me.zhangls.settings.domain.Preference
import me.zhangls.theme.component.CenteredTopAppBar

/**
 * @author zhangls
 */
@Composable
fun SettingsScreen(viewmodel: SettingsViewModel = hiltViewModel(), onResult: (SettingsResult) -> Unit = {}) {
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
      CenteredTopAppBar(title = stringResource(R.string.settings_label_settings))
    }
  ) { padding ->
    ProvidePreferenceLocals {
      LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = padding) {
        items(
          count = state.preferences.size,
          key = { index -> state.preferences[index].key },
          contentType = { index -> state.preferences[index] }
        ) {
          when (val preference = state.preferences[it]) {
            is Preference.Switch -> {
              SwitchItem(preference = preference) { preference, result ->
                viewmodel.handleIntent(SettingsIntent.UpdateSettings(preference, result))
              }
            }

            is Preference.Alert<*> -> {
              AlertItem(preference = preference) { preference, result ->
                viewmodel.handleIntent(SettingsIntent.UpdateSettings(preference, result))
              }
            }
          }
        }
      }
    }
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
  )
}

@Preview
@Composable
fun SettingsScreenPreview() {
  SettingsScreen(onResult = {})
}