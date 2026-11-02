package com.kylecorry.trail_sense.tools.celestial_navigation.ui

import android.app.AlertDialog
import android.content.Context
import android.text.InputType
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import com.kylecorry.sol.science.astronomy.navigation.StarIdentifierSettings
import com.kylecorry.trail_sense.tools.celestial_navigation.domain.CelestialNavigationConfig
import com.kylecorry.trail_sense.tools.celestial_navigation.domain.DetectorSettings

/**
 * POC settings stored in their own shared preferences so they can be tweaked in the field
 */
class CelestialNavigationSettings(context: Context) {

    sealed class Setting(val key: String, val label: String)
    class NumberSetting(key: String, label: String, val default: Double) : Setting(key, label)
    class BooleanSetting(key: String, label: String, val default: Boolean) : Setting(key, label)
    class ChoiceSetting(key: String, label: String, val options: List<String>, val default: Int) :
        Setting(key, label)

    private val prefs = context.getSharedPreferences("celestial_navigation_poc", Context.MODE_PRIVATE)

    val all = listOf(
        // Capture
        NumberSetting(EXPOSURE_MS, "Exposure (ms, 0 = auto)", 500.0),
        NumberSetting(ISO, "ISO (0 = auto)", 1600.0),
        NumberSetting(STACK, "Photos stacked per capture", 1.0),
        NumberSetting(SETTLE_MS, "Settle time before capture (ms)", 600.0),
        BooleanSetting(FOCUS_INFINITY, "Focus at infinity", true),
        BooleanSetting(USE_INTRINSICS, "Use lens intrinsic calibration (else FOV)", true),
        NumberSetting(FOCAL_OVERRIDE, "Focal length override (px, 0 = off)", 0.0),
        ChoiceSetting(GRAVITY_SOURCE, "Gravity source", listOf("Accelerometer", "Gravity sensor"), 0),
        NumberSetting(GRAVITY_WINDOW_MS, "Handheld gravity window around capture (ms)", 300.0),
        // Detection
        NumberSetting(BINNING, "Detection binning (1, 2, 4)", 2.0),
        NumberSetting(DETECT_SIGMA, "Detection threshold (sigma)", 6.0),
        NumberSetting(MAX_STARS, "Max detected stars", 40.0),
        NumberSetting(TAP_RADIUS, "Tap search radius (px)", 60.0),
        NumberSetting(MAX_BLOB, "Max star blob size (binned px)", 80.0),
        // Matching
        NumberSetting(MAX_MAGNITUDE, "Catalog magnitude limit", 4.5),
        NumberSetting(SEED_MAGNITUDE, "Seed magnitude limit", 3.0),
        BooleanSetting(INCLUDE_PLANETS, "Include planets", true),
        NumberSetting(SEED_COUNT, "Brightest stars used as seeds", 10.0),
        NumberSetting(PAIR_TOLERANCE, "Pair tolerance (deg)", 0.25),
        NumberSetting(FOCAL_UNCERTAINTY, "Focal length uncertainty (fraction)", 0.06),
        NumberSetting(INLIER_TOLERANCE, "Inlier tolerance (deg)", 0.35),
        NumberSetting(MAX_ZENITH_OFFSET, "Max zenith offset from guess (deg)", 30.0),
        NumberSetting(MIN_MATCHES, "Min matched stars", 4.0),
        BooleanSetting(FIT_FOCAL, "Fit focal length", true),
        BooleanSetting(FIT_DISTORTION, "Fit radial distortion", true),
        NumberSetting(SOL_TOLERANCE, "Sensor plate solve tolerance (fraction)", 0.04),
        NumberSetting(SOL_MIN_MATCHES, "Sensor plate solve min matches", 5.0),
        BooleanSetting(REFRACTION, "Atmospheric refraction", true),
        // Location
        ChoiceSetting(GUESS_SOURCE, "Initial guess", listOf("Last GPS location", "Time zone"), 0),
        ChoiceSetting(
            DEFLECTION,
            "Vertical deflection correction",
            listOf("None", "Geoid slope"),
            1
        ),
        // Ground mode
        NumberSetting(FRAMES_PER_SESSION, "Ground: frames before rotate beep", 30.0),
        NumberSetting(TARGET_ERROR, "Ground: target standard error (m)", 50.0),
        NumberSetting(MOTION_THRESHOLD, "Ground: motion threshold (deg)", 0.3),
        NumberSetting(SETTLE_AFTER_MOTION_S, "Ground: settle after motion (s)", 5.0),
        NumberSetting(INTERVAL_S, "Ground: extra delay between frames (s)", 0.0),
        BooleanSetting(AUTO_STOP, "Ground: stop when target reached", true),
        BooleanSetting(BEEP, "Ground: beep", true)
    )

    fun number(key: String): Double {
        val setting = all.first { it.key == key } as NumberSetting
        return prefs.getString(key, null)?.toDoubleOrNull() ?: setting.default
    }

    fun boolean(key: String): Boolean {
        val setting = all.first { it.key == key } as BooleanSetting
        return prefs.getBoolean(key, setting.default)
    }

    fun choice(key: String): Int {
        val setting = all.first { it.key == key } as ChoiceSetting
        return prefs.getInt(key, setting.default).coerceIn(0, setting.options.size - 1)
    }

    fun config(): CelestialNavigationConfig {
        return CelestialNavigationConfig(
            StarIdentifierSettings(
                maxMagnitude = number(MAX_MAGNITUDE).toFloat(),
                seedMagnitude = number(SEED_MAGNITUDE).toFloat(),
                includePlanets = boolean(INCLUDE_PLANETS),
                seedCount = number(SEED_COUNT).toInt(),
                pairToleranceDegrees = number(PAIR_TOLERANCE),
                focalUncertainty = number(FOCAL_UNCERTAINTY),
                inlierToleranceDegrees = number(INLIER_TOLERANCE),
                maxZenithOffsetDegrees = number(MAX_ZENITH_OFFSET),
                minMatches = number(MIN_MATCHES).toInt(),
                fitFocalLength = boolean(FIT_FOCAL),
                fitDistortion = boolean(FIT_DISTORTION),
                withRefraction = boolean(REFRACTION)
            ),
            plateSolveTolerance = number(SOL_TOLERANCE).toFloat(),
            plateSolveMinMatches = number(SOL_MIN_MATCHES).toInt()
        )
    }

    fun detector(): DetectorSettings {
        return DetectorSettings(
            sigma = number(DETECT_SIGMA),
            maxStars = number(MAX_STARS).toInt(),
            maxBlobPixels = number(MAX_BLOB).toInt()
        )
    }

    fun show(context: Context, onSaved: () -> Unit) {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 16, 48, 16)
        }
        val editors = all.map { setting ->
            layout.addView(TextView(context).apply {
                text = setting.label
                setPadding(0, 24, 0, 0)
            })
            val view = when (setting) {
                is NumberSetting -> EditText(context).apply {
                    inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or
                            InputType.TYPE_NUMBER_FLAG_SIGNED
                    setText(number(setting.key).toString())
                }

                is BooleanSetting -> CheckBox(context).apply {
                    isChecked = boolean(setting.key)
                    text = "Enabled"
                }

                is ChoiceSetting -> Spinner(context).apply {
                    adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, setting.options)
                    setSelection(choice(setting.key))
                }
            }
            layout.addView(view)
            setting to view
        }
        AlertDialog.Builder(context)
            .setTitle("Celestial navigation settings")
            .setView(ScrollView(context).apply { addView(layout) })
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val editor = prefs.edit()
                editors.forEach { (setting, view) ->
                    when (setting) {
                        is NumberSetting -> editor.putString(setting.key, (view as EditText).text.toString())
                        is BooleanSetting -> editor.putBoolean(setting.key, (view as CheckBox).isChecked)
                        is ChoiceSetting -> editor.putInt(setting.key, (view as Spinner).selectedItemPosition)
                    }
                }
                editor.apply()
                onSaved()
            }
            .setNeutralButton("Reset") { _, _ ->
                prefs.edit().clear().apply()
                onSaved()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    companion object {
        const val EXPOSURE_MS = "exposure_ms"
        const val ISO = "iso"
        const val STACK = "stack"
        const val SETTLE_MS = "settle_ms"
        const val FOCUS_INFINITY = "focus_infinity"
        const val USE_INTRINSICS = "use_intrinsics"
        const val FOCAL_OVERRIDE = "focal_override"
        const val GRAVITY_SOURCE = "gravity_source"
        const val GRAVITY_WINDOW_MS = "gravity_window_ms"
        const val BINNING = "binning"
        const val DETECT_SIGMA = "detect_sigma"
        const val MAX_STARS = "max_stars"
        const val TAP_RADIUS = "tap_radius"
        const val MAX_BLOB = "max_blob"
        const val MAX_MAGNITUDE = "max_magnitude"
        const val SEED_MAGNITUDE = "seed_magnitude"
        const val INCLUDE_PLANETS = "include_planets"
        const val SEED_COUNT = "seed_count"
        const val PAIR_TOLERANCE = "pair_tolerance"
        const val FOCAL_UNCERTAINTY = "focal_uncertainty"
        const val INLIER_TOLERANCE = "inlier_tolerance"
        const val MAX_ZENITH_OFFSET = "max_zenith_offset"
        const val MIN_MATCHES = "min_matches"
        const val FIT_FOCAL = "fit_focal"
        const val FIT_DISTORTION = "fit_distortion"
        const val SOL_TOLERANCE = "sol_tolerance"
        const val SOL_MIN_MATCHES = "sol_min_matches"
        const val REFRACTION = "refraction"
        const val GUESS_SOURCE = "guess_source"
        const val DEFLECTION = "deflection"
        const val FRAMES_PER_SESSION = "frames_per_session"
        const val TARGET_ERROR = "target_error"
        const val MOTION_THRESHOLD = "motion_threshold"
        const val SETTLE_AFTER_MOTION_S = "settle_after_motion_s"
        const val INTERVAL_S = "interval_s"
        const val AUTO_STOP = "auto_stop"
        const val BEEP = "beep"
    }
}
