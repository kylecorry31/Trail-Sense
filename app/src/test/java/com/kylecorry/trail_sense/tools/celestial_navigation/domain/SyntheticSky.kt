package com.kylecorry.trail_sense.tools.celestial_navigation.domain

import com.kylecorry.sol.math.Vector3Precise
import com.kylecorry.sol.math.geometry.Rotation
import com.kylecorry.sol.science.astronomy.Astronomy
import com.kylecorry.sol.science.astronomy.locators.Planet
import com.kylecorry.sol.science.astronomy.navigation.ImageStar
import com.kylecorry.sol.science.astronomy.navigation.SkyImage
import com.kylecorry.sol.science.astronomy.stars.STAR_CATALOG
import com.kylecorry.sol.science.optics.PinholeCamera
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import java.time.ZonedDateTime
import kotlin.math.pow
import kotlin.random.Random

data class SkyScenario(
    val name: String,
    val limitingMagnitude: Float = 3.5f,
    val pixelNoise: Double = 1.0,
    val focalError: Double = 0.03,
    val maxK1: Double = 0.03,
    val tiltBiasDegrees: Double = 0.3,
    val tiltNoiseDegrees: Double = 0.1,
    val compassErrorDegrees: Double = 5.0,
    val falseStars: Int = 2,
    val dropout: Double = 0.1,
    val guessErrorKm: Double = 800.0,
    val minPointingAltitude: Double = 35.0,
    val maxPointingAltitude: Double = 80.0,
    val width: Int = 3000,
    val height: Int = 4000,
    val verticalFov: Double = 66.0,
    val maxStars: Int = 40
)

/**
 * @param deviceUp gravity's up in device coordinates
 * @param deviceToEnu the true device orientation
 * @param compassDeviceToEnu the device orientation from the (noisy) compass
 */
class SyntheticPhoto(
    val stars: List<ImageStar>,
    val nominalCamera: PinholeCamera,
    val deviceUp: DoubleArray,
    val compassDeviceToEnu: Rotation,
    val time: ZonedDateTime,
    val truth: Coordinate,
    val guess: Coordinate,
    val deviceToEnu: Rotation
) {
    val photo: StarPhoto
        get() = StarPhoto(
            SkyImage(stars, nominalCamera, CameraFrame.fromDevice(deviceUp), time),
            CameraFrame.cameraToEnu(compassDeviceToEnu.toDoubleArray())
        )
}

/**
 * Generates star photos from Sol's catalog with realistic sensor and lens errors. Everything is
 * generated in Android device coordinates and converted with CameraFrame like the app does.
 */
class SyntheticSky(private val random: Random) {

    private val planetMagnitudes = mapOf(
        Planet.Mercury to 0f,
        Planet.Venus to -4f,
        Planet.Mars to 0.5f,
        Planet.Jupiter to -2f,
        Planet.Saturn to 0.7f
    )

    fun randomLocation(): Coordinate {
        return Coordinate(random.nextDouble(-55.0, 60.0), random.nextDouble(-180.0, 180.0))
    }

    fun randomTime(): ZonedDateTime {
        return ZonedDateTime.parse("2026-01-01T00:00:00Z")
            .plusSeconds(random.nextLong(0, 365L * 24 * 3600))
    }

    fun nominalCamera(scenario: SkyScenario): PinholeCamera {
        return PinholeCamera.fromVerticalFieldOfView(scenario.width, scenario.height, scenario.verticalFov)
    }

    /**
     * Device -> ENU rotation for a back camera pointing at the azimuth / altitude with a roll about the camera axis
     */
    fun orientation(azimuth: Double, altitude: Double, roll: Double): Rotation {
        val pointing = SkyVectors.fromHorizon(azimuth, altitude)
        val z = pointing * -1.0
        val reference = if (altitude > 89.0) SkyVectors.fromHorizon(azimuth, 0.0) else Vector3Precise(0.0, 0.0, 1.0)
        var y = (reference - pointing * reference.dot(pointing)).normalize()
        y = Rotation.fromAxisAngle(pointing, roll).rotate(y)
        val x = y.cross(z)
        return Rotation.fromMatrix(
            doubleArrayOf(
                x.x, y.x, z.x,
                x.y, y.y, z.y,
                x.z, y.z, z.z
            )
        )
    }

    /**
     * @param tiltBias a rotation vector (axis * degrees) in device coordinates
     */
    fun generate(
        scenario: SkyScenario,
        location: Coordinate = randomLocation(),
        time: ZonedDateTime = randomTime(),
        deviceToEnu: Rotation? = null,
        tiltBias: Vector3Precise? = null,
        trueCamera: PinholeCamera? = null,
        nominalCamera: PinholeCamera? = null
    ): SyntheticPhoto {
        val rotation = deviceToEnu ?: orientation(
            random.nextDouble(0.0, 360.0),
            random.nextDouble(scenario.minPointingAltitude, scenario.maxPointingAltitude),
            random.nextDouble(-30.0, 30.0)
        )
        val nominal = nominalCamera ?: nominalCamera(scenario)
        val actual = trueCamera ?: nominal.copy(
            focalLength = nominal.focalLength * (1 + uniform(scenario.focalError)),
            k1 = uniform(scenario.maxK1)
        )

        val enuToCamera = CameraFrame.cameraToEnu(rotation.toDoubleArray()).inverse()
        val stars = mutableListOf<ImageStar>()
        for ((magnitude, vector) in catalog(scenario.limitingMagnitude, time, location)) {
            if (random.nextDouble() < scenario.dropout) continue
            val (px, py) = actual.toPixel(enuToCamera.rotate(vector)) ?: continue
            val x = px + gaussian() * scenario.pixelNoise
            val y = py + gaussian() * scenario.pixelNoise
            if (x < 0 || y < 0 || x >= scenario.width || y >= scenario.height) continue
            val brightness = 10.0.pow(-0.4 * magnitude) * (1 + 0.2 * gaussian())
            stars.add(ImageStar(x, y, brightness.coerceAtLeast(1e-4)))
        }
        val kept = stars.sortedByDescending { it.brightness }.take(scenario.maxStars).toMutableList()
        repeat(scenario.falseStars) {
            kept.add(
                ImageStar(
                    random.nextDouble(0.0, scenario.width.toDouble()),
                    random.nextDouble(0.0, scenario.height.toDouble()),
                    10.0.pow(-0.4 * random.nextDouble(0.0, 3.0))
                )
            )
        }
        kept.shuffle(random)

        val trueUp = rotation.inverse().rotate(Vector3Precise(0.0, 0.0, 1.0))
        val bias = tiltBias ?: perpendicular(trueUp, scenario.tiltBiasDegrees)
        val measuredUp = tilt(tilt(trueUp, bias), perpendicular(trueUp, scenario.tiltNoiseDegrees * gaussian()))

        // The compass is off in heading and shares the gravity error
        val axis = trueUp.cross(measuredUp)
        val tiltCorrection = if (axis.magnitude() < 1e-12) {
            Rotation.identity
        } else {
            Rotation.fromAxisAngle(axis, -SkyVectors.angle(trueUp, measuredUp))
        }
        val compass = Rotation.fromAxisAngle(Vector3Precise(0.0, 0.0, 1.0), gaussian() * scenario.compassErrorDegrees) *
                rotation * tiltCorrection

        val guess = location.plus(
            scenario.guessErrorKm * 1000 * random.nextDouble(0.5, 1.0),
            Bearing.from(random.nextDouble(0.0, 360.0).toFloat())
        )

        return SyntheticPhoto(
            kept,
            nominal,
            measuredUp.toDoubleArray(),
            compass,
            time,
            location,
            guess,
            rotation
        )
    }

    /**
     * The magnitude and ENU direction of every catalog body above the horizon
     */
    fun catalog(maxMagnitude: Float, time: ZonedDateTime, location: Coordinate): List<Pair<Float, Vector3Precise>> {
        val stars = STAR_CATALOG.filter { it.magnitude <= maxMagnitude }.map {
            it.magnitude to Astronomy.getStarPosition(it, time, location, true)
        }
        val planets = planetMagnitudes.map { (planet, magnitude) ->
            magnitude to Astronomy.getPlanetPosition(planet, time, location, true)
        }
        return (stars + planets).filter { it.second.altitude >= 0 }.map { (magnitude, position) ->
            magnitude to SkyVectors.fromHorizon(position.azimuth.value.toDouble(), position.altitude.toDouble())
        }
    }

    /**
     * A small rotation vector (axis * degrees) perpendicular to the given direction, in a random direction
     */
    fun perpendicular(direction: Vector3Precise, degrees: Double): Vector3Precise {
        val randomDirection = Vector3Precise(gaussian(), gaussian(), gaussian()).normalize()
        return direction.cross(randomDirection).normalize() * degrees
    }

    fun tilt(v: Vector3Precise, rotationVector: Vector3Precise): Vector3Precise {
        val angle = rotationVector.magnitude()
        if (angle < 1e-12) return v
        return Rotation.fromAxisAngle(rotationVector, angle).rotate(v)
    }

    fun gaussian(): Double {
        return kotlin.math.sqrt(-2 * kotlin.math.ln(random.nextDouble(1e-12, 1.0))) *
                kotlin.math.cos(2 * Math.PI * random.nextDouble())
    }

    private fun uniform(max: Double): Double {
        return if (max <= 0) 0.0 else random.nextDouble(-max, max)
    }
}
