package com.kylecorry.trail_sense.settings.ui

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.andromeda.sense.location.GPS
import com.kylecorry.andromeda.sense.location.IGPS
import com.kylecorry.luna.time.Throttle
import com.kylecorry.sol.units.Coordinate
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
import kotlinx.coroutines.launch


class CalibrateGPSFragment : AndromedaPreferenceFragment() {

    private val prefs by lazy { UserPreferences(requireContext()) }
    private val sensorService by lazy { SensorService(requireContext()) }
    private val throttle = Throttle(20)

    private lateinit var locationTxt: Preference
    private lateinit var locationSourceList: ListPreference
    private lateinit var permissionBtn: Preference
    private lateinit var locationOverridePref: CoordinatePreference
    private lateinit var accuracyFilterList: ListPreference
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
        gps = sensorService.getGPS()
        realGps = getRealGPS()
        bindPreferences()
    }

    private fun bindPreferences() {
        locationTxt = findPreference(getString(R.string.pref_holder_location))!!
        locationSourceList = list(R.string.pref_auto_location)!!
        permissionBtn = findPreference(getString(R.string.pref_gps_request_permission))!!
        locationOverridePref = findPreference(getString(R.string.pref_gps_override))!!
        clearCacheBtn = preference(R.string.pref_gps_clear_cache)
        accuracyFilterList = list(R.string.pref_gps_accuracy_requirement)!!
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
        seekBar(R.string.pref_gps_smoothing)?.apply {
            summary = formatService.formatPercentage(prefs.gps.smoothing.toFloat())
            setOnPreferenceChangeListener { _, newValue ->
                summary = formatService.formatPercentage((newValue as Int).toFloat())
                true
            }
        }
        setAccuracyFilterEntries()
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

    private fun setAccuracyFilterEntries() {
        val names = mapOf(
            GPSAccuracyFilter.None to getString(R.string.none),
            GPSAccuracyFilter.Low to getString(R.string.low),
            GPSAccuracyFilter.Moderate to getString(R.string.moderate),
            GPSAccuracyFilter.High to getString(R.string.high)
        )
        accuracyFilterList.entries = names.values.toTypedArray()
        accuracyFilterList.entryValues = names.keys.map { it.id.toString() }.toTypedArray()
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
        gps = sensorService.getGPS()
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
                CustomGPS(requireContext())
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
        accuracyFilterList.isVisible = gpsSettingsEnabled
        seekBar(R.string.pref_gps_smoothing)?.isVisible = gpsSettingsEnabled
        clearCacheBtn?.isVisible = gpsSettingsEnabled

        locationTxt.summary = formatService.formatLocation(gps.location)
    }


}
