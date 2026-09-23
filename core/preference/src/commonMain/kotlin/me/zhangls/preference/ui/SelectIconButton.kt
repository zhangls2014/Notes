package me.zhangls.preference.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import me.zhangls.preference.PreferenceSpec
import me.zhangls.theme.component.TooltipIconButton
import me.zhangls.theme.layout.LocalWindowAdaptiveInfo
import me.zhangls.theme.layout.hasHinges
import org.jetbrains.compose.resources.stringResource

/**
 * 把 [PreferenceSpec.Select] 渲染成"纯图标按钮 + 长按提示 + 下拉单选"的形态。
 *
 * 这是设置项的**图标形态** —— 与 [PreferenceRow] 的列表行形态展示同一批 spec，只是排版不同。
 * 设置项的**渲染**同样是跨 feature 复用的资产：只共享元数据会把两份逐行同构的控件代码
 * 留在各自 feature 里（本控件就是为消掉登录页那两个 55 行同构函数而生的）。
 * 因此每种形态都实现一次、都住在本模块，消费方只提供取值与回调。
 *
 * 按钮本体复用 `core:theme` 的 [TooltipIconButton]，下拉菜单由本控件补上。
 * 与设置页的列表行共用同一份 [PreferenceSpec.Select]，故标题 / 图标 / 选项文案永远不会漂移。
 *
 * 两处刻意的设计：
 * ① 回调签名是 `(T) -> Unit` 而非 `(T) -> SettingsIntent` 之类的 feature 类型 ——
 *    消费方的 Intent 类型不进入本模块，因此不会形成 impl → impl 的依赖环；
 * ② 长按提示只显示按钮名称，**不含当前选中值**（下拉内的 check 标记已表达选中态）。
 *
 * @param spec 设置项的静态展示元数据
 * @param value 当前取值
 * @param onValueChange 取值变化回调，由消费方决定如何派发
 */
// DropdownMenuPopup / MenuDefaults 需要 Expressive
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> SelectIconButton(
  spec: PreferenceSpec.Select<T>,
  value: T,
  onValueChange: (T) -> Unit,
  modifier: Modifier = Modifier,
) {
  val label = stringResource(spec.title)
  var expanded by rememberSaveable { mutableStateOf(false) }

  Box(modifier = modifier) {
    TooltipIconButton(
      icon = spec.icon,
      label = label,
      onClick = { expanded = expanded.not() },
      // 按钮通常位于页面顶部，提示放在下方避免被屏幕边缘裁掉
      tooltipPosition = TooltipAnchorPosition.Below,
    )

    if (LocalWindowAdaptiveInfo.current.hasHinges) {
      if (expanded) {
        PreferenceSelectionDialog(
          spec = spec,
          value = value,
          onValueChange = onValueChange,
          onDismissRequest = { expanded = false },
        )
      }
    } else {
      DropdownMenuPopup(
        expanded = expanded,
        onDismissRequest = { expanded = false },
      ) {
        DropdownMenuGroup(
          shapes = MenuDefaults.groupShapes(),
        ) {
          spec.options.forEach { option ->
            DropdownMenuItem(
              text = { Text(text = stringResource(option.label)) },
              shapes = MenuDefaults.itemShapes(),
              checked = option.value == value,
              onCheckedChange = {
                expanded = false
                onValueChange(option.value)
              },
            )
          }
        }
      }
    }
  }
}
