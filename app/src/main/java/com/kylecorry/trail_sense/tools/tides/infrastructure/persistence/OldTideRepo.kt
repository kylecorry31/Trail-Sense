package com.kylecorry.trail_sense.tools.tides.infrastructure.persistence

import android.content.Context
import com.kylecorry.trail_sense.shared.database.AppDatabase

class OldTideRepo(context: Context) {

    private val tideDao = AppDatabase.getInstance(context).tideDao()

    suspend fun getTides(): List<TideEntity> = tideDao.getAllSuspend()

    suspend fun deleteAll() = tideDao.deleteAll()

}