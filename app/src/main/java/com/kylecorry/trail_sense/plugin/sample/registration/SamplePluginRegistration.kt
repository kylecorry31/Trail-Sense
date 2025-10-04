package com.kylecorry.trail_sense.plugin.sample.registration

import android.content.Context
import com.kylecorry.trail_sense.plugins.plugins.Plugin
import com.kylecorry.trail_sense.plugins.plugins.PluginRegistration
import com.kylecorry.trail_sense.plugins.plugins.PluginService
import com.kylecorry.trail_sense.plugins.plugins.Plugins

object SamplePluginRegistration : PluginRegistration {
    override fun getPlugin(context: Context): Plugin {
        return Plugin(
            Plugins.PLUGIN_SAMPLE,
            "Sample Plugin",
            listOf("com.kylecorry.trail_sense.plugin.sample"),
            services = listOf(
                PluginService(
                    PLUGIN_SERVICE_ID_SAMPLE_ONE_SERVICE,
                    "SAMPLE_ONE_SERVICE"
                )
            )
        )
    }

    const val PLUGIN_SERVICE_ID_SAMPLE_ONE_SERVICE = "plugin-service-sample-sample-one"
}