package com.kylecorry.trail_sense.tools.experimentation

import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin

/** A position relative to the start, in meters. */
data class LocalPoint(val east: Float, val north: Float, val up: Float)

/**
 * Estimates position relative to a starting fix by moving one stride along the average heading
 * every time a step is counted. Altitude comes from pressure changes since the start.
 */
class DeadReckoner(var stepLength: Float, var declination: Float = 0f) {

    val path = CopyOnWriteArrayList<LocalPoint>()
    var steps = 0
        private set
    var start: Coordinate = Coordinate.zero
        private set

    private var east = 0f
    private var north = 0f
    private var up = 0f
    private var startPressure: Float? = null

    // Headings are averaged as vectors to handle the wrap at 360 degrees
    private var headingSin = 0f
    private var headingCos = 0f
    private var lastHeading = 0f

    val position: LocalPoint
        get() = LocalPoint(east, north, up)

    val distanceWalked: Float
        get() = steps * stepLength

    val estimatedCoordinate: Coordinate
        get() = start.plus(hypot(east, north).toDouble(), Bearing.from(bearingFromStart()))

    val distanceToStart: Float
        get() = hypot(east, north)

    val bearingToStart: Float
        get() = (bearingFromStart() + 180f) % 360f

    fun reset(start: Coordinate) {
        this.start = start
        east = 0f
        north = 0f
        up = 0f
        steps = 0
        startPressure = null
        headingSin = 0f
        headingCos = 0f
        path.clear()
        path.add(position)
    }

    fun addHeading(magneticDegrees: Float) {
        val degrees = magneticDegrees + declination
        lastHeading = degrees
        headingSin += sin(Math.toRadians(degrees.toDouble())).toFloat()
        headingCos += cos(Math.toRadians(degrees.toDouble())).toFloat()
    }

    fun addPressure(hpa: Float) {
        val p0 = startPressure ?: hpa.also { startPressure = it }
        up = 44330f * (1f - (hpa / p0).pow(0.1903f))
    }

    fun addSteps(count: Int) {
        val heading = if (headingSin == 0f && headingCos == 0f) {
            lastHeading
        } else {
            Math.toDegrees(atan2(headingSin, headingCos).toDouble()).toFloat()
        }
        headingSin = 0f
        headingCos = 0f

        val rad = Math.toRadians(heading.toDouble())
        east += (count * stepLength * sin(rad)).toFloat()
        north += (count * stepLength * cos(rad)).toFloat()
        steps += count
        path.add(position)
    }

    private fun bearingFromStart(): Float {
        return (Math.toDegrees(atan2(east, north).toDouble()).toFloat() + 360f) % 360f
    }
}
