package com.kylecorry.trail_sense.navigation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.time.Timer
import com.kylecorry.andromeda.core.units.CoordinateFormat
import com.kylecorry.andromeda.fragments.BoundBottomSheetDialogFragment
import com.kylecorry.andromeda.location.IGPS
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentLocationBinding
import com.kylecorry.trail_sense.navigation.infrastructure.share.LocationCopy
import com.kylecorry.trail_sense.navigation.infrastructure.share.LocationGeoSender
import com.kylecorry.trail_sense.navigation.infrastructure.share.LocationQRSender
import com.kylecorry.trail_sense.navigation.infrastructure.share.LocationSharesheet
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.CustomUiUtils.setCompoundDrawables
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sharing.Share
import com.kylecorry.trail_sense.shared.sharing.ShareAction
import java.time.Duration
import java.time.Instant

class LocationBottomSheet : BoundBottomSheetDialogFragment<FragmentLocationBinding>() {

    var gps: IGPS? = null

    private val formatService by lazy { FormatService(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private var format = CoordinateFormat.DecimalDegrees

    private val intervalometer = Timer {
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

        val locationSenders = mapOf(
            ShareAction.Copy to LocationCopy(requireContext()),
            ShareAction.QR to LocationQRSender(this),
            ShareAction.Maps to LocationGeoSender(requireContext()),
            ShareAction.Send to LocationSharesheet(requireContext())
        )

        binding.locationTitle.rightButton.setOnClickListener {
            Share.share(
                this,
                getString(R.string.location),
                listOf(ShareAction.Copy, ShareAction.QR, ShareAction.Send, ShareAction.Maps)
            ) {
                it?.let {
                    gps?.location?.let { location ->
                        locationSenders[it]?.send(location, format)
                    }
                }
            }
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
        binding.satellites.text = getString(R.string.num_satellites, gps.satellites)

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