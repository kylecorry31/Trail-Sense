package com.kylecorry.trail_sense.tools.celestial_navigation.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock

/**
 * Records raw gravity and magnetometer samples so that a capture can use the average over its
 * exposure, and ground mode can use the average over an entire session.
 */
class GravityTracker(context: Context) : SensorEventListener {

    private class Sample(val time: Long, val x: Double, val y: Double, val z: Double)

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val samples = ArrayDeque<Sample>()
    private val magnetic = ArrayDeque<Sample>()
    private val lock = Any()

    private var sessionX = 0.0
    private var sessionY = 0.0
    private var sessionZ = 0.0
    private var sessionCount = 0L
    var sessionStart = 0L
        private set

    fun start(useGravitySensor: Boolean) {
        stop()
        val type = if (useGravitySensor) Sensor.TYPE_GRAVITY else Sensor.TYPE_ACCELEROMETER
        sensorManager.getDefaultSensor(type)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        resetSession()
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    fun now(): Long = SystemClock.elapsedRealtimeNanos()

    fun resetSession() {
        synchronized(lock) {
            sessionX = 0.0
            sessionY = 0.0
            sessionZ = 0.0
            sessionCount = 0
            sessionStart = now()
        }
    }

    fun sessionMean(): DoubleArray? = synchronized(lock) {
        if (sessionCount == 0L) null else unit(doubleArrayOf(sessionX, sessionY, sessionZ))
    }

    fun sessionSampleCount(): Long = synchronized(lock) { sessionCount }

    fun mean(from: Long, to: Long): DoubleArray? = synchronized(lock) {
        mean(samples, from, to)
    }

    fun recentMean(windowMs: Long): DoubleArray? {
        val end = now()
        return mean(end - windowMs * 1_000_000, end)
    }

    fun magneticMean(from: Long, to: Long): DoubleArray? = synchronized(lock) {
        mean(magnetic, from, to, normalize = false)
    }

    /**
     * Device -> ENU rotation (magnetic north) from gravity and magnetometer
     */
    fun orientation(up: DoubleArray, magneticField: DoubleArray?): DoubleArray? {
        magneticField ?: return null
        val gravity = up.map { it.toFloat() }.toFloatArray()
        val field = magneticField.map { it.toFloat() }.toFloatArray()
        val r = FloatArray(9)
        if (!SensorManager.getRotationMatrix(r, null, gravity, field)) return null
        return r.map { it.toDouble() }.toDoubleArray()
    }

    private fun mean(source: ArrayDeque<Sample>, from: Long, to: Long, normalize: Boolean = true): DoubleArray? {
        var x = 0.0
        var y = 0.0
        var z = 0.0
        var count = 0
        for (sample in source) {
            if (sample.time < from || sample.time > to) continue
            x += sample.x; y += sample.y; z += sample.z
            count++
        }
        if (count == 0) {
            // Fall back to the nearest sample
            val nearest = source.minByOrNull { kotlin.math.abs(it.time - (from + to) / 2) } ?: return null
            x = nearest.x; y = nearest.y; z = nearest.z; count = 1
        }
        val v = doubleArrayOf(x / count, y / count, z / count)
        return if (normalize) unit(v) else v
    }

    private fun unit(v: DoubleArray): DoubleArray {
        val n = kotlin.math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])
        return doubleArrayOf(v[0] / n, v[1] / n, v[2] / n)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val sample = Sample(
            event.timestamp,
            event.values[0].toDouble(),
            event.values[1].toDouble(),
            event.values[2].toDouble()
        )
        synchronized(lock) {
            if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                magnetic.addLast(sample)
                while (magnetic.size > MAX_SAMPLES / 4) magnetic.removeFirst()
            } else {
                samples.addLast(sample)
                while (samples.size > MAX_SAMPLES) samples.removeFirst()
                sessionX += sample.x
                sessionY += sample.y
                sessionZ += sample.z
                sessionCount++
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    companion object {
        private const val MAX_SAMPLES = 20_000
    }
}
