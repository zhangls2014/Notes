package me.zhangls.notes.util

import android.graphics.Rect
import androidx.window.layout.FoldingFeature
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * 检查折叠屏设备是否半开且垂直方向折叠
 */
@OptIn(ExperimentalContracts::class)
fun isHalfOpened(foldFeature: FoldingFeature?): Boolean {
  contract { returns(true) implies (foldFeature != null) }
  return foldFeature?.state == FoldingFeature.State.HALF_OPENED &&
      foldFeature.orientation == FoldingFeature.Orientation.VERTICAL
}

/**
 * 检查折叠屏设备是否打开且分离
 */
@OptIn(ExperimentalContracts::class)
fun isSeparating(foldFeature: FoldingFeature?): Boolean {
  contract { returns(true) implies (foldFeature != null) }
  return foldFeature?.isSeparating ?: false
}

/**
 * 设备显示内容窗格
 */
enum class ContentPane {
  // 单窗格
  SINGLE_PANE,

  // 双窗格
  DUAL_PANE,
}

/**
 * 设备折叠姿态，
 */
sealed interface DevicePosture {
  object NormalPosture : DevicePosture
  data class Separating(val hingePosition: Rect, var orientation: FoldingFeature.Orientation) : DevicePosture
}