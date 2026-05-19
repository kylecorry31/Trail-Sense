package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.mapsforge

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import com.kylecorry.andromeda.canvas.CanvasDrawer
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.tryOrDefault
import org.mapsforge.map.rendertheme.XmlThemeResourceProvider
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

class DrawableResourceProvider(private val context: Context) : XmlThemeResourceProvider {

    private val sourcePattern = Regex("@drawable/(\\w+)(#[\\da-zA-Z]+)?(!)?")

    override fun createInputStream(relativePath: String?, source: String?): InputStream? {
        val bitmap = getDrawable(source) ?: return null
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return ByteArrayInputStream(outputStream.toByteArray())
    }

    private fun getDrawable(source: String?): Bitmap? {
        val (resourceId, color, isIconOnly) = getResourceId(source) ?: return null
        val size = Resources.dp(context, 24f).toInt()
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        val drawer = CanvasDrawer(context, canvas)
        val padding = if (isIconOnly) 0f else drawer.dp(1.5f)
        drawer.smooth()
        if (!isIconOnly) {
            drawer.noStroke()
            drawer.fill(color ?: Color.BLACK)
            drawer.rect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), drawer.dp(4f))
            drawer.tint(Color.WHITE)
        } else {
            color?.let { drawer.tint(it) }
        }
        val icon = drawer.loadImage(resourceId, size, size)
        drawer.image(icon, padding, padding, canvas.width - 2 * padding, canvas.height - 2 * padding)
        return bitmap
    }

    private fun getResourceId(source: String?): Triple<Int, Int?, Boolean>? {
        val matches = sourcePattern.find(source ?: "") ?: return null
        val resourceName = matches.groupValues[1]
        val color = tryOrDefault(null) {
            matches.groupValues[2].toColorInt()
        }
        val iconOnly = matches.groupValues[3].isNotEmpty()
        val resourceId = context.resources.getIdentifier(resourceName, "drawable", context.packageName)
        return if (resourceId != 0) Triple(resourceId, color, iconOnly) else null
    }
}
