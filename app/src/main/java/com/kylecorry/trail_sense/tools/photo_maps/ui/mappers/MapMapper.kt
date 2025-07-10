package com.kylecorry.trail_sense.tools.photo_maps.ui.mappers

import android.content.Context
import android.graphics.Bitmap
import android.util.Size
import androidx.lifecycle.LifecycleOwner
import com.kylecorry.andromeda.bitmaps.BitmapUtils.rotate
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.print.Printer
import com.kylecorry.andromeda.sense.location.IGPS
import com.kylecorry.andromeda.views.list.AsyncListIcon
import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.andromeda.views.list.ListItemMapper
import com.kylecorry.andromeda.views.list.ListItemTag
import com.kylecorry.andromeda.views.list.ListMenuItem
import com.kylecorry.andromeda.views.list.ResourceListIcon
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils.getPrimaryColor
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.photo_maps.domain.PhotoMap

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
        val icon = if (prefs.photoMaps.showMapPreviews) {
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
                        Resources.getPrimaryColor(context)
                    )
                } else {
                    null
                }
            ),
            trailingIcon = ResourceListIcon(
                if (value.visible) {
                    R.drawable.ic_visible
                } else {
                    R.drawable.ic_not_visible
                },
                Resources.androidTextColorSecondary(context),
                onClick = {
                    actionHandler(value, MapAction.ToggleVisibility)
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

        val rotation = if (prefs.photoMaps.keepMapFacingUp) map.baseRotation().toFloat() else map.calibration.rotation

        if (rotation != 0f) {
            val rotated = bitmap.rotate(rotation)
            if (rotated != bitmap) {
                bitmap.recycle()
            }
            return@onIO rotated
        }

        bitmap
    }

    private fun getDefaultMapThumbnail(): Bitmap {
        val size = Resources.dp(context, 48f).toInt()
        return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    }

}