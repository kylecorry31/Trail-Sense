package com.kylecorry.trail_sense.tools.experimentation

import android.hardware.SensorManager
import android.os.Bundle
import android.util.Range
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kylecorry.andromeda.core.ui.setTextDistinct
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.interval
import com.kylecorry.andromeda.fragments.observe
import com.kylecorry.andromeda.signal.CellSignal
import com.kylecorry.andromeda.signal.CellSignalSensor
import com.kylecorry.luna.coroutines.onDefault
import com.kylecorry.sol.math.SolMath.roundPlaces
import com.kylecorry.sol.math.SolMath.toDegrees
import com.kylecorry.sol.science.geology.Geology
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.databinding.FragmentExperimentationBinding
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.toRelativeDistance
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.log10
import kotlin.math.pow

class ExperimentationFragment : BoundFragment<FragmentExperimentationBinding>() {

    private var inclination by state(0f)
    private var strength by state(0f)
    private var location by state(Coordinate.zero)
    private val I = FloatArray(9)
    private val lock = Any()
    private val formatter by lazy { FormatService.getInstance(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private var signals by state(emptyList<CellSignal>())

    override fun generateBinding(
        layoutInflater: LayoutInflater, container: ViewGroup?
    ): FragmentExperimentationBinding {
        return FragmentExperimentationBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val cellSignalSensor = CellSignalSensor(
            requireContext(),
            updateCellCache = true,
            removeUnregisteredSignals = false
        )
        observe(cellSignalSensor) {
            signals = cellSignalSensor.signals
        }

    }


    override fun onUpdate() {
        super.onUpdate()

        val distanceText = signals.joinToString("\n\n") { signal ->
            val (lower, upper) = estimateDistanceBoundsRSRP(signal.dbm.toFloat(), 1800f, 43f)
            val lowerFormatted = formatter.formatDistance(
                Distance.meters(lower).convertTo(prefs.baseDistanceUnits).toRelativeDistance(),
                2
            )
            val upperFormatted = formatter.formatDistance(
                Distance.meters(upper).convertTo(prefs.baseDistanceUnits).toRelativeDistance(),
                2
            )
            "${signal.id} ${signal.network.name}\nLower: $lowerFormatted, Upper: $upperFormatted\n${if (signal.isRegistered) "Registered" else "Unregistered"}\n${
                formatter.formatPercentage(
                    signal.strength
                )
            }"
        }

        binding.text.text = distanceText

    }

    fun estimateDistanceBoundsRSRP(
        rsrp: Float,
        frequency: Float,
        transmitPower: Float
    ): Pair<Float, Float> {
        val pld0 = 20 * log10(frequency) + 20
        val pld = transmitPower - rsrp
        val ns = listOf(2f, 4f)
        val distances = ns.map { n -> 10f.pow((pld - pld0) / (10 * n)) }

        // Return the lower and upper bounds
        val lowerBound = distances.minOrNull() ?: 0f
        val upperBound = distances.maxOrNull() ?: 0f

        return Pair(lowerBound, upperBound)
    }

    private fun estimateDistanceBounds(
        rssi: Float,
        frequency: Float,
        transmitPower: Float
    ): Pair<Float, Float> {
        // Speed of light in m/s
        val c = 3f * 10f.pow(8)

        // Frequency in Hz
        val fHz = frequency * 10f.pow(6)

        // Reference path loss at 1 meter (free space)
        val PL0 = 20 * log10((4 * Math.PI.toFloat() * fHz) / c)

        // Calculate path loss
        val PL = transmitPower - rssi

        // Path loss exponents for different environments
        val nValues = listOf(2f, 3f, 4f)

        // Calculate distances for each path loss exponent
        val distances = nValues.map { n -> 10f.pow((PL - PL0) / (10 * n)) }

        // Return the lower and upper bounds
        val lowerBound = distances.minOrNull() ?: 0f
        val upperBound = distances.maxOrNull() ?: 0f

        return Pair(lowerBound, upperBound)
    }

    private fun setupGeomagneticLocation() {
        val sensors = SensorService(requireContext())
        val accelerometer = sensors.getGravity()
        val magnetometer = sensors.getMagnetometer(true)

        observe(accelerometer) {
            updateInclination(accelerometer.rawAcceleration, magnetometer.rawMagneticField)
        }

        observe(magnetometer) {
            updateInclination(accelerometer.rawAcceleration, magnetometer.rawMagneticField)
            strength = magnetometer.magneticField.magnitude()
        }

        interval(1000) {
            geomagneticLocationGridSearch()
        }
    }

    private fun updateInclination(gravity: FloatArray, geomagnetic: FloatArray) {
        synchronized(lock) {
            SensorManager.getRotationMatrix(null, I, gravity, geomagnetic)
            inclination = -SensorManager.getInclination(I).toDegrees()
        }

    }


    private fun updateGeomagneticLocation() {
        val inclinationText = memo("inclination", inclination.roundPlaces(2)) {
            formatter.formatDegrees(inclination, 2)
        }

        val strengthText = memo("strength", strength.roundPlaces(2)) {
            formatter.formatMagneticField(strength, 2)
        }

        val locationText = memo("location", location) {
            formatter.formatLocation(location)
        }

        binding.text.setTextDistinct("$inclinationText\n$strengthText\n${locationText}")
    }

    private suspend fun geomagneticLocationGridSearch() = onDefault {
        // TODO: It can limit the search using inclination
        location = search(
            Range(-90.0, 90.0),
            Range(-180.0, 180.0),
            0.5,
            strength,
            inclination
        )

//        val optimizer = SimulatedAnnealingOptimizer(
//            100.0,
//            0.1,
//            8000
//        )
//
//        val strength = strength
//        val inclination = inclination
//
//        val best = optimizer.optimize(
//            com.kylecorry.sol.math.Range(-90.0, 90.0),
//            com.kylecorry.sol.math.Range(-180.0, 180.0),
//            false
//        ) { latitude, longitude ->
//            val posField = Geology.getGeomagneticField(Coordinate(latitude, longitude))
//            val posInclination = Geology.getGeomagneticInclination(Coordinate(latitude, longitude))
//            hypot(
//                abs(posField.magnitude() - strength),
//                abs(posInclination - inclination)
//            ).toDouble()
//        }
//
//        location = Coordinate(best.first, best.second)

        // TODO: Progressively increase search resolution
    }

    // TODO: Return top X best matches
    private suspend fun search(
        latRange: Range<Double>,
        lonRange: Range<Double>,
        step: Double,
        strength: Float,
        inclination: Float
    ): Coordinate = onDefault {
        var nearestLocation = Coordinate.zero
        var nearestDelta = Float.MAX_VALUE
        val latStart = (latRange.lower * 10).toInt()
        val latEnd = (latRange.upper * 10).toInt()
        val lonStart = (lonRange.lower * 10).toInt()
        val lonEnd = (lonRange.upper * 10).toInt()
        for (latSearch in latStart..latEnd step (step * 10).toInt()) {
            for (lonSearch in lonStart..lonEnd step (step * 10).toInt()) {
                val lat = latSearch / 10.0
                val lon = lonSearch / 10.0
                val posField =
                    Geology.getGeomagneticField(Coordinate(lat, lon))
                // TODO: Calculate this from the field itself
                val posInclination =
                    Geology.getGeomagneticInclination(Coordinate(lat, lon))

                val delta = hypot(
                    abs(posField.magnitude() - strength),
                    abs(posInclination - inclination)
                )

                if (delta < nearestDelta) {
                    nearestDelta = delta
                    nearestLocation = Coordinate(lat, lon)
                }
            }
        }

        nearestLocation
    }

}