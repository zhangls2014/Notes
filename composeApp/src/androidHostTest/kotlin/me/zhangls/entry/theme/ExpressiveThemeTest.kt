package me.zhangls.entry.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.junit4.createComposeRule
import me.zhangls.theme.ComposeAppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Settings changes must survive the shared theme's Expressive migration. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37])
class ExpressiveThemeTest {
  @get:Rule val compose = createComposeRule()

  @Test fun systemFontScaleIsPreservedAndAppSizeIsRelative() {
    var systemScale by mutableStateOf(1.3f)
    var appScale by mutableStateOf(1f)
    var observedScale = 0f
    var observedDensity = 0f
    compose.setContent {
      CompositionLocalProvider(LocalDensity provides Density(2f, systemScale)) {
        ComposeAppTheme(fontScale = appScale) {
          val density = LocalDensity.current
          SideEffect {
            observedScale = density.fontScale
            observedDensity = density.density
          }
        }
      }
    }
    compose.runOnIdle {
      assertEquals(1.3f, observedScale, 0.001f)
      assertEquals(2f, observedDensity, 0.001f)
      appScale = 1.5f
    }
    compose.runOnIdle {
      assertEquals(1.95f, observedScale, 0.001f)
      systemScale = 2f
    }
    compose.runOnIdle {
      assertEquals(3f, observedScale, 0.001f)
      appScale = 1f
    }
    compose.runOnIdle { assertEquals(2f, observedScale, 0.001f) }
  }

  @Test fun themeSwitchesPreserveDynamicColorsAndUserFontScale() {
    var dark by mutableStateOf(false)
    var dynamic by mutableStateOf<ColorScheme?>(null)
    var scale by mutableStateOf(1f)
    lateinit var colors: ColorScheme
    var observedScale = 0f
    compose.setContent {
      ComposeAppTheme(darkTheme = dark, dynamicScheme = dynamic, fontScale = scale) {
        val currentColors = MaterialTheme.colorScheme
        val currentScale = LocalDensity.current.fontScale
        SideEffect { colors = currentColors; observedScale = currentScale }
      }
    }
    lateinit var builtInLight: ColorScheme
    compose.runOnIdle { builtInLight = colors }
    compose.runOnIdle { dark = true; scale = 1.5f }
    compose.runOnIdle {
      assertNotEquals(builtInLight.surface, colors.surface)
      assertEquals(1.5f, observedScale)
      dynamic = darkColorScheme(primary = Color.Magenta, surface = Color.Black)
    }
    compose.runOnIdle {
      assertEquals(Color.Magenta, colors.primary)
      assertEquals(Color.Black, colors.surface)
      dark = false
      scale = 2f
      dynamic = lightColorScheme(primary = Color.Blue, surface = Color.White)
    }
    compose.runOnIdle {
      assertEquals(Color.Blue, colors.primary)
      assertEquals(Color.White, colors.surface)
      assertEquals(2f, observedScale)
      dynamic = null
    }
    compose.runOnIdle { assertEquals(builtInLight, colors) }
  }

  @Test fun actionLabelsAndTitlesHaveEmphasisWithoutEnlargingBodyText() {
    lateinit var typography: Typography
    compose.setContent {
      ComposeAppTheme {
        val current = MaterialTheme.typography
        SideEffect { typography = current }
      }
    }
    compose.runOnIdle {
      assertTrue("Action label weight: ${typography.labelLarge.fontWeight}", typography.labelLarge.fontWeight!!.weight >= 600)
      assertTrue("Title weight: ${typography.titleMedium.fontWeight}", typography.titleMedium.fontWeight!!.weight >= 600)
      assertEquals(Typography().bodyLarge.fontSize, typography.bodyLarge.fontSize)
      assertEquals(Typography().bodyMedium.fontSize, typography.bodyMedium.fontSize)
    }
  }
}
