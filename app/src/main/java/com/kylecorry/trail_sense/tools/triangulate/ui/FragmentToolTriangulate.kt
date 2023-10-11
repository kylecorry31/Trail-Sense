package com.kylecorry.trail_sense.tools.triangulate.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.buildSpannedString
import androidx.core.text.scale
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.setCompoundDrawables
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.science.geology.Geofence
import com.kylecorry.sol.science.geology.Geology
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolTriangulateBinding
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.infrastructure.share.LocationCopy
import com.kylecorry.trail_sense.navigation.paths.domain.LineStyle
import com.kylecorry.trail_sense.navigation.ui.IMappablePath
import com.kylecorry.trail_sense.navigation.ui.MappableLocation
import com.kylecorry.trail_sense.navigation.ui.MappablePath
import com.kylecorry.trail_sense.navigation.ui.layers.BeaconLayer
import com.kylecorry.trail_sense.navigation.ui.layers.PathLayer
import com.kylecorry.trail_sense.shared.AppUtils
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.extensions.from
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem

class FragmentToolTriangulate : BoundFragment<FragmentToolTriangulateBinding>() {

    private val formatService by lazy { FormatService.getInstance(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }

    private var location: Coordinate? = null

    private var shouldCalculateMyLocation = false

    private val beaconLayer = BeaconLayer()
    private val pathLayer = PathLayer()

    private val radius = Distance.meters(100f)

    // TODO: Determine what this should actually be
    private val recommendedMinDistance by lazy {
        if (prefs.distanceUnits == UserPreferences.DistanceUnits.Feet) {
            Distance.feet(100f)
        } else {
            Distance.meters(30f)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.bearing1.setOnBearingChangeListener { _, _ ->
            update()
        }

        binding.bearing2.setOnBearingChangeListener { _, _ ->
            update()
        }

        binding.location1.setOnCoordinateChangeListener {
            update()
        }

        binding.location2.setOnCoordinateChangeListener {
            update()
        }

        binding.triangulateTitle.rightButton.setOnClickListener {
            location?.let {
                val share = LocationCopy(requireContext())
                share.send(it)
            }
        }

        binding.createBeacon.setOnClickListener {
            location?.let {
                AppUtils.placeBeacon(requireContext(), GeoUri(it))
            }
        }

        binding.updateGpsOverride.setOnClickListener {
            location?.let { coord ->
                prefs.locationOverride = coord
                Alerts.toast(requireContext(), getString(R.string.location_override_updated))
            }
        }

        if (prefs.useAutoLocation) {
            binding.updateGpsOverride.isVisible = false
        }

        binding.locationButtonGroup.check(if (shouldCalculateMyLocation) binding.locationButtonSelf.id else binding.locationButtonOther.id)
        binding.locationButtonGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) {
                return@addOnButtonCheckedListener
            }
            shouldCalculateMyLocation = checkedId == binding.locationButtonSelf.id
            update()
        }

        // Handle expansion
        binding.location1Expansion.setOnExpandStateChangedListener {
            binding.location1DropdownIcon.rotation = if (it) 180f else 0f
        }

        binding.location2Expansion.setOnExpandStateChangedListener {
            binding.location2DropdownIcon.rotation = if (it) 180f else 0f
        }

        // Expand the first location by default (this will change once it loads the last recorded values)
        binding.location1Expansion.expand()

        // TODO: Display the distance to the location in the title
        beaconLayer.setOutlineColor(Color.WHITE)
        binding.map.setLayers(listOf(pathLayer, beaconLayer))

        binding.resetBtn.setOnClickListener {
            reset()
        }

        update()
    }

    private fun updateMap() {
        val location1 = binding.location1.coordinate
        val location2 = binding.location2.coordinate
        val destination = location

        // Update map bounds
        val fences = listOfNotNull(
            location1,
            location2,
            destination
        ).map {
            Geofence(it, radius)
        }

        val bounds = CoordinateBounds.from(fences)

        binding.map.bounds = bounds
        binding.map.isInteractive = true
        binding.map.recenter()

        // Show the locations on the map
        beaconLayer.setBeacons(listOfNotNull(
            location1?.let { Beacon.temporary(it, id = 1) },
            location2?.let { Beacon.temporary(it, id = 2) },
            destination?.let { Beacon.temporary(it, id = 3, color = AppColor.Green.color) }
        ))

        // Draw bearing lines
        val path1 = getPath(1)
        val path2 = getPath(2)
        pathLayer.setPaths(listOfNotNull(path1, path2))
    }

    private fun updateDistances() {
        if (!isBound) {
            return
        }

        binding.location1Title.text = getLocationTitle(1)
        binding.location2Title.text = getLocationTitle(2)

        // TODO: Display distance between locations
    }

    private fun getLocationTitle(locationIdx: Int): CharSequence {
        val distance = getDistanceToDestination(locationIdx)
        val formattedDistance = distance?.let {
            formatService.formatDistance(it, Units.getDecimalPlaces(it.units))
        }
        return buildSpannedString {
            append(getString(if (locationIdx == 1) R.string.beacon_1 else R.string.beacon_2))
            if (distance != null) {
                appendLine()
                scale(0.75f) {
                    append(
                        if (shouldCalculateMyLocation) {
                            getString(R.string.distance_away_from_self, formattedDistance)
                        } else {
                            getString(R.string.distance_away_from_destination, formattedDistance)
                        }
                    )
                }
            }
        }
    }

    private fun updateInstructions() {
        if (!isBound) {
            return
        }

        if (shouldCalculateMyLocation) {
            binding.location1Instructions.text =
                getString(R.string.triangulate_self_location_1_instructions)
            binding.location2Instructions.text =
                getString(R.string.triangulate_self_location_2_instructions)
        } else {
            binding.location1Instructions.text =
                getString(R.string.triangulate_destination_location_1_instructions)
            binding.location2Instructions.text =
                getString(
                    R.string.triangulate_destination_location_2_instructions,
                    formatService.formatDistance(
                        recommendedMinDistance,
                        Units.getDecimalPlaces(recommendedMinDistance.units)
                    )
                )
        }
    }

    private fun updateCompletionState() {
        if (!isBound) {
            return
        }

        binding.location1Title.setCompoundDrawables(
            left = if (isComplete(1)) R.drawable.ic_check_outline else R.drawable.ic_info
        )
        binding.location2Title.setCompoundDrawables(
            left = if (isComplete(2)) R.drawable.ic_check_outline else R.drawable.ic_info
        )
        CustomUiUtils.setImageColor(
            binding.location1Title,
            if (isComplete(1)) AppColor.Green.color else Resources.androidTextColorSecondary(
                requireContext()
            )
        )
        CustomUiUtils.setImageColor(
            binding.location2Title,
            if (isComplete(2)) AppColor.Green.color else Resources.androidTextColorSecondary(
                requireContext()
            )
        )
    }

    private fun getDistanceToDestination(locationIdx: Int): Distance? {
        val start =
            (if (locationIdx == 1) binding.location1.coordinate else binding.location2.coordinate)
                ?: return null
        val destination = location ?: return null
        return Distance.meters(start.distanceTo(destination)).convertTo(prefs.baseDistanceUnits)
            .toRelativeDistance()
    }

    private fun getDistanceBetweenLocations(): Distance? {
        val location1 = binding.location1.coordinate ?: return null
        val location2 = binding.location2.coordinate ?: return null
        return Distance.meters(location1.distanceTo(location2))
    }

    private fun getPath(locationIdx: Int): IMappablePath? {
        val destination = location
        val start =
            if (locationIdx == 1) binding.location1.coordinate else binding.location2.coordinate
        val direction = if (locationIdx == 1) binding.bearing1.bearing else binding.bearing2.bearing
        val trueNorth =
            if (locationIdx == 1) binding.bearing1.trueNorth else binding.bearing2.trueNorth

        val end = if (start != null && direction != null) {
            val declination = if (trueNorth) 0f else Geology.getGeomagneticDeclination(start)
            val bearing = direction.withDeclination(declination)
            destination ?: start.plus(
                Distance.kilometers(10f),
                if (shouldCalculateMyLocation) bearing.inverse() else bearing
            )
        } else {
            null
        }

        return if (start != null && end != null) {
            val pts = listOf(
                MappableLocation(0, start, AppColor.Orange.color, null),
                MappableLocation(0, end, AppColor.Orange.color, null),
            )
            MappablePath(
                locationIdx.toLong(),
                if (shouldCalculateMyLocation) pts.reversed() else pts,
                AppColor.Orange.color, LineStyle.Arrow
            )
        } else {
            null
        }
    }

    override fun onResume() {
        super.onResume()
        binding.bearing1.start()
        binding.bearing2.start()
        restoreState()
    }

    override fun onPause() {
        super.onPause()
        binding.bearing1.stop()
        binding.bearing2.stop()
        binding.location1.pause()
        binding.location2.pause()
        saveState()
    }

    private fun update() {
        if (!isBound) {
            return
        }

        updateInstructions()
        updateCompletionState()

        val location1 = binding.location1.coordinate
        val location2 = binding.location2.coordinate
        val direction1 = binding.bearing1.bearing
        val direction2 = binding.bearing2.bearing

        if (location1 == null || location2 == null || direction1 == null || direction2 == null) {
            setLocation(null)
            return
        }

        // All information is available to triangulate
        val declination1 =
            if (binding.bearing1.trueNorth) 0f else Geology.getGeomagneticDeclination(location1)
        val declination2 =
            if (binding.bearing2.trueNorth) 0f else Geology.getGeomagneticDeclination(location2)
        val bearing1 = direction1.withDeclination(declination1)
        val bearing2 = direction2.withDeclination(declination2)

        val location = if (shouldCalculateMyLocation) {
            Geology.triangulateSelf(location1, bearing1, location2, bearing2)
        } else {
            Geology.triangulateDestination(location1, bearing1, location2, bearing2)
        }
        setLocation(location)
    }

    private fun setLocation(location: Coordinate?) {
        this.location = location
        if (location == null || location.latitude.isNaN() || location.longitude.isNaN()) {
            binding.triangulateTitle.title.text = getString(R.string.could_not_triangulate)
            binding.triangulateTitle.rightButton.isInvisible = true
            binding.actions.isVisible = false
        } else {
            binding.triangulateTitle.title.text = formatService.formatLocation(location)
            binding.triangulateTitle.rightButton.isInvisible = false
            binding.actions.isVisible = true
        }
        updateMap()
        updateDistances()
    }

    private fun saveState() {
        val preferences = PreferencesSubsystem.getInstance(requireContext()).preferences
        preferences.putBoolean("state_triangulate_self", shouldCalculateMyLocation)
        binding.bearing1.bearing?.let {
            preferences.putFloat("state_triangulate_bearing1", it.value)
        }
        binding.bearing2.bearing?.let {
            preferences.putFloat("state_triangulate_bearing2", it.value)
        }
        binding.location1.coordinate?.let {
            preferences.putCoordinate("state_triangulate_location1", it)
        }
        binding.location2.coordinate?.let {
            preferences.putCoordinate("state_triangulate_location2", it)
        }
        preferences.putBoolean("state_triangulate_true_north1", binding.bearing1.trueNorth)
        preferences.putBoolean("state_triangulate_true_north2", binding.bearing2.trueNorth)
    }

    private fun restoreState() {
        val preferences = PreferencesSubsystem.getInstance(requireContext()).preferences
        shouldCalculateMyLocation = preferences.getBoolean("state_triangulate_self") ?: false
        binding.locationButtonGroup.check(if (shouldCalculateMyLocation) binding.locationButtonSelf.id else binding.locationButtonOther.id)
        binding.bearing1.bearing =
            preferences.getFloat("state_triangulate_bearing1")?.let { Bearing(it) }
        binding.bearing2.bearing =
            preferences.getFloat("state_triangulate_bearing2")?.let { Bearing(it) }
        binding.bearing1.trueNorth =
            preferences.getBoolean("state_triangulate_true_north1") ?: false
        binding.bearing2.trueNorth =
            preferences.getBoolean("state_triangulate_true_north2") ?: false
        binding.location1.coordinate = preferences.getCoordinate("state_triangulate_location1")
        binding.location2.coordinate = preferences.getCoordinate("state_triangulate_location2")
        update()
    }

    private fun reset() {
        binding.location1.coordinate = null
        binding.location2.coordinate = null
        binding.bearing1.bearing = null
        binding.bearing2.bearing = null
        binding.bearing1.trueNorth = false
        binding.bearing2.trueNorth = false

        val preferences = PreferencesSubsystem.getInstance(requireContext()).preferences
        preferences.remove("state_triangulate_self")
        preferences.remove("state_triangulate_bearing1")
        preferences.remove("state_triangulate_bearing2")
        preferences.remove("state_triangulate_location1")
        preferences.remove("state_triangulate_location2")
        preferences.remove("state_triangulate_true_north1")
        preferences.remove("state_triangulate_true_north2")

        update()
    }

    private fun isComplete(locationIdx: Int): Boolean {
        val location =
            if (locationIdx == 1) binding.location1.coordinate else binding.location2.coordinate
        val bearing = if (locationIdx == 1) binding.bearing1.bearing else binding.bearing2.bearing
        return location != null && bearing != null
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater, container: ViewGroup?
    ): FragmentToolTriangulateBinding {
        return FragmentToolTriangulateBinding.inflate(layoutInflater, container, false)
    }
}