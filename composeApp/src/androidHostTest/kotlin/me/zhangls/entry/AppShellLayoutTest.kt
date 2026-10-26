package me.zhangls.entry

import androidx.compose.ui.layout.Layout
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import me.zhangls.main.AppShell
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37], qualifiers = "w400dp-h800dp-mdpi")
class AppShellLayoutTest {
  private val compose = createComposeRule()

  @get:Rule val rules = drainMainLooperAfter(compose)

  @Test
  fun shellWithoutNavigationPreservesTightChildConstraints() {
    assertChildConstraints(Constraints.fixed(200, 100), IntSize(200, 100))
  }

  @Test
  fun shellWithoutNavigationPreservesLooseChildConstraintsAndNaturalSize() {
    assertChildConstraints(
      Constraints(minWidth = 20, maxWidth = 200, minHeight = 30, maxHeight = 100),
      IntSize(80, 60),
    )
  }

  private fun assertChildConstraints(requested: Constraints, expectedSize: IntSize) {
    var received: Constraints? = null
    var measured: IntSize? = null
    compose.setContent {
      Layout(content = {
        AppShell(selected = null, onSelectTab = {}) {
          Layout(content = {}) { _, childConstraints ->
            received = childConstraints
            val width = childConstraints.constrainWidth(80)
            val height = childConstraints.constrainHeight(60)
            measured = IntSize(width, height)
            layout(width, height) {}
          }
        }
      }) { measurables, _ ->
        val child = measurables.single().measure(requested)
        layout(240, 140) { child.place(0, 0) }
      }
    }
    compose.runOnIdle {
      assertEquals(requested, received)
      assertEquals(expectedSize, measured)
    }
  }
}
