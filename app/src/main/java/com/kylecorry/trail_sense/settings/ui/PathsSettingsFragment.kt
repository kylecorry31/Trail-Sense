package com.kylecorry.trail_sense.settings.ui

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.preference.SwitchPreferenceCompat
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.DistanceUtils
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.preferences.setupDistanceSetting
import com.kylecorry.trail_sense.shared.preferences.setupNotificationSetting
import com.kylecorry.trail_sense.shared.sensors.gps.GPSPowerMode
import com.kylecorry.trail_sense.tools.paths.PathsToolRegistration
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.domain.hiking.HikingService
import com.kylecorry.trail_sense.tools.paths.infrastructure.BacktrackScheduler
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.tools.paths.infrastructure.services.BacktrackService
import com.kylecorry.trail_sense.tools.paths.ui.ElevationSmoothingChartPreference
import com.kylecorry.trail_sense.tools.paths.ui.commands.ChangeBacktrackFrequencyCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.ToggleBacktrackCommand
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class PathsSettingsFragment : AndromedaPreferenceFragment() {

    private var prefBacktrack: SwitchPreferenceCompat? = null
    private val formatService by lazy { FormatService.getInstance(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val hikingService = HikingService()
    private val elevationSmoothingChart
        get() = findPreference<ElevationSmoothingChartPreference>(getString(R.string.pref_holder_elevation_smoothing_chart))
    private var elevationPreviewPath = emptyList<PathPoint>()
    private var elevationPreviewJob: Job? = null

    override fun onResume() {
        super.onResume()
        Tools.subscribe(
            PathsToolRegistration.BROADCAST_BACKTRACK_ENABLED,
            ::onBacktrackEnabled
        )
        Tools.subscribe(
            PathsToolRegistration.BROADCAST_BACKTRACK_DISABLED,
            ::onBacktrackDisabled
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupElevationSmoothingPreview()
    }

    override fun onDestroyView() {
        elevationSmoothingChart?.releaseChart()
        super.onDestroyView()
    }

    override fun onPause() {
        super.onPause()
        Tools.unsubscribe(PathsToolRegistration.BROADCAST_BACKTRACK_ENABLED, ::onBacktrackEnabled)
        Tools.unsubscribe(PathsToolRegistration.BROADCAST_BACKTRACK_DISABLED, ::onBacktrackDisabled)
    }

    private fun setupElevationSmoothingPreview() {
        val context = requireContext()
        viewLifecycleOwner.lifecycleScope.launch {
            val pathService = PathService.getInstance(context)
            val path = onIO {
                val pathId = pathService.getMostRecentPathIdWithElevationChange(MIN_PREVIEW_ELEVATION_CHANGE_METERS)
                    ?: return@onIO null
                pathService.getWaypoints(pathId)
                    .sortedBy { it.id }
                    .filter { it.elevation != null }
                    .downsample(MAX_PREVIEW_POINTS)
            } ?: return@launch
            elevationPreviewPath = path
            elevationSmoothingChart?.setPath(path)
            elevationSmoothingChart?.isVisible = true
            updateElevationSmoothingPreview(prefs.paths.elevationSmoothing)
        }
    }

    private fun updateElevationSmoothingPreview(smoothing: Float) {
        elevationPreviewJob?.cancel()
        elevationPreviewJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(PREVIEW_DEBOUNCE_MS.milliseconds)
            val smoothed = onDefault { hikingService.correctElevations(elevationPreviewPath, smoothing) }
            elevationSmoothingChart?.plot(smoothed)
        }
    }

    private suspend fun onBacktrackEnabled(data: Bundle) = onMain {
        prefBacktrack?.isChecked = true
    }

    private suspend fun onBacktrackDisabled(data: Bundle) = onMain {
        prefBacktrack?.isChecked = false
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.paths_preferences, rootKey)
        prefBacktrack = switch(R.string.pref_backtrack_enabled)

        seekBar(R.string.pref_elevation_smoothing)?.apply {
            value = (prefs.paths.elevationSmoothing * prefs.paths.elevationSmoothingScale).toInt()
            summary = formatService.formatPercentage(prefs.paths.elevationSmoothing * 100)
            setOnPreferenceChangeListener { _, newValue ->
                val smoothing = newValue.toString().toFloat() / prefs.paths.elevationSmoothingScale
                summary = formatService.formatPercentage(smoothing * 100)
                updateElevationSmoothingPreview(smoothing)
                true
            }
        }

        val backtrackService =
            Tools.getService(requireContext(), PathsToolRegistration.SERVICE_BACKTRACK) ?: return

        prefBacktrack?.isEnabled = !backtrackService.isBlocked()

        prefBacktrack?.setOnPreferenceClickListener {
            val command = ToggleBacktrackCommand(this) {
                prefBacktrack?.isChecked = false
            }
            command.execute()
            true
        }

        switch(R.string.pref_backtrack_keep_awake)?.setOnPreferenceChangeListener { _, newValue ->
            val keepAwake = newValue as Boolean
            list(R.string.pref_backtrack_gps_power_usage)?.isVisible = !keepAwake
            lifecycleScope.launch {
                BacktrackScheduler.restart(requireContext())
            }
            true
        }

        list(R.string.pref_backtrack_gps_power_usage)?.apply {
            val names = mapOf(
                GPSPowerMode.Inherit to getString(R.string.gps_power_usage_inherit),
                GPSPowerMode.Low to getString(R.string.gps_power_usage_low),
                GPSPowerMode.Balanced to getString(R.string.gps_power_usage_balanced),
                GPSPowerMode.High to getString(R.string.gps_power_usage_high)
            )
            entries = names.values.toTypedArray()
            entryValues = names.keys.map { it.id.toString() }.toTypedArray()
            isVisible = !prefs.paths.backtrackKeepDeviceAwake
            setOnPreferenceChangeListener { _, _ ->
                lifecycleScope.launch {
                    BacktrackScheduler.restart(requireContext())
                }
                true
            }
        }

        val prefBacktrackInterval = preference(R.string.pref_backtrack_interval)
        prefBacktrackInterval?.summary =
            formatService.formatDuration(prefs.paths.backtrackRecordFrequency, includeSeconds = true)

        prefBacktrackInterval?.setOnPreferenceClickListener {
            ChangeBacktrackFrequencyCommand(requireContext(), lifecycleScope) {
                prefBacktrackInterval.summary =
                    formatService.formatDuration(it, includeSeconds = true)
            }.execute()
            true
        }

        setupDistanceSetting(
            getString(R.string.pref_backtrack_min_distance_holder),
            {
                prefs.paths.backtrackMinimumDistance
                    .convertTo(prefs.baseDistanceUnits)
                    .toRelativeDistance()
            },
            { distance ->
                prefs.paths.backtrackMinimumDistance = distance ?: Distance.meters(0f)
            },
            DistanceUtils.hikingDistanceUnits,
            description = getString(R.string.pref_backtrack_min_distance_summary)
        )

        val prefBacktrackPathColor = preference(R.string.pref_backtrack_path_color)
        prefBacktrackPathColor?.icon?.setTint(
            prefs.paths.defaultPathColor.color
        )

        prefBacktrackPathColor?.setOnPreferenceClickListener {
            CustomUiUtils.pickColor(
                requireContext(),
                prefs.paths.defaultPathColor,
                it.title.toString()
            ) {
                if (it != null) {
                    prefs.paths.defaultPathColor = it
                    prefBacktrackPathColor.icon?.setTint(it.color)
                }
            }
            true
        }

        val backtrackHistory = preference(R.string.pref_backtrack_history_days)
        backtrackHistory?.summary =
            formatService.formatDays(prefs.paths.backtrackHistory.toDays().toInt())
        backtrackHistory?.setOnPreferenceClickListener {
            Pickers.number(
                requireContext(),
                it.title.toString(),
                null,
                prefs.paths.backtrackHistory.toDays().toInt(),
                allowDecimals = false,
                allowNegative = false,
                hint = getString(R.string.days)
            ) { days ->
                if (days != null) {
                    prefs.paths.backtrackHistory = Duration.ofDays(days.toLong())
                    it.summary = formatService.formatDays(if (days.toInt() > 0) days.toInt() else 1)
                }
            }
            true
        }

        setupNotificationSetting(
            getString(R.string.pref_backtrack_notifications_link),
            BacktrackService.FOREGROUND_CHANNEL_ID,
            getString(R.string.backtrack)
        )
    }

    companion object {
        private const val MIN_PREVIEW_ELEVATION_CHANGE_METERS = 50f
        private const val MAX_PREVIEW_POINTS = 200
        private const val PREVIEW_DEBOUNCE_MS = 100L

        private fun <T> List<T>.downsample(maxSize: Int): List<T> {
            if (size <= maxSize) {
                return this
            }
            return List(maxSize) { this[it * lastIndex / (maxSize - 1)] }
        }
    }
}
