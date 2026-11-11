package me.zhangls.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.zhangls.framework.mvi.DialogResult
import me.zhangls.preference.ui.PreferenceRow
import me.zhangls.preference.ui.PreferenceUiModel
import me.zhangls.preference.ui.ProvidePreferenceLocals
import me.zhangls.settings.api.LocalPasskeyManagementAvailable
import me.zhangls.settings.api.SettingsResult
import me.zhangls.settings.mvi.SettingsIntent
import me.zhangls.settings.mvi.SettingsViewModel
import me.zhangls.settings.ui.DialogUiModel
import me.zhangls.settings.ui.toDialogUiModel
import me.zhangls.settings.ui.toPreferenceUiModels
import me.zhangls.theme.component.AdaptiveContent
import me.zhangls.theme.component.CenteredTopAppBar
import me.zhangls.theme.component.SimpleDialog
import notes.feature.settings.generated.resources.Res
import notes.feature.settings.generated.resources.settings_group_account
import notes.feature.settings.generated.resources.settings_group_app
import notes.feature.settings.generated.resources.settings_group_general
import notes.feature.settings.generated.resources.settings_label_security
import notes.feature.settings.generated.resources.settings_label_settings
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * @author zhangls
 */
@Composable
internal fun SettingsScreen(viewModel: SettingsViewModel = koinViewModel(), onResult: (SettingsResult) -> Unit = {}) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  // 领域模型在 UI 层映射为表现层模型（回调在此绑到 viewModel）。
  val showPasskeys = LocalPasskeyManagementAvailable.current
  val preferences = remember(state.settings, state.biometricUnlockEnabled, showPasskeys, viewModel) {
    state.settings.toPreferenceUiModels(
      viewModel::sendIntent,
      biometricUnlockEnabled = state.biometricUnlockEnabled,
      showPasskeys = showPasskeys,
    )
  }

  val currentOnResult by rememberUpdatedState(onResult)
  LaunchedEffect(viewModel) {
    viewModel.effect.collect { effect ->
      when (effect) {
        is SettingsResult -> currentOnResult(effect)
      }
    }
  }

  Scaffold(
    topBar = {
      CenteredTopAppBar(title = stringResource(Res.string.settings_label_settings))
    },
  ) { padding ->
    // 行的渲染全在 core:preference（PreferenceRow），本屏只负责列表装配与顺序
    ProvidePreferenceLocals {
      // 行宽上限取 ContentWidth.Prose：1200dp 以上的窗口上，一行设置项如果横跨整幅宽度，
      // 既难读也难扫；上限之外居中留白。设置页不做分栏，所以它确实是全仓内容最容易被
      // 拉满整幅宽度的页面。
      AdaptiveContent {
        // 固定少量设置项全部参与组合，避免行内选择弹窗在旋转后等待滚动才恢复。
        Column(
          // padding 位于滚动内容内，保留原列表的内容留白和底部安全距离。
          modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding),
          verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          preferences.forEachIndexed { index, model ->
            val group = model.settingsGroup
            val groupStart = index == 0 || preferences[index - 1].settingsGroup != group
            val groupEnd = index == preferences.lastIndex || preferences[index + 1].settingsGroup != group
            if (groupStart && group != null) {
              Text(
                text = stringResource(group),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 32.dp, top = 16.dp, bottom = 4.dp).semantics { heading() },
              )
            }
            PreferenceRow(
              model = model,
              isGroupStart = groupStart,
              isGroupEnd = groupEnd,
              modifier = Modifier.padding(
                start = 16.dp,
                end = 16.dp,
                top = if (groupStart && group == null) 16.dp else 0.dp,
                bottom = if (index == preferences.lastIndex) 16.dp else 0.dp,
              ),
            )
          }
        }
      }
    }
  }

  state.dialog?.let { dialog ->
    SettingsDialog(dialogState = dialog.toDialogUiModel(), onResult = {
      viewModel.sendIntent(SettingsIntent.DialogCallback(it))
    })
  }
}

private val PreferenceUiModel.settingsGroup: StringResource?
  get() = when (spec.key) {
    "profile" -> Res.string.settings_group_account
    "passkeys", "biometricUnlock" -> Res.string.settings_label_security
    "about" -> Res.string.settings_group_app
    "logout" -> null
    else -> Res.string.settings_group_general
  }

@Composable
private fun SettingsDialog(dialogState: DialogUiModel, onResult: (DialogResult) -> Unit) {
  SimpleDialog(
    title = stringResource(dialogState.title),
    content = stringResource(dialogState.message),
    confirmText = stringResource(dialogState.confirm),
    confirm = {
      val result = DialogResult.Confirm(dialogState.dialogId)
      onResult(result)
    },
    dismissText = dialogState.dismiss?.let { stringResource(it) },
    dismiss = {
      val result = DialogResult.Dismiss(dialogState.dialogId)
      onResult(result)
    },
  )
}
