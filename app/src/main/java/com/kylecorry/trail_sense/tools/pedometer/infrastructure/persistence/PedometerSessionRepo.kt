package com.kylecorry.trail_sense.tools.pedometer.infrastructure.persistence

import android.annotation.SuppressLint
import android.content.Context
import com.kylecorry.luna.coroutines.onIO
import com.kylecorry.trail_sense.main.persistence.AppDatabase
import com.kylecorry.trail_sense.tools.pedometer.domain.PedometerSession
import java.time.Instant

// #1397: Singleton repository for pedometer session persistence
class PedometerSessionRepo private constructor(context: Context) {

    private val dao = AppDatabase.getInstance(context).pedometerSessionDao()

    suspend fun add(session: PedometerSession): Long = onIO {
        dao.upsert(PedometerSessionEntity.from(session))
    }

    suspend fun delete(session: PedometerSession) = onIO {
        dao.delete(PedometerSessionEntity.from(session))
    }

    suspend fun get(id: Long): PedometerSession? = onIO {
        dao.get(id)?.toPedometerSession()
    }

    suspend fun getAll(): List<PedometerSession> = onIO {
        dao.getAllSync().map { it.toPedometerSession() }
    }

    suspend fun getEarliest(): PedometerSession? = onIO {
        dao.getEarliest()?.toPedometerSession()
    }

    suspend fun getRange(from: Instant, to: Instant): List<PedometerSession> = onIO {
        dao.getRange(from.toEpochMilli(), to.toEpochMilli())
            .map { it.toPedometerSession() }
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: PedometerSessionRepo? = null

        @Synchronized
        fun getInstance(context: Context): PedometerSessionRepo {
            if (instance == null) {
                instance = PedometerSessionRepo(context.applicationContext)
            }
            return instance!!
        }
    }
}
