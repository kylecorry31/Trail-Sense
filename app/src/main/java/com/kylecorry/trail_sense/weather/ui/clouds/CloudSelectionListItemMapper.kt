package com.kylecorry.trail_sense.weather.ui.clouds

import android.content.Context
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.ceres.list.*
import com.kylecorry.sol.science.meteorology.Precipitation
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.weather.domain.clouds.CloudService
import com.kylecorry.trail_sense.weather.infrastructure.clouds.CloudDetailsService

internal data class CloudSelection(
    val genus: CloudGenus?,
    val confidence: Float?,
    val isSelected: Boolean
)


internal class CloudSelectionListItemMapper(
    private val context: Context,
    private val onSelectionChanged: (CloudGenus?, Boolean) -> Unit
) : ListItemMapper<CloudSelection> {
    private val repo: CloudDetailsService = CloudDetailsService(context)
    private val cloudService: CloudService = CloudService()
    private val formatter = FormatService(context)

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
            repo.getCloudName(value.genus),
            repo.getCloudDescription(value.genus),
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
            icon = ClippedResourceListIcon(
                repo.getCloudImage(value.genus),
                if (value.genus == null) AppColor.Blue.color else null,
                size = 48f,
                background = R.drawable.rounded_rectangle
            ) {
                if (value.genus != null) {
                    Alerts.image(
                        context,
                        repo.getCloudName(value.genus),
                        repo.getCloudImage(value.genus)
                    )
                }
            },
            checkbox = ListItemCheckbox(value.isSelected) {
                onSelectionChanged(value.genus, !value.isSelected)
            }
        ) {
            val precipitation = cloudService.getPrecipitation(value.genus)
            Alerts.dialog(
                context,
                repo.getCloudName(value.genus),
                repo.getCloudDescription(value.genus) + "\n\n" +
                        repo.getCloudForecast(value.genus) + "\n\n" +
                        getPrecipitationDescription(
                            context,
                            value.genus,
                            precipitation,
                            formatter
                        ),
                cancelText = null
            )
        }
    }

    private fun getPrecipitationDescription(
        context: Context,
        type: CloudGenus?,
        precipitation: List<Precipitation>,
        formatter: FormatService
    ): String {
        return context.getString(
            R.string.precipitation_chance,
            formatter.formatProbability(cloudService.getPrecipitationProbability(type))
        ) + "\n\n" +
                if (precipitation.isEmpty()) context.getString(R.string.precipitation_none) else precipitation.joinToString(
                    "\n"
                ) { formatter.formatPrecipitation(it) }
    }
}

