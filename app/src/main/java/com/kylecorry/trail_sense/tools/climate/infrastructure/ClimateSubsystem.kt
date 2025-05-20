package com.kylecorry.trail_sense.tools.climate.infrastructure

import android.annotation.SuppressLint
import android.content.Context
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.science.meteorology.Meteorology
import com.kylecorry.sol.time.Time
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Reading
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.tools.climate.infrastructure.dewpoint.HistoricMonthlyDewpointRepo
import com.kylecorry.trail_sense.tools.climate.infrastructure.precipitation.HistoricMonthlyPrecipitationRepo
import com.kylecorry.trail_sense.tools.climate.infrastructure.temperatures.HistoricTemperatureRepo
import com.kylecorry.trail_sense.tools.climate.infrastructure.temperatures.ITemperatureRepo
import java.time.LocalDate
import java.time.Month
import java.time.ZonedDateTime

class ClimateSubsystem private constructor(private val context: Context) : ITemperatureRepo {

    private val temperatureRepo = HistoricTemperatureRepo(context)
    private val dewpointInterpolator = MonthlyValueInterpolator()

    suspend fun getMonthlyDewpoints(
        location: Coordinate,
        elevation: Distance
    ): Map<Month, Temperature> = onIO {
        HistoricMonthlyDewpointRepo.getMonthlyDewpoint(context, location).mapValues {
            Meteorology.getTemperatureAtElevation(it.value, Distance.meters(0f), elevation)
        }
    }

    suspend fun getDewpoint(
        location: Coordinate,
        elevation: Distance,
        date: LocalDate
    ): Temperature = onIO {
        val dewpoints =
            getMonthlyDewpoints(location, elevation).mapValues { it.value.celsius().temperature }
        val interpolated = dewpointInterpolator.interpolate(date, dewpoints)
        Temperature.celsius(interpolated)
    }

    suspend fun getYearlyDewpoints(
        year: Int,
        location: Coordinate,
        elevation: Distance
    ): List<Pair<LocalDate, Temperature>> = onIO {
        Time.getYearlyValues(year) {
            getDewpoint(location, elevation, it)
        }
    }

    suspend fun getMonthlyPrecipitation(
        location: Coordinate
    ): Map<Month, Distance> = onIO {
        HistoricMonthlyPrecipitationRepo.getMonthlyPrecipitation(context, location)
    }

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