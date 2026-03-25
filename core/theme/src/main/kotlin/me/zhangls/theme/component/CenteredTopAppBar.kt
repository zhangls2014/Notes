package me.zhangls.theme.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import me.zhangls.theme.R
import me.zhangls.theme.icon.ArrowBackIosNew
import me.zhangls.theme.icon.Icons

/**
 * 标题居中的 [CenterAlignedTopAppBar]，通过在右侧添加一个空白不可用的 [IconButton] 来实现标题居中
 *
 * @param title 标题
 * @param modifier 修饰符
 * @param navigate 返回按钮点击事件，如果为 null，则不显示返回按钮
 *
 * @author zhangls
 */
@OptIn(ExperimentalMaterial3Api::class)
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
        IconButton(onClick = navigate) {
          Icon(
            imageVector = Icons.Rounded.ArrowBackIosNew,
            contentDescription = stringResource(id = R.string.theme_action_navigate_before)
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