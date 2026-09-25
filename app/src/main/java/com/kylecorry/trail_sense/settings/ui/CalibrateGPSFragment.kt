package com.kylecorry.trail_sense.settings.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.andromeda.sense.location.GPS
import com.kylecorry.andromeda.sense.location.IGPS
import com.kylecorry.luna.time.Throttle
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.CustomGPS
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.gps.GPSAccuracyFilter
import com.kylecorry.trail_sense.shared.sensors.gps.GPSLocationSource
import com.kylecorry.trail_sense.shared.sensors.gps.GPSPowerMode
import com.kylecorry.trail_sense.shared.sensors.gps.modules.CacheGPSModule
import com.kylecorry.trail_sense.shared.sensors.overrides.CachedGPS
import com.kylecorry.trail_sense.shared.sensors.overrides.OverrideGPS
import com.kylecorry.trail_sense.shared.views.CoordinatePreference
import com.kylecorry.trail_sense.shared.views.Slider
import kotlinx.coroutines.launch
import java.time.Duration
import kotlin.math.roundToInt


class CalibrateGPSFragment : AndromedaPreferenceFragment() {

    private val prefs by lazy { UserPreferences(requireContext()) }
    private val sensorService by lazy { SensorService(requireContext()) }
    private val throttle = Throttle(20)

    private lateinit var locationTxt: Preference
    private lateinit var locationSourceList: ListPreference
    private lateinit var permissionBtn: Preference
    private lateinit var locationOverridePref: CoordinatePreference
    private lateinit var accuracyFilterPreference: Preference
    private var clearCacheBtn: Preference? = null
    private val formatService by lazy { FormatService.getInstance(requireContext()) }

    private lateinit var gps: IGPS
    private lateinit var realGps: IGPS

    private var wasUsingRealGPS = false
    private var wasUsingCachedGPS = false

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.gps_calibration, rootKey)
        setIconColor(Resources.androidTextColorSecondary(requireContext()))
        wasUsingRealGPS = shouldUseRealGPS()
        wasUsingCachedGPS = shouldUseCachedGPS()
        gps = sensorService.getGPS(tag = "CalibrateGPSFragment")
        realGps = getRealGPS()
        bindPreferences()
    }

    private fun bindPreferences() {
        locationTxt = findPreference(getString(R.string.pref_holder_location))!!
        locationSourceList = list(R.string.pref_auto_location)!!
        permissionBtn = findPreference(getString(R.string.pref_gps_request_permission))!!
        locationOverridePref = findPreference(getString(R.string.pref_gps_override))!!
        clearCacheBtn = preference(R.string.pref_gps_clear_cache)
        accuracyFilterPreference = findPreference(getString(R.string.pref_gps_accuracy_requirement))!!
        val locationSources = mapOf(
            GPSLocationSource.GPS to getString(R.string.gps),
            GPSLocationSource.Manual to getString(R.string.manual)
        )
        locationSourceList.entries = locationSources.values.toTypedArray()
        locationSourceList.entryValues = locationSources.keys.map { it.id }.toTypedArray()
        list(R.string.pref_gps_power_usage)?.apply {
            val names = mapOf(
                GPSPowerMode.Low to getString(R.string.gps_power_usage_low),
                GPSPowerMode.Balanced to getString(R.string.gps_power_usage_balanced),
                GPSPowerMode.High to getString(R.string.gps_power_usage_high)
            )
            entries = names.values.toTypedArray()
            entryValues = names.keys.map { it.id.toString() }.toTypedArray()
        }
        updateAccuracyFilterSummary()
        accuracyFilterPreference.setOnPreferenceClickListener {
            showAccuracyFilterDialog()
            true
        }
        locationOverridePref.setGPS(realGps)
        locationOverridePref.setLocation(prefs.gps.locationOverride)
        locationOverridePref.setTitle(getString(R.string.pref_gps_override_title))

        locationOverridePref.setOnLocationChangeListener {
            prefs.gps.locationOverride = it ?: Coordinate.zero
            resetGPS()
            update()
        }

        locationSourceList.setOnPreferenceChangeListener { _, newValue ->
            val source = GPSLocationSource.entries.first { it.id == newValue }
            prefs.gps.locationSource = source
            locationSourceList.value = source.id
            locationOverridePref.isVisible = isLocationOverrideEnabled()
            resetGPS()
            update()
            false
        }

        permissionBtn.setOnPreferenceClickListener {
            val intent = Intents.appSettings(requireContext())
            getResult(intent) { _, _ ->
                // Do nothing
            }
            true
        }

        onClick(clearCacheBtn) {
            clearCache()
        }

        update()
    }

    private fun showAccuracyFilterDialog() {
        val view = View.inflate(requireContext(), R.layout.view_gps_accuracy_filter, null)
        val accuracyLabel = view.findViewById<TextView>(R.id.accuracy_label)
        val waitLabel = view.findViewById<TextView>(R.id.wait_label)
        val accuracySlider = view.findViewById<Slider>(R.id.accuracy_slider)
        val waitSlider = view.findViewById<Slider>(R.id.wait_slider)
        val current = prefs.gps.accuracyFilter
        val distanceUnits = prefs.baseDistanceUnits
        val minimumAccuracy = Distance.meters(GPSAccuracyFilter.MIN_ACCURACY_METERS.toFloat())
            .convertTo(distanceUnits).value.roundToInt()
        val maximumAccuracy = Distance.meters(GPSAccuracyFilter.MAX_ACCURACY_METERS.toFloat())
            .convertTo(distanceUnits).value.roundToInt()

        var accuracy = Distance.meters(
            current.minAccuracy ?: GPSAccuracyFilter.Default.minAccuracy!!
        ).convertTo(distanceUnits).value.roundToInt().coerceIn(minimumAccuracy, maximumAccuracy)
        var wait = current.maxAccuracyWait?.seconds?.toInt()
            ?: GPSAccuracyFilter.Default.maxAccuracyWait!!.seconds.toInt()

        accuracySlider.valueFrom = minimumAccuracy.toFloat()
        accuracySlider.valueTo = maximumAccuracy.toFloat()
        accuracySlider.stepSize = 1f
        accuracySlider.value = accuracy.toFloat()
        accuracySlider.addOnChangeListener { _, value, _ ->
            accuracy = value.toInt()
            accuracyLabel.text = getString(
                R.string.gps_accuracy_target,
                formatService.formatDistance(Distance.from(accuracy.toFloat(), distanceUnits))
            )
        }

        waitSlider.valueFrom = GPSAccuracyFilter.MIN_WAIT_SECONDS.toFloat()
        waitSlider.valueTo = GPSAccuracyFilter.MAX_WAIT_SECONDS.toFloat()
        waitSlider.stepSize = 1f
        waitSlider.value = wait.toFloat()
        waitSlider.addOnChangeListener { _, value, _ ->
            wait = value.toInt()
            waitLabel.text = getString(
                R.string.gps_accuracy_wait,
                formatService.formatDuration(Duration.ofSeconds(wait.toLong()), includeSeconds = true)
            )
        }

        accuracyLabel.text = getString(
            R.string.gps_accuracy_target,
            formatService.formatDistance(Distance.from(accuracy.toFloat(), distanceUnits))
        )
        waitLabel.text = getString(
            R.string.gps_accuracy_wait,
            formatService.formatDuration(Duration.ofSeconds(wait.toLong()), includeSeconds = true)
        )

        Alerts.dialog(
            requireContext(),
            getString(R.string.pref_gps_accuracy_requirement_title),
            contentView = view
        ) { cancelled ->
            if (!cancelled) {
                val accuracyMeters = Distance.from(accuracy.toFloat(), distanceUnits).meters().value
                prefs.gps.accuracyFilter = GPSAccuracyFilter.custom(accuracyMeters, wait)
                updateAccuracyFilterSummary()
            }
        }
    }

    private fun updateAccuracyFilterSummary() {
        val filter = prefs.gps.accuracyFilter
        val accuracy = filter.minAccuracy
        val wait = filter.maxAccuracyWait
        accuracyFilterPreference.summary = if (accuracy == null || wait == null) {
            getString(R.string.none)
        } else {
            getString(
                R.string.gps_accuracy_filter_summary,
                formatService.formatElevation(Distance.meters(accuracy)),
                formatService.formatDuration(wait, includeSeconds = true)
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (gps.hasValidReading) {
            update()
        }
        startGPS()
    }

    override fun onPause() {
        super.onPause()
        stopGPS()
        locationOverridePref.pause()
    }

    private fun resetGPS() {
        stopGPS()
        gps = sensorService.getGPS(tag = "CalibrateGPSFragment")
        startGPS()
    }

    private fun startGPS() {
        gps.start(this::onLocationUpdate)
    }

    private fun stopGPS() {
        gps.stop(this::onLocationUpdate)
    }


    private fun onLocationUpdate(): Boolean {
        update()
        return true
    }

    private fun resetRealGPS() {
        locationOverridePref.pause()
        realGps = getRealGPS()
        locationOverridePref.setGPS(realGps)
    }

    private fun getRealGPS(): IGPS {
        return when {
            shouldUseRealGPS() -> {
                CustomGPS(requireContext(), tag = "CalibrateGPSFragment")
            }

            shouldUseCachedGPS() -> {
                CachedGPS(requireContext())
            }

            else -> {
                OverrideGPS(requireContext())
            }
        }
    }

    private fun isLocationOverrideEnabled(): Boolean {
        // Either there are no other options for GPS or auto location is off
        return !isAutoGPSPreferenceEnabled() || prefs.gps.locationSource == GPSLocationSource.Manual
    }

    private fun isAutoGPSPreferenceEnabled(): Boolean {
        // Only disable when GPS permission is denied
        return sensorService.hasLocationPermission()
    }

    private fun shouldUseCachedGPS(): Boolean {
        // Permission is granted, but GPS is disabled
        return sensorService.hasLocationPermission() && !GPS.isAvailable(requireContext())
    }

    private fun shouldUseRealGPS(): Boolean {
        // When both permission is granted and GPS is enabled
        return GPS.isAvailable(requireContext())
    }

    private fun clearCache() {
        lifecycleScope.launch { CacheGPSModule.clearCache() }
    }

    private fun update() {
        if (throttle.isThrottled()) {
            return
        }

        val useReal = shouldUseRealGPS()
        val useCached = shouldUseCachedGPS()

        if (useReal != wasUsingRealGPS || useCached != wasUsingCachedGPS) {
            resetRealGPS()
            resetGPS()
            wasUsingCachedGPS = useCached
            wasUsingRealGPS = useReal
        }


        permissionBtn.isVisible = !isAutoGPSPreferenceEnabled()
        locationSourceList.isEnabled = isAutoGPSPreferenceEnabled()
        locationOverridePref.isVisible = isLocationOverrideEnabled()
        val gpsSettingsEnabled = isAutoGPSPreferenceEnabled() && prefs.gps.locationSource == GPSLocationSource.GPS
        list(R.string.pref_gps_power_usage)?.isVisible = gpsSettingsEnabled
        accuracyFilterPreference.isVisible = gpsSettingsEnabled
        seekBar(R.string.pref_gps_smoothing)?.isVisible = gpsSettingsEnabled
        clearCacheBtn?.isVisible = gpsSettingsEnabled

        locationTxt.summary = formatService.formatLocation(gps.location)
    }


}
