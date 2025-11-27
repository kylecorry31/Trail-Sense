package com.kylecorry.trail_sense.tools.battery.ui

import android.content.Context
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.andromeda.views.list.ListItemMapper
import com.kylecorry.andromeda.views.list.ResourceListIcon
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.tools.battery.domain.BatteryUsage
import com.kylecorry.trail_sense.tools.battery.domain.RunningService
import java.time.Duration

class RunningServiceListItemMapper(
    private val context: Context,
    private val onStopService: (RunningService) -> Unit
) : ListItemMapper<RunningService> {

    private val formatter = AppServiceRegistry.get<FormatService>()

    override fun map(value: RunningService): ListItem {
        val batteryUsage = getBatteryUsage(value)
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
            context.getString(
                R.string.dash_separated_pair,
                frequency,
                formatBatteryUsage(batteryUsage)
            ),
            icon = ResourceListIcon(getUsageIcon(batteryUsage), tint = getUsageColor(batteryUsage)),
            trailingIcon = ResourceListIcon(
                R.drawable.ic_cancel,
                Resources.androidTextColorSecondary(context)
            ) {
                onStopService(value)
            }
        )
    }

    private fun getUsageIcon(usage: BatteryUsage): Int {
        return when (usage) {
            BatteryUsage.Unknown -> R.drawable.ic_help
            BatteryUsage.Low -> R.drawable.ic_check_outline
            BatteryUsage.Moderate -> R.drawable.ic_alert
            BatteryUsage.High -> R.drawable.ic_alert
        }
    }

    private fun getUsageColor(usage: BatteryUsage): Int {
        return when (usage) {
            BatteryUsage.Unknown -> AppColor.Gray.color
            BatteryUsage.Low -> AppColor.Green.color
            BatteryUsage.Moderate -> AppColor.Yellow.color
            BatteryUsage.High -> AppColor.Red.color
        }
    }

    private fun formatBatteryUsage(usage: BatteryUsage): String {
        val usageString = when (usage) {
            BatteryUsage.Unknown -> context.getString(R.string.unknown)
            BatteryUsage.Low -> context.getString(R.string.low)
            BatteryUsage.Moderate -> context.getString(R.string.moderate)
            BatteryUsage.High -> context.getString(R.string.high)
        }
        return context.getString(R.string.battery_usage, usageString)
    }

    private fun getBatteryUsage(service: RunningService): BatteryUsage {
        return when {
            service.frequency < Duration.ofMinutes(15) -> BatteryUsage.High
            service.frequency <= Duration.ofMinutes(25) -> BatteryUsage.Moderate
            else -> BatteryUsage.Low
        }
    }
}