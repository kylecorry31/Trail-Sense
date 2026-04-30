package com.kylecorry.trail_sense.plugins.infrastructure

import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import com.kylecorry.andromeda.core.system.Package
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.trail_sense.plugins.domain.Plugin
import com.kylecorry.trail_sense.plugins.infrastructure.PluginResourceServiceConnection
import com.kylecorry.trail_sense.plugins.domain.PluginResourceServiceDetails
import com.kylecorry.trail_sense.plugins.domain.PluginResourceServiceFeatures
import com.kylecorry.trail_sense.shared.ProguardIgnore

class PluginLoader(private val context: Context) {

    fun getResourceServicePlugins(): List<Plugin> {
        val filter = Intent(PLUGIN_RESOURCE_SERVICE_ACTION)
        val services = context.packageManager.queryIntentServices(filter, 0)
        return services.mapNotNull {
            getPlugin(it.serviceInfo.packageName)
        }.distinctBy { it.packageId }
    }

    fun getPlugin(packageId: String): Plugin? {
        return tryOrDefault(null) {
            val appInfo = context.packageManager.getApplicationInfo(packageId, 0)
            val appName = context.packageManager.getApplicationLabel(appInfo).toString()
            val version = context.packageManager.getPackageInfo(packageId, 0).versionName
            val signatures = Package.getSignatureSha256Fingerprints(context, packageId).sorted()

            Plugin(packageId, appName, version, signatures)
        }
    }

    suspend fun getPluginResourceService(packageId: String): PluginResourceServiceDetails? {
        val filter = pluginResourceServiceIntent(packageId)
        val service = context.packageManager.queryIntentServices(filter, 0).firstOrNull() ?: return null
        return getPluginResourceService(service.serviceInfo)
    }

    private suspend fun getPluginResourceService(serviceInfo: ServiceInfo): PluginResourceServiceDetails {
        val servicePackageId = serviceInfo.packageName
        val appInfo = context.packageManager.getApplicationInfo(servicePackageId, 0)
        val appName = context.packageManager.getApplicationLabel(appInfo).toString()
        val version = context.packageManager.getPackageInfo(servicePackageId, 0).versionName

        val registration = PluginResourceServiceConnection(context, servicePackageId).use {
            it.send("/registration")?.payloadAsJson<RegistrationResponse>()
        }

        return PluginResourceServiceDetails(
            servicePackageId,
            registration?.name ?: appName,
            registration?.version ?: version,
            PluginResourceServiceFeatures(
                registration?.features?.weather ?: emptyList(),
                registration?.features?.mapLayers ?: emptyList()
            )
        )
    }

    private data class RegistrationFeaturesResponse(
        val weather: List<String> = emptyList(),
        val mapLayers: List<String> = emptyList()
    ) : ProguardIgnore

    private data class RegistrationResponse(
        val name: String,
        val version: String,
        val features: RegistrationFeaturesResponse
    ) : ProguardIgnore
}
