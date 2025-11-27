package com.kylecorry.trail_sense.plugins.plugins

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.kylecorry.andromeda.core.system.Package
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.trail_sense.shared.ProguardIgnore

data class PluginResourceService(
    val packageId: String,
    val name: String,
    val version: String?,
    val isOfficial: Boolean,
    val features: PluginResourceServiceFeatures,
    val allPermissions: List<String>,
    val grantedPermissions: List<String>
)

data class PluginResourceServiceFeatures(
    val weather: List<String>,
    val mapLayers: List<String>
)

private data class RegistrationFeaturesResponse(
    // TODO: This would be the list of features that Trail Sense can detect and allow plugins to override
    val weather: List<String> = emptyList(),
    val mapLayers: List<String> = emptyList()
) : ProguardIgnore

private data class RegistrationResponse(
    val name: String,
    val version: String,
    val features: RegistrationFeaturesResponse
) : ProguardIgnore

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

class PluginLoader(private val context: Context) {

    suspend fun getPluginResourceServices(): List<PluginResourceService> {
        val filter = Intent(PLUGIN_RESOURCE_SERVICE_ACTION)
        val services = context.packageManager.queryIntentServices(filter, 0)
        return services.map {
            val serviceInfo = it.serviceInfo
            val packageId = serviceInfo.packageName
            val appInfo = context.packageManager.getApplicationInfo(packageId, 0)
            val appName = context.packageManager.getApplicationLabel(appInfo).toString()
            val version = context.packageManager.getPackageInfo(packageId, 0).versionName
            val isOfficial = isOfficialPlugin(context, packageId)
            val allPermissions = context.packageManager
                .getPackageInfo(
                    packageId,
                    PackageManager.GET_PERMISSIONS
                ).requestedPermissions?.toList() ?: emptyList()
            val grantedPermissions = allPermissions.filter {
                Permissions.hasPermission(context, packageId, it)
            }

            val registration = ipcSend(
                context,
                packageId,
                "/registration"
            ).payloadAsJson<RegistrationResponse>()

            PluginResourceService(
                packageId,
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
    }
}