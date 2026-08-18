package com.kylecorry.trail_sense.shared

import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DistanceUtilsTest {

    @Test
    fun shortMetricDistancesAreShownInMeters() {
        val relative = Distance.meters(500f).toRelativeDistance()

        assertEquals(DistanceUnits.Meters, relative.units)
        assertEquals(500f, relative.value, 0.001f)
    }

    @Test
    fun longMetricDistancesAreShownInKilometers() {
        val relative = Distance.meters(1500f).toRelativeDistance()

        assertEquals(DistanceUnits.Kilometers, relative.units)
        assertEquals(1.5f, relative.value, 0.001f)
    }

    @Test
    fun exactlyOneThousandMetersStaysInMeters() {
        val relative = Distance.meters(1000f).toRelativeDistance()

        assertEquals(DistanceUnits.Meters, relative.units)
        assertEquals(1000f, relative.value, 0.001f)
    }

    @Test
    fun otherMetricUnitsAreConvertedBeforeTheThresholdIsApplied() {
        assertEquals(DistanceUnits.Meters, Distance.kilometers(0.5f).toRelativeDistance().units)
        assertEquals(DistanceUnits.Kilometers, Distance.kilometers(1.5f).toRelativeDistance().units)
        assertEquals(
            DistanceUnits.Meters,
            Distance.from(50000f, DistanceUnits.Centimeters).toRelativeDistance().units
        )
    }

    @Test
    fun shortImperialDistancesAreShownInFeet() {
        val relative = Distance.feet(500f).toRelativeDistance()

        assertEquals(DistanceUnits.Feet, relative.units)
        assertEquals(500f, relative.value, 0.001f)
    }

    @Test
    fun imperialDistancesSwitchToMilesAtOneThousandFeet() {
        val relative = Distance.feet(1500f).toRelativeDistance()

        assertEquals(DistanceUnits.Miles, relative.units)
        assertEquals(1500f / 5280f, relative.value, 0.001f)
    }

    @Test
    fun exactlyOneThousandFeetStaysInFeet() {
        val relative = Distance.feet(1000f).toRelativeDistance()

        assertEquals(DistanceUnits.Feet, relative.units)
        assertEquals(1000f, relative.value, 0.001f)
    }


    @Test
    fun aFractionOfAMileStaysInFeet() {
        val relative = Distance.miles(0.1f).toRelativeDistance()

        assertEquals(DistanceUnits.Feet, relative.units)
        assertEquals(528f, relative.value, 0.1f)
    }

    @Test
    fun nauticalMilesAreTreatedAsImperial() {
        val relative = Distance.nauticalMiles(1f).toRelativeDistance()

        assertEquals(DistanceUnits.Miles, relative.units)
        assertEquals(1.1508f, relative.value, 0.001f)
    }

    @Test
    fun yardsAreTreatedAsImperial() {
        assertEquals(DistanceUnits.Feet, Distance.yards(100f).toRelativeDistance().units)
        assertEquals(DistanceUnits.Miles, Distance.yards(500f).toRelativeDistance().units)
    }

    @Test
    fun millimetersAreTreatedAsMetric() {
        val relative = Distance.from(1000f, DistanceUnits.Millimeters).toRelativeDistance()

        assertEquals(DistanceUnits.Meters, relative.units)
        assertEquals(1f, relative.value, 0.001f)
    }

    @Test
    fun longMillimeterDistancesAreShownInKilometers() {
        val relative = Distance.from(1_500_000f, DistanceUnits.Millimeters).toRelativeDistance()

        assertEquals(DistanceUnits.Kilometers, relative.units)
        assertEquals(1.5f, relative.value, 0.001f)
    }

    @Test
    fun inchesAreTreatedAsImperial() {
        assertEquals(
            DistanceUnits.Feet,
            Distance.from(12f, DistanceUnits.Inches).toRelativeDistance().units
        )
    }
}
