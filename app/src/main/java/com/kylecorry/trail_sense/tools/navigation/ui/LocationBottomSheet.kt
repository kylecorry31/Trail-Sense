package com.kylecorry.trail_sense.tools.navigation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.time.CoroutineTimer
import com.kylecorry.andromeda.core.ui.setCompoundDrawables
import com.kylecorry.andromeda.fragments.BoundBottomSheetDialogFragment
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.andromeda.sense.location.ISatelliteGPS
import com.kylecorry.sol.science.geography.CoordinateFormat
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentLocationBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sharing.Share
import java.time.Duration
import java.time.Instant

class LocationBottomSheet : BoundBottomSheetDialogFragment<FragmentLocationBinding>() {

    var gps: ISatelliteGPS? = null

    private val formatService by lazy { FormatService.getInstance(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private var format = CoordinateFormat.DecimalDegrees

    private val intervalometer = CoroutineTimer {
        updateUI()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        format = prefs.navigation.coordinateFormat

        binding.locationTitle.subtitle.setCompoundDrawables(
            Resources.dp(requireContext(), 24f).toInt(),
            right = R.drawable.ic_drop_down
        )
        CustomUiUtils.setImageColor(
            binding.locationTitle.subtitle,
            Resources.androidTextColorSecondary(requireContext())
        )
        binding.locationTitle.subtitle.text = formatService.formatCoordinateType(format)

        binding.locationTitle.subtitle.setOnClickListener {
            val formats = CoordinateFormat.values()
            val formatStrings = formats.map { formatService.formatCoordinateType(it) }
            Pickers.item(
                requireContext(),
                getString(R.string.pref_coordinate_format_title),
                formatStrings,
                defaultSelectedIndex = formats.indexOf(format)
            ) {
                if (it != null) {
                    format = formats[it]
                    binding.locationTitle.subtitle.text = formatStrings[it]
                    updateUI()
                }
            }
        }

        binding.locationTitle.rightButton.setOnClickListener {
            Share.shareLocation(
                this,
                gps?.location ?: return@setOnClickListener,
                format
            )
        }
    }

    override fun onResume() {
        super.onResume()
        intervalometer.interval(100)
    }

    override fun onPause() {
        super.onPause()
        intervalometer.stop()
    }

    private fun updateUI() {
        if (!isBound) {
            return
        }

        val gps = this.gps ?: return

        binding.locationTitle.title.text = formatService.formatLocation(gps.location, format)
        binding.satellites.text = getString(R.string.num_satellites, gps.satellites ?: 0)

        val accuracy = gps.horizontalAccuracy
        binding.accuracy.isVisible = accuracy != null
        if (accuracy != null) {
            val accuracyStr = formatService.formatDistance(
                Distance.meters(accuracy).convertTo(prefs.baseDistanceUnits)
            )
            binding.accuracy.text = getString(R.string.accuracy_distance_format, accuracyStr)
        }

        val timeAgo = Duration.between(gps.time, Instant.now())
        binding.time.text = getString(R.string.time_ago, formatService.formatDuration(timeAgo))
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentLocationBinding {
        return FragmentLocationBinding.inflate(layoutInflater, container, false)
    }
}