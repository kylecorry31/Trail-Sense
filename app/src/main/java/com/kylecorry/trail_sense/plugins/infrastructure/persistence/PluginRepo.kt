package com.kylecorry.trail_sense.plugins.infrastructure.persistence

import android.annotation.SuppressLint
import android.content.Context
import com.kylecorry.luna.coroutines.onIO
import com.kylecorry.trail_sense.main.persistence.AppDatabase

class PluginRepo private constructor(context: Context) {

    private val dao = AppDatabase.getInstance(context).pluginDao()

    suspend fun getAll(): List<PersistedPlugin> = onIO {
        dao.getAll().map { it.toPlugin() }
    }

    suspend fun get(packageId: String): PersistedPlugin? = onIO {
        dao.get(packageId)?.toPlugin()
    }

    suspend fun upsert(plugin: PersistedPlugin) = onIO {
        dao.upsert(plugin.toEntity())
    }

    suspend fun deleteByPackageId(packageId: String) = onIO {
        dao.deleteByPackageId(packageId)
    }

    private fun PluginEntity.toPlugin(): PersistedPlugin {
        return PersistedPlugin(packageId, signature)
    }

    private fun PersistedPlugin.toEntity(): PluginEntity {
        return PluginEntity(packageId, signature)
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: PluginRepo? = null

        @Synchronized
        fun getInstance(context: Context): PluginRepo {
            if (instance == null) {
                instance = PluginRepo(context.applicationContext)
            }
            return instance!!
        }
    }
}
