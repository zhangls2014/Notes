package me.zhangls.about

import androidx.compose.material3.adaptive.HingeInfo
import androidx.compose.material3.adaptive.Posture
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReleaseHistoryHingeTest {
  private val viewport = Rect(16f, 80f, 1184f, 1000f)
  private val hinge = Rect(360f, 0f, 380f, 1000f)

  @Test fun onlyReportedFlatHingesMeanFullyOpen() {
    assertFalse(Posture().isFullyOpen)
    assertTrue(Posture(hingeList = listOf(HingeInfo(hinge, true, true, false, false))).isFullyOpen)
    assertFalse(Posture(hingeList = listOf(HingeInfo(hinge, false, true, true, false))).isFullyOpen)
  }

  @Test fun asymmetricHingePreservesPhysicalPositionAndGap() {
    assertEquals(
      listOf(ReleaseHistoryLane(0, 336), ReleaseHistoryLane(372, 796)),
      releaseHistoryLanes(viewport, listOf(hinge), 16, LayoutDirection.Ltr),
    )
  }

  @Test fun rtlReversesReadingOrderWithoutMovingPhysicalHinge() {
    assertEquals(
      listOf(ReleaseHistoryLane(0, 796), ReleaseHistoryLane(832, 336)),
      releaseHistoryLanes(viewport, listOf(hinge), 16, LayoutDirection.Rtl),
    )
  }

  @Test fun cellSizingConvertsPaddingAndGapToPhysicalPixels() {
    val columns = ReleaseHistoryHingeColumns(
      Rect(0f, 160f, 2400f, 2000f),
      listOf(Rect(720f, 0f, 760f, 2000f)),
      16.dp,
      16.dp,
      LayoutDirection.Ltr,
    )
    with(columns) {
      with(Density(2f)) {
        assertEquals(listOf(672, 1592), calculateCrossAxisCellSizes(2336, 32).toList())
        val positions = IntArray(2)
        arrange(2336, intArrayOf(672, 1592), LayoutDirection.Ltr, positions)
        assertEquals(listOf(0, 744), positions.toList())
      }
    }
  }

  @Test fun multipleHingesCreateThreeLanesAndOutsideHingesDoNotSplit() {
    val lanes = releaseHistoryLanes(viewport, listOf(hinge, Rect(800f, 0f, 800f, 1000f)), 16, LayoutDirection.Ltr)
    assertEquals(3, lanes.size)
    assertEquals(ReleaseHistoryLane(792, 376), lanes.last())
    assertEquals(
      listOf(ReleaseHistoryLane(0, 1168)),
      releaseHistoryLanes(viewport, listOf(Rect(0f, 0f, 0f, 1000f)), 16, LayoutDirection.Ltr),
    )
  }
}
