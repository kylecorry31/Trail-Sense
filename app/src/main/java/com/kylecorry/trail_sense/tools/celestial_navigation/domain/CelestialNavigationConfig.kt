package com.kylecorry.trail_sense.tools.celestial_navigation.domain

import com.kylecorry.sol.math.Vector3Precise
import com.kylecorry.sol.math.geometry.Rotation
import com.kylecorry.sol.science.astronomy.navigation.SkyImage
import com.kylecorry.sol.science.astronomy.navigation.StarIdentification
import com.kylecorry.sol.science.astronomy.navigation.StarIdentifierSettings
import com.kylecorry.sol.science.optics.PinholeCamera
import com.kylecorry.sol.units.Coordinate
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

enum class LocationMethod(val label: String) {
    GravityZenith("Gravity zenith (photo)"),
    Altitudes("Star altitudes (gravity)"),
    AltitudeAzimuth("Altitude + compass azimuth"),
    SensorPlateSolve("Plate solve from sensors")
}

data class CelestialNavigationConfig(
    val identifier: StarIdentifierSettings = StarIdentifierSettings(),
    val plateSolveTolerance: Float = 0.04f,
    val plateSolveMinMatches: Int = 5
)

/**
 * A photo of the sky and the sensor readings taken with it
 * @param compass camera to ENU (true north) from the compass, if available
 */
data class StarPhoto(val image: SkyImage, val compass: Rotation?)

/**
 * @param matches the index of each identified star in the photo and its name
 */
data class CelestialFix(
    val location: Coordinate,
    val method: LocationMethod,
    val matches: List<Pair<Int, String>>,
    val rmsDegrees: Double? = null,
    val camera: PinholeCamera? = null
)

/**
 * Conversions from Android device coordinates (x right, y up, out of the screen) to Sol's camera
 * coordinates (x right, y down, looking down +z) for the back camera in the device's natural
 * orientation
 */
object CameraFrame {
    private val fromDevice = Rotation.fromMatrix(
        doubleArrayOf(
            1.0, 0.0, 0.0,
            0.0, -1.0, 0.0,
            0.0, 0.0, -1.0
        )
    )

    fun fromDevice(vector: DoubleArray): Vector3Precise {
        return Vector3Precise(vector[0], -vector[1], -vector[2]).normalize()
    }

    /**
     * @param deviceToEnu a row major device to ENU rotation matrix
     */
    fun cameraToEnu(deviceToEnu: DoubleArray): Rotation {
        return Rotation.fromMatrix(deviceToEnu) * fromDevice
    }

    /**
     * Camera to ENU from gravity alone. The heading is arbitrary (the camera looks north), so only
     * altitudes and the angles between stars are meaningful.
     */
    fun gravityOnly(up: Vector3Precise): Rotation {
        val u = up.normalize()
        val forward = Vector3Precise(0.0, 0.0, 1.0)
        var north = forward - u * forward.dot(u)
        if (north.magnitude() < MIN_HORIZONTAL) {
            val top = Vector3Precise(0.0, -1.0, 0.0)
            north = top - u * top.dot(u)
        }
        north = north.normalize()
        val east = north.cross(u)
        // Rows are the ENU axes in camera coordinates
        return Rotation.fromMatrix(
            doubleArrayOf(
                east.x, east.y, east.z,
                north.x, north.y, north.z,
                u.x, u.y, u.z
            )
        )
    }

    private const val MIN_HORIZONTAL = 1e-3
}

object SkyVectors {
    fun fromHorizon(azimuth: Double, altitude: Double): Vector3Precise {
        val az = Math.toRadians(azimuth)
        val alt = Math.toRadians(altitude)
        return Vector3Precise(cos(alt) * sin(az), cos(alt) * cos(az), sin(alt))
    }

    /**
     * @return the azimuth (0 - 360) and altitude in degrees
     */
    fun toHorizon(vector: Vector3Precise): Pair<Double, Double> {
        val n = vector.normalize()
        val altitude = Math.toDegrees(asin(n.z.coerceIn(-1.0, 1.0)))
        val azimuth = (Math.toDegrees(atan2(n.x, n.y)) + 360) % 360
        return azimuth to altitude
    }

    fun angle(a: Vector3Precise, b: Vector3Precise): Double {
        return Math.toDegrees(acos((a.dot(b) / (a.magnitude() * b.magnitude())).coerceIn(-1.0, 1.0)))
    }
}

fun StarIdentification.toMatches(): List<Pair<Int, String>> {
    return stars.map { it.index to it.body.name }
}
