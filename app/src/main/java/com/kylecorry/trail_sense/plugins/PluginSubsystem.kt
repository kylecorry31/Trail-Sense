package com.kylecorry.trail_sense.plugins

import android.annotation.SuppressLint
import android.content.Context
import com.kylecorry.trail_sense.plugins.domain.Plugin
import com.kylecorry.trail_sense.plugins.domain.PluginResourceServiceDetails
import com.kylecorry.trail_sense.plugins.infrastructure.PluginLoader
import com.kylecorry.trail_sense.plugins.infrastructure.PluginResourceServiceConnection
import com.kylecorry.trail_sense.plugins.infrastructure.persistence.PersistedPlugin
import com.kylecorry.trail_sense.plugins.infrastructure.persistence.PluginRegistrationRepo
import com.kylecorry.trail_sense.plugins.infrastructure.persistence.PluginRepo
import com.kylecorry.trail_sense.shared.debugging.isDebug

@Suppress("TooManyFunctions")
class PluginSubsystem private constructor(private val context: Context) {

    private val pluginLoader = PluginLoader(context)
    private val pluginRepo = PluginRepo.getInstance(context)
    private val pluginRegistrationRepo = PluginRegistrationRepo.getInstance(context)

    fun arePluginsEnabled(): Boolean {
        return isDebug()
    }

    // ======== Connection ========

    /**
     * Get all installed plugins
     */
    fun getAvailablePlugins(): List<Plugin> {
        return if (!arePluginsEnabled()) {
            emptyList()
        } else {
            pluginLoader.getResourceServicePlugins()
        }
    }

    /**
     * Get all installed plugins that are connected
     */
    suspend fun getConnectedPlugins(): List<Plugin> {
        if (!arePluginsEnabled()) {
            return emptyList()
        }
        val available = getAvailablePlugins()
        val connected = pluginRepo.getAll().associateBy { it.packageId }
        return available.filter { plugin ->
            connected[plugin.packageId]?.signature == plugin.signatureString()
        }
    }

    /**
     * Get all installed plugins that are not connected
     */
    suspend fun getUnconnectedPlugins(): List<Plugin> {
        val connectedIds = getConnectedPlugins().map { it.packageId }.toSet()
        return getAvailablePlugins().filter { it.packageId !in connectedIds }
    }

    /**
     * Connect a plugin
     */
    suspend fun connect(plugin: Plugin) {
        pluginRepo.upsert(PersistedPlugin(plugin.packageId, plugin.signatureString()))
    }

    /**
     * Disconnect a plugin
     */
    suspend fun disconnect(plugin: Plugin) {
        pluginRepo.deleteByPackageId(plugin.packageId)
        pluginRegistrationRepo.deleteByPackageId(plugin.packageId)
    }

    suspend fun reloadRegistration(packageId: String): PluginResourceServiceDetails? {
        pluginRegistrationRepo.deleteByPackageId(packageId)
        return getPluginResourceServiceDetails(packageId)
    }

    /**
     * Determines if a plugin is installed and connected
     */
    suspend fun isConnected(packageId: String): Boolean {
        val plugin = pluginLoader.getPlugin(packageId) ?: return false
        return pluginRepo.get(packageId)?.signature == plugin.signatureString()
    }

    // ======== Resource Services ========

    /**
     * Get details about a connected plugin's resource service
     */
    suspend fun getPluginResourceServiceDetails(packageId: String): PluginResourceServiceDetails? {
        if (!isConnected(packageId)) {
            return null
        }

        return pluginLoader.getPluginResourceService(packageId)
    }

    /**
     * Get details about all connected plugins' resource services
     */
    suspend fun getPluginResourceServiceDetails(): List<PluginResourceServiceDetails> {
        return getConnectedPlugins().mapNotNull {
            pluginLoader.getPluginResourceService(it.packageId)
        }
    }

    /**
     * Get a connection handle for a connected plugin's resource service
     */
    suspend fun getPluginResourceServiceConnection(packageId: String): PluginResourceServiceConnection? {
        if (!isConnected(packageId)) {
            return null
        }
        return PluginResourceServiceConnection(context, packageId)
    }

    private fun Plugin.signatureString(): String {
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
