package com.kylecorry.trail_sense.tools.clouds.ui

import android.content.Context
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.andromeda.views.list.ListItemCheckbox
import com.kylecorry.andromeda.views.list.ListItemData
import com.kylecorry.andromeda.views.list.ListItemMapper
import com.kylecorry.andromeda.views.list.ResourceListIcon
import com.kylecorry.andromeda.views.list.DrawableListIcon
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.tools.clouds.infrastructure.CloudDetailsService


internal class CloudSelectionListItemMapper(
    private val context: Context,
    private val onSelectionChanged: (CloudGenus?, Boolean) -> Unit
) : ListItemMapper<CloudSelection> {
    private val details = CloudDetailsService(context)
    private val formatter = FormatService.getInstance(context)
    private val imageModal = CloudImageModal(context)
    private val detailsModal = CloudDetailsModal(context)

    // Clouds with limited training data / low accuracy
    private val unreliable =
        listOf(
            CloudGenus.Cumulonimbus,
            CloudGenus.Altostratus,
            CloudGenus.Stratus,
            CloudGenus.Cirrostratus,
            null
        )

    override fun map(value: CloudSelection): ListItem {
        return ListItem(
            value.genus?.ordinal?.toLong() ?: -1L,
            details.getCloudName(value.genus),
            details.getCloudDescription(value.genus),
            data = if (value.confidence != null) listOfNotNull(
                ListItemData(
                    formatter.formatPercentage(
                        value.confidence * 100,
                        0
                    ),
                    ResourceListIcon(
                        R.drawable.ic_help,
                        Resources.androidTextColorSecondary(context)
                    )
                ),
                if (unreliable.contains(value.genus)) {
                    ListItemData(
                        context.getString(R.string.experimental),
                        ResourceListIcon(
                            R.drawable.ic_experimental,
                            Resources.androidTextColorSecondary(context)
                        )
                    )
                } else null
            ) else emptyList(),
            icon = DrawableListIcon(
                details.getCloudImage(context, value.genus),
                size = 48f,
                backgroundDrawable = Resources.drawable(context, R.drawable.rounded_rectangle),
                clipToBackground = true
            ) {
                imageModal.show(value.genus)
            },
            checkbox = ListItemCheckbox(value.isSelected) {
                onSelectionChanged(value.genus, !value.isSelected)
            }
        ) {
            detailsModal.show(value.genus)
        }
    }

}

