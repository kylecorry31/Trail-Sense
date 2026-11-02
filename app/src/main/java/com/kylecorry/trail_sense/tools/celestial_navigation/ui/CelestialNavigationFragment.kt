package com.kylecorry.trail_sense.tools.celestial_navigation.ui

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.kylecorry.andromeda.camera.ImageCaptureSettings
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.sol.science.geophysics.Geophysics
import com.kylecorry.sol.time.Time
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentCelestialNavigationBinding
import com.kylecorry.trail_sense.shared.permissions.alertNoCameraPermission
import com.kylecorry.trail_sense.shared.permissions.requestCamera
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.sol.math.Vector3Precise
import com.kylecorry.sol.math.geometry.Rotation
import com.kylecorry.sol.science.astronomy.navigation.ImageStar
import com.kylecorry.sol.science.astronomy.navigation.SkyImage
import com.kylecorry.sol.science.optics.PinholeCamera
import com.kylecorry.trail_sense.tools.celestial_navigation.domain.CameraFrame
import com.kylecorry.trail_sense.tools.celestial_navigation.domain.CelestialNavigator
import com.kylecorry.trail_sense.tools.celestial_navigation.domain.CombinedFix
import com.kylecorry.trail_sense.tools.celestial_navigation.domain.FixCombiner
import com.kylecorry.trail_sense.tools.celestial_navigation.domain.FrameFix
import com.kylecorry.trail_sense.tools.celestial_navigation.domain.LocationMethod
import com.kylecorry.trail_sense.tools.celestial_navigation.domain.LuminanceImage
import com.kylecorry.trail_sense.tools.celestial_navigation.domain.SkyVectors
import com.kylecorry.trail_sense.tools.celestial_navigation.domain.StarDetector
import com.kylecorry.trail_sense.tools.celestial_navigation.domain.StarPhoto
import com.kylecorry.trail_sense.tools.celestial_navigation.domain.toMatches
import com.kylecorry.trail_sense.tools.celestial_navigation.domain.zenithStep
import com.kylecorry.trail_sense.tools.celestial_navigation.infrastructure.DeflectionModels
import com.kylecorry.trail_sense.tools.celestial_navigation.infrastructure.StarPhotoCapture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

class CelestialNavigationFragment : BoundFragment<FragmentCelestialNavigationBinding>() {

    private class Capture(
        var image: LuminanceImage?,
        val display: Bitmap,
        val start: Long,
        val end: Long,
        val time: ZonedDateTime
    )

    private class CurrentPhoto(
        val capture: Capture,
        val width: Int,
        val height: Int,
        val binning: Int,
        val prepared: StarDetector.Prepared,
        val camera: PinholeCamera,
        // Device coordinates
        val up: DoubleArray,
        val deviceToEnu: DoubleArray?,
        val stars: MutableList<ImageStar> = mutableListOf()
    )

    /**
     * @param rotation camera to ENU, fit at the reference location
     */
    private class GroundFrame(
        val photo: StarPhoto,
        val rotation: Rotation,
        val reference: Coordinate,
        val session: Int,
        val matches: List<Pair<Int, String>>,
        val rmsDegrees: Double,
        val camera: PinholeCamera
    )

    private val settings by lazy { CelestialNavigationSettings(requireContext()) }
    private val tracker by lazy { GravityTracker(requireContext()) }
    private val locationSubsystem by lazy { LocationSubsystem.getInstance(requireContext()) }
    private val prefs by lazy { requireContext().getSharedPreferences("celestial_navigation_poc", 0) }

    private var current: CurrentPhoto? = null
    private var isCameraOn = false
    private var job: Job? = null
    private var groundRunning = false
    private val handheldFixes = mutableListOf<FrameFix>()
    private val groundFrames = mutableListOf<GroundFrame>()
    private val sessionUps = mutableMapOf<Int, DoubleArray>()
    private var tone: ToneGenerator? = null
    private var isFullscreen = false
    private var fixtureIndex = 0
    private var fixtureTruth: Coordinate? = null

    private val modes = listOf("Single photo", "Ground (continuous)")
    private val detections = listOf("Manual taps", "Auto detect", "Auto + manual edit")

    override fun generateBinding(layoutInflater: LayoutInflater, container: ViewGroup?): FragmentCelestialNavigationBinding {
        return FragmentCelestialNavigationBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSpinner(binding.mode, "mode", modes)
        setupSpinner(binding.detection, "detection", detections)
        setupSpinner(binding.method, "method", LocationMethod.entries.map { it.label })

        binding.camera.setScaleType(PreviewView.ScaleType.FIT_CENTER)
        binding.camera.setShowTorch(false)

        binding.title.rightButton.setOnClickListener {
            settings.show(requireContext()) {
                applyCameraSettings()
                tracker.start(settings.choice(CelestialNavigationSettings.GRAVITY_SOURCE) == 1)
            }
        }
        binding.title.leftButton.setOnClickListener {
            stopGround()
            handheldFixes.clear()
            groundFrames.clear()
            sessionUps.clear()
            current = null
            showLive()
            binding.results.text = ""
            setStatus("Cleared")
        }
        binding.capture.setOnClickListener {
            if (isGroundMode()) {
                if (groundRunning) stopGround() else startGround()
            } else {
                captureSingle()
            }
        }
        binding.capture.setOnLongClickListener {
            loadFixture()
            true
        }
        binding.overlayCapture.setOnClickListener { binding.capture.performClick() }
        binding.overlayCapture.setOnLongClickListener {
            loadFixture()
            true
        }
        binding.overlaySolve.setOnClickListener { solveCurrent() }
        binding.fullscreen.setOnClickListener { setFullscreen(!isFullscreen) }
        binding.solve.setOnClickListener { solveCurrent() }
        binding.live.setOnClickListener { showLive() }
        binding.photo.onTap = { x, y -> onPhotoTap(x, y) }
        updateButtons()
        setStatus("Point at the sky and capture. Hold still.")
    }

    override fun onResume() {
        super.onResume()
        tracker.start(settings.choice(CelestialNavigationSettings.GRAVITY_SOURCE) == 1)
        tone = ToneGenerator(AudioManager.STREAM_ALARM, 100)
        requestCamera {
            if (it) {
                startCamera()
            } else {
                alertNoCameraPermission()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        stopGround()
        tracker.stop()
        binding.camera.stop()
        isCameraOn = false
        tone?.release()
        tone = null
    }

    // UI helpers

    private fun setupSpinner(spinner: Spinner, key: String, options: List<String>) {
        spinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, options)
        spinner.setSelection(prefs.getInt("spinner_$key", 0).coerceIn(0, options.size - 1))
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                prefs.edit().putInt("spinner_$key", position).apply()
                updateButtons()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
    }

    private fun isGroundMode() = binding.mode.selectedItemPosition == 1
    private fun detectionMode() = binding.detection.selectedItemPosition
    private fun method() = LocationMethod.entries[binding.method.selectedItemPosition]
    private fun deflectionIndex() = settings.choice(CelestialNavigationSettings.DEFLECTION)

    private fun updateButtons() {
        if (!isBound) return
        binding.capture.text = when {
            isGroundMode() && groundRunning -> "Stop"
            isGroundMode() -> "Start"
            else -> "Capture"
        }
        binding.overlayCapture.text = binding.capture.text
        binding.solve.isEnabled = !isGroundMode()
    }

    private fun setFullscreen(fullscreen: Boolean) {
        isFullscreen = fullscreen
        binding.controls.isVisible = !fullscreen
        binding.title.isVisible = !fullscreen
        binding.overlayButtons.isVisible = fullscreen
        binding.fullscreen.setImageResource(if (fullscreen) R.drawable.ic_fullscreen_exit else R.drawable.ic_fullscreen)
    }

    private fun setStatus(text: String) {
        if (isBound) binding.status.text = text
    }

    private fun releasePhoto() {
        current = null
        binding.photo.bitmap = null
        binding.photo.markers = emptyList()
    }

    private fun showLive() {
        binding.photo.isVisible = false
        binding.camera.isVisible = true
    }

    private fun showPhoto(photo: CurrentPhoto) {
        binding.photo.imageWidth = photo.width
        binding.photo.imageHeight = photo.height
        binding.photo.bitmap = photo.capture.display
        binding.photo.isVisible = true
        updateMarkers(photo, null)
    }

    private fun updateMarkers(photo: CurrentPhoto, matches: List<Pair<Int, String>>?) {
        val names = matches?.toMap() ?: emptyMap()
        binding.photo.markers = photo.stars.mapIndexed { i, star ->
            val name = names[i]
            StarPhotoView.Marker(
                star.x,
                star.y,
                name,
                if (name != null) StarPhotoView.MATCHED else StarPhotoView.DETECTED
            )
        }
    }

    private fun beep(count: Int, long: Boolean = false) {
        if (!settings.boolean(CelestialNavigationSettings.BEEP)) return
        lifecycleScope.launch {
            repeat(count) {
                tone?.startTone(ToneGenerator.TONE_PROP_BEEP, if (long) 1500 else 200)
                delay(if (long) 1700 else 350)
            }
        }
    }

    // Camera

    private fun startCamera() {
        if (isCameraOn) return
        binding.camera.start(
            readFrames = false,
            shouldStabilizePreview = false,
            captureSettings = ImageCaptureSettings(
                quality = 100,
                captureMode = ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
            )
        )
        isCameraOn = true
        applyCameraSettings()
    }

    private fun applyCameraSettings() {
        val exposure = settings.number(CelestialNavigationSettings.EXPOSURE_MS)
        val iso = settings.number(CelestialNavigationSettings.ISO).toInt()
        binding.camera.setManualExposure(
            if (exposure > 0) Duration.ofNanos((exposure * 1_000_000).toLong()) else null,
            if (iso > 0) iso else null
        )
        binding.camera.setFocus(if (settings.boolean(CelestialNavigationSettings.FOCUS_INFINITY)) 1f else null)
    }

    private suspend fun capture(): Capture? {
        val camera = binding.camera.camera ?: return null
        delay(settings.number(CelestialNavigationSettings.SETTLE_MS).toLong())
        val start = tracker.now()
        val frame = StarPhotoCapture.capture(
            camera,
            settings.number(CelestialNavigationSettings.STACK).toInt()
        ) ?: return null
        val end = tracker.now()
        val display = withContext(Dispatchers.Default) { createDisplay(frame.image) }
        val time = ZonedDateTime.ofInstant(
            Instant.ofEpochMilli((frame.startMillis + frame.endMillis) / 2),
            ZoneId.systemDefault()
        )
        return Capture(frame.image, display, start, end, time)
    }

    private fun createDisplay(image: LuminanceImage): Bitmap {
        val factor = max(1, max(image.width, image.height) / 1600)
        val small = image.binned(factor)
        val sorted = small.data.copyOf().also { it.sort() }
        val low = sorted[sorted.size / 2]
        val high = max(low + 1e-3f, sorted[(sorted.size * 0.9995).toInt().coerceAtMost(sorted.size - 1)])
        val pixels = IntArray(small.width * small.height) {
            val v = ((small.data[it] - low) / (high - low)).coerceIn(0f, 1f)
            val g = (sqrt(v) * 255).roundToInt()
            Color.rgb(g, g, g)
        }
        return Bitmap.createBitmap(pixels, small.width, small.height, Bitmap.Config.ARGB_8888)
    }

    private fun cameraModel(width: Int, height: Int): PinholeCamera {
        return StarPhotoCapture.cameraModel(
            binding.camera.camera,
            width,
            height,
            settings.boolean(CelestialNavigationSettings.USE_INTRINSICS),
            settings.number(CelestialNavigationSettings.FOCAL_OVERRIDE)
        )
    }

    private fun guess(time: ZonedDateTime): Coordinate {
        return if (settings.choice(CelestialNavigationSettings.GUESS_SOURCE) == 0) {
            locationSubsystem.location.takeIf { it != Coordinate.zero } ?: Time.getLocationFromTimeZone(time.zone)
        } else {
            Time.getLocationFromTimeZone(time.zone)
        }
    }

    private fun trueOrientation(up: DoubleArray, start: Long, end: Long): DoubleArray? {
        val magnetic = tracker.orientation(up, tracker.magneticMean(start - 500_000_000, end + 500_000_000)) ?: return null
        val declination = Geophysics.getGeomagneticDeclination(locationSubsystem.location)
        val toTrueNorth = Rotation.fromAxisAngle(Vector3Precise(0.0, 0.0, 1.0), -declination.toDouble())
        return (toTrueNorth * Rotation.fromMatrix(magnetic)).toDoubleArray()
    }

    private suspend fun prepare(
        capture: Capture,
        up: DoubleArray,
        camera: PinholeCamera? = null,
        deviceToEnu: DoubleArray? = null
    ): CurrentPhoto {
        val binning = settings.number(CelestialNavigationSettings.BINNING).toInt().coerceIn(1, 8)
        val image = checkNotNull(capture.image)
        // Only the binned image is kept, a full resolution photo is too large to hold on to
        capture.image = null
        val prepared = withContext(Dispatchers.Default) {
            StarDetector(settings.detector()).prepare(image.binned(binning))
        }
        return CurrentPhoto(
            capture,
            image.width,
            image.height,
            binning,
            prepared,
            camera ?: cameraModel(image.width, image.height),
            up,
            deviceToEnu ?: trueOrientation(up, capture.start, capture.end)
        )
    }

    private suspend fun autoDetect(photo: CurrentPhoto) {
        val detected = withContext(Dispatchers.Default) {
            StarDetector(settings.detector()).detect(photo.prepared)
        }
        photo.stars.clear()
        photo.stars.addAll(detected.map {
            ImageStar(it.x * photo.binning, it.y * photo.binning, it.brightness)
        })
    }

    /**
     * Converts the device frame sensor readings to Sol's camera frame
     */
    private fun toStarPhoto(photo: CurrentPhoto): StarPhoto {
        val image = SkyImage(photo.stars.toList(), photo.camera, CameraFrame.fromDevice(photo.up), photo.capture.time)
        return StarPhoto(image, photo.deviceToEnu?.let { CameraFrame.cameraToEnu(it) })
    }

    // Single photo

    private fun captureSingle() {
        if (job?.isActive == true) return
        job = viewLifecycleOwner.lifecycleScope.launch {
            showLive()
            releasePhoto()
            fixtureTruth = null
            setStatus("Capturing, hold still...")
            binding.capture.isEnabled = false
            try {
                val capture = capture()
                if (capture == null) {
                    setStatus("Capture failed")
                    return@launch
                }
                val window = settings.number(CelestialNavigationSettings.GRAVITY_WINDOW_MS).toLong() * 1_000_000
                val up = tracker.mean(capture.start - window, capture.end + window) ?: run {
                    setStatus("No gravity data")
                    return@launch
                }
                val photo = prepare(capture, up)
                current = photo
                showPhoto(photo)
                if (detectionMode() != 0) {
                    autoDetect(photo)
                    updateMarkers(photo, null)
                }
                setStatus("${photo.stars.size} stars, ${photo.width}x${photo.height}, f=${photo.camera.focalLength.roundToInt()} px. Tap stars to add / remove.")
                if (detectionMode() == 1) {
                    solveCurrent()
                }
            } finally {
                binding.capture.isEnabled = true
            }
        }
    }

    private fun onPhotoTap(x: Double, y: Double) {
        val photo = current ?: return
        if (detectionMode() == 1) return
        val radius = settings.number(CelestialNavigationSettings.TAP_RADIUS)
        val existing = photo.stars.indexOfFirst { (it.x - x) * (it.x - x) + (it.y - y) * (it.y - y) < radius * radius }
        if (existing >= 0) {
            photo.stars.removeAt(existing)
        } else {
            val star = StarDetector(settings.detector()).refine(
                photo.prepared,
                x / photo.binning,
                y / photo.binning,
                (radius / photo.binning).roundToInt().coerceAtLeast(2)
            ) ?: return
            photo.stars.add(ImageStar(star.x * photo.binning, star.y * photo.binning, star.brightness))
        }
        updateMarkers(photo, null)
        setStatus("${photo.stars.size} stars selected")
    }

    private fun solveCurrent() {
        val photo = current ?: return
        if (photo.stars.size < 2) {
            setStatus("Select at least 2 stars (4+ recommended)")
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            setStatus("Solving...")
            val starPhoto = toStarPhoto(photo)
            val config = settings.config()
            val navigator = CelestialNavigator(config)
            val guess = guess(starPhoto.image.time)
            val gps = fixtureTruth ?: locationSubsystem.location

            val all = withContext(Dispatchers.Default) { navigator.locateAll(starPhoto, guess) }
            val primary = all.firstOrNull { it.first == method() }?.second

            val lines = mutableListOf<String>()
            lines.add("Time ${starPhoto.image.time.toLocalTime()}, ${photo.stars.size} stars, guess ${format(guess)}")
            lines.add("${if (fixtureTruth != null) "TRUTH" else "GPS"} ${format(gps)}  deflection ${DeflectionModels.describe(gps, deflectionIndex())}")
            for ((method, fix) in all) {
                if (fix == null) {
                    lines.add("%-28s failed".format(method.label))
                    continue
                }
                val corrected = DeflectionModels.correct(fix.location, deflectionIndex())
                val lens = fix.camera?.let { ", f %.0f, k1 %.3f".format(it.focalLength, it.k1) } ?: ""
                val rms = fix.rmsDegrees?.let { ", rms %.3f°".format(it) } ?: ""
                lines.add(
                    "%-28s %s err %s (%d stars%s%s)".format(
                        method.label, format(corrected), km(corrected, gps), fix.matches.size, rms, lens
                    )
                )
            }

            if (primary != null) {
                val rotation = withContext(Dispatchers.Default) {
                    navigator.orientation(starPhoto, primary.location)?.rotation
                }
                if (rotation != null) {
                    handheldFixes.add(FrameFix(primary.location, rotation, handheldFixes.size))
                }
                val mean = FixCombiner.mean(handheldFixes.map { it.location })
                val biased = FixCombiner.meanWithDeviceBias(handheldFixes)
                lines.add(0, "")
                lines.add(0, combinedLine("Bias solved (vary phone roll)", biased, gps))
                lines.add(0, combinedLine("Mean of ${handheldFixes.size} photos", mean, gps))
                val corrected = DeflectionModels.correct(primary.location, deflectionIndex())
                lines.add(0, "PRIMARY ${format(corrected)} err ${km(corrected, gps)}")
                updateMarkers(photo, primary.matches)
                setStatus("Fix: ${format(corrected)} (${km(corrected, gps)} from ${if (fixtureTruth != null) "truth" else "GPS"})")
            } else {
                setStatus("Primary method failed, see results")
                all.firstNotNullOfOrNull { it.second }?.let { updateMarkers(photo, it.matches) }
            }
            binding.results.text = lines.joinToString("\n")
            save(photo, gps, lines)
        }
    }

    private suspend fun combinedLine(label: String, fix: CombinedFix?, gps: Coordinate): String {
        fix ?: return "$label: n/a"
        val corrected = DeflectionModels.correct(fix.location, deflectionIndex())
        val bias = fix.biasMeters?.let { ", bias %.1f km".format(it / 1000) } ?: ""
        return "$label: ${format(corrected)} err ${km(corrected, gps)} ±%.0f m$bias".format(fix.standardErrorMeters)
    }

    /**
     * Loads a synthetic sky photo + sensor readings pushed to <external files>/celestial_navigation/fixtures
     */
    private fun loadFixture() {
        if (job?.isActive == true) return
        val dir = File(requireContext().getExternalFilesDir(null), "celestial_navigation/fixtures")
        val files = dir.listFiles { file -> file.extension == "json" }?.sortedBy { it.name } ?: emptyList()
        if (files.isEmpty()) {
            setStatus("No fixtures in ${dir.absolutePath}")
            return
        }
        val file = files[fixtureIndex % files.size]
        fixtureIndex++
        job = viewLifecycleOwner.lifecycleScope.launch {
            setStatus("Loading ${file.nameWithoutExtension}...")
            releasePhoto()
            val json = JSONObject(file.readText())
            fun doubles(key: String): DoubleArray {
                val array = json.getJSONArray(key)
                return DoubleArray(array.length()) { array.getDouble(it) }
            }
            val image = withContext(Dispatchers.IO) {
                // Decode in strips, a full size ARGB bitmap would be too large
                val path = File(dir, file.nameWithoutExtension + ".png").absolutePath
                val decoder = android.graphics.BitmapRegionDecoder.newInstance(path)
                val w = decoder.width
                val h = decoder.height
                val data = FloatArray(w * h)
                val row = IntArray(w)
                var top = 0
                while (top < h) {
                    val bottom = minOf(h, top + 256)
                    val strip = decoder.decodeRegion(android.graphics.Rect(0, top, w, bottom), null)
                    for (y in 0 until strip.height) {
                        strip.getPixels(row, 0, w, 0, y, w, 1)
                        for (x in 0 until w) {
                            val p = row[x]
                            data[(top + y) * w + x] = 0.299f * ((p shr 16) and 0xFF) + 0.587f * ((p shr 8) and 0xFF) + 0.114f * (p and 0xFF)
                        }
                    }
                    strip.recycle()
                    top = bottom
                }
                decoder.recycle()
                LuminanceImage(w, h, data)
            }
            val display = withContext(Dispatchers.Default) { createDisplay(image) }
            val time = ZonedDateTime.parse(json.getString("time"))
            val capture = Capture(image, display, 0, 0, time)
            val truth = doubles("truth")
            fixtureTruth = Coordinate(truth[0], truth[1])
            val photo = prepare(
                capture,
                doubles("up"),
                PinholeCamera(image.width, image.height, json.getDouble("focalLength")),
                doubles("deviceToEnu")
            )
            current = photo
            showPhoto(photo)
            if (detectionMode() != 0) {
                autoDetect(photo)
                updateMarkers(photo, null)
            }
            setStatus("Fixture ${file.nameWithoutExtension}: ${photo.stars.size} stars")
            if (detectionMode() == 1) {
                solveCurrent()
            }
        }
    }

    // Ground mode

    private fun startGround() {
        if (job?.isActive == true) return
        groundRunning = true
        updateButtons()
        groundFrames.clear()
        sessionUps.clear()
        job = viewLifecycleOwner.lifecycleScope.launch {
            var session = 0
            var beeped = false
            tracker.resetSession()
            try {
                while (isActive && groundRunning) {
                    val threshold = settings.number(CelestialNavigationSettings.MOTION_THRESHOLD)
                    if (isMoving(threshold)) {
                        setStatus("Motion detected, waiting to settle...")
                        waitForSettle(threshold)
                        tracker.resetSession()
                        if (groundFrames.any { it.session == session }) {
                            session++
                            beeped = false
                        }
                        continue
                    }

                    setStatus("Session ${session + 1}: capturing (${groundFrames.count { it.session == session }} frames)")
                    releasePhoto()
                    val capture = capture() ?: continue
                    if (isMoving(threshold)) continue
                    val up = tracker.sessionMean() ?: continue
                    sessionUps[session] = up
                    val photo = prepare(capture, up)
                    autoDetect(photo)
                    current = photo
                    showPhoto(photo)

                    val config = settings.config()
                    val navigator = CelestialNavigator(config)
                    val starPhoto = toStarPhoto(photo)
                    // Identify once at the best known location, then later gravity averages only need a zenith step
                    val frame = withContext(Dispatchers.Default) {
                        val reference = lastGroundLocation()
                            ?: navigator.locate(starPhoto, guess(starPhoto.image.time), LocationMethod.GravityZenith)?.location
                        reference?.let { location ->
                            navigator.orientation(starPhoto, location)?.let {
                                GroundFrame(starPhoto, it.rotation, location, session, it.toMatches(), it.rmsDegrees, it.camera)
                            }
                        }
                    }
                    if (frame == null) {
                        setStatus("No match (${photo.stars.size} stars)")
                        continue
                    }
                    updateMarkers(photo, frame.matches)
                    groundFrames.add(frame)

                    val done = updateGroundResults()
                    val frameCount = groundFrames.count { it.session == session }
                    if (!beeped && frameCount >= settings.number(CelestialNavigationSettings.FRAMES_PER_SESSION)) {
                        beeped = true
                        beep(2)
                        setStatus("Rotate the phone 180° on the ground for the next session")
                    }
                    if (done && settings.boolean(CelestialNavigationSettings.AUTO_STOP)) {
                        beep(1, long = true)
                        setStatus("Target accuracy reached")
                        break
                    }
                    delay((settings.number(CelestialNavigationSettings.INTERVAL_S) * 1000).toLong())
                }
            } finally {
                groundRunning = false
                updateButtons()
            }
        }
    }

    private fun stopGround() {
        groundRunning = false
        if (isGroundMode()) job?.cancel()
        updateButtons()
    }

    private fun isMoving(threshold: Double): Boolean {
        val recent = tracker.recentMean(500) ?: return false
        if (tracker.sessionSampleCount() < 50) return false
        val session = tracker.sessionMean() ?: return false
        return angle(recent, session) > threshold
    }

    private suspend fun waitForSettle(threshold: Double) {
        val settleMs = (settings.number(CelestialNavigationSettings.SETTLE_AFTER_MOTION_S) * 1000).toLong()
        var stableSince = System.currentTimeMillis()
        while (System.currentTimeMillis() - stableSince < settleMs) {
            delay(200)
            val recent = tracker.recentMean(300) ?: continue
            val longer = tracker.recentMean(settleMs.coerceAtLeast(1000)) ?: continue
            if (angle(recent, longer) > threshold / 2) {
                stableSince = System.currentTimeMillis()
            }
        }
    }

    private fun angle(a: DoubleArray, b: DoubleArray): Double {
        return SkyVectors.angle(Vector3Precise(a[0], a[1], a[2]), Vector3Precise(b[0], b[1], b[2]))
    }

    private fun frameLocation(frame: GroundFrame): Coordinate {
        val up = sessionUps[frame.session] ?: frame.photo.image.up?.let { doubleArrayOf(it.x, -it.y, -it.z) }
        return up?.let { zenithStep(frame.reference, frame.rotation, CameraFrame.fromDevice(it)) } ?: frame.reference
    }

    private fun lastGroundLocation(): Coordinate? {
        val fixes = FixCombiner.rejectOutliers(groundFrames.map { FrameFix(frameLocation(it), it.rotation, it.session) })
        return FixCombiner.mean(fixes.map { it.location })?.location
    }

    /**
     * Recomputes every frame with its session's latest gravity average and returns true when the
     * target accuracy is reached
     */
    private suspend fun updateGroundResults(): Boolean {
        val gps = locationSubsystem.location
        val fixes = groundFrames.map { FrameFix(frameLocation(it), it.rotation, it.session) }
        val kept = FixCombiner.rejectOutliers(fixes)
        val lines = mutableListOf<String>()
        lines.add("Frames ${groundFrames.size}, kept ${kept.size}, gravity samples ${tracker.sessionSampleCount()} (gravity zenith)")
        lines.add("GPS ${format(gps)}  deflection ${DeflectionModels.describe(gps, deflectionIndex())}")
        val sessions = kept.groupBy { it.session }
        for ((session, sessionFixes) in sessions) {
            lines.add(combinedLine("Session ${session + 1} (${sessionFixes.size})", FixCombiner.mean(sessionFixes.map { it.location }), gps))
        }
        val all = FixCombiner.mean(kept.map { it.location })
        val biased = FixCombiner.meanWithDeviceBias(kept)
        lines.add(combinedLine("All frames mean", all, gps))
        lines.add(combinedLine("Reversal (bias solved)", biased, gps))
        kept.lastOrNull()?.let {
            val corrected = DeflectionModels.correct(it.location, deflectionIndex())
            lines.add("Last frame ${format(corrected)} err ${km(corrected, gps)}")
        }
        groundFrames.lastOrNull()?.let {
            lines.add("Last match: ${it.matches.size} stars, rms %.4f°, f %.1f, k1 %.4f".format(it.rmsDegrees, it.camera.focalLength, it.camera.k1))
        }
        binding.results.text = lines.joinToString("\n")

        val target = settings.number(CelestialNavigationSettings.TARGET_ERROR)
        return biased != null && sessions.size >= 2 && biased.standardErrorMeters < target
    }

    // Debug output

    private suspend fun save(photo: CurrentPhoto, gps: Coordinate, results: List<String>) = withContext(Dispatchers.IO) {
        try {
            val dir = File(requireContext().getExternalFilesDir(null), "celestial_navigation").also { it.mkdirs() }
            val name = "capture_${System.currentTimeMillis()}"
            val json = JSONObject()
            json.put("time", photo.capture.time.toString())
            json.put("width", photo.width)
            json.put("height", photo.height)
            json.put("focalLength", photo.camera.focalLength)
            // Device coordinates
            json.put("up", JSONArray(photo.up.toList()))
            photo.deviceToEnu?.let { json.put("deviceToEnu", JSONArray(it.toList())) }
            json.put("gps", JSONArray(listOf(gps.latitude, gps.longitude)))
            json.put("stars", JSONArray(photo.stars.map { JSONArray(listOf(it.x, it.y, it.brightness)) }))
            json.put("results", JSONArray(results))
            File(dir, "$name.json").writeText(json.toString(2))
            File(dir, "$name.jpg").outputStream().use {
                photo.capture.display.compress(Bitmap.CompressFormat.JPEG, 90, it)
            }
        } catch (e: Exception) {
            Log.e("CelestialNavigation", "Unable to save capture", e)
        }
    }

    private fun format(location: Coordinate): String {
        return "%.5f, %.5f".format(location.latitude, location.longitude)
    }

    private fun km(a: Coordinate, b: Coordinate): String {
        val meters = a.distanceTo(b)
        return if (meters < 10_000) "%.0f m".format(meters) else "%.1f km".format(meters / 1000)
    }
}
