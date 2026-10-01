package me.zhangls.email.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import me.zhangls.theme.ComposeAppTheme
import me.zhangls.theme.layout.ProvideModalViewport
import me.zhangls.theme.layout.ImeHandoffFocusState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37], qualifiers = "en-rUS-w600dp-h800dp-mdpi")
class NewEmailSheetContentTest {
  @get:Rule val compose = createComposeRule()

  @Test fun unboundedDialogMeasurementKeepsFormScrollableAndActionsReachable() {
    var saves = 0
    compose.setContent {
      ComposeAppTheme {
        MeasureWithHeight(Constraints.Infinity) {
          Form(onSave = { saves++ })
        }
      }
    }
    val viewport = compose.onNode(hasScrollAction()).fetchSemanticsNode().boundsInRoot
    assertTrue("Form must fit its 260dp host: $viewport", viewport.height <= 260f)
    compose.onNodeWithText("Save").performScrollTo().assertIsDisplayed().performClick()
    compose.runOnIdle { assertEquals(1, saves) }
  }

  @Test fun keyboardConstraintCanShrinkViewportBelowHostHeight() {
    compose.setContent {
      ComposeAppTheme {
        MeasureWithHeight(160) { Form() }
      }
    }
    val viewport = compose.onNode(hasScrollAction()).fetchSemanticsNode().boundsInRoot
    assertTrue("Form must respect tighter keyboard constraints: $viewport", viewport.height <= 160f)
    compose.onNodeWithText("Cancel").performScrollTo().assertIsDisplayed()
  }

  @Test fun sheetUsesNewHostHeightAfterClosingAndRotating() {
    var visible by mutableStateOf(true)
    var width by mutableStateOf(280.dp)
    var height by mutableStateOf(300.dp)
    compose.setContent {
      ComposeAppTheme {
        Box(Modifier.requiredSize(width, height)) {
          ProvideModalViewport {
            NewEmailSheet(
              recipients = emptyList(),
              visible = visible,
              selectedRecipientIds = emptySet(),
              subject = "",
              body = "",
              isSending = false,
              onToggleRecipient = { _, _ -> },
              onSubjectChange = {},
              onBodyChange = {},
              onDismiss = { visible = false },
              onCancel = { visible = false },
              onSave = {},
            )
          }
        }
      }
    }
    for ((newWidth, newHeight) in listOf(400.dp to 180.dp, 280.dp to 300.dp)) {
      compose.onNodeWithText("Cancel").performScrollTo().performClick()
      compose.onNodeWithText("Recipients").assertDoesNotExist()
      compose.runOnIdle { width = newWidth; height = newHeight }
      compose.runOnIdle { visible = true }
      compose.onNodeWithText("Recipients").assertIsDisplayed()
      val viewport = compose.onNode(hasScrollAction()).fetchSemanticsNode().boundsInRoot
      assertTrue("Reopened form must use rotated host height $newHeight: $viewport",
        viewport.height <= newHeight.value)
      compose.onNodeWithText("Save").performScrollTo().assertIsDisplayed()
    }
  }

  @OptIn(ExperimentalMaterial3Api::class)
  @Test fun iosPreferredSizePassKeepsSheetInsideRotatedViewport() {
    var viewport by mutableStateOf(DpSize(280.dp, 400.dp))
    compose.setContent {
      ComposeAppTheme {
        // Both dimensions are unconstrained in the iOS preferred-size pass.
        Layout(content = {
          val sheet = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
          )
          DraftSheetLayout(
            viewport = viewport,
            sheetState = sheet,
            onDismissRequest = {},
            onScrimClick = {},
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
          ) { Form(maxHeight = viewport.height) }
        }) { measurables, _ ->
          val placeable = measurables.single().measure(Constraints())
          layout(placeable.width, placeable.height) { placeable.place(0, 0) }
        }
      }
    }
    for (size in listOf(DpSize(280.dp, 400.dp), DpSize(400.dp, 220.dp), DpSize(280.dp, 400.dp))) {
      compose.runOnIdle { viewport = size }
      compose.waitForIdle()
      val bounds = compose.onNode(hasScrollAction()).fetchSemanticsNode().boundsInRoot
      assertTrue("Sheet must remain within $size after preferred-size measurement: $bounds",
        bounds.left >= 0f && bounds.top >= 0f && bounds.right <= size.width.value &&
          bounds.bottom <= size.height.value && bounds.height > 0f)
    }
  }

  @Composable private fun Form(maxHeight: Dp = 260.dp, onSave: () -> Unit = {}) {
    NewEmailSheetContent(
      maxHeight = maxHeight,
      recipients = emptyList(),
      selectedRecipientIds = emptySet(),
      subject = "",
      body = "",
      isSending = false,
      inputFocus = ImeHandoffFocusState(),
      onToggleRecipient = { _, _ -> },
      onSubjectChange = {},
      onBodyChange = {},
      onCancel = {},
      onSave = onSave,
    )
  }

  /** iOS measures dialog preferred size with unbounded height before its final layout. */
  @Composable private fun MeasureWithHeight(maxHeight: Int, content: @Composable () -> Unit) {
    Layout(content = content) { measurables, constraints ->
      val placeable = measurables.single().measure(
        constraints.copy(minHeight = 0, maxHeight = maxHeight),
      )
      layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }
  }
}
