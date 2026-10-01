package me.zhangls.entry

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Test

@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [37])
class DeviceCornerShapeTest {
  @Test fun physicalCornersRemainDistinctInRtlAndCompensateEachAxis() {
    val shape = DeviceCornerShape(DeviceCorners(10f, 20f, 30f, 40f), 0.88f, 0.97f)
    val outline = shape.createOutline(Size(400f, 800f), LayoutDirection.Rtl, Density(1f)) as Outline.Rounded
    assertEquals(10f, outline.roundRect.topLeftCornerRadius.x * 0.88f, 0.001f)
    assertEquals(20f, outline.roundRect.topRightCornerRadius.x * 0.88f, 0.001f)
    assertEquals(30f, outline.roundRect.bottomRightCornerRadius.x * 0.88f, 0.001f)
    assertEquals(40f, outline.roundRect.bottomLeftCornerRadius.x * 0.88f, 0.001f)
    assertEquals(10f, outline.roundRect.topLeftCornerRadius.y * 0.97f, 0.001f)
    assertEquals(40f, outline.roundRect.bottomLeftCornerRadius.y * 0.97f, 0.001f)
  }

  @Test fun displayedRadiusStaysFixedThroughoutForwardAndReverseGesture() {
    val pageSize = Size(400f, 736f)
    for (progress in listOf(0f, 0.1f, 0.5f, 1f, 0.5f, 0.1f, 0f)) {
      val pose = PageMotionGeometry.transform(pageSize, progress, 48f, 24f)
      val outline = DeviceCornerShape(DeviceCorners(10f, 20f, 30f, 40f), pose.scaleX, pose.scaleX)
        .createOutline(pageSize, LayoutDirection.Ltr, Density(1f)) as Outline.Rounded
      val radii = listOf(outline.roundRect.topLeftCornerRadius, outline.roundRect.topRightCornerRadius,
        outline.roundRect.bottomRightCornerRadius, outline.roundRect.bottomLeftCornerRadius)
      for ((radius, expected) in radii.zip(listOf(10f, 20f, 30f, 40f))) {
        assertEquals("Displayed X radius at progress=$progress", expected, radius.x * pose.scaleX, 0.001f)
        assertEquals("Displayed Y radius at progress=$progress", expected, radius.y * pose.scaleX, 0.001f)
      }
    }
  }
  @Test fun restorationReturnsCornersToOriginalContourWithoutChangingScale() {
    for (visibility in listOf(1f, 0.5f, 0f)) {
      val outline = DeviceCornerShape(DeviceCorners(10f, 20f, 30f, 40f), 0.88f, 0.88f, visibility)
        .createOutline(Size(400f, 736f), LayoutDirection.Ltr, Density(1f)) as Outline.Rounded
      assertEquals(40f * visibility, outline.roundRect.bottomLeftCornerRadius.x * 0.88f, 0.001f)
      assertEquals(40f * visibility, outline.roundRect.bottomLeftCornerRadius.y * 0.88f, 0.001f)
    }
  }

}
