package com.kylecorry.trail_sense.tools.weather.domain.sealevel

import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.PressureUnits
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.tools.weather.domain.RawWeatherObservation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.Instant
import java.util.Random
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

internal class LoessSeaLevelCalibrationStrategyTest {

    @Test
    fun calibrateWithConsecutiveNoisyPressureReadings() {
        val pressures = List(30) { index ->
            if (index in 10..19) {
                if (index % 2 == 0) 900f else 1100f
            } else {
                1000f + (index % 3 - 1) * 0.1f
            }
        }
        val readings = readings(pressures, intervalMinutes = 1)

        val calibrated = LoessSeaLevelCalibrationStrategy(NullSeaLevelCalibrationStrategy()).calibrate(readings)

        assertEquals(readings.size, calibrated.size)
        assertEquals(readings.map { it.time }, calibrated.map { it.time })
        assertTrue(calibrated.all { it.value.value.isFinite() })
    }

    @ParameterizedTest
    @ValueSource(ints = [10, 30, 60])
    fun reducesNoise(intervalMinutes: Int) {
        val clean = frontPressure(intervalMinutes)
        val random = Random(3)
        val noisy = clean.map { it + (random.nextGaussian() * 0.3).toFloat() }

        val calibrated = LoessSeaLevelCalibrationStrategy(NullSeaLevelCalibrationStrategy())
            .calibrate(readings(noisy, intervalMinutes))
            .map { it.value.value }

        assertTrue(rmse(calibrated, clean) < rmse(noisy, clean) * 0.6f)
    }

    @Test
    fun keepsCleanPressureClose() {
        val clean = diurnalPressure(30)

        val calibrated = LoessSeaLevelCalibrationStrategy(NullSeaLevelCalibrationStrategy())
            .calibrate(readings(clean, 30))
            .map { it.value.value }

        assertTrue(rmse(calibrated, clean) < 0.05f)
    }

    @Test
    fun moreSmoothingReducesMoreNoise() {
        val clean = frontPressure(10)
        val random = Random(3)
        val noisy = readings(clean.map { it + (random.nextGaussian() * 0.8).toFloat() }, 10)

        fun error(smoothing: Float): Float {
            val calibrated = LoessSeaLevelCalibrationStrategy(NullSeaLevelCalibrationStrategy(), smoothing)
                .calibrate(noisy)
                .map { it.value.value }
            return rmse(calibrated, clean)
        }

        assertTrue(error(0.75f) < error(0.25f))
    }

    @Test
    fun noSmoothingReturnsTheReadings() {
        val pressures = listOf(1000f, 1003f, 998f, 1001f)

        val calibrated = LoessSeaLevelCalibrationStrategy(NullSeaLevelCalibrationStrategy(), 0f)
            .calibrate(readings(pressures, 30))

        assertEquals(pressures, calibrated.map { it.value.value })
    }

    @Test
    fun tooFewReadingsAreNotSmoothed() {
        val strategy = LoessSeaLevelCalibrationStrategy(NullSeaLevelCalibrationStrategy())

        assertEquals(emptyList<Reading<Pressure>>(), strategy.calibrate(emptyList()))
        assertEquals(
            listOf(1000f, 1010f),
            strategy.calibrate(readings(listOf(1000f, 1010f), 30)).map { it.value.value }
        )
    }

    @Test
    fun keepsThePressureUnits() {
        val inHg = object : ISeaLevelCalibrationStrategy {
            override fun calibrate(readings: List<Reading<RawWeatherObservation>>): List<Reading<Pressure>> {
                return readings.map {
                    Reading(Pressure.hpa(it.value.pressure).convertTo(PressureUnits.Inhg), it.time)
                }
            }
        }
        val pressures = diurnalPressure(30)

        val calibrated = LoessSeaLevelCalibrationStrategy(inHg).calibrate(readings(pressures, 30))

        assertTrue(calibrated.all { it.value.units == PressureUnits.Inhg })
        assertEquals(pressures.first(), calibrated.first().value.hpa().value, 0.1f)
    }

    private fun diurnalPressure(intervalMinutes: Int): List<Float> {
        return List(72 * 60 / intervalMinutes) {
            val hours = it * intervalMinutes / 60f
            1013f + 0.5f * sin(2 * Math.PI * hours / 12).toFloat() + hours * 0.05f
        }
    }

    private fun frontPressure(intervalMinutes: Int): List<Float> {
        return List(72 * 60 / intervalMinutes) {
            val hours = it * intervalMinutes / 60f
            1015f - 8f / (1 + exp(-(hours - 36) / 4f))
        }
    }

    private fun readings(pressures: List<Float>, intervalMinutes: Int): List<Reading<RawWeatherObservation>> {
        return pressures.mapIndexed { index, pressure ->
            Reading(
                RawWeatherObservation(index.toLong(), pressure, 0f, 20f),
                START.plusSeconds(index * intervalMinutes * 60L)
            )
        }
    }

    private fun rmse(a: List<Float>, b: List<Float>): Float {
        return sqrt(a.zip(b).sumOf { (x, y) -> ((x - y) * (x - y)).toDouble() } / a.size).toFloat()
    }

    companion object {
        private val START = Instant.parse("2026-01-01T00:00:00Z")
    }
}
