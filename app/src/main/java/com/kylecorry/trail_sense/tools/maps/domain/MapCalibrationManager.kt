package com.kylecorry.trail_sense.tools.maps.domain

import com.kylecorry.sol.units.Coordinate

class MapCalibrationManager(
    private val maxPoints: Int = 2,
    private val onCalibrationChange: () -> Unit
) {

    private var points = mutableListOf<MapCalibrationPoint>()
    private var originalPoints = listOf<MapCalibrationPoint>()
    private var calibratedPoints = mutableSetOf<Int>()

    init {
        fillCalibrationPoints()
    }


    fun calibrate(index: Int, location: PercentCoordinate) {
        points[index] = MapCalibrationPoint(points[index].location, location)
        onCalibrationChange()
    }

    fun calibrate(index: Int, coordinate: Coordinate?) {
        points[index] =
            MapCalibrationPoint(coordinate ?: Coordinate.zero, points[index].imageLocation)
        if (coordinate == null) {
            calibratedPoints.remove(index)
        } else {
            calibratedPoints.add(index)
        }
        onCalibrationChange()
    }

    fun getCalibration(filterComplete: Boolean = true): List<MapCalibrationPoint> {
        return if (filterComplete) {
            points.filterIndexed { index, _ -> calibratedPoints.contains(index) }
        } else {
            points
        }
    }

    fun reset(calibration: List<MapCalibrationPoint>) {
        originalPoints = calibration
        points = calibration.toMutableList()
        calibratedPoints.clear()
        calibratedPoints.addAll(points.indices)
        fillCalibrationPoints()
    }

    fun isCalibrated(index: Int): Boolean {
        return calibratedPoints.contains(index)
    }

    fun getCalibrationPoint(index: Int): MapCalibrationPoint {
        return points[index]
    }

    fun getNextUncalibratedIndex(): Int {
        return points.indexOfFirst { !calibratedPoints.contains(points.indexOf(it)) }
    }

    private fun fillCalibrationPoints() {
        while (points.size < maxPoints) {
            points.add(MapCalibrationPoint(Coordinate.zero, PercentCoordinate(0.5f, 0.5f)))
        }
    }

    fun hasChanges(): Boolean {
        return getCalibration() != originalPoints
    }

}