package com.kylecorry.trail_sense.tools.backtrack.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kylecorry.andromeda.core.time.Throttle
import com.kylecorry.andromeda.core.time.toZonedDateTime
import com.kylecorry.andromeda.core.units.Coordinate
import com.kylecorry.andromeda.fragments.BoundBottomSheetDialogFragment
import com.kylecorry.trail_sense.databinding.FragmentPathBottomSheetBinding
import com.kylecorry.trail_sense.shared.DistanceUtils.isLarge
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatServiceV2
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.backtrack.domain.WaypointEntity
import com.kylecorry.trailsensecore.domain.navigation.NavigationService
import java.time.Duration

class PathBottomSheet : BoundBottomSheetDialogFragment<FragmentPathBottomSheetBinding>() {

    private val navigationService = NavigationService()
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val formatService by lazy { FormatServiceV2(requireContext()) }
    private val throttle = Throttle(20)

    var path: List<WaypointEntity> = emptyList()
        set(value) {
            field = value
            onPathChanged()
        }

    var location: Coordinate? = null
        set(value) {
            field = value
            onPathChanged()
        }

    var azimuth: Float = 0f
        set(value) {
            field = value
            onPathChanged()
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onPathChanged()
    }

    private fun onPathChanged() {
        if (!isBound || throttle.isThrottled()){
            return
        }

        val distance = navigationService.getPathDistance(path.map { it.coordinate })
            .convertTo(prefs.baseDistanceUnits).toRelativeDistance()

        val start = path.firstOrNull()?.createdInstant
        val end = path.lastOrNull()?.createdInstant

        binding.pathTimes.text = if (start != null && end != null) {
            formatService.formatTimeSpan(start.toZonedDateTime(), end.toZonedDateTime(), true)
        } else {
            ""
        }

        binding.pathDuration.text = if (start != null && end != null) {
            formatService.formatDuration(Duration.between(start, end), false)
        } else {
            ""
        }

        binding.pathWaypoints.text = path.size.toString()

        binding.pathDistance.text =
            formatService.formatDistance(distance, if (distance.units.isLarge()) 2 else 0, false)

        binding.pathImage.path = path
        binding.pathImage.location = location
        binding.pathImage.azimuth = azimuth
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentPathBottomSheetBinding {
        return FragmentPathBottomSheetBinding.inflate(layoutInflater, container, false)
    }
}