package com.kylecorry.trail_sense.tools.photo_maps.domain

import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.sol.units.Coordinate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class MapCalibrationValidatorTest {

    @Test
    fun validateReturnsUncalibratedWhenMissingCalibrationPoints() {
        val map = PhotoMap(
            0,
            "",
            "",
            MapCalibration(true, true, 0f, emptyList()),
            MapMetadata(Size(1000f, 1000f), null, 0)
        )

        val result = MapCalibrationValidator.validate(map)

        assertEquals(MapCalibrationValidationResult.Uncalibrated, result)
    }

    @Test
    fun validateIsValidForReasonableScale() {
        val map = map(
            firstLocation = Coordinate(0.0, 0.0),
            secondLocation = Coordinate(0.0, 0.001),
            firstImageLocation = PercentCoordinate(0f, 0f),
            secondImageLocation = PercentCoordinate(1f, 1f)
        )

        val result = MapCalibrationValidator.validate(map)

        assertEquals(MapCalibrationValidationResult.Valid, result)
    }

    @Test
    fun validateReturnsSamePixel() {
        val map = map(
            firstLocation = Coordinate(0.0, 0.0),
            secondLocation = Coordinate(0.0, 0.001),
            firstImageLocation = PercentCoordinate(0.5f, 0.5f),
            secondImageLocation = PercentCoordinate(0.5f, 0.5f)
        )

        val result = MapCalibrationValidator.validate(map)

        assertEquals(MapCalibrationValidationResult.SamePixel, result)
    }

    @Test
    fun isCalibratedReturnsFalseForInvalidCalibration() {
        val map = map(
            firstLocation = Coordinate(0.0, 0.0),
            secondLocation = Coordinate(0.0, 0.001),
            firstImageLocation = PercentCoordinate(0.5f, 0.5f),
            secondImageLocation = PercentCoordinate(0.5f, 0.5f)
        )

        assertEquals(false, map.isCalibrated)
    }

    @Test
    fun validateReturnsSameLocation() {
        val map = map(
            firstLocation = Coordinate(0.0, 0.0),
            secondLocation = Coordinate(0.0, 0.0),
            firstImageLocation = PercentCoordinate(0f, 0f),
            secondImageLocation = PercentCoordinate(1f, 0f)
        )

        val result = MapCalibrationValidator.validate(map)

        assertEquals(MapCalibrationValidationResult.SameLocation, result)
    }

    @Test
    fun validateReturnsImplausibleScaleWhenScaleIsTooSmall() {
        val map = map(
            firstLocation = Coordinate(0.0, 0.0),
            secondLocation = Coordinate(0.0, 0.000001),
            firstImageLocation = PercentCoordinate(0f, 0f),
            secondImageLocation = PercentCoordinate(1f, 1f)
        )

        val result = MapCalibrationValidator.validate(map)

        assertEquals(MapCalibrationValidationResult.ImplausibleScale, result)
    }

    @Test
    fun validateReturnsImplausibleScaleWhenScaleIsTooLarge() {
        val map = map(
            firstLocation = Coordinate(0.0, 0.0),
            secondLocation = Coordinate(0.0, 90.0),
            firstImageLocation = PercentCoordinate(0f, 0f),
            secondImageLocation = PercentCoordinate(0.001f, 0.001f)
        )

        val result = MapCalibrationValidator.validate(map)

        assertEquals(MapCalibrationValidationResult.ImplausibleScale, result)
    }

    @Test
    fun validateReturnsSameImageAxisWhenPointsAreInSameImageRow() {
        val map = map(
            firstLocation = Coordinate(0.0, 0.0),
            secondLocation = Coordinate(0.0, 0.001),
            firstImageLocation = PercentCoordinate(0f, 0.5f),
            secondImageLocation = PercentCoordinate(1f, 0.5f)
        )

        val result = MapCalibrationValidator.validate(map)

        assertEquals(MapCalibrationValidationResult.SameImageAxis, result)
    }

    @Test
    fun validateReturnsSameImageAxisWhenPointsAreInSameImageColumn() {
        val map = map(
            firstLocation = Coordinate(0.0, 0.0),
            secondLocation = Coordinate(0.001, 0.0),
            firstImageLocation = PercentCoordinate(0.5f, 0f),
            secondImageLocation = PercentCoordinate(0.5f, 1f)
        )

        val result = MapCalibrationValidator.validate(map)

        assertEquals(MapCalibrationValidationResult.SameImageAxis, result)
    }

    private fun map(
        firstLocation: Coordinate,
        secondLocation: Coordinate,
        firstImageLocation: PercentCoordinate,
        secondImageLocation: PercentCoordinate
    ): PhotoMap {
        return PhotoMap(
            0,
            "",
            "",
            MapCalibration(
                true,
                true,
                0f,
                listOf(
                    MapCalibrationPoint(firstLocation, firstImageLocation),
                    MapCalibrationPoint(secondLocation, secondImageLocation)
                )
            ),
            MapMetadata(Size(1000f, 1000f), null, 0)
        )
    }
}
