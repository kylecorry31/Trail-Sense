package com.kylecorry.trail_sense.tools.augmented_reality

import android.content.Context
import android.graphics.Color
import android.graphics.Path
import android.util.AttributeSet
import com.kylecorry.andromeda.canvas.CanvasView
import com.kylecorry.andromeda.core.ui.Colors.withAlpha
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.sense.clinometer.CameraClinometer
import com.kylecorry.andromeda.sense.clinometer.SideClinometer
import com.kylecorry.sol.math.Euler
import com.kylecorry.sol.math.Quaternion
import com.kylecorry.sol.math.SolMath.real
import com.kylecorry.sol.math.SolMath.toDegrees
import com.kylecorry.sol.math.SolMath.toRadians
import com.kylecorry.sol.math.Vector3
import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.camera.AugmentedRealityUtils
import com.kylecorry.trail_sense.shared.declination.DeclinationFactory
import com.kylecorry.trail_sense.shared.declination.DeclinationUtils
import com.kylecorry.trail_sense.shared.sensors.SensorService
import java.time.Duration
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

// TODO: This needs a parent view that has the camera, this, and any buttons (like the freeform button)
class AugmentedRealityView : CanvasView {

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    var fov: Size = Size(45f, 45f)

    private var orientation = Quaternion.zero
    private var inverseOrientation = Quaternion.zero

    private val horizon = Path()

    private val userPrefs = UserPreferences(context)
    private val sensors = SensorService(context)
    private val compass = sensors.getCompass()
    private val clinometer = CameraClinometer(context)
    private val sideClinometer = SideClinometer(context)
    private val gps = sensors.getGPS(frequency = Duration.ofMillis(200))
    private val altimeter = sensors.getAltimeter(gps = gps)
    private val declinationProvider = DeclinationFactory().getDeclinationStrategy(
        userPrefs,
        gps
    )
    private val isTrueNorth = userPrefs.compass.useTrueNorth

    var azimuth = 0f
        private set
    var inclination = 0f
        private set
    var sideInclination = 0f
        private set
    val location: Coordinate
        get() = gps.location

    val altitude: Float
        get() = altimeter.altitude

    val reticleDiameter: Float
        get() = dp(36f)

    private val layers = mutableListOf<ARLayer>()
    private val layerLock = Any()

    private var useSensors = true
    private val orientationLock = Any()

    fun start() {
        compass.start(this::onSensorUpdate)
        clinometer.start(this::onSensorUpdate)
        sideClinometer.start(this::onSensorUpdate)
        gps.start(this::onSensorUpdate)
        altimeter.start(this::onSensorUpdate)
    }

    fun stop() {
        compass.stop(this::onSensorUpdate)
        clinometer.stop(this::onSensorUpdate)
        sideClinometer.stop(this::onSensorUpdate)
        gps.stop(this::onSensorUpdate)
        altimeter.stop(this::onSensorUpdate)
    }

    fun addLayer(layer: ARLayer) {
        synchronized(layerLock) {
            layers.add(layer)
        }
    }

    fun removeLayer(layer: ARLayer) {
        synchronized(layerLock) {
            layers.remove(layer)
        }
    }

    fun clearLayers() {
        synchronized(layerLock) {
            layers.clear()
        }
    }

    fun setLayers(layers: List<ARLayer>) {
        synchronized(layerLock) {
            this.layers.clear()
            this.layers.addAll(layers)
        }
    }

    // TODO: Take in zoom
    // TODO: Interpolate
    fun pointAt(coordinate: HorizonCoordinate) {
        synchronized(orientationLock) {
            useSensors = false
            azimuth = getActualBearing(coordinate)
            inclination = coordinate.elevation
            sideInclination = 0f
        }
    }

    fun setFreeform(isFreeform: Boolean) {
        synchronized(orientationLock) {
            useSensors = !isFreeform
            sideInclination = 0f
            // TODO: Allow dragging
        }
    }

    private fun onSensorUpdate(): Boolean {
        if (isTrueNorth) {
            compass.declination = declinationProvider.getDeclination()
        } else {
            compass.declination = 0f
        }
        synchronized(orientationLock) {
            if (useSensors) {
                azimuth = compass.rawBearing
                inclination = clinometer.incline
                sideInclination = sideClinometer.angle - 90f
            }
        }
        return true
    }


    override fun setup() {
        updateOrientation()
    }

    override fun draw() {
        updateOrientation()
        clear()

        // TODO: Extract these to layers
        drawNorth()
        drawHorizon()

        layers.forEach {
            it.draw(this, this)
        }

        layers.reversed().forEach {
            it.onFocus(this, this)
        }

        // TODO: Draw the reticle label
        drawReticle()
    }

    private fun drawNorth() {
        // TODO: This is not rendering properly when rotated and pointing south
        val north = Path()

        for (i in -90..90 step 5) {
            val pixel = toPixel(HorizonCoordinate(0f, i.toFloat()))
            if (i == -90) {
                north.moveTo(pixel.x, pixel.y)
            } else {
                north.lineTo(pixel.x, pixel.y)
            }
        }

        noFill()
        stroke(Color.WHITE)
        strokeWeight(2f)
        path(north)
        noStroke()
    }

    private fun drawReticle() {
        stroke(Color.WHITE.withAlpha(127))
        strokeWeight(dp(2f))
        noFill()
        circle(width / 2f, height / 2f, reticleDiameter)
    }

    private fun drawHorizon() {
        horizon.reset()
        var horizonPathStarted = false

        val minAngle = (azimuth - fov.width).toInt()
        val maxAngle = (azimuth + fov.width).toInt()

        for (i in minAngle..maxAngle step 5) {
            val pixel = toPixel(HorizonCoordinate(i.toFloat(), 0f))
            if (!horizonPathStarted) {
                horizon.moveTo(pixel.x, pixel.y)
                horizonPathStarted = true
            } else {
                horizon.lineTo(pixel.x, pixel.y)
            }
        }

        noFill()
        stroke(Color.WHITE)
        strokeWeight(2f)
        path(horizon)
        noStroke()
    }

    private fun toWorldSpace(bearing: Float, elevation: Float, distance: Float): Vector3 {
        val thetaRad = elevation.toRadians()
        val phiRad = bearing.toRadians()

        val cosTheta = cos(thetaRad)
        val x = distance * cosTheta * sin(phiRad)
        val y = distance * cosTheta * cos(phiRad)
        val z = distance * sin(thetaRad)
        return Vector3(x, y, z)
    }

    private fun applyRotation(vector: Vector3): Vector3 {
        return inverseOrientation.rotate(vector)
    }

    private fun toSpherical(vector: Vector3): Vector3 {
        val r = vector.magnitude()
        val theta = asin(vector.z / r).toDegrees().real(0f)
        val phi = atan2(vector.x, vector.y).toDegrees().real(0f)
        return Vector3(r, theta, phi)
    }

    /**
     * Converts an angular size to a pixel size
     * @param angularSize The angular size in degrees
     * @return The pixel size
     */
    fun sizeToPixel(angularSize: Float): Float {
        return (width / fov.width) * angularSize
    }

    fun sizeToPixel(diameter: Distance, distance: Distance): Float {
        val angularSize = AugmentedRealityUtils.getAngularSize(diameter, distance)
        return sizeToPixel(angularSize)
    }

    // TODO: These are off by a about a degree when you point the device at around 45 degrees (ex. a north line appears 1 degree to the side of actual north)
    // TODO: This may just be the compass being off
    /**
     * Gets the pixel coordinate of a point on the screen given the bearing and azimuth.
     * @param coordinate The horizon coordinate of the point
     * @return The pixel coordinate of the point
     */
    fun toPixel(coordinate: HorizonCoordinate): PixelCoordinate {
        val bearing = getActualBearing(coordinate)

        val world = toWorldSpace(bearing, coordinate.elevation, 1f)
        val rotated = applyRotation(world)
        val spherical = toSpherical(rotated)

        // TODO: Try out Matrix.perspectiveM

        // The rotation of the device has been negated, so azimuth = 0 and inclination = 0 is used
        return AugmentedRealityUtils.getPixelLinear(
            spherical.z,
            0f,
            spherical.y,
            0f,
            Size(width.toFloat(), height.toFloat()),
            fov
        )
    }

    fun toPixel(coordinate: Coordinate, elevation: Float? = null): PixelCoordinate {
        val bearing = gps.location.bearingTo(coordinate).value
        val distance = gps.location.distanceTo(coordinate)
        val elevationAngle = if (elevation == null) {
            0f
        } else {
            atan2((elevation - gps.altitude), distance).toDegrees()
        }
        return toPixel(HorizonCoordinate(bearing, elevationAngle, true))
    }

    private fun getActualBearing(coordinate: HorizonCoordinate): Float {
        return if (isTrueNorth && !coordinate.isTrueNorth) {
            // Convert coordinate to true north
            DeclinationUtils.toTrueNorthBearing(
                coordinate.bearing,
                declinationProvider.getDeclination()
            )
        } else if (!isTrueNorth && coordinate.isTrueNorth) {
            // Convert coordinate to magnetic north
            DeclinationUtils.fromTrueNorthBearing(
                coordinate.bearing,
                declinationProvider.getDeclination()
            )
        } else {
            coordinate.bearing
        }
    }

    private fun updateOrientation() {
        orientation = Quaternion.from(Euler(inclination, -sideInclination, -azimuth))
        inverseOrientation = orientation.inverse()
    }

    data class HorizonCoordinate(
        val bearing: Float,
        val elevation: Float,
        val isTrueNorth: Boolean = true
    )

    data class Point(
        val coordinate: HorizonCoordinate,
        val size: Float,
        val color: Int,
        val text: String? = null
    )

}