package com.kylecorry.trail_sense.plugins.plugins

import android.annotation.SuppressLint
import android.content.Context
import com.kylecorry.luna.coroutines.onIO
import com.kylecorry.trail_sense.plugins.infrastructure.persistence.PersistedPlugin
import com.kylecorry.trail_sense.plugins.infrastructure.persistence.PluginRepo

class PluginSubsystem private constructor(context: Context) {

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

    suspend fun connect(plugin: AvailablePlugin) {
        pluginRepo.upsert(PersistedPlugin(plugin.packageId, plugin.signatureString()))
    }

    suspend fun disconnect(plugin: AvailablePlugin) {
        pluginRepo.deleteByPackageId(plugin.packageId)
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
