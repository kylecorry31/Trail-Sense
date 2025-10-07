package com.kylecorry.trail_sense.plugins.plugins

import android.content.Context
import android.content.Intent
import com.kylecorry.andromeda.core.system.Package
import com.kylecorry.trail_sense.shared.ProguardIgnore

data class PluginService2(
    val name: String,
    val version: String?,
    val packageId: String,
    val isOfficial: Boolean,
    val features: PluginFeatures
)

data class PluginFeatures(
    val weather: List<String>,
    val mapLayers: List<String>
)

data class RegistrationResponse(
    val name: String,
    // TODO: This would be the list of features that Trail Sense can detect and allow plugins to override
    val weather: List<String> = emptyList(),
    val mapLayers: List<String> = emptyList()
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

class PluginFinder(private val context: Context) {

    suspend fun queryPlugins(): List<PluginService2> {
        val filter = Intent("com.kylecorry.trail_sense.PLUGIN_SERVICE")
        val services = context.packageManager.queryIntentServices(filter, 0)
        return services.map {
            val serviceInfo = it.serviceInfo
            val packageId = serviceInfo.packageName
            val appInfo = context.packageManager.getApplicationInfo(packageId, 0)
            val appName = context.packageManager.getApplicationLabel(appInfo).toString()
            val version = context.packageManager.getPackageInfo(packageId, 0).versionName
            val isOfficial = isOfficialPlugin(context, packageId)

            val registration = IpcServicePlugin(packageId, context).use {
                it.send("/registration")?.fromJson<RegistrationResponse>()
            }

            PluginService2(
                registration?.name ?: appName, version, packageId, isOfficial,
                PluginFeatures(
                    registration?.weather ?: emptyList(),
                    registration?.mapLayers ?: emptyList()
                )
            )
        }
    }
}