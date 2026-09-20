package me.zhangls.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.zhangls.framework.mvi.DialogResult
import me.zhangls.preference.ui.PreferenceRow
import me.zhangls.preference.ui.ProvidePreferenceLocals
import me.zhangls.settings.api.SettingsResult
import me.zhangls.settings.mvi.SettingsIntent
import me.zhangls.settings.mvi.SettingsViewModel
import me.zhangls.settings.ui.toDialogUiModel
import me.zhangls.settings.ui.toPreferenceUiModels
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
internal fun SettingsScreen(
  isBottomNavigationBar: Boolean,
  viewModel: SettingsViewModel = koinViewModel(),
  onResult: (SettingsResult) -> Unit = {}
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  // 领域模型在 UI 层映射为表现层模型（回调在此绑到 viewModel）。
  // 以 settings 与 viewModel 为 key：只在取值变化时重建，且不会捕获过期的 sendIntent
  val preferences = remember(state.settings, viewModel) {
    state.settings.toPreferenceUiModels(viewModel::sendIntent)
  }

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

    // 行的渲染全在 core:preference（PreferenceRow），本屏只负责列表装配与顺序
    ProvidePreferenceLocals {
      LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = contentPadding) {
        items(
          count = preferences.size,
          key = { index -> preferences[index].spec.key },
          contentType = { index -> preferences[index]::class }
        ) { index ->
          PreferenceRow(model = preferences[index])
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
        viewModel.sendIntent(SettingsIntent.DialogCallback(result))
      },
      dismissText = dialogState.dismiss?.let { stringResource(it) },
      dismiss = {
        val result = DialogResult.Dismiss(dialogState.dialogId)
        viewModel.sendIntent(SettingsIntent.DialogCallback(result))
      }
    )
  }
}
