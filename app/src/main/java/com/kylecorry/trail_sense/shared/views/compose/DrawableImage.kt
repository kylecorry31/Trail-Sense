package com.kylecorry.trail_sense.shared.views.compose

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toDrawable
import kotlin.math.roundToInt

@Composable
fun DrawableImage(
    drawable: Drawable,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Fit
) {
    val painter = remember(drawable) {
        DrawablePainter(drawable)
    }
    Image(
        painter = painter,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun DrawableImagePreview() {
    DrawableImage(
        drawable = Color.Blue.toArgb().toDrawable(),
        contentDescription = "Blue box",
        modifier = Modifier.size(64.dp)
    )
}

private class DrawablePainter(
    private val drawable: Drawable
) : Painter() {

    override val intrinsicSize: Size
        get() {
            val width = drawable.intrinsicWidth
            val height = drawable.intrinsicHeight
            return if (width > 0 && height > 0) {
                Size(width.toFloat(), height.toFloat())
            } else {
                Size.Unspecified
            }
        }

    override fun DrawScope.onDraw() {
        drawIntoCanvas { canvas ->
            drawable.setBounds(
                0,
                0,
                size.width.roundToInt(),
                size.height.roundToInt()
            )
            drawable.draw(canvas.nativeCanvas)
        }
    }
}
