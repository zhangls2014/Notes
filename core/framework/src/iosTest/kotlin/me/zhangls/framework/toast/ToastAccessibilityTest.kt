@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package me.zhangls.framework.toast

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import platform.UIKit.UIFontDescriptorTextStyleAttribute
import platform.UIKit.UIFontTextStyleSubheadline
import platform.UIKit.UIContentSizeCategoryLarge
import platform.UIKit.UIContentSizeCategoryAccessibilityExtraExtraExtraLarge

class ToastAccessibilityTest {
  @Test
  fun toastFontActuallyGrowsWhenItsContentSizeCategoryChanges() {
    val label = ToastViewController("Saved").label
    label.traitOverrides.preferredContentSizeCategory = UIContentSizeCategoryLarge
    label.updateTraitsIfNeeded()
    val initialSize = label.font.pointSize
    label.traitOverrides.preferredContentSizeCategory = UIContentSizeCategoryAccessibilityExtraExtraExtraLarge
    label.updateTraitsIfNeeded()
    assertTrue(label.font.pointSize > initialSize, "Accessibility text size must increase the Toast font")
  }

  @Test
  fun toastLabelUsesASystemTextStyleAndTracksContentSizeChanges() {
    val controller = ToastViewController("Unable to save. Please try again.")
    val label = controller.label
    assertTrue(label.adjustsFontForContentSizeCategory, "Toast must track Dynamic Type changes")
    assertEquals(
      UIFontTextStyleSubheadline,
      label.font.fontDescriptor.objectForKey(UIFontDescriptorTextStyleAttribute),
    )
    assertEquals("Unable to save. Please try again.", label.text)
    assertEquals(0L, label.numberOfLines)
  }
}
