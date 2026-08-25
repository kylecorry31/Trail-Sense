package com.kylecorry.trail_sense.tools.climate.infrastructure

import android.annotation.SuppressLint
import android.content.Context
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Reading
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.tools.climate.infrastructure.temperatures.HistoricTemperatureRepo
import com.kylecorry.trail_sense.tools.climate.infrastructure.temperatures.ITemperatureRepo
import java.time.LocalDate
import java.time.ZonedDateTime

class ClimateSubsystem private constructor(private val context: Context) : ITemperatureRepo {

    private val temperatureRepo = HistoricTemperatureRepo(context)

    override suspend fun getYearlyTemperatures(
        year: Int,
        location: Coordinate
    ): List<Pair<LocalDate, Range<Temperature>>> {
        return temperatureRepo.getYearlyTemperatures(year, location)
    }

    override suspend fun getTemperatures(
        location: Coordinate,
        start: ZonedDateTime,
        end: ZonedDateTime
    ): List<Reading<Temperature>> {
        return temperatureRepo.getTemperatures(location, start, end)
    }

    override suspend fun getTemperature(location: Coordinate, time: ZonedDateTime): Temperature {
        return temperatureRepo.getTemperature(location, time)
    }

    override suspend fun getDailyTemperatureRange(
        location: Coordinate,
        date: LocalDate
    ): Range<Temperature> {
        return temperatureRepo.getDailyTemperatureRange(location, date)
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: ClimateSubsystem? = null

        @Synchronized
        fun getInstance(context: Context): ClimateSubsystem {
            if (instance == null) {
                instance = ClimateSubsystem(context.applicationContext)
            }
            return instance!!
        }
    }


}
