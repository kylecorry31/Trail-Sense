package com.kylecorry.trail_sense.tools.maps.ui.mappers

import android.content.Context
import android.graphics.Bitmap
import android.util.Size
import androidx.lifecycle.LifecycleOwner
import com.kylecorry.andromeda.core.bitmap.BitmapUtils.rotate
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.location.IGPS
import com.kylecorry.ceres.list.*
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.maps.domain.Map

class MapMapper(
    private val gps: IGPS,
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val actionHandler: (Map, MapAction) -> Unit
) : ListItemMapper<Map> {

    private val prefs = UserPreferences(context)
    private val formatter = FormatService.getInstance(context)

    override fun map(value: Map): ListItem {
        val onMap = value.boundary()?.contains(gps.location) ?: false
        val icon = if (prefs.navigation.showMapPreviews) {
            AsyncListIcon(
                lifecycleOwner,
                { loadMapThumbnail(value) },
                size = 48f,
                clearOnPause = true
            )
        } else {
            null
        }

        return ListItem(
            value.id,
            value.name,
            icon = icon,
            subtitle = formatter.formatFileSize(value.metadata.fileSize),
            tags = listOfNotNull(
                if (onMap) {
                    ListItemTag(
                        context.getString(R.string.on_map),
                        null,
                        AppColor.Orange.color
                    )
                } else {
                    null
                }
            ),
            menu = listOf(
                ListMenuItem(context.getString(R.string.rename)) {
                    actionHandler(value, MapAction.Rename)
                },
                ListMenuItem(context.getString(R.string.change_resolution)) {
                    actionHandler(value, MapAction.Resize)
                },
                ListMenuItem(context.getString(R.string.export)) {
                    actionHandler(value, MapAction.Export)
                },
                ListMenuItem(context.getString(R.string.delete)) {
                    actionHandler(value, MapAction.Delete)
                },
            )
        ){
            actionHandler(value, MapAction.View)
        }
    }

    private suspend fun loadMapThumbnail(map: Map): Bitmap = onIO {
        val size = Resources.dp(context, 48f).toInt()
        val bitmap = try {
            FileSubsystem.getInstance(context).bitmap(map.filename, Size(size, size))
        } catch (e: Exception) {
            Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        }

        if (map.calibration.rotation != 0) {
            val rotated = bitmap.rotate(map.calibration.rotation.toFloat())
            bitmap.recycle()
            return@onIO rotated
        }

        bitmap
    }

}