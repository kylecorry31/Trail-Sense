package com.kylecorry.trail_sense.tools.battery.ui

import android.content.Context
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.andromeda.views.list.ListItemMapper
import com.kylecorry.andromeda.views.list.ResourceListIcon
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.tools.battery.domain.BatteryUsage
import com.kylecorry.trail_sense.tools.battery.domain.SystemBatteryTip

class SystemBatteryTipListItemMapper(private val context: Context) :
    ListItemMapper<SystemBatteryTip> {

    override fun map(value: SystemBatteryTip): ListItem {
        val action = {
            value.manage?.invoke(context)
        }
        return ListItem(
            value.name.hashCode().toLong(),
            value.name,
            value.description,
            icon = ResourceListIcon(
                getUsageIcon(value.batteryUsage),
                tint = getUsageColor(value.batteryUsage)
            ) {
                action()
            },
            trailingIcon = if (value.manage != null) {
                ResourceListIcon(
                    R.drawable.ic_keyboard_arrow_right,
                    Resources.androidTextColorSecondary(context)
                ) {
                    action()
                }
            } else {
                null
            }
        ) {
            action()
        }
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
}