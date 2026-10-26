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
  @Test fun allPageSizesFinishAtEightyFivePercent() {
    for (size in listOf(Size(320f, 736f), Size(400f, 800f), Size(800f, 400f), Size(1200f, 800f))) {
      val pose = PageMotionGeometry.transform(size, 1f)
      assertEquals(0.85f, pose.scaleX, 0.001f)
      assertEquals(0.85f, pose.scaleY, 0.001f)
      assertEquals(size.width * 0.075f, pose.translationX, 0.001f)
      assertEquals(size.height * 0.075f, pose.translationY, 0.001f)
    }
  }

  @Test fun halfProgressUsesNinetyTwoPointFivePercentWithoutDistortion() {
    val pose = PageMotionGeometry.transform(Size(800f, 400f), 0.5f)
    assertEquals(0.925f, pose.scaleX, 0.001f)
    assertEquals(0.925f, pose.scaleY, 0.001f)
  }

  @Test fun pageCenterStaysFixedAndCornersFollowVisiblePage() {
    val pose = PageMotionGeometry.transform(Size(400f, 736f), 1f)
    assertEquals(368f, 368f * pose.scaleY + pose.translationY, 0.001f)
    assertEquals(200f, 200f * pose.scaleX + pose.translationX, 0.001f)
    val outline = DeviceCornerShape(DeviceCorners(20f, 30f, 40f, 50f),
      pose.scaleX, pose.scaleX)
      .createOutline(Size(400f, 736f), LayoutDirection.Ltr, Density(1f)) as Outline.Rounded
    assertEquals(736f, outline.roundRect.bottom, 0.001f)
    assertEquals(40f, outline.roundRect.bottomRightCornerRadius.y * pose.scaleX, 0.001f)
  }

  @Test fun oppositeEdgeAnchorsStayFixedAtEveryProgress() {
    val size = Size(800f, 400f)
    for (progress in listOf(0f, 0.25f, 0.5f, 1f)) {
      val right = PageMotionGeometry.transform(size, progress, anchorX = 0.75f)
      assertEquals(600f, size.width * 0.75f * right.scaleX + right.translationX, 0.001f)
      val left = PageMotionGeometry.transform(size, progress, anchorX = 0.25f)
      assertEquals(200f, size.width * 0.25f * left.scaleX + left.translationX, 0.001f)
      assertEquals(200f, size.height / 2f * left.scaleY + left.translationY, 0.001f)
    }
  }
}
