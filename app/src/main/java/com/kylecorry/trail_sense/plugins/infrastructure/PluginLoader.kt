package com.kylecorry.trail_sense.plugins.infrastructure

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import com.kylecorry.andromeda.core.system.Package
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.trail_sense.plugins.domain.AvailablePlugin
import com.kylecorry.trail_sense.plugins.domain.PluginResourceServiceDetails
import com.kylecorry.trail_sense.plugins.domain.PluginResourceServiceFeatures
import com.kylecorry.trail_sense.shared.ProguardIgnore

class PluginLoader(private val context: Context) {

    fun getAvailablePlugins(): List<AvailablePlugin> {
        val filter = Intent(PLUGIN_RESOURCE_SERVICE_ACTION)
        val services = context.packageManager.queryIntentServices(filter, 0)
        return services.mapNotNull {
            getPlugin(it.serviceInfo.packageName)
        }.distinctBy { it.packageId }
    }

    fun getPlugin(packageId: String): AvailablePlugin? {
        return tryOrDefault(null) {
            val appInfo = context.packageManager.getApplicationInfo(packageId, 0)
            val appName = context.packageManager.getApplicationLabel(appInfo).toString()
            val version = context.packageManager.getPackageInfo(packageId, 0).versionName
            val allPermissions = context.packageManager
                .getPackageInfo(
                    packageId,
                    PackageManager.GET_PERMISSIONS
                ).requestedPermissions?.toList()?.sorted() ?: emptyList()
            val grantedPermissions = allPermissions.filter { permission ->
                Permissions.hasPermission(context, packageId, permission)
            }
            val signatures = Package.getSignatureSha256Fingerprints(context, packageId).sorted()

            AvailablePlugin(
                packageId,
                appName,
                version,
                signatures,
                allPermissions,
                grantedPermissions
            )
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
        val isOfficial = isOfficialPlugin(context, servicePackageId)
        val allPermissions = context.packageManager
            .getPackageInfo(
                servicePackageId,
                PackageManager.GET_PERMISSIONS
            ).requestedPermissions?.toList() ?: emptyList()
        val grantedPermissions = allPermissions.filter {
            Permissions.hasPermission(context, servicePackageId, it)
        }

        val registration = ipcSend(
            context,
            servicePackageId,
            "/registration"
        ).payloadAsJson<RegistrationResponse>()

        return PluginResourceServiceDetails(
            servicePackageId,
            registration?.name ?: appName,
            registration?.version ?: version,
            isOfficial,
            PluginResourceServiceFeatures(
                registration?.features?.weather ?: emptyList(),
                registration?.features?.mapLayers ?: emptyList()
            ),
            allPermissions,
            grantedPermissions
        )
    }

    private fun isOfficialPlugin(
        context: Context,
        packageName: String
    ): Boolean {
        val allowedSignatures = Package.getSelfSignatureSha256Fingerprints(context) + listOf(
            // TODO: Other trail sense signatures here
        )
        val signatures = Package.getSignatureSha256Fingerprints(context, packageName)
        return signatures.any { it in allowedSignatures }
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
