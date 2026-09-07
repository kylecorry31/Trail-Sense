package com.kylecorry.trail_sense.shared.sensors.gps.modules

import com.kylecorry.sol.math.algebra.Matrix
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.settings.infrastructure.IGPSPreferences
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.logging.Logger
import com.kylecorry.trail_sense.shared.sensors.gps.GPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.GPSKalmanState
import com.kylecorry.trail_sense.shared.sensors.gps.KalmanFilter
import com.kylecorry.trail_sense.shared.sensors.gps.ModularGPSData
import com.kylecorry.trail_sense.shared.sensors.gps.SpeedSource
import java.time.Duration
import java.time.Instant
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class KalmanGPSModule(
    private val prefs: IGPSPreferences = getAppService<UserPreferences>().gps,
    private val logger: Logger = getAppService()
) : GPSModule {
    private var filter: KalmanFilter? = null
    private var reference = Coordinate.zero
    private var reportedAccuracy = DEFAULT_ACCURACY
    private var time: Instant? = null
    private val transition = Matrix.identity(STATE_SIZE)
    private val processNoise = Matrix.zeros(STATE_SIZE, STATE_SIZE)
    private val fullObservation = Matrix.identity(STATE_SIZE)
    private val positionObservation = Matrix.create(POSITION_MEASUREMENT_SIZE, STATE_SIZE) { i, j ->
        if (i == j) 1f else 0f
    }
    private val fullMeasurement = Matrix.zeros(STATE_SIZE, 1)
    private val positionMeasurement = Matrix.zeros(POSITION_MEASUREMENT_SIZE, 1)
    private val fullMeasurementNoise = Matrix.zeros(STATE_SIZE, STATE_SIZE)
    private val positionMeasurementNoise = Matrix.zeros(
        POSITION_MEASUREMENT_SIZE,
        POSITION_MEASUREMENT_SIZE
    )

    override suspend fun update(previousData: ModularGPSData, newData: ModularGPSData): Boolean {
        val smoothing = prefs.smoothing.coerceIn(0, 100)
        if (smoothing == 0) {
            newData.kalmanState = null
            reset()
            return true
        }
        // Another consumer can repeat the accepted fix after the filter has advanced
        // through candidates rejected by a later module.
        if (newData.time == previousData.time && previousData.location != Coordinate.zero) {
            newData.location = previousData.location
            newData.kalmanState = previousData.kalmanState
            newData.horizontalAccuracy = previousData.horizontalAccuracy
            return true
        }
        val hasNewerPrevious = time?.let { previousData.time > it } == true
        if (shouldRestore(previousData, newData, hasNewerPrevious)) {
            restore(previousData)
        }
        if (needsReset(previousData, newData)) {
            logger.debug(
                TAG,
                "Kalman filter reset: fix time moved backward " +
                    "(new: ${newData.time}, previous: ${previousData.time}, filter: $time)"
            )
            reset()
        }

        val lastTime = time
        val sameFix = lastTime != null &&
            (newData.time <= previousData.time || lastTime.toEpochMilli() == newData.time.toEpochMilli())
        if (!sameFix) {
            if (filter == null) {
                restore(newData)
            } else if (lastTime != null) {
                val dt = Duration.between(lastTime, newData.time)
                    .let { it.seconds + it.nano / 1_000_000_000.0 }.toFloat()
                predict(dt, smoothing)
                correct(newData)
                rebaseIfNeeded()
                time = newData.time
            }
        }

        filter?.let {
            newData.location = fromLocal(it.Xk_k[POSITION_EAST, 0], it.Xk_k[POSITION_NORTH, 0])
            newData.kalmanState = snapshot(it)
            newData.horizontalAccuracy = reportedAccuracy
        }
        return true
    }

    private fun predict(dt: Float, smoothing: Int) {
        val kalman = filter ?: return
        transition[POSITION_EAST, VELOCITY_EAST] = dt
        transition[POSITION_NORTH, VELOCITY_NORTH] = dt
        kalman.F = transition
        val accelerationNoise = if (smoothing <= 50) {
            MIN_SMOOTHING_NOISE * (BALANCED_SMOOTHING_NOISE / MIN_SMOOTHING_NOISE)
                .pow(smoothing / 50f)
        } else {
            BALANCED_SMOOTHING_NOISE * (MAX_SMOOTHING_NOISE / BALANCED_SMOOTHING_NOISE)
                .pow((smoothing - 50) / 50f)
        }
        val dt2 = dt * dt
        val dt3 = dt2 * dt
        processNoise[POSITION_EAST, POSITION_EAST] = accelerationNoise * dt3 / 3f
        processNoise[POSITION_NORTH, POSITION_NORTH] = accelerationNoise * dt3 / 3f
        processNoise[POSITION_EAST, VELOCITY_EAST] = accelerationNoise * dt2 / 2f
        processNoise[POSITION_NORTH, VELOCITY_NORTH] = accelerationNoise * dt2 / 2f
        processNoise[VELOCITY_EAST, POSITION_EAST] = accelerationNoise * dt2 / 2f
        processNoise[VELOCITY_NORTH, POSITION_NORTH] = accelerationNoise * dt2 / 2f
        processNoise[VELOCITY_EAST, VELOCITY_EAST] = accelerationNoise * dt
        processNoise[VELOCITY_NORTH, VELOCITY_NORTH] = accelerationNoise * dt
        kalman.Q = processNoise
        kalman.predict()
    }

    private fun correct(data: ModularGPSData) {
        val kalman = filter ?: return
        val accuracy = getAccuracy(data)
        val positionVariance = accuracy * accuracy
        val position = toLocal(data.location)
        val velocity = getVelocity(data)
        if (velocity != null) {
            fullMeasurement[POSITION_EAST, 0] = position.first
            fullMeasurement[POSITION_NORTH, 0] = position.second
            fullMeasurement[VELOCITY_EAST, 0] = velocity.east
            fullMeasurement[VELOCITY_NORTH, 0] = velocity.north
            fullMeasurementNoise[POSITION_EAST, POSITION_EAST] = positionVariance
            fullMeasurementNoise[POSITION_NORTH, POSITION_NORTH] = positionVariance
            fullMeasurementNoise[VELOCITY_EAST, VELOCITY_EAST] = velocity.variance
            fullMeasurementNoise[VELOCITY_NORTH, VELOCITY_NORTH] = velocity.variance
            kalman.H = fullObservation
            kalman.Zk = fullMeasurement
            kalman.R = fullMeasurementNoise
        } else {
            positionMeasurement[POSITION_EAST, 0] = position.first
            positionMeasurement[POSITION_NORTH, 0] = position.second
            positionMeasurementNoise[POSITION_EAST, POSITION_EAST] = positionVariance
            positionMeasurementNoise[POSITION_NORTH, POSITION_NORTH] = positionVariance
            kalman.H = positionObservation
            kalman.Zk = positionMeasurement
            kalman.R = positionMeasurementNoise
        }
        kalman.update()
        val posteriorAccuracy = sqrt(
            max(
                kalman.Pk_k[POSITION_EAST, POSITION_EAST],
                kalman.Pk_k[POSITION_NORTH, POSITION_NORTH]
            ).coerceAtLeast(0f)
        )
        reportedAccuracy = max(accuracy, posteriorAccuracy)
    }

    private fun restore(data: ModularGPSData) {
        val saved = data.kalmanState?.takeIf { isValid(it) }
        if (saved != null) {
            reference = Coordinate(saved.referenceLatitude, saved.referenceLongitude)
            filter = KalmanFilter(STATE_SIZE, STATE_SIZE, CONTROL_SIZE).apply {
                Xk_k = Matrix.column(*saved.state.toFloatArray())
                Pk_k = Matrix.create(STATE_SIZE, STATE_SIZE) { row, column ->
                    saved.covariance[row][column]
                }
            }
            reportedAccuracy = getAccuracy(data)
            time = data.time
            return
        }
        reference = data.location
        val accuracy = getAccuracy(data)
        val positionVariance = accuracy * accuracy
        val velocity = getVelocity(data)
        val velocityVariance = velocity?.variance ?: DEFAULT_VELOCITY_VARIANCE
        filter = KalmanFilter(STATE_SIZE, STATE_SIZE, CONTROL_SIZE).apply {
            Xk_k = Matrix.column(0f, 0f, velocity?.east ?: 0f, velocity?.north ?: 0f)
            Pk_k = Matrix.zeros(STATE_SIZE, STATE_SIZE).apply {
                this[POSITION_EAST, POSITION_EAST] = positionVariance
                this[POSITION_NORTH, POSITION_NORTH] = positionVariance
                this[VELOCITY_EAST, VELOCITY_EAST] = velocityVariance
                this[VELOCITY_NORTH, VELOCITY_NORTH] = velocityVariance
            }
        }
        reportedAccuracy = accuracy
        time = data.time
    }

    private fun snapshot(kalman: KalmanFilter): GPSKalmanState {
        return GPSKalmanState(
            state = List(STATE_SIZE) { kalman.Xk_k[it, 0] },
            covariance = List(STATE_SIZE) { row ->
                List(STATE_SIZE) { column -> kalman.Pk_k[row, column] }
            },
            referenceLatitude = reference.latitude,
            referenceLongitude = reference.longitude
        )
    }

    private fun isValid(state: GPSKalmanState): Boolean {
        return state.referenceLatitude.isFinite() && state.referenceLongitude.isFinite() &&
            state.state.size == STATE_SIZE && state.state.all { it.isFinite() } &&
            state.covariance.size == STATE_SIZE && state.covariance.all { row ->
                row.size == STATE_SIZE && row.all { it.isFinite() }
            }
    }

    private fun getVelocity(data: ModularGPSData): VelocityMeasurement? {
        if (data.speedSource != SpeedSource.Provider) return null
        val speed = data.speed.convertTo(DistanceUnits.Meters, TimeUnits.Seconds).value
            .takeIf { it.isFinite() && it >= 0f } ?: return null
        val direction = data.rawBearing?.takeIf { it.isFinite() }
            ?: data.bearing?.value?.takeIf { it.isFinite() }
            ?: return null
        val speedError = data.speedAccuracy?.takeIf { it.isFinite() && it > 0f }
            ?: max(3f, speed * 0.25f)
        val directionError = data.bearingAccuracy?.takeIf { it.isFinite() && it >= 0f }
            ?.coerceAtMost(90f) ?: 30f
        val radians = Math.toRadians(direction.toDouble())
        val lateralError = speed * sin(Math.toRadians(directionError.toDouble())).toFloat()
        return VelocityMeasurement(
            speed * sin(radians).toFloat(),
            speed * cos(radians).toFloat(),
            (speedError * speedError + lateralError * lateralError).coerceAtLeast(0.01f)
        )
    }

    private fun getAccuracy(data: ModularGPSData): Float =
        data.horizontalAccuracy?.takeIf { it.isFinite() && it > 0f } ?: DEFAULT_ACCURACY

    private fun toLocal(location: Coordinate): Pair<Float, Float> {
        val distance = reference.distanceTo(location)
        val bearing = Math.toRadians(reference.bearingTo(location).value.toDouble())
        return Pair(distance * sin(bearing).toFloat(), distance * cos(bearing).toFloat())
    }

    private fun fromLocal(east: Float, north: Float): Coordinate {
        val distance = sqrt(east * east + north * north)
        if (distance == 0f) return reference
        val bearing = Bearing.from(Math.toDegrees(atan2(east, north).toDouble()).toFloat())
        return reference.plus(Distance.meters(distance), bearing)
    }

    private fun rebaseIfNeeded() {
        val kalman = filter ?: return
        val east = kalman.Xk_k[POSITION_EAST, 0]
        val north = kalman.Xk_k[POSITION_NORTH, 0]
        if (sqrt(east * east + north * north) <= MAX_REFERENCE_DISTANCE) return
        reference = fromLocal(east, north)
        kalman.Xk_k[POSITION_EAST, 0] = 0f
        kalman.Xk_k[POSITION_NORTH, 0] = 0f
    }

    private fun needsReset(previous: ModularGPSData, next: ModularGPSData): Boolean {
        if (next.time < previous.time) return true
        val lastTime = time ?: return false
        return next.time > previous.time && next.time < lastTime
    }

    private fun shouldRestore(
        previous: ModularGPSData,
        next: ModularGPSData,
        hasNewerPrevious: Boolean
    ): Boolean {
        val needsState = filter == null || hasNewerPrevious
        val hasPreviousFix = previous.location != Coordinate.zero
        return needsState && hasPreviousFix && previous.time <= next.time
    }

    private fun reset() {
        filter = null
        reference = Coordinate.zero
        reportedAccuracy = DEFAULT_ACCURACY
        time = null
    }

    private data class VelocityMeasurement(val east: Float, val north: Float, val variance: Float)

    companion object {
        private const val TAG = "KalmanGPSModule"
        private const val STATE_SIZE = 4
        private const val POSITION_MEASUREMENT_SIZE = 2
        private const val CONTROL_SIZE = 2
        private const val POSITION_EAST = 0
        private const val POSITION_NORTH = 1
        private const val VELOCITY_EAST = 2
        private const val VELOCITY_NORTH = 3
        private const val DEFAULT_ACCURACY = 50f
        private const val DEFAULT_VELOCITY_VARIANCE = 9f
        private const val MIN_SMOOTHING_NOISE = 10_000f
        private const val BALANCED_SMOOTHING_NOISE = 0.1f
        private const val MAX_SMOOTHING_NOISE = 0.01f
        private const val MAX_REFERENCE_DISTANCE = 200f
    }
}
