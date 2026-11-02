package com.kylecorry.trail_sense.tools.celestial_navigation.domain

import com.kylecorry.sol.math.Vector3Precise
import com.kylecorry.sol.science.astronomy.Astronomy
import com.kylecorry.sol.science.astronomy.navigation.ImageStar
import com.kylecorry.sol.science.astronomy.navigation.SkyImage
import com.kylecorry.sol.science.optics.PinholeCamera
import com.kylecorry.sol.units.Coordinate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.random.Random

/**
 * Mostly not regression tests, these print error statistics for each method under synthetic conditions
 */
class CelestialNavigationSyntheticTest {

    private val config = CelestialNavigationConfig()
    private val navigator = CelestialNavigator(config)

    @Test
    fun cameraFrame() {
        // Portrait, pointing at the horizon: up is the top of the phone, which is -y in the camera frame
        val up = CameraFrame.fromDevice(doubleArrayOf(0.0, 1.0, 0.0))
        assertEquals(0.0, up.x, 1e-12)
        assertEquals(-1.0, up.y, 1e-12)
        assertEquals(0.0, up.z, 1e-12)

        // The back camera looks out of the back of the phone
        val sky = SyntheticSky(Random(1))
        val deviceToEnu = sky.orientation(123.0, 45.0, 10.0)
        val cameraAxis = CameraFrame.cameraToEnu(deviceToEnu.toDoubleArray()).rotate(Vector3Precise(0.0, 0.0, 1.0))
        val (azimuth, altitude) = SkyVectors.toHorizon(cameraAxis)
        assertEquals(123.0, azimuth, 1e-9)
        assertEquals(45.0, altitude, 1e-9)

        // Gravity only orientation puts up at the zenith
        val gravityUp = Vector3Precise(0.2, -0.9, 0.3).normalize()
        val zenith = CameraFrame.gravityOnly(gravityUp).rotate(gravityUp)
        assertEquals(1.0, zenith.z, 1e-9)
    }

    @Test
    fun perfectData() {
        report(
            SkyScenario(
                "Perfect",
                pixelNoise = 0.0, focalError = 0.0, maxK1 = 0.0, tiltBiasDegrees = 0.0,
                tiltNoiseDegrees = 0.0, compassErrorDegrees = 0.0, falseStars = 0, dropout = 0.0
            ),
            trials = 10
        )
    }

    @Test
    fun handheldCity() {
        val results = report(SkyScenario("Handheld, city (mag 3.5), 0.3° tilt bias"), trials = 30)
        assertTrue(results.getValue(LocationMethod.GravityZenith).successRate > 0.8, "Gravity zenith success rate")
    }

    @Test
    fun handheldDarkSky() {
        report(SkyScenario("Handheld, dark sky (mag 4.5), 0.3° tilt bias", limitingMagnitude = 4.5f), trials = 30)
    }

    @Test
    fun handheldNoBias() {
        report(
            SkyScenario("Handheld, calibrated (no bias), 0.05° tilt noise", tiltBiasDegrees = 0.0, tiltNoiseDegrees = 0.05),
            trials = 30
        )
    }

    /**
     * Phone flat on the ground, camera at the zenith. The gravity vector is averaged for a long
     * time (tiny noise) but keeps a fixed bias. Frames are taken every 2 minutes for an hour in each
     * of two sessions, rotated 180° between them. Mirrors the app: identify once at the best known
     * location, then a zenith step.
     */
    @Test
    fun groundReversal() {
        val random = Random(42)
        val sky = SyntheticSky(random)
        val scenario = SkyScenario(
            "Ground",
            limitingMagnitude = 4.0f,
            pixelNoise = 0.3,
            tiltNoiseDegrees = 0.002,
            guessErrorKm = 300.0,
            falseStars = 3
        )
        val errors = mutableListOf<Triple<Double, Double, Double>>()
        repeat(6) {
            val truth = sky.randomLocation()
            val start = sky.randomTime()
            val nominal = sky.nominalCamera(scenario)
            val trueCamera = nominal.copy(focalLength = nominal.focalLength * 1.02, k1 = 0.02)
            val heading = random.nextDouble(0.0, 360.0)
            // Bias fixed in the phone frame (perpendicular to the device z axis)
            val bias = Vector3Precise(random.nextDouble(-0.3, 0.3), random.nextDouble(-0.3, 0.3), 0.0)

            val frames = mutableListOf<FrameFix>()
            for (session in 0..1) {
                val rotation = sky.orientation(heading + session * 180.0, 89.9, 0.0)
                for (minute in 0 until 60 step 2) {
                    val synthetic = sky.generate(
                        scenario, truth, start.plusMinutes(minute.toLong() + session * 60L), rotation,
                        bias, trueCamera, nominal
                    )
                    groundFrame(synthetic, frames)?.let { frames.add(it.copy(session = session)) }
                }
            }
            val kept = FixCombiner.rejectOutliers(frames)
            val single = kept.first().location.distanceTo(truth) / 1000.0
            val mean = FixCombiner.mean(kept.filter { it.session == 0 }.map { it.location })!!
            val combined = FixCombiner.meanWithDeviceBias(kept)
            errors.add(
                Triple(
                    single,
                    mean.location.distanceTo(truth) / 1000.0,
                    combined?.location?.distanceTo(truth)?.div(1000.0) ?: Double.NaN
                )
            )
            println(
                "Ground trial: frames=${kept.size}/${frames.size}, single=${"%.2f".format(single)} km, " +
                        "session mean=${"%.2f".format(errors.last().second)} km (±${"%.0f".format(mean.standardErrorMeters)} m), " +
                        "reversal=${"%.3f".format(errors.last().third)} km (±${"%.0f".format(combined?.standardErrorMeters ?: Double.NaN)} m, bias ${"%.1f".format((combined?.biasMeters ?: 0.0) / 1000)} km)"
            )
        }
        val medianReversal = errors.map { it.third }.sorted()[errors.size / 2]
        println(
            "Ground median: single=${"%.2f".format(errors.map { it.first }.sorted()[errors.size / 2])} km, " +
                    "session=${"%.2f".format(errors.map { it.second }.sorted()[errors.size / 2])} km, reversal=${"%.3f".format(medianReversal)} km"
        )
        assertTrue(medianReversal < 5.0)
    }

    /**
     * Ground mode for a given total time. Gravity is averaged over each session, so its noise is
     * constant within a session: accelerometer noise density / sqrt(session seconds).
     */
    @Test
    fun groundDuration() {
        val noiseDensity = 150e-6 // g / sqrt(Hz), typical phone accelerometer
        val durations = (System.getProperty("celestial.durations") ?: System.getenv("CELESTIAL_DURATIONS"))
            ?.split(",")?.map { it.trim().toDouble() } ?: listOf(1.0, 5.0)
        val trials = (System.getenv("CELESTIAL_TRIALS") ?: "8").toInt()
        for (totalMinutes in durations) {
            val random = Random(99)
            val sky = SyntheticSky(random)
            val scenario = SkyScenario(
                "Ground", limitingMagnitude = 4.0f, pixelNoise = 0.3, tiltNoiseDegrees = 0.0,
                guessErrorKm = 300.0, falseStars = 3
            )
            val sessionSeconds = totalMinutes * 60 / 2
            val sessionNoise = Math.toDegrees(noiseDensity / kotlin.math.sqrt(sessionSeconds))
            val frameInterval = 5L
            val framesPerSession = (sessionSeconds / frameInterval).toInt()
            val errors = mutableListOf<Double>()
            repeat(trials) {
                val truth = sky.randomLocation()
                val start = sky.randomTime()
                val nominal = sky.nominalCamera(scenario)
                val trueCamera = nominal.copy(focalLength = nominal.focalLength * 1.02, k1 = 0.02)
                val heading = random.nextDouble(0.0, 360.0)
                val bias = Vector3Precise(random.nextDouble(-0.3, 0.3), random.nextDouble(-0.3, 0.3), 0.0)
                val frames = mutableListOf<FrameFix>()
                for (session in 0..1) {
                    val rotation = sky.orientation(heading + session * 180.0, 89.9, 0.0)
                    val noise = sky.perpendicular(Vector3Precise(0.0, 0.0, 1.0), sessionNoise * abs(random.nextDouble(-1.5, 1.5)))
                    val sessionBias = Vector3Precise(bias.x + noise.x, bias.y + noise.y, 0.0)
                    for (frame in 0 until framesPerSession) {
                        val time = start.plusSeconds(frame * frameInterval + session * (sessionSeconds.toLong() + 30))
                        val synthetic = sky.generate(scenario, truth, time, rotation, sessionBias, trueCamera, nominal)
                        groundFrame(synthetic, frames)?.let { frames.add(it.copy(session = session)) }
                    }
                }
                val combined = FixCombiner.meanWithDeviceBias(FixCombiner.rejectOutliers(frames))
                errors.add(combined?.location?.distanceTo(truth)?.toDouble() ?: Double.NaN)
            }
            val sorted = errors.sorted()
            println(
                "Ground $totalMinutes min ($framesPerSession frames/session, gravity noise %.5f°): median %.0f m, p90 %.0f m, worst %.0f m".format(
                    sessionNoise, sorted[sorted.size / 2], sorted[(sorted.size * 0.9).toInt().coerceAtMost(sorted.size - 1)], sorted.last()
                )
            )
        }
    }

    /**
     * AR compass calibration from stars: the location is known, how well does the star fit recover
     * the camera's heading? The compass and gravity errors should not matter.
     */
    @Test
    fun starHeading() {
        val sky = SyntheticSky(Random(5))
        val scenario = SkyScenario("Heading", limitingMagnitude = 3.5f, compassErrorDegrees = 8.0, tiltBiasDegrees = 0.5)
        val errors = mutableListOf<Double>()
        var failed = 0
        repeat(30) {
            val synthetic = sky.generate(scenario)
            val identification = Astronomy.getOrientationFromStars(synthetic.photo.image, synthetic.truth)
            if (identification == null) {
                failed++
                return@repeat
            }
            val axis = Vector3Precise(0.0, 0.0, 1.0)
            val estimated = SkyVectors.toHorizon(identification.rotation.rotate(axis)).first
            val truth = SkyVectors.toHorizon(CameraFrame.cameraToEnu(synthetic.deviceToEnu.toDoubleArray()).rotate(axis)).first
            errors.add(abs(((estimated - truth + 540) % 360) - 180))
        }
        val sorted = errors.sorted()
        println("Star heading: failed $failed/30, median %.4f°, worst %.4f°".format(sorted[sorted.size / 2], sorted.last()))
        assertTrue(sorted[sorted.size / 2] < 0.05)
    }

    @Test
    fun renderedImageEndToEnd() {
        val random = Random(11)
        val sky = SyntheticSky(random)
        val scenario = SkyScenario(
            "Rendered", limitingMagnitude = 4.0f, pixelNoise = 0.0, falseStars = 0, dropout = 0.0,
            width = 1500, height = 2000, maxStars = 200
        )
        var solved = 0
        repeat(8) {
            val synthetic = sky.generate(scenario)
            val w = scenario.width
            val h = scenario.height
            val data = FloatArray(w * h) { 20f + 3f * sky.gaussian().toFloat() }
            // Render each star as a gaussian, plus a hot pixel and a bright blob (a light)
            for (star in synthetic.stars) {
                val amplitude = (2000 * star.brightness).toFloat().coerceAtMost(235f)
                val sigma = 1.3
                for (dy in -6..6) for (dx in -6..6) {
                    val px = star.x.toInt() + dx
                    val py = star.y.toInt() + dy
                    if (px !in 0 until w || py !in 0 until h) continue
                    val ddx = px + 0.5 - star.x
                    val ddy = py + 0.5 - star.y
                    data[py * w + px] += (amplitude * exp(-(ddx * ddx + ddy * ddy) / (2 * sigma * sigma))).toFloat()
                }
            }
            data[100 * w + 100] = 255f
            for (dy in -20..20) for (dx in -20..20) data[(1000 + dy) * w + 700 + dx] = 255f
            val binning = 2
            val detector = StarDetector(DetectorSettings(maxStars = 40))
            val detected = detector.detect(detector.prepare(LuminanceImage(w, h, data).binned(binning)))
                .map { ImageStar(it.x * binning, it.y * binning, it.brightness) }
            val centroidErrors = detected.mapNotNull { d ->
                synthetic.stars.minOfOrNull { hypot(it.x - d.x, it.y - d.y) }?.takeIf { it < 5 }
            }
            val photo = StarPhoto(synthetic.photo.image.copy(stars = detected), null)
            val fix = navigator.locate(photo, synthetic.guess, LocationMethod.GravityZenith)
            val error = fix?.location?.distanceTo(synthetic.truth)?.div(1000.0)
            if (error != null) solved++
            println(
                "Rendered: true=${synthetic.stars.size} detected=${detected.size} " +
                        "centroid median=${"%.3f".format(centroidErrors.sorted().getOrElse(centroidErrors.size / 2) { Double.NaN })} px " +
                        "matched=${fix?.matches?.size} error=${error?.let { "%.1f".format(it) }} km"
            )
        }
        assertTrue(solved >= 6)
    }

    /**
     * Writes realistic night sky photos over Providence, RI with their sensor readings (device
     * coordinates) so the app can load them (long press Capture) after they are pushed to the
     * device. Run with -Dcelestial.fixtures=<dir> or the CELESTIAL_FIXTURES environment variable.
     */
    @Test
    fun generateDeviceFixtures() {
        val dir = System.getProperty("celestial.fixtures") ?: System.getenv("CELESTIAL_FIXTURES") ?: return
        val out = java.io.File(dir).also { it.mkdirs() }
        val random = Random(2026)
        val sky = SyntheticSky(random)
        val providence = Coordinate(41.8240, -71.4128)
        val time = java.time.ZonedDateTime.parse("2026-11-03T02:30:00Z")
        val scenario = SkyScenario(
            "Fixture", limitingMagnitude = 5.0f, pixelNoise = 0.0, focalError = 0.0, maxK1 = 0.0,
            tiltBiasDegrees = 0.0, tiltNoiseDegrees = 0.0, compassErrorDegrees = 0.0, falseStars = 0,
            dropout = 0.0, maxStars = 500
        )
        val nominal = sky.nominalCamera(scenario)
        // The lens is a little different than reported
        val lens = nominal.copy(focalLength = nominal.focalLength * 1.015, k1 = 0.015)
        val shots = listOf(
            "handheld_east" to sky.orientation(95.0, 55.0, 8.0),
            "handheld_south" to sky.orientation(185.0, 45.0, -5.0),
            "ground" to sky.orientation(20.0, 89.9, 0.0)
        )
        for ((name, rotation) in shots) {
            // No deflection is simulated, so set the deflection correction to None when loading these
            val synthetic = sky.generate(scenario, providence, time, rotation, Vector3Precise.zero, lens, nominal)
            val w = scenario.width
            val h = scenario.height
            val flux = FloatArray(w * h)
            // Sky glow with a gradient toward the horizon
            for (y in 0 until h) for (x in 0 until w) flux[y * w + x] = 18f + 10f * y / h
            for (star in synthetic.stars) {
                // Magnitude 4.5 is ~15x the noise
                val total = 1.5e4 * star.brightness
                val sigma = 1.6
                for (dy in -8..8) for (dx in -8..8) {
                    val px = star.x.toInt() + dx
                    val py = star.y.toInt() + dy
                    if (px !in 0 until w || py !in 0 until h) continue
                    val ddx = px + 0.5 - star.x
                    val ddy = py + 0.5 - star.y
                    flux[py * w + px] += (total / (2 * Math.PI * sigma * sigma) * exp(-(ddx * ddx + ddy * ddy) / (2 * sigma * sigma))).toFloat()
                }
            }
            // Street light glow and a hot pixel
            for (y in 0 until h) for (x in 0 until w) {
                val d2 = ((x - 400.0) * (x - 400.0) + (y - 3800.0) * (y - 3800.0))
                flux[y * w + x] += (250 * exp(-d2 / (2 * 120.0 * 120.0))).toFloat()
            }
            flux[1234 * w + 2222] = 255f
            val image = java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_RGB)
            for (y in 0 until h) for (x in 0 until w) {
                val signal = flux[y * w + x].toDouble()
                // Shot noise (gain 1) + read noise
                val noisy = signal + sky.gaussian() * kotlin.math.sqrt(signal) + sky.gaussian() * 2.0
                val v = kotlin.math.round(noisy).toInt().coerceIn(0, 255)
                image.setRGB(x, y, (v shl 16) or (v shl 8) or v)
            }
            javax.imageio.ImageIO.write(image, "png", java.io.File(out, "$name.png"))
            fun arr(values: List<Double>) = values.joinToString(",", "[", "]")
            val json = """{
  "time": "$time",
  "focalLength": ${nominal.focalLength},
  "up": ${arr(synthetic.deviceUp.toList())},
  "deviceToEnu": ${arr(synthetic.compassDeviceToEnu.toDoubleArray().toList())},
  "truth": ${arr(listOf(providence.latitude, providence.longitude))}
}"""
            java.io.File(out, "$name.json").writeText(json)
            println("Wrote $name with ${synthetic.stars.size} stars")
        }
    }

    @Test
    fun replayDeviceFixtures() {
        val dir = System.getProperty("celestial.fixtures") ?: System.getenv("CELESTIAL_FIXTURES") ?: return
        for (name in listOf("handheld_east", "handheld_south", "ground")) {
            val png = javax.imageio.ImageIO.read(java.io.File(dir, "$name.png"))
            val text = java.io.File(dir, "$name.json").readText()
            fun doubles(key: String): DoubleArray {
                val raw = Regex("\"$key\": \\[([^\\]]*)\\]").find(text)!!.groupValues[1]
                return raw.split(",").map { it.trim().toDouble() }.toDoubleArray()
            }
            val focal = Regex("\"focalLength\": ([0-9.]+)").find(text)!!.groupValues[1].toDouble()
            val time = java.time.ZonedDateTime.parse(Regex("\"time\": \"([^\"]+)\"").find(text)!!.groupValues[1])
            val w = png.width
            val h = png.height
            val data = FloatArray(w * h) { (png.getRGB(it % w, it / w) and 0xFF).toFloat() }
            val binning = 2
            val detector = StarDetector(DetectorSettings())
            val prepared = detector.prepare(LuminanceImage(w, h, data).binned(binning))
            val detected = detector.detect(prepared).map { ImageStar(it.x * binning, it.y * binning, it.brightness) }
            val truth = doubles("truth").let { Coordinate(it[0], it[1]) }
            val photo = StarPhoto(
                SkyImage(detected, PinholeCamera(w, h, focal), CameraFrame.fromDevice(doubles("up")), time),
                CameraFrame.cameraToEnu(doubles("deviceToEnu"))
            )
            val guess = Coordinate(41.8, -71.4)
            for ((method, fix) in navigator.locateAll(photo, guess)) {
                println(
                    "$name ${method.name}: matched=${fix?.matches?.size} rms=${fix?.rmsDegrees} " +
                            "error=${fix?.location?.distanceTo(truth)?.let { "%.0f m".format(it) }}"
                )
            }
        }
    }

    data class Stats(val successRate: Double, val median: Double, val p90: Double)

    /**
     * One ground frame like the app: identify at the best known location, then a zenith step
     */
    private fun groundFrame(synthetic: SyntheticPhoto, previous: List<FrameFix>): FrameFix? {
        val photo = synthetic.photo
        val reference = FixCombiner.mean(FixCombiner.rejectOutliers(previous).map { it.location })?.location
            ?: navigator.locate(photo, synthetic.guess, LocationMethod.GravityZenith)?.location
            ?: return null
        val identification = navigator.orientation(photo, reference) ?: return null
        val location = zenithStep(reference, identification.rotation, checkNotNull(photo.image.up))
        return FrameFix(location, identification.rotation, 0)
    }

    private fun report(scenario: SkyScenario, trials: Int, seed: Int = 7): Map<LocationMethod, Stats> {
        val sky = SyntheticSky(Random(seed))
        val errors = mutableMapOf<LocationMethod, MutableList<Double?>>()
        var totalStars = 0
        repeat(trials) {
            val synthetic = sky.generate(scenario)
            totalStars += synthetic.stars.size
            for ((method, fix) in navigator.locateAll(synthetic.photo, synthetic.guess)) {
                errors.getOrPut(method) { mutableListOf() }.add(fix?.location?.distanceTo(synthetic.truth)?.div(1000.0))
            }
        }
        println("=== ${scenario.name} ($trials trials, ${totalStars / trials} stars/photo avg) ===")
        val stats = errors.mapValues { (_, values) ->
            val ok = values.filterNotNull().sorted()
            val successRate = ok.size.toDouble() / values.size
            val median = ok.getOrNull(ok.size / 2) ?: Double.NaN
            val p90 = ok.getOrNull((ok.size * 0.9).toInt().coerceAtMost(ok.size - 1)) ?: Double.NaN
            Stats(successRate, median, p90)
        }
        for ((method, s) in stats) {
            println(
                "%-28s solved %3.0f%%  median %8.2f km  p90 %8.2f km".format(
                    method.label, s.successRate * 100, s.median, s.p90
                )
            )
        }
        return stats
    }
}
