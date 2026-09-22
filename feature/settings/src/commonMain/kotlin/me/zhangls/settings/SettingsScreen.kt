package me.zhangls.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.zhangls.framework.mvi.DialogResult
import me.zhangls.preference.ui.PreferenceRow
import me.zhangls.preference.ui.ProvidePreferenceLocals
import me.zhangls.settings.api.SettingsResult
import me.zhangls.settings.mvi.SettingsIntent
import me.zhangls.settings.mvi.SettingsViewModel
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
internal fun SettingsScreen(
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
    // 行的渲染全在 core:preference（PreferenceRow），本屏只负责列表装配与顺序
    ProvidePreferenceLocals {
      // 行宽上限：1200dp 以上的窗口上，一行设置项如果横跨整幅宽度，既难读也难扫；
      // 上限之外居中留白。设置页没有分栏，所以它确实是全仓唯一"内容会被拉满整幅宽度"的页面
      AdaptiveContent {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          // 直接用 Scaffold 给的 padding：导航套件占用的那部分系统内边距已由外壳消费掉
          contentPadding = padding,
        ) {
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
