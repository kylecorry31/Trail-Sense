package com.kylecorry.trail_sense.shared.alerts

import android.content.Context
import android.os.Build
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.system.Android
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.trail_sense.R

class MissingSensorAlert(private val context: Context) : IValueAlerter<String> {

    override fun alert(value: String) {
        Alerts.dialog(
            context,
            getMissingSensorTitle(context, value),
            getMissingSensorMessage(context, value),
            cancelText = null
        )
    }

    companion object {
        fun getMissingSensorTitle(context: Context, sensor: String): String {
            return context.getString(R.string.no_sensor_message, sensor.lowercase())
        }

        fun getMissingSensorMessage(context: Context, sensor: String): CharSequence {
            val deviceName = "${Android.manufacturer} ${Build.MODEL}"

            val part1 = context.getString(
                R.string.missing_sensor_message_1,
                deviceName,
                sensor.lowercase(),
                context.getString(R.string.app_name)
            )

            val part2Title = context.getString(R.string.is_there_a_way_to_fix_this)
            val part2 = context.getString(R.string.no_sensor_unable_to_fix)

            val part3Title = context.getString(R.string.why_missing_sensor)
            val part3 = context.getString(R.string.missing_sensor_explanation)

            val markdown = "$part1\n\n## $part2Title\n\n$part2\n\n## $part3Title\n\n$part3"

            return AppServiceRegistry.get<MarkdownService>().toMarkdown(markdown)
        }
    }

}