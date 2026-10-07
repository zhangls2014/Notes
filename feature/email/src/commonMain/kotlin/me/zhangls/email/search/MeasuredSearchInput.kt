package me.zhangls.email.search

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.constrainWidth

/**
 * 锚点使用空查询的首次自然宽度，避免恢复的长查询参与基准测量。
 * 基准只测量、不放置；真实输入在同一帧按基准测量，无坐标回调后的尺寸跳变。
 * 父约束变窄仅收缩当前宽度，不覆盖基准；显示环境变化才重新测量。
 */
@Composable
internal fun MeasuredSearchInput(
  measurementKey: Any,
  modifier: Modifier = Modifier,
  inputField: @Composable (measuring: Boolean) -> Unit,
) {
  val density = LocalDensity.current
  val layoutDirection = LocalLayoutDirection.current
  val measurement = remember(density.density, density.fontScale, layoutDirection, measurementKey) {
    SearchInputMeasurement()
  }
  SubcomposeLayout(modifier = modifier) { constraints ->
    val naturalWidth = measurement.width ?: subcompose(SearchInputSlot.Measurement) {
      Box(Modifier.clearAndSetSemantics {}, propagateMinConstraints = true) {
        inputField(true)
      }
    }.single().measure(constraints).width.also {
      // 零宽宿主尚未就绪，不把临时零尺寸保留为基准。
      if (it > 0) measurement.width = it
    }
    val width = constraints.constrainWidth(naturalWidth)
    val input = subcompose(SearchInputSlot.Input) {
      inputField(false)
    }.single().measure(constraints.copy(minWidth = width, maxWidth = width))
    layout(input.width, input.height) {
      input.placeRelative(0, 0)
    }
  }
}

private class SearchInputMeasurement {
  var width: Int? = null
}

private enum class SearchInputSlot { Measurement, Input }
