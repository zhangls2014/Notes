package me.zhangls.entry

import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction

/** Text editors have their own scrolling; this selects the surrounding viewport. */
internal fun viewportScrollMatcher() = hasScrollAction() and hasSetTextAction().not()
