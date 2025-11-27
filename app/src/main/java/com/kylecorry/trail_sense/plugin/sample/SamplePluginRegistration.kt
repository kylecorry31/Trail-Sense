package com.kylecorry.trail_sense.plugin.sample

import android.Manifest
import android.content.Context
import com.kylecorry.andromeda.core.system.Package
import com.kylecorry.trail_sense.plugins.plugins.Plugin
import com.kylecorry.trail_sense.plugins.plugins.PluginPackage
import com.kylecorry.trail_sense.plugins.plugins.PluginRegistration
import com.kylecorry.trail_sense.plugins.plugins.PluginService
import com.kylecorry.trail_sense.plugins.plugins.Plugins

object SamplePluginRegistration : PluginRegistration {
    override fun getPlugin(context: Context): Plugin {
        return Plugin(
            Plugins.PLUGIN_SAMPLE,
            "Sample Plugin",
            listOf(
                PluginPackage(
                    "com.kylecorry.trail_sense.plugin.sample",
                    requiredPermissions = listOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ),
                    allowedSignatures = Package.getSelfSignatureSha256Fingerprints(context)
                )
            ),
            services = listOf(
                PluginService(
                    PLUGIN_SERVICE_SAMPLE_PLUGIN_SERVICE,
                    "com.kylecorry.trail_sense.PLUGIN_SERVICE"
                )
            )
        )
    }

    const val PLUGIN_SERVICE_SAMPLE_PLUGIN_SERVICE = "plugin-service-sample-sample-two"
}