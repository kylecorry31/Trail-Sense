package com.kylecorry.trail_sense.weather.infrastructure.temperatures

import android.content.Context
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.time.Time.atEndOfDay
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Reading
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.shared.extensions.getReadings
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.math.ceil
import kotlin.math.floor

internal class TemperatureEstimator(private val context: Context) {

    fun getTemperaturesForDay(location: Coordinate, date: LocalDate): List<Reading<Temperature>> {
        val calculator = TemperatureCalculator(context, location, date)
        return getReadings(
            date.atStartOfDay().toZonedDateTime(),
            date.atEndOfDay().toZonedDateTime(),
            Duration.ofMinutes(10)
        ) {
            calculator.getTemperature(it)
        }
    }

    fun getTemperature(location: Coordinate, time: ZonedDateTime): Temperature {
        val calculator = TemperatureCalculator(context, location, time.toLocalDate())
        return calculator.getTemperature(time)
    }

    fun getDailyTemperatureRange(location: Coordinate, date: LocalDate): Range<Temperature> {
        return if (date.dayOfMonth == 15) {
            getMonthlyTemperatureLongitudeLerp(location, date)
        } else if (date.dayOfMonth > 15) {
            // Load this month and next
            val thisMonth = getMonthlyTemperatureLongitudeLerp(
                location,
                date
            )
            val nextMonth = getMonthlyTemperatureLongitudeLerp(
                location,
                date.plusMonths(1)
            )
            val daysSinceMiddle = Duration.between(
                date.withDayOfMonth(15).atStartOfDay(),
                date.atStartOfDay()
            )
            val daysBetweenMonths = Duration.between(
                date.withDayOfMonth(15).atStartOfDay(),
                date.plusMonths(1).withDayOfMonth(15).atStartOfDay()
            )
            val pct = daysSinceMiddle.toDays() / daysBetweenMonths.toDays().toDouble()
            lerp(pct.toFloat(), thisMonth, nextMonth)
        } else {
            // Load this month and previous
            val thisMonth = getMonthlyTemperatureLongitudeLerp(
                location,
                date
            )
            val prevMonth = getMonthlyTemperatureLongitudeLerp(
                location,
                date.minusMonths(1)
            )
            val daysSinceMiddle = Duration.between(
                date.minusMonths(1).withDayOfMonth(15).atStartOfDay(),
                date.atStartOfDay()
            )
            val daysBetweenMonths = Duration.between(
                date.minusMonths(1).withDayOfMonth(15).atStartOfDay(),
                date.withDayOfMonth(15).atStartOfDay()
            )
            val pct = daysSinceMiddle.toDays() / daysBetweenMonths.toDays().toDouble()
            lerp(pct.toFloat(), prevMonth, thisMonth)
        }
    }

    private fun getRegion(location: Coordinate): CoordinateBounds {
        val idx = (floor(location.longitude).toInt() + 180) / HistoricTemperatureLookup.lonStep
        val minLon = Coordinate.toLongitude(idx * HistoricTemperatureLookup.lonStep.toDouble() - 180)
        val maxLon =
            Coordinate.toLongitude((idx + 1) * HistoricTemperatureLookup.lonStep.toDouble() - 180)
        val minLat = floor(location.latitude)
        val maxLat = ceil(location.latitude)
        return CoordinateBounds(maxLat, maxLon, minLat, minLon)
    }

    private fun getRegionToNorth(location: Coordinate): CoordinateBounds? {
        val region = getRegion(location)
        if (region.north == 90.0) {
            return null
        }
        return CoordinateBounds(region.north + 1, region.east, region.south + 1, region.west)
    }

    private fun getRegionToSouth(location: Coordinate): CoordinateBounds? {
        val region = getRegion(location)
        if (region.south == -90.0) {
            return null
        }
        return CoordinateBounds(region.north - 1, region.east, region.south - 1, region.west)
    }

    private fun getRegionToEast(location: Coordinate): CoordinateBounds {
        val region = getRegion(location)

        val eastern = Coordinate.toLongitude(region.east + HistoricTemperatureLookup.lonStep)
        val western = Coordinate.toLongitude(region.west + HistoricTemperatureLookup.lonStep)

        return CoordinateBounds(region.north, eastern, region.south, western)
    }

    private fun getRegionToWest(location: Coordinate): CoordinateBounds {
        val region = getRegion(location)

        val eastern = Coordinate.toLongitude(region.east - HistoricTemperatureLookup.lonStep)
        val western = Coordinate.toLongitude(region.west - HistoricTemperatureLookup.lonStep)

        return CoordinateBounds(region.north, eastern, region.south, western)
    }

    private fun getMonthlyTemperatureLongitudeLerp(
        location: Coordinate,
        date: LocalDate
    ): Range<Temperature> {
        val region = getRegion(location)
        val western =
            CoordinateBounds(region.north, region.center.longitude, region.south, region.west)
        val currentRange = getMonthlyTemperatureLatitudeLerp(location, date)
        return if (western.contains(location)) {
            val neighbor = getRegionToWest(location)
            val range = getMonthlyTemperatureLatitudeLerp(neighbor.center, date)
            val distanceBetweenNeighbors = neighbor.center.distanceTo(region.center)
            val distanceToPoint =
                neighbor.center.distanceTo(location.copy(latitude = neighbor.center.latitude))
            lerp(distanceToPoint / distanceBetweenNeighbors, range, currentRange)
        } else {
            val neighbor = getRegionToEast(location)
            val range = getMonthlyTemperatureLatitudeLerp(neighbor.center, date)
            val distanceBetweenNeighbors = neighbor.center.distanceTo(region.center)
            val distanceToPoint =
                region.center.distanceTo(location.copy(latitude = region.center.latitude))
            lerp(distanceToPoint / distanceBetweenNeighbors, range, currentRange)
        }
    }

    // TODO: Lerp across longitude (find nearest longitude divisible by 18 (lonStep))
    private fun getMonthlyTemperatureLatitudeLerp(
        location: Coordinate,
        date: LocalDate
    ): Range<Temperature> {
        // TODO: Use the regions to lerp
        val start = floor(location.latitude)
        val end = ceil(location.latitude)

        if (start == end) {
            return HistoricTemperatureLookup.getMonthlyTemperatureRange(
                context,
                location,
                date.month
            )
        }

        val lower = HistoricTemperatureLookup.getMonthlyTemperatureRange(
            context,
            location.copy(latitude = start),
            date.month
        )
        val upper = HistoricTemperatureLookup.getMonthlyTemperatureRange(
            context,
            location.copy(latitude = end),
            date.month
        )

        val pct = SolMath.norm(location.latitude, start, end)

        return lerp(pct.toFloat(), lower, upper)
    }

    private fun lerp(
        pct: Float,
        lower: Range<Temperature>,
        upper: Range<Temperature>
    ): Range<Temperature> {
        val lerpedLow = SolMath.lerp(pct, lower.start.temperature, upper.start.temperature)
        val lerpedHigh = SolMath.lerp(pct, lower.end.temperature, upper.end.temperature)
        return Range(Temperature.celsius(lerpedLow), Temperature.celsius(lerpedHigh))
    }

}