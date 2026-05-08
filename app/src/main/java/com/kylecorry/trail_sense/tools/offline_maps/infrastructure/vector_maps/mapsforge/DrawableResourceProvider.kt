package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.mapsforge

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
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
        val drawable = getDrawable(source) ?: return null
        val bitmap = drawable.toBitmap()
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return ByteArrayInputStream(outputStream.toByteArray())
    }

    private fun getDrawable(source: String?): Drawable? {
        val (resourceId, color) = getResourceId(source) ?: return null
        return Resources.drawable(context, resourceId)?.also { drawable ->
            color?.let { drawable.setTint(it) }
        }
    }

    private fun getResourceId(source: String?): Pair<Int, Int?>? {
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

        val actualResourceName = resourceName.substringBefore('#')
        val resourceId = context.resources.getIdentifier(actualResourceName, "drawable", context.packageName)
        return if (resourceId != 0) resourceId to color else null
    }
}
