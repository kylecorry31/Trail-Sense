package com.kylecorry.trail_sense.tools.offline_maps.ui.mappers

import android.content.Context
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.sense.location.IGPS
import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.andromeda.views.list.ListItemMapper
import com.kylecorry.andromeda.views.list.ListItemTag
import com.kylecorry.andromeda.views.list.ListMenuItem
import com.kylecorry.andromeda.views.list.ResourceListIcon
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.CustomUiUtils.getPrimaryColor
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps.VectorMap

class VectorMapListItemMapper(
    private val gps: IGPS,
    private val context: Context,
    private val actionHandler: (VectorMap, VectorMapAction) -> Unit
) : ListItemMapper<VectorMap> {

    private val formatter = getAppService<FormatService>()

    override fun map(value: VectorMap): ListItem {
        return ListItem(
            value.id + 10000,
            value.name,
            icon = ResourceListIcon(R.drawable.maps, AppColor.Gray.color, size = 48f, foregroundSize = 24f),
            subtitle = formatter.join(
                formatter.formatDate(value.createdOn.toZonedDateTime(), includeWeekDay = false),
                formatter.formatFileSize(value.sizeBytes),
                separator = FormatService.Separator.Dot
            ),
            tags = getTags(value),
            trailingIcon = ResourceListIcon(
                if (value.visible) {
                    R.drawable.ic_visible
                } else {
                    R.drawable.ic_not_visible
                },
                Resources.androidTextColorSecondary(context),
                onClick = {
                    actionHandler(value, VectorMapAction.ToggleVisibility)
                }
            ),
            menu = getMenu(value)
        ) {
            actionHandler(value, VectorMapAction.View)
        }
    }

    private fun getTags(value: VectorMap): List<ListItemTag> {
        val onMap = value.bounds?.contains(gps.location) ?: false
        return listOfNotNull(
            if (onMap) {
                ListItemTag(
                    context.getString(R.string.on_map),
                    null,
                    Resources.getPrimaryColor(context)
                )
            } else {
                null
            },
            ListItemTag(
                context.getString(R.string.map_type_trail),
                null,
                Resources.androidTextColorSecondary(context)
            ),
            if (value.isExternal) {
                ListItemTag(
                    context.getString(R.string.map_external),
                    null,
                    Resources.androidTextColorSecondary(context)
                )
            } else {
                null
            },
            if (!value.isAvailable) {
                ListItemTag(
                    context.getString(R.string.map_file_missing),
                    null,
                    AppColor.Red.color
                )
            } else {
                null
            }
        )
    }

    private fun getMenu(value: VectorMap): List<ListMenuItem> {
        return listOfNotNull(
            ListMenuItem(context.getString(R.string.rename)) {
                actionHandler(value, VectorMapAction.Rename)
            },
            ListMenuItem(context.getString(R.string.attribution)) {
                actionHandler(value, VectorMapAction.EditAttribution)
            },
            ListMenuItem(context.getString(R.string.move_to)) {
                actionHandler(value, VectorMapAction.Move)
            },
            if (value.isExternal && value.isAvailable) {
                ListMenuItem(context.getString(R.string.copy_to_trail_sense)) {
                    actionHandler(value, VectorMapAction.CopyToAppStorage)
                }
            } else {
                null
            },
            ListMenuItem(context.getString(R.string.delete)) {
                actionHandler(value, VectorMapAction.Delete)
            },
        )
    }
}
