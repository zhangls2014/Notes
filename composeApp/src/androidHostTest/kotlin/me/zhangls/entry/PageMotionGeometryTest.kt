package me.zhangls.entry

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37])
class PageMotionGeometryTest {
  @Test fun pageSizeDeterminesScaleRatherThanWindowSize() {
    val pose = PageMotionGeometry.transform(Size(320f, 736f), 1f, 48f, 24f)
    assertEquals(0.85f, pose.scaleX, 0.001f)
    assertEquals(1f - 24f / 736f, pose.scaleY, 0.001f)
    assertEquals(24f, pose.translationX, 0.001f)
    assertEquals(12f, pose.translationY, 0.001f)
  }

  @Test fun pageCenterStaysFixedAndCornersFollowVisiblePage() {
    val pose = PageMotionGeometry.transform(Size(400f, 736f), 1f, 48f, 24f)
    assertEquals(368f, 368f * pose.scaleY + pose.translationY, 0.001f)
    assertEquals(200f, 200f * pose.scaleX + pose.translationX, 0.001f)
    val outline = DeviceCornerShape(DeviceCorners(20f, 30f, 40f, 50f),
      pose.scaleX, pose.scaleX)
      .createOutline(Size(400f, 736f), LayoutDirection.Ltr, Density(1f)) as Outline.Rounded
    assertEquals(736f, outline.roundRect.bottom, 0.001f)
    assertEquals(40f, outline.roundRect.bottomRightCornerRadius.y * pose.scaleX, 0.001f)
  }
}
