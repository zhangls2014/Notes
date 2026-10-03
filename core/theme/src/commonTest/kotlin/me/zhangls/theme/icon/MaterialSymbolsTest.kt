package me.zhangls.theme.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.VectorPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class MaterialSymbolsTest {
  @Test
  fun allConvertedVectorsHavePathsAndBlackFill() {
    val icons = listOf(
      Icons.Rounded.AccountCircle,
      Icons.Rounded.ArrowBackIosNew,
      Icons.Rounded.Cancel,
      Icons.Rounded.Check,
      Icons.Rounded.Clear,
      Icons.Rounded.DarkMode,
      Icons.Rounded.Delete,
      Icons.Rounded.Edit,
      Icons.Rounded.ExitToApp,
      Icons.Rounded.Favorite,
      Icons.Rounded.FormatSize,
      Icons.Rounded.Home,
      Icons.Rounded.Info,
      Icons.Rounded.Language,
      Icons.Rounded.Lock,
      Icons.Rounded.Palette,
      Icons.Rounded.Refresh,
      Icons.Rounded.Search,
      Icons.Rounded.Settings,
      Icons.Rounded.Star,
      Icons.Rounded.StarFill,
      Icons.Rounded.Visibility,
      Icons.Rounded.VisibilityOff,
    )
    assertEquals(23, icons.size)
    assertEquals(icons.size, icons.map { it.name }.toSet().size)
    for (icon in icons) {
      assertEquals(960f, icon.viewportWidth, icon.name)
      assertEquals(960f, icon.viewportHeight, icon.name)
      assertTrue(icon.root.size > 0, icon.name)
      for (index in 0 until icon.root.size) {
        val path = assertIs<VectorPath>(icon.root[index], icon.name)
        assertTrue(path.pathData.isNotEmpty(), icon.name)
        assertEquals(SolidColor(Color.Black), path.fill, icon.name)
      }
    }
    assertNotEquals(
      assertIs<VectorPath>(Icons.Rounded.Star.root[0]).pathData,
      assertIs<VectorPath>(Icons.Rounded.StarFill.root[0]).pathData,
    )
  }
}
