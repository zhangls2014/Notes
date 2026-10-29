package me.zhangls.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.zhangls.framework.mvi.DialogResult
import me.zhangls.preference.ui.PreferenceRow
import me.zhangls.preference.ui.PreferenceUiModel
import me.zhangls.preference.ui.ProvidePreferenceLocals
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
import notes.feature.settings.generated.resources.settings_label_settings
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * @author zhangls
 */
@Composable
internal fun SettingsScreen(viewModel: SettingsViewModel = koinViewModel(), onResult: (SettingsResult) -> Unit = {}) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  // 领域模型在 UI 层映射为表现层模型（回调在此绑到 viewModel）。
  // 以 settings 与 viewModel 为 key：只在取值变化时重建，且不会捕获过期的 sendIntent
  val preferences = remember(state.settings, viewModel) {
    state.settings.toPreferenceUiModels(viewModel::sendIntent)
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
            val standalone = model is PreferenceUiModel.Action
            PreferenceRow(
              model = model,
              isGroupStart = index == 0 || standalone || preferences[index - 1] is PreferenceUiModel.Action,
              isGroupEnd =
              index == preferences.lastIndex || standalone || preferences[index + 1] is PreferenceUiModel.Action,
              modifier = Modifier.padding(
                start = 16.dp,
                end = 16.dp,
                top = if (index == 0 || standalone || preferences[index - 1] is PreferenceUiModel.Action) {
                  16.dp
                } else {
                  0.dp
                },
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
