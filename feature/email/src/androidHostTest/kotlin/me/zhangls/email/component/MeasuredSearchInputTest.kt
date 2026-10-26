package me.zhangls.email.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import me.zhangls.email.search.MeasuredSearchInput
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37], qualifiers = "en-rUS-w600dp-h800dp-mdpi")
class MeasuredSearchInputTest {
  @get:Rule val compose = createComposeRule()

  @Test fun remeasurementKeepsDisplayedInputStateIndependentOfMeasurement() {
    var measurementWidth by mutableIntStateOf(60)
    var inputInstances = 0
    compose.setContent {
      MeasuredSearchInput(
        measurementKey = measurementWidth,
        measurementField = { Box(Modifier.size(measurementWidth.dp, 32.dp)) },
        inputField = {
          val instance = remember { ++inputInstances }
          Box(Modifier.height(32.dp).testTag("input")) {
            Text(instance.toString(), modifier = Modifier.testTag("instance"))
          }
        },
      )
    }
    compose.onNodeWithTag("input").assertWidthIsEqualTo(60.dp)
    compose.onNodeWithTag("instance").assertTextEquals("1")

    compose.runOnIdle { measurementWidth = 128 }
    compose.onNodeWithTag("input").assertWidthIsEqualTo(128.dp)
    compose.onNodeWithTag("instance").assertTextEquals("1")
  }
}
