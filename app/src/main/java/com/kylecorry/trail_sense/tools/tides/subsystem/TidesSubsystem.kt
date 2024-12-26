package com.kylecorry.trail_sense.tools.tides.subsystem

import android.annotation.SuppressLint
import android.content.Context
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.tides.domain.TideDetails
import com.kylecorry.trail_sense.tools.tides.domain.TideService
import com.kylecorry.trail_sense.tools.tides.domain.TideTable
import com.kylecorry.trail_sense.tools.tides.domain.commands.CurrentTideCommand
import com.kylecorry.trail_sense.tools.tides.domain.commands.DailyTideCommand
import com.kylecorry.trail_sense.tools.tides.domain.loading.TideLoaderFactory
import com.kylecorry.trail_sense.tools.tides.domain.waterlevel.TideEstimator
import com.kylecorry.trail_sense.tools.tides.ui.DailyTideData
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

class TidesSubsystem private constructor(private val context: Context) {

    private val tideService = TideService(context)
    private val tideLoaderFactory = TideLoaderFactory()

    private var lastTable: TideTable? = null
    private var lastDate: LocalDate? = null
    private var lastDailyTide: DailyTideData? = null
    private val mutex = Mutex()

    suspend fun getNearestTide(location: Coordinate? = null): TideDetails? {
        val table = getTideTable(location) ?: return null
        val tide = CurrentTideCommand(tideService).execute(table)
        val times = mutex.withLock {
            if (isDailyStillValid(table)) {
                lastDailyTide!!
            } else {
                val newDaily = DailyTideCommand(tideService).execute(table, LocalDate.now())
                lastDailyTide = newDaily
                lastTable = table
                lastDate = LocalDate.now()
                newDaily
            }
        }

        if (times.waterLevels.all { it.value == 0f }) {
            return null
        }

        return TideDetails(table, tide, times)
    }

    private suspend fun getTideTable(location: Coordinate?): TideTable? {
        val loader = tideLoaderFactory.getTideLoader(context, false, location)
        val table = loader.getTideTable() ?: return null

        if (table.isEditable || location == null) {
            return table
        }

        return TideTable(
            -1,
            emptyList(),
            null,
            estimator = TideEstimator.TideModel,
            isEditable = false,
            location = location
        )
    }

    private fun isDailyStillValid(table: TideTable): Boolean {
        if (table != lastTable || lastDate == null || lastDailyTide == null) {
            return false
        }

        if (lastDate != LocalDate.now()) {
            return false
        }

        return true
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: TidesSubsystem? = null

        @Synchronized
        fun getInstance(context: Context): TidesSubsystem {
            if (instance == null) {
                instance = TidesSubsystem(context.applicationContext)
            }
            return instance!!
        }
    }

}