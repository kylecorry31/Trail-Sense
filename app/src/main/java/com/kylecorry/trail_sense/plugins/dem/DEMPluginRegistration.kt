package com.kylecorry.trail_sense.plugins.dem

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.plugins.plugins.Plugin
import com.kylecorry.trail_sense.plugins.plugins.PluginRegistration
import com.kylecorry.trail_sense.plugins.plugins.PluginService
import com.kylecorry.trail_sense.plugins.plugins.Plugins

object DEMPluginRegistration : PluginRegistration {
    override fun getPlugin(context: Context): Plugin {
        return Plugin(
            Plugins.DIGITAL_ELEVATION_MODEL,
            context.getString(R.string.plugin_digital_elevation_model),
            listOf(PACKAGE_NAME),
            services = listOf(
                PluginService(SERVICE_DEM, "DEM_SERVICE")
            )
        )
    }

    const val PACKAGE_NAME = "com.kylecorry.trail_sense_dem"
    const val SERVICE_DEM = "dem-plugin-service-dem"
}