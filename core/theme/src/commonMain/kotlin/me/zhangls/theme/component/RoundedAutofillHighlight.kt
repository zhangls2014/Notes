package me.zhangls.theme.component

import androidx.compose.foundation.text.LocalAutofillHighlightBrush
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import kotlin.math.ceil

/**
 * Draws the current Compose autofill highlight within [shape] for fields in [content].
 *
 * Use the same shape as the field container. The outline is sized to the highlight area
 * supplied by Compose, which may include supporting text for some fields.
 */
@Composable
fun RoundedAutofillHighlight(shape: Shape, content: @Composable () -> Unit) {
  val highlight = LocalAutofillHighlightBrush.current
  val density = LocalDensity.current
  val direction = LocalLayoutDirection.current
  val roundedHighlight = remember(highlight, shape, density, direction) {
    // Compose draws the autofill brush over the text field after its decoration.
    object : ShaderBrush() {
      override fun createShader(size: Size): Shader {
        val image = ImageBitmap(ceil(size.width).toInt(), ceil(size.height).toInt())
        val outline = shape.createOutline(size, direction, density)
        CanvasDrawScope().draw(density, direction, Canvas(image), size) {
          drawOutline(outline, highlight)
        }
        return ImageShader(image)
      }
    }
  }
  CompositionLocalProvider(LocalAutofillHighlightBrush provides roundedHighlight, content = content)
}
