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
import com.kylecorry.luna.coroutines.onDefault
import com.kylecorry.sol.math.SolMath.roundPlaces
import com.kylecorry.sol.math.SolMath.toDegrees
import com.kylecorry.sol.math.optimization.HillClimbingOptimizer
import com.kylecorry.sol.math.optimization.SimulatedAnnealingOptimizer
import com.kylecorry.sol.science.geology.Geology
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.databinding.FragmentExperimentationBinding
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.sensors.SensorService
import kotlin.math.abs
import kotlin.math.hypot

class ExperimentationFragment : BoundFragment<FragmentExperimentationBinding>() {

    private var inclination by state(0f)
    private var strength by state(0f)
    private var location by state(Coordinate.zero)
    private val I = FloatArray(9)
    private val lock = Any()
    private val formatter by lazy { FormatService.getInstance(requireContext()) }

    override fun generateBinding(
        layoutInflater: LayoutInflater, container: ViewGroup?
    ): FragmentExperimentationBinding {
        return FragmentExperimentationBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

    override fun onUpdate() {
        super.onUpdate()

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