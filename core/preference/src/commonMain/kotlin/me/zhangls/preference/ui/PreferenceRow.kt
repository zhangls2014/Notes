package me.zhangls.preference.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import me.zhanghai.compose.preference.ListPreference
import me.zhanghai.compose.preference.MapPreferences
import me.zhanghai.compose.preference.Preferences
import me.zhanghai.compose.preference.Preference
import me.zhanghai.compose.preference.SwitchPreference
import me.zhangls.theme.toColor
import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.compose.resources.stringResource
import me.zhanghai.compose.preference.ProvidePreferenceLocals as ProvideLocals

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
  // 行控件的值和回调由消费方提供，不使用底层库的持久化存储。
  // 默认 flow 会读取整个 NSUserDefaults 域；UIKit 写入的嵌套字典不受库支持，
  // 导致 iOS 进入设置页时抛异常，也会意外引入第二套设置数据源。
  val flow = remember { MutableStateFlow<Preferences>(MapPreferences()) }
  ProvideLocals(flow = flow, content = content)
}

/**
 * 按 [model] 的形态渲染一个列表行设置项。
 *
 * 这是设置项的**行形态**（标题 / 图标 / 摘要 / 尾部控件）—— 与 [SelectIconButton] 的图标形态
 * 展示同一批 [PreferenceSpec]，只是排版不同。两种形态都实现一次、都住在本模块，因此
 * 新增设置项不需要任何 feature 写渲染代码，新增**形态**也只需在这里加一个控件。
 *
 * 消费方只需保证自己在 [ProvidePreferenceLocals] 的子树内。
 * [isGroupStart] / [isGroupEnd] 仅控制外角；分组清单和顺序由消费方拥有。
 */
@Composable
fun PreferenceRow(
  model: PreferenceUiModel,
  modifier: Modifier = Modifier,
  isGroupStart: Boolean = true,
  isGroupEnd: Boolean = true,
) {
  val outer = MaterialTheme.shapes.extraLarge
  val inner = MaterialTheme.shapes.extraSmall
  val shape = outer.copy(
    topStart = if (isGroupStart) outer.topStart else inner.topStart,
    topEnd = if (isGroupStart) outer.topEnd else inner.topEnd,
    bottomStart = if (isGroupEnd) outer.bottomStart else inner.bottomStart,
    bottomEnd = if (isGroupEnd) outer.bottomEnd else inner.bottomEnd,
  )
  Surface(
    modifier = modifier,
    shape = shape,
    color = MaterialTheme.colorScheme.surfaceContainerLow,
  ) {
    when (model) {
      is PreferenceUiModel.Toggle -> ToggleRow(model = model)
      is PreferenceUiModel.Select<*> -> SelectRow(model = model)
      is PreferenceUiModel.Action -> ActionRow(model = model)
    }
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
