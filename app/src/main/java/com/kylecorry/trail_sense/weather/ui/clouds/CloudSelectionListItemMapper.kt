package com.kylecorry.trail_sense.weather.ui.clouds

import android.content.Context
import com.kylecorry.ceres.list.*
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.weather.infrastructure.clouds.CloudDetailsService


internal class CloudSelectionListItemMapper(
    private val context: Context,
    private val onSelectionChanged: (CloudGenus?, Boolean) -> Unit
) : ListItemMapper<CloudSelection> {
    private val details = CloudDetailsService(context)
    private val formatter = FormatService(context)
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
                    ResourceListIcon(R.drawable.ic_help)
                ),
                if (unreliable.contains(value.genus)) {
                    ListItemData(
                        context.getString(R.string.experimental),
                        ResourceListIcon(R.drawable.ic_experimental)
                    )
                } else null
            ) else emptyList(),
            icon = ResourceListIcon(
                details.getCloudImage(value.genus),
                if (value.genus == null) AppColor.Blue.color else null,
                size = 48f,
                backgroundId = R.drawable.rounded_rectangle,
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

