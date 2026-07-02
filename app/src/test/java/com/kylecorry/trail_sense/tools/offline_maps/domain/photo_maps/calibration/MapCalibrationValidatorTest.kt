package com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.calibration

import com.kylecorry.andromeda.core.units.PercentCoordinate
import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapFile
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapState
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMapGeoreference
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class MapCalibrationValidatorTest {

    @Test
    fun validateReturnsUncalibratedWhenMissingCalibrationPoints() {
        val map = PhotoMap(
            0,
            "",
            listOf(OfflineMapFile("", 0, PhotoMap.FILE_ROLE_IMAGE)),
            PhotoMapGeoreference(Size(1000f, 1000f))
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
    fun stateReturnsDraftForInvalidCalibration() {
        val map = map(
            firstLocation = Coordinate(0.0, 0.0),
            secondLocation = Coordinate(0.0, 0.001),
            firstImageLocation = PercentCoordinate(0.5f, 0.5f),
            secondImageLocation = PercentCoordinate(0.5f, 0.5f)
        )

        assertEquals(OfflineMapState.Draft, map.state)
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
            listOf(OfflineMapFile("", 0, PhotoMap.FILE_ROLE_IMAGE)),
            PhotoMapGeoreference(
                Size(1000f, 1000f),
                isWarpingCompleted = true,
                calibrationPoints = listOf(
                    MapCalibrationPoint(firstLocation, firstImageLocation),
                    MapCalibrationPoint(secondLocation, secondImageLocation)
                )
            )
        )
    }
}
