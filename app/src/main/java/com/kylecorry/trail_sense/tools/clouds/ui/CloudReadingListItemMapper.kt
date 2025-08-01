package com.kylecorry.trail_sense.tools.clouds.ui

import android.content.Context
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.andromeda.views.list.ListItemData
import com.kylecorry.andromeda.views.list.ListItemMapper
import com.kylecorry.andromeda.views.list.ListMenuItem
import com.kylecorry.andromeda.views.list.ResourceListIcon
import com.kylecorry.andromeda.views.list.DrawableListIcon
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.tools.clouds.infrastructure.CloudDetailsService
import com.kylecorry.trail_sense.tools.clouds.infrastructure.persistence.CloudObservation


internal class CloudReadingListItemMapper(
    private val context: Context,
    private val onAction: (action: CloudReadingAction, reading: Reading<CloudObservation>) -> Unit
) :
    ListItemMapper<Reading<CloudObservation>> {
    private val details = CloudDetailsService(context)
    private val detailsModal = CloudDetailsModal(context)
    private val imageModal = CloudImageModal(context)
    private val formatter = FormatService.getInstance(context)

    override fun map(value: Reading<CloudObservation>): ListItem {
        val cloud = value.value.genus
        return ListItem(
            cloud?.ordinal?.toLong() ?: -1L,
            details.getCloudName(cloud),
            details.getCloudForecast(cloud),
            data = listOf(
                ListItemData(
                    formatter.formatDateTime(
                        value.time.toZonedDateTime(),
                        relative = true,
                        abbreviateMonth = true
                    ),
                    ResourceListIcon(
                        R.drawable.ic_tool_clock,
                        Resources.androidTextColorSecondary(context)
                    )
                )
            ),
            icon = DrawableListIcon(
                details.getCloudImage(context, cloud),
                size = 48f,
                backgroundDrawable = Resources.drawable(context, R.drawable.rounded_rectangle),
                clipToBackground = true
            ) {
                imageModal.show(cloud)
            },
            menu = listOf(
                ListMenuItem(context.getString(R.string.delete)) {
                    onAction(
                        CloudReadingAction.Delete,
                        value
                    )
                }
            )
        ) {
            detailsModal.show(cloud)
        }
    }

}