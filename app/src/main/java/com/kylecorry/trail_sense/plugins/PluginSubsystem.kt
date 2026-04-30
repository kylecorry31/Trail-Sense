package com.kylecorry.trail_sense.plugins

import android.annotation.SuppressLint
import android.content.Context
import com.kylecorry.andromeda.ipc.InterprocessCommunicationResponse
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.luna.coroutines.onIO
import com.kylecorry.trail_sense.plugins.domain.AvailablePlugin
import com.kylecorry.trail_sense.plugins.domain.PluginResourceServiceDetails
import com.kylecorry.trail_sense.plugins.infrastructure.PLUGIN_RESOURCE_SERVICE_ACTION
import com.kylecorry.trail_sense.plugins.infrastructure.PluginLoader
import com.kylecorry.trail_sense.plugins.infrastructure.ipcSend
import com.kylecorry.trail_sense.plugins.infrastructure.persistence.PersistedPlugin
import com.kylecorry.trail_sense.plugins.infrastructure.persistence.PluginRepo

class PluginSubsystem private constructor(private val context: Context) {

    private val pluginLoader = PluginLoader(context)
    private val pluginRepo = PluginRepo.getInstance(context)

    suspend fun getAvailablePlugins(): List<AvailablePlugin> = onIO {
        pluginLoader.getAvailablePlugins()
    }

    suspend fun getConnectedPlugins(): List<AvailablePlugin> {
        val available = getAvailablePlugins()
        val connected = pluginRepo.getAll().associateBy { it.packageId }
        return available.filter { plugin ->
            connected[plugin.packageId]?.signature == plugin.signatureString()
        }
    }

    suspend fun getUnconnectedPlugins(): List<AvailablePlugin> {
        val connectedIds = getConnectedPlugins().map { it.packageId }.toSet()
        return getAvailablePlugins().filter { it.packageId !in connectedIds }
    }

    suspend fun getPluginResourceService(packageId: String): PluginResourceServiceDetails? {
        if (!isConnected(packageId)) {
            return null
        }

        return pluginLoader.getPluginResourceService(packageId)
    }

    suspend fun getConnectedPluginResourceServices(): List<PluginResourceServiceDetails> {
        return getConnectedPlugins().mapNotNull {
            pluginLoader.getPluginResourceService(it.packageId)
        }
    }

    suspend fun connect(plugin: AvailablePlugin) {
        pluginRepo.upsert(PersistedPlugin(plugin.packageId, plugin.signatureString()))
    }

    suspend fun disconnect(plugin: AvailablePlugin) {
        pluginRepo.deleteByPackageId(plugin.packageId)
    }

    suspend fun callPluginEndpoint(
        packageId: String,
        route: String,
        payload: Any? = null,
        requiredPermissions: List<String> = emptyList()
    ): InterprocessCommunicationResponse? {
        if (!isConnected(packageId)) {
            // Not connected
            return null
        }

        if (requiredPermissions.isNotEmpty() && requiredPermissions.any {
                !Permissions.hasPermission(context, packageId, it)
            }) {
            // Permission not granted
            return null
        }

        return ipcSend(context, packageId, route, payload, PLUGIN_RESOURCE_SERVICE_ACTION)
    }

    private suspend fun isConnected(packageId: String): Boolean {
        val plugin = pluginLoader.getPlugin(packageId) ?: return false
        return pluginRepo.get(packageId)?.signature == plugin.signatureString()
    }

    private fun AvailablePlugin.signatureString(): String {
        return signatures.sorted().joinToString(", ")
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: PluginSubsystem? = null

        @Synchronized
        fun getInstance(context: Context): PluginSubsystem {
            if (instance == null) {
                instance = PluginSubsystem(context.applicationContext)
            }
            return instance!!
        }
    }
}
