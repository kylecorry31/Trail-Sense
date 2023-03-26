package com.kylecorry.trail_sense.tools.maps.ui.mappers

import android.content.Context
import android.graphics.Bitmap
import android.util.Size
import androidx.lifecycle.LifecycleOwner
import com.kylecorry.andromeda.core.bitmap.BitmapUtils.rotate
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.location.IGPS
import com.kylecorry.andromeda.print.Printer
import com.kylecorry.ceres.list.*
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.maps.domain.PhotoMap

class MapMapper(
    private val gps: IGPS,
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val actionHandler: (PhotoMap, MapAction) -> Unit
) : ListItemMapper<PhotoMap> {

    private val prefs = UserPreferences(context)
    private val formatter = FormatService.getInstance(context)
    private val files = FileSubsystem.getInstance(context)

    override fun map(value: PhotoMap): ListItem {
        val onMap = value.boundary()?.contains(gps.location) ?: false
        val icon = if (prefs.navigation.showMapPreviews) {
            AsyncListIcon(
                lifecycleOwner,
                { loadMapThumbnail(value) },
                size = 48f,
                clearOnPause = true
            )
        } else {
            ResourceListIcon(R.drawable.maps, AppColor.Gray.color, size = 48f, foregroundSize = 24f)
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
            menu = listOfNotNull(
                ListMenuItem(context.getString(R.string.rename)) {
                    actionHandler(value, MapAction.Rename)
                },
                ListMenuItem(context.getString(R.string.move_to)) {
                    actionHandler(value, MapAction.Move)
                },
                ListMenuItem(context.getString(R.string.change_resolution)) {
                    actionHandler(value, MapAction.Resize)
                },
                ListMenuItem(context.getString(R.string.export)) {
                    actionHandler(value, MapAction.Export)
                },
                if (!Printer.canPrint()) {
                    null
                } else {
                    ListMenuItem(context.getString(R.string.print)) {
                        actionHandler(value, MapAction.Print)
                    }
                },
                ListMenuItem(context.getString(R.string.delete)) {
                    actionHandler(value, MapAction.Delete)
                },
            )
        ) {
            actionHandler(value, MapAction.View)
        }
    }

    private suspend fun loadMapThumbnail(map: PhotoMap): Bitmap = onIO {
        val size = Resources.dp(context, 48f).toInt()
        val bitmap = try {
            files.bitmap(map.filename, Size(size, size)) ?: getDefaultMapThumbnail()
        } catch (e: Exception) {
            getDefaultMapThumbnail()
        }

        if (map.calibration.rotation != 0) {
            val rotated = bitmap.rotate(map.calibration.rotation.toFloat())
            bitmap.recycle()
            return@onIO rotated
        }

        bitmap
    }

    private fun getDefaultMapThumbnail(): Bitmap {
        val size = Resources.dp(context, 48f).toInt()
        return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    }

}