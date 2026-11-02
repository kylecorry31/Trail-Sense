package com.kylecorry.trail_sense.tools.augmented_reality.domain.calibration

import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.sol.math.Vector3
import com.kylecorry.sol.math.trigonometry.Trigonometry
import com.kylecorry.trail_sense.shared.views.CameraView
import com.kylecorry.trail_sense.tools.augmented_reality.domain.position.AugmentedRealityCoordinate
import com.kylecorry.trail_sense.tools.augmented_reality.ui.AugmentedRealityView
import com.kylecorry.sol.math.Vector3Precise
import com.kylecorry.sol.science.astronomy.Astronomy
import com.kylecorry.sol.science.astronomy.navigation.ImageStar
import com.kylecorry.sol.science.astronomy.navigation.SkyImage
import com.kylecorry.trail_sense.tools.celestial_navigation.domain.CameraFrame
import com.kylecorry.trail_sense.tools.celestial_navigation.domain.StarDetector
import com.kylecorry.trail_sense.tools.celestial_navigation.infrastructure.StarPhotoCapture
import com.kylecorry.trail_sense.tools.celestial_navigation.ui.GravityTracker
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Takes a long exposure photo of the night sky, identifies the stars and uses them to find the
 * camera's true heading. The location is known (GPS), so only the orientation is solved.
 */
class ARStarCalibrator(
    private val exposure: Duration = Duration.ofMillis(500),
    private val iso: Int = 1600
) : IARCalibrator {

    data class Result(
        val offset: Float,
        val stars: Int,
        val matched: Int,
        val rmsDegrees: Double,
        val sensorAltitude: Float,
        val starAltitude: Float
    )

    var lastResult: Result? = null
        private set

    var failureReason: String? = null
        private set

    override suspend fun calibrateBearing(view: AugmentedRealityView, camera: CameraView): Float? {
        val icamera = camera.camera ?: return fail("Camera unavailable")
        val tracker = GravityTracker(view.context)
        tracker.start(false)
        val azimuths = mutableListOf<Float>()
        val altitudes = mutableListOf<Float>()
        try {
            onMain {
                camera.setManualExposure(exposure, iso)
                camera.setFocus(1f)
            }
            delay(800)
            val start = tracker.now()
            val frame = coroutineScope {
                val sampler = launch {
                    while (isActive) {
                        onMain {
                            azimuths.add(view.azimuth)
                            altitudes.add(view.inclination)
                        }
                        delay(50)
                    }
                }
                val frame = StarPhotoCapture.capture(icamera)
                sampler.cancel()
                frame
            } ?: return fail("Photo failed")
            val end = tracker.now()
            val up = tracker.mean(start, end) ?: return fail("No gravity reading")

            return onDefault {
                val binning = 2
                val detector = StarDetector()
                val stars = detector.detect(detector.prepare(frame.image.binned(binning)))
                    .map { ImageStar(it.x * binning, it.y * binning, it.brightness) }
                val time = ZonedDateTime.ofInstant(
                    Instant.ofEpochMilli((frame.startMillis + frame.endMillis) / 2),
                    ZoneId.systemDefault()
                )
                val image = SkyImage(
                    stars,
                    StarPhotoCapture.cameraModel(icamera, frame.image.width, frame.image.height),
                    CameraFrame.fromDevice(up),
                    time
                )
                // The location is known, so gravity only narrows the search
                val identification = Astronomy.getOrientationFromStars(image, view.location)
                    ?: return@onDefault fail("Could not identify the stars (${stars.size} detected)")
                val axis = identification.rotation.rotate(Vector3Precise(0.0, 0.0, 1.0))
                val actual = AugmentedRealityCoordinate(
                    view.getActualPoint(Vector3(axis.x.toFloat(), axis.y.toFloat(), axis.z.toFloat()), true),
                    view.isTrueNorth
                )
                val sensorAzimuth = circularMean(azimuths)
                val offset = Trigonometry.deltaAngle(sensorAzimuth, actual.bearing)
                lastResult = Result(
                    offset,
                    stars.size,
                    identification.stars.size,
                    identification.rmsDegrees,
                    altitudes.average().toFloat(),
                    actual.elevation
                )
                offset
            }
        } finally {
            tracker.stop()
            onMain {
                camera.setManualExposure(null, null)
                camera.setFocus(null)
            }
        }
    }

    private fun fail(reason: String): Float? {
        failureReason = reason
        return null
    }

    private fun circularMean(angles: List<Float>): Float {
        val x = angles.sumOf { cos(Math.toRadians(it.toDouble())) }
        val y = angles.sumOf { sin(Math.toRadians(it.toDouble())) }
        return Math.toDegrees(atan2(y, x)).toFloat()
    }
}
