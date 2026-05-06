package com.kylecorry.trail_sense.tools.map.infrastructure.mapsforge

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.toColorInt
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.tryOrDefault
import org.mapsforge.map.rendertheme.XmlThemeResourceProvider
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

class DrawableResourceProvider(private val context: Context) : XmlThemeResourceProvider {

    override fun createInputStream(relativePath: String?, source: String?): InputStream? {
        val (resourceName, color) = getResourceName(source) ?: return null

        val resourceId = context.resources.getIdentifier(resourceName, "drawable", context.packageName)
        if (resourceId == 0) {
            return null
        }

        val drawable = Resources.drawable(context, resourceId) ?: return null
        color?.let { drawable.setTint(it) }
        val bitmap = drawable.toBitmap()
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return ByteArrayInputStream(outputStream.toByteArray())
    }

    private fun getResourceName(source: String?): Pair<String, Int?>? {
        val resourceName = source?.substringAfter("@drawable/", "")
        if (resourceName.isNullOrEmpty()) {
            return null
        }

        val color = if (resourceName.contains('#')) {
            val hex = "#${resourceName.substringAfter('#')}"
            tryOrDefault(null) {
                hex.toColorInt()
            }
        } else {
            null
        }

        return resourceName.substringBefore('#') to color
    }
}
