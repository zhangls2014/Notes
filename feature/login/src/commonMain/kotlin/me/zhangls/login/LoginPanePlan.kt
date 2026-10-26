@file:OptIn(ExperimentalMaterial3AdaptiveApi::class)

package me.zhangls.login

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldValue

internal class LoginPanePlan(val directive: PaneScaffoldDirective, val value: ThreePaneScaffoldValue) {
  private val mainValue: PaneAdaptedValue
    get() = value[SupportingPaneScaffoldRole.Main]
  private val supportingValue: PaneAdaptedValue
    get() = value[SupportingPaneScaffoldRole.Supporting]

  val isBrandPaneShown: Boolean
    get() = mainValue != PaneAdaptedValue.Hidden
  val isFormPaneSideBySide: Boolean
    get() = mainValue == PaneAdaptedValue.Expanded && supportingValue == PaneAdaptedValue.Expanded
}
