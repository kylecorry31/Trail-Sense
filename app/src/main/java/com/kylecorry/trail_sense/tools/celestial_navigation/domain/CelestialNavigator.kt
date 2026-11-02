package com.kylecorry.trail_sense.tools.celestial_navigation.domain

import com.kylecorry.sol.math.Vector3Precise
import com.kylecorry.sol.math.geometry.Rotation
import com.kylecorry.sol.science.astronomy.Astronomy
import com.kylecorry.sol.science.astronomy.navigation.StarIdentification
import com.kylecorry.sol.science.astronomy.stars.StarReading
import com.kylecorry.sol.science.astronomy.units.CelestialObservation
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

class CelestialNavigator(private val config: CelestialNavigationConfig) {

    fun identify(photo: StarPhoto, guess: Coordinate): StarIdentification? {
        return tryOrNull { Astronomy.identifyStars(photo.image, guess, config.identifier) }
    }

    /**
     * The camera to ENU rotation at a known location (the compass isn't used)
     */
    fun orientation(photo: StarPhoto, location: Coordinate): StarIdentification? {
        return tryOrNull {
            Astronomy.getOrientationFromStars(
                photo.image,
                location,
                config.identifier.copy(maxZenithOffsetDegrees = ORIENTATION_ZENITH_OFFSET)
            )
        }
    }

    fun locate(
        photo: StarPhoto,
        guess: Coordinate,
        method: LocationMethod,
        identification: StarIdentification? = null
    ): CelestialFix? {
        return tryOrNull {
            when (method) {
                LocationMethod.GravityZenith -> {
                    val location = Astronomy.getLocationFromStars(photo.image, guess, config.identifier)
                    val id = identification ?: identify(photo, guess)
                    location?.let { CelestialFix(it, method, id?.toMatches().orEmpty(), id?.rmsDegrees, id?.camera) }
                }

                LocationMethod.Altitudes -> fromIdentification(photo, guess, method, identification) { ray ->
                    val up = checkNotNull(photo.image.up)
                    (90 - SkyVectors.angle(ray, up)) to null
                }

                LocationMethod.AltitudeAzimuth -> {
                    val compass = photo.compass ?: return@tryOrNull null
                    fromIdentification(photo, guess, method, identification) { ray ->
                        val (azimuth, altitude) = SkyVectors.toHorizon(compass.rotate(ray))
                        altitude to azimuth
                    }
                }

                LocationMethod.SensorPlateSolve -> plateSolve(photo, guess)
            }
        }?.takeIf { it.location.latitude.isFinite() && it.location.longitude.isFinite() }
    }

    /**
     * Runs every method, sharing a single star identification
     */
    fun locateAll(photo: StarPhoto, guess: Coordinate): List<Pair<LocationMethod, CelestialFix?>> {
        val identification = identify(photo, guess)
        return LocationMethod.entries.map { it to locate(photo, guess, it, identification) }
    }

    private fun fromIdentification(
        photo: StarPhoto,
        guess: Coordinate,
        method: LocationMethod,
        identification: StarIdentification?,
        observe: (Vector3Precise) -> Pair<Double, Double?>
    ): CelestialFix? {
        val id = identification ?: identify(photo, guess) ?: return null
        val readings = id.stars.mapNotNull { identified ->
            val star = identified.body.star ?: return@mapNotNull null
            val (altitude, azimuth) = observe(id.camera.toRay(identified.star.x, identified.star.y))
            StarReading(star, altitude.toFloat(), azimuth?.toFloat(), photo.image.time)
        }
        val location = Astronomy.getLocationFromStars(readings, guess) ?: return null
        return CelestialFix(location, method, id.toMatches(), id.rmsDegrees, id.camera)
    }

    /**
     * Uses the sensors (compass, or gravity alone) to turn every detected star into an altitude and
     * azimuth, then lets Sol's plate solver identify them
     */
    private fun plateSolve(photo: StarPhoto, guess: Coordinate): CelestialFix? {
        val image = photo.image
        val orientation = photo.compass ?: CameraFrame.gravityOnly(checkNotNull(image.up))
        val readings = image.stars.map {
            val (azimuth, altitude) = SkyVectors.toHorizon(orientation.rotate(image.camera.toRay(it.x, it.y)))
            CelestialObservation(Bearing.from(azimuth.toFloat()), altitude.toFloat())
        }
        val detected = Astronomy.plateSolve(
            readings,
            image.time,
            guess,
            tolerance = config.plateSolveTolerance,
            minMatches = config.plateSolveMinMatches,
            minMagnitude = config.identifier.maxMagnitude
        )
        val matches = detected.map { readings.indexOfFirst { reading -> reading === it.reading } to it }
            .filter { it.first >= 0 }
        val starReadings = matches.map { (_, star) ->
            StarReading(star.star, star.reading.altitude, star.reading.azimuth.value, image.time)
        }
        val location = Astronomy.getLocationFromStars(starReadings, guess) ?: return null
        return CelestialFix(location, LocationMethod.SensorPlateSolve, matches.map { it.first to it.second.star.name })
    }

    private inline fun <T> tryOrNull(block: () -> T): T? {
        return try {
            block()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    companion object {
        private const val ORIENTATION_ZENITH_OFFSET = 5.0
    }
}

/**
 * Moves a location so that gravity's up is the zenith. The rotation must be the camera to ENU
 * rotation fit at that location, then one step is exact for small moves.
 */
fun zenithStep(location: Coordinate, cameraToEnu: Rotation, up: Vector3Precise): Coordinate {
    val (azimuth, altitude) = SkyVectors.toHorizon(cameraToEnu.rotate(up.normalize()))
    return location.plus(Math.toRadians(90 - altitude) * EARTH_RADIUS, Bearing.from(azimuth.toFloat()))
}

// Matches Coordinate.plus
private const val EARTH_RADIUS = 6371.2e3

/**
 * A single frame's fix and the camera to ENU rotation from the star fit, so that a gravity error
 * that is fixed in the phone's frame can be separated from the location
 */
data class FrameFix(val location: Coordinate, val rotation: Rotation, val session: Int)

data class CombinedFix(
    val location: Coordinate,
    val standardErrorMeters: Double,
    val count: Int,
    val biasMeters: Double? = null
)

object FixCombiner {

    /**
     * Removes fixes far from their session's median (bad star matches)
     */
    fun rejectOutliers(fixes: List<FrameFix>, minimumMeters: Double = 300.0): List<FrameFix> {
        return fixes.groupBy { it.session }.flatMap { (_, session) ->
            if (session.size < 4) return@flatMap session
            val reference = session.first().location
            val local = session.map { toLocal(reference, it.location) }
            val medianE = local.map { it.first }.sorted()[local.size / 2]
            val medianN = local.map { it.second }.sorted()[local.size / 2]
            val distances = local.map { hypot(it.first - medianE, it.second - medianN) }
            val mad = distances.sorted()[distances.size / 2]
            val threshold = maxOf(minimumMeters, mad * 5)
            session.filterIndexed { i, _ -> distances[i] <= threshold }
        }
    }

    fun mean(fixes: List<Coordinate>): CombinedFix? {
        if (fixes.isEmpty()) return null
        val reference = fixes.first()
        val offsets = fixes.map { toLocal(reference, it) }
        val e = offsets.map { it.first }.average()
        val n = offsets.map { it.second }.average()
        val variance = offsets.sumOf { (it.first - e) * (it.first - e) + (it.second - n) * (it.second - n) } /
                (offsets.size - 1).coerceAtLeast(1)
        val standardError = sqrt(variance / offsets.size)
        return CombinedFix(fromLocal(reference, e, n), standardError, fixes.size)
    }

    /**
     * Solves location + a gravity error that is fixed in the phone's frame (accelerometer bias,
     * camera to IMU misalignment). A gravity error d (radians, camera frame) moves the fix by
     * R * d * earth radius, so frames with different phone orientations (like rotating the phone
     * 180° on the ground) make it observable.
     */
    fun meanWithDeviceBias(fixes: List<FrameFix>): CombinedFix? {
        if (fixes.size < 3) return null
        val axes = fixes.map { it.rotation.rotate(Vector3Precise(1.0, 0.0, 0.0)) }
        val diversity = axes.maxOf { a -> axes.maxOf { b -> SkyVectors.angle(a, b) } }
        if (diversity < 90) return null

        val reference = fixes.first().location
        // Unknowns: pE, pN, dx, dy, dz (scaled by the earth radius so they are in meters)
        val n = 5
        val ata = Array(n) { DoubleArray(n) }
        val atb = DoubleArray(n)
        val rows = mutableListOf<Pair<DoubleArray, Double>>()
        for (fix in fixes) {
            val (e, nn) = toLocal(reference, fix.location)
            val r = fix.rotation.toDoubleArray()
            rows.add(doubleArrayOf(1.0, 0.0, r[0], r[1], r[2]) to e)
            rows.add(doubleArrayOf(0.0, 1.0, r[3], r[4], r[5]) to nn)
        }
        for ((row, value) in rows) {
            for (i in 0 until n) {
                atb[i] += row[i] * value
                for (j in 0 until n) ata[i][j] += row[i] * row[j]
            }
        }
        // The component of the bias along gravity is not observable, keep it small
        for (i in 2 until n) ata[i][i] += 1e-6 * rows.size
        val inverse = invert(ata) ?: return null
        val x = DoubleArray(n) { i -> (0 until n).sumOf { inverse[i][it] * atb[it] } }
        val sse = rows.sumOf { (row, value) ->
            val predicted = (0 until n).sumOf { row[it] * x[it] }
            (value - predicted) * (value - predicted)
        }
        val sigma2 = sse / (rows.size - n).coerceAtLeast(1)
        val standardError = sqrt(sigma2 * (inverse[0][0] + inverse[1][1]))
        val bias = sqrt(x[2] * x[2] + x[3] * x[3] + x[4] * x[4])
        // Sanity: bias should be well under 3 degrees
        if (bias > Math.toRadians(3.0) * EARTH_RADIUS) return null
        return CombinedFix(fromLocal(reference, x[0], x[1]), standardError, fixes.size, bias)
    }

    fun toLocal(reference: Coordinate, point: Coordinate): Pair<Double, Double> {
        val distance = reference.distanceTo(point, false).toDouble()
        val bearing = Math.toRadians(reference.bearingTo(point, false).value.toDouble())
        return distance * sin(bearing) to distance * cos(bearing)
    }

    fun fromLocal(reference: Coordinate, east: Double, north: Double): Coordinate {
        val distance = hypot(east, north)
        val bearing = Math.toDegrees(atan2(east, north))
        return reference.plus(distance, Bearing.from(bearing.toFloat()))
    }

    private fun invert(input: Array<DoubleArray>): Array<DoubleArray>? {
        val n = input.size
        val a = Array(n) { i -> DoubleArray(2 * n) { j -> if (j < n) input[i][j] else if (j - n == i) 1.0 else 0.0 } }
        for (col in 0 until n) {
            val pivot = (col until n).maxBy { kotlin.math.abs(a[it][col]) }
            if (kotlin.math.abs(a[pivot][col]) < 1e-15) return null
            val tmp = a[pivot]; a[pivot] = a[col]; a[col] = tmp
            val p = a[col][col]
            for (j in 0 until 2 * n) a[col][j] /= p
            for (row in 0 until n) {
                if (row == col) continue
                val factor = a[row][col]
                for (j in 0 until 2 * n) a[row][j] -= factor * a[col][j]
            }
        }
        return Array(n) { i -> DoubleArray(n) { j -> a[i][j + n] } }
    }
}
