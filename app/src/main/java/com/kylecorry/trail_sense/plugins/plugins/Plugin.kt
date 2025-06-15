package com.kylecorry.trail_sense.plugins.plugins

import android.content.Context
import com.kylecorry.andromeda.core.system.Package
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.trail_sense.shared.data.Identifiable

data class PluginService(
    val id: String,
    val actionId: String,
    val prefixActionWithPackageName: Boolean = true
)

data class Plugin(
    override val id: Long,
    val name: String,
    val packageIds: List<String>,
    val description: String? = null,
    val hasLauncher: Boolean = true,
    val isAvailable: (context: Context) -> Boolean = { context ->
        packageIds.any { Package.isPackageInstalled(context, it) }
    },
    val services: List<PluginService> = emptyList()
) : Identifiable {

    fun getInstalledPackageId(context: Context): String? {
        return packageIds.firstOrNull { Package.isPackageInstalled(context, it) }
    }

    fun open(context: Context) {
        Package.openApp(context, getInstalledPackageId(context) ?: return)
    }

    fun getVersion(context: Context): String? {
        return packageIds.firstNotNullOfOrNull { packageId ->
            tryOrDefault(null) {
                Package.getPackageInfo(context, packageId).versionName
            }
        }
    }
}