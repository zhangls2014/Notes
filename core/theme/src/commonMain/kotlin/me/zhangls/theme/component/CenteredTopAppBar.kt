package me.zhangls.theme.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import me.zhangls.theme.icon.ArrowBackIosNew
import me.zhangls.theme.icon.Icons
import notes.core.theme.generated.resources.Res
import notes.core.theme.generated.resources.theme_action_navigate_before
import org.jetbrains.compose.resources.stringResource

/**
 * 标题对齐由 [CenterAlignedTopAppBar] 处理；本组件提供本地化返回按钮与操作插槽。
 *
 * @param title 标题
 * @param modifier 修饰符
 * @param navigate 返回按钮点击事件，如果为 null，则不显示返回按钮
 * @param actions 标题栏右侧的操作内容
 *
 * @author zhangls
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CenteredTopAppBar(
  title: String,
  modifier: Modifier = Modifier,
  navigate: (() -> Unit)? = null,
  actions: (@Composable RowScope.() -> Unit) = {}
) {
  CenterAlignedTopAppBar(
    title = { Text(text = title) },
    modifier = modifier,
    colors = TopAppBarDefaults.topAppBarColors(),
    navigationIcon = {
      if (navigate != null) {
        IconButton(onClick = navigate, shapes = IconButtonDefaults.shapes()) {
          Icon(
            imageVector = Icons.Rounded.ArrowBackIosNew,
            contentDescription = stringResource(Res.string.theme_action_navigate_before)
          )
        }
      }
    },
    actions = actions
  )
}

@Preview
@Composable
private fun CenteredTopAppBarPreview() {
  CenteredTopAppBar(title = "标题")
}
