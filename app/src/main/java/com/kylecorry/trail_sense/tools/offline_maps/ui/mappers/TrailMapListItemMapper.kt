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
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapState
import com.kylecorry.trail_sense.tools.offline_maps.domain.trail_maps.TrailMap

class TrailMapListItemMapper(
    private val gps: IGPS,
    private val context: Context,
    private val actionHandler: (TrailMap, TrailMapAction) -> Unit
) : ListItemMapper<TrailMap> {

    private val formatter = getAppService<FormatService>()

    override fun map(value: TrailMap): ListItem {
        return ListItem(
            value.id + 10000,
            value.name,
            icon = ResourceListIcon(R.drawable.maps, AppColor.Gray.color, size = 48f, foregroundSize = 24f),
            subtitle = formatter.join(
                formatter.formatDate(value.createdOn.toZonedDateTime(), includeWeekDay = false),
                formatter.formatFileSize(value.fileSizeBytes),
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
                    actionHandler(value, TrailMapAction.ToggleVisibility)
                }
            ),
            menu = getMenu(value)
        ) {
            actionHandler(value, TrailMapAction.View)
        }
    }

    private fun getTags(value: TrailMap): List<ListItemTag> {
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
            if (value.state == OfflineMapState.Draft) {
                ListItemTag(
                    context.getString(R.string.draft),
                    null,
                    AppColor.Yellow.color
                )
            } else {
                null
            },
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

    private fun getMenu(value: TrailMap): List<ListMenuItem> {
        return listOfNotNull(
            ListMenuItem(context.getString(R.string.rename)) {
                actionHandler(value, TrailMapAction.Rename)
            },
            ListMenuItem(context.getString(R.string.attribution)) {
                actionHandler(value, TrailMapAction.EditAttribution)
            },
            ListMenuItem(context.getString(R.string.move_to)) {
                actionHandler(value, TrailMapAction.Move)
            },
            if (value.isExternal && value.isAvailable) {
                ListMenuItem(context.getString(R.string.copy_to_trail_sense)) {
                    actionHandler(value, TrailMapAction.CopyToAppStorage)
                }
            } else {
                null
            },
            ListMenuItem(context.getString(R.string.delete)) {
                actionHandler(value, TrailMapAction.Delete)
            },
        )
    }
}
