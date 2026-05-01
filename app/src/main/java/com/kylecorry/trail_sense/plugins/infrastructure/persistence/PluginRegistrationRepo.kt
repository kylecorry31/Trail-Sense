package com.kylecorry.trail_sense.plugins.infrastructure.persistence

import android.annotation.SuppressLint
import android.content.Context
import com.kylecorry.luna.coroutines.onIO
import com.kylecorry.trail_sense.main.persistence.AppDatabase

class PluginRegistrationRepo private constructor(context: Context) {

    private val dao = AppDatabase.getInstance(context).pluginRegistrationDao()

    suspend fun get(packageId: String): PluginRegistrationEntity? = onIO {
        dao.get(packageId)
    }

    suspend fun upsert(registration: PluginRegistrationEntity) = onIO {
        dao.upsert(registration)
    }

    suspend fun deleteByPackageId(packageId: String) = onIO {
        dao.deleteByPackageId(packageId)
    }

    suspend fun deleteAll() = onIO {
        dao.deleteAll()
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: PluginRegistrationRepo? = null

        @Synchronized
        fun getInstance(context: Context): PluginRegistrationRepo {
            if (instance == null) {
                instance = PluginRegistrationRepo(context.applicationContext)
            }
            return instance!!
        }
    }
}
