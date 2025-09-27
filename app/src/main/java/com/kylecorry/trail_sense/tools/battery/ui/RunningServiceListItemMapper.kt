package com.kylecorry.trail_sense.tools.battery.ui

import android.content.Context
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.andromeda.views.list.ListItemMapper
import com.kylecorry.andromeda.views.list.ResourceListIcon
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.tools.battery.domain.RunningService
import java.time.Duration

class RunningServiceListItemMapper(
    private val context: Context,
    private val onStopService: (RunningService) -> Unit
) : ListItemMapper<RunningService> {

    val formatter = AppServiceRegistry.get<FormatService>()

    override fun map(value: RunningService): ListItem {
        val frequency = if (value.frequency == Duration.ZERO) {
            context.getString(R.string.always_on)
        } else {
            context.getString(
                R.string.service_update_frequency,
                formatter.formatDuration(value.frequency)
            )
        }

        return ListItem(
            value.name.hashCode().toLong(),
            value.name,
            context.getString(R.string.dash_separated_pair, frequency, getBatteryUsage(value)),
            trailingIcon = ResourceListIcon(
                R.drawable.ic_cancel,
                Resources.androidTextColorSecondary(context)
            ) {
                onStopService(value)
            }
        )
    }

    private fun getBatteryUsage(service: RunningService): String {
        val usage = when {
            service.frequency < Duration.ofMinutes(15) -> {
                context.getString(R.string.high)
            }

            service.frequency <= Duration.ofMinutes(25) -> {
                context.getString(R.string.moderate)
            }

            else -> {
                context.getString(R.string.low)
            }
        }
        return context.getString(R.string.battery_usage, usage)
    }
}