package com.kylecorry.trail_sense.plugins.plugins

import android.content.Context
import com.kylecorry.andromeda.core.system.Package
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.trail_sense.shared.data.Identifiable


data class PluginService(
    val id: String,
    val actionId: String,
    val prefixActionWithPackageName: Boolean = true
)

private fun hasValidSignature(
    context: Context,
    packageName: String,
    allowedSignatures: List<String>? = null
): Boolean {
    // No signature required
    if (allowedSignatures == null) {
        return true
    }
    val signatures = Package.getSignatureSha256Fingerprints(context, packageName)
    return signatures.any { it in allowedSignatures }
}

private fun getInstalledPackageId(context: Context, ids: List<PluginPackage>): PluginPackage? {
    return ids.firstOrNull {
        Package.isPackageInstalled(context, it.packageId) && hasValidSignature(
            context,
            it.packageId,
            it.allowedSignatures
        ) && it.requiredPermissions.all { p ->
            Permissions.hasPermission(context, it.packageId, p)
        }
    }
}

data class PluginPackage(
    val packageId: String,
    val requiredPermissions: List<String> = emptyList(),
    val allowedSignatures: List<String>? = null
)

data class Plugin(
    override val id: Long,
    val name: String,
    val packages: List<PluginPackage>,
    val description: String? = null,
    val hasLauncher: Boolean = true,
    private val isAvailableChecks: (context: Context) -> Boolean = { true },
    val services: List<PluginService> = emptyList()
) : Identifiable {

    // TODO: If the package is installed with the expected signature, then it is available. Permissions should be handled separately.
    fun isAvailable(context: Context): Boolean {
        return getInstalledPackageId(context, packages) != null && isAvailableChecks(context)
    }

    fun getInstalledPackageId(context: Context): String? {
        return getInstalledPackageId(context, packages)?.packageId
    }

    fun open(context: Context) {
        Package.openApp(context, getInstalledPackageId(context) ?: return)
    }

    fun getVersion(context: Context): String? {
        val packageId = getInstalledPackageId(context) ?: return null
        return tryOrDefault(null) {
            Package.getPackageInfo(context, packageId).versionName
        }
    }
}