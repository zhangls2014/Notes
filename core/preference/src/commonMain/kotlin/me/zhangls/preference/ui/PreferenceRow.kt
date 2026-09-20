package me.zhangls.preference.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import me.zhanghai.compose.preference.ListPreference
import me.zhanghai.compose.preference.Preference
import me.zhanghai.compose.preference.ProvidePreferenceLocals as ProvideLocals
import me.zhanghai.compose.preference.SwitchPreference
import me.zhangls.theme.toColor
import org.jetbrains.compose.resources.stringResource

/**
 * 列表行形态的设置项容器：为下方所有 [PreferenceRow] 提供所需的 composition locals。
 *
 * 存在的唯一理由是**挡住实现库**：`me.zhanghai.compose.preference` 在本模块是 `implementation`
 * 依赖，消费方无法（也不应）引用它，故由本包装器转出这对 locals。
 *
 * 用法与 `LazyColumn` 嵌套即可，设置项行必须在它的子树内：
 * ```
 * ProvidePreferenceLocals {
 *   LazyColumn { items(models) { PreferenceRow(models[it]) } }
 * }
 * ```
 */
@Composable
fun ProvidePreferenceLocals(content: @Composable () -> Unit) {
  ProvideLocals(content = content)
}

/**
 * 按 [model] 的形态渲染一个列表行设置项。
 *
 * 这是设置项的**行形态**（标题 / 图标 / 摘要 / 尾部控件）—— 与 [SelectIconButton] 的图标形态
 * 展示同一批 [PreferenceSpec]，只是排版不同。两种形态都实现一次、都住在本模块，因此
 * 新增设置项不需要任何 feature 写渲染代码，新增**形态**也只需在这里加一个控件。
 *
 * 消费方只需保证自己在 [ProvidePreferenceLocals] 的子树内。
 */
@Composable
fun PreferenceRow(
  model: PreferenceUiModel,
  modifier: Modifier = Modifier,
) {
  when (model) {
    is PreferenceUiModel.Toggle -> ToggleRow(model = model, modifier = modifier)
    is PreferenceUiModel.Select<*> -> SelectRow(model = model, modifier = modifier)
    is PreferenceUiModel.Action -> ActionRow(model = model, modifier = modifier)
  }
}

@Composable
private fun ToggleRow(
  model: PreferenceUiModel.Toggle,
  modifier: Modifier = Modifier,
) {
  val spec = model.spec
  SwitchPreference(
    value = model.value,
    onValueChange = model.onValueChange,
    modifier = modifier,
    title = { Text(text = stringResource(spec.title)) },
    summary = spec.summary(model.value)?.let {
      { Text(text = stringResource(it)) }
    },
    icon = {
      Icon(imageVector = spec.icon, contentDescription = null)
    }
  )
}

@Composable
private fun <T> SelectRow(
  model: PreferenceUiModel.Select<T>,
  modifier: Modifier = Modifier,
) {
  val spec = model.spec
  val optionTextMap = spec.options.associate {
    it.value to stringResource(it.label)
  }

  ListPreference(
    value = model.value,
    onValueChange = model.onValueChange,
    values = spec.options.map { it.value },
    modifier = modifier,
    valueToText = { AnnotatedString(optionTextMap[it] ?: "") },
    title = { Text(text = stringResource(spec.title)) },
    // 摘要直接取当前取值对应的选项文案（见 PreferenceSpec.Select.summary 的单一来源说明）
    summary = spec.summary(model.value)?.let {
      { Text(text = stringResource(it)) }
    },
    icon = {
      Icon(imageVector = spec.icon, contentDescription = null)
    }
  )
}

@Composable
private fun ActionRow(
  model: PreferenceUiModel.Action,
  modifier: Modifier = Modifier,
) {
  val spec = model.spec
  // tint 是"危险操作"之类的语义着色，null 表示交给主题默认色
  val tint = spec.tint?.toColor() ?: Color.Unspecified

  Preference(
    title = {
      Text(text = stringResource(spec.title), color = tint)
    },
    modifier = modifier,
    onClick = model.onClick,
    summary = spec.summary?.let {
      { Text(text = stringResource(it)) }
    },
    icon = {
      Icon(imageVector = spec.icon, contentDescription = null, tint = tint)
    }
  )
}
