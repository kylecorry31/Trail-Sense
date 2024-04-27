package com.kylecorry.trail_sense.tools.triangulate.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.buildSpannedString
import androidx.core.text.scale
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
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
import com.kylecorry.trail_sense.shared.AppUtils
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.CustomUiUtils.getPrimaryMarkerColor
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.extensions.from
import com.kylecorry.trail_sense.shared.extensions.putOrRemoveCoordinate
import com.kylecorry.trail_sense.shared.extensions.putOrRemoveFloat
import com.kylecorry.trail_sense.shared.navigation.NavControllerAppNavigation
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sharing.Share
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.navigation.ui.IMappablePath
import com.kylecorry.trail_sense.tools.navigation.ui.MappableLocation
import com.kylecorry.trail_sense.tools.navigation.ui.MappablePath
import com.kylecorry.trail_sense.tools.navigation.ui.layers.BeaconLayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.PathLayer
import com.kylecorry.trail_sense.tools.paths.domain.LineStyle

class FragmentToolTriangulate : BoundFragment<FragmentToolTriangulateBinding>() {

    private val formatService by lazy { FormatService.getInstance(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val appNavigation by lazy { NavControllerAppNavigation(findNavController()) }
    private val navigator by lazy { Navigator.getInstance(requireContext()) }

    private var location: Coordinate? = null

    private var shouldCalculateMyLocation = false

    private val beaconLayer = BeaconLayer(showLabels = true)
    private val pathLayer = PathLayer()

    private val radius = Distance.meters(100f)

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

        binding.createBeacon.setOnClickListener {
            location?.let {
                AppUtils.placeBeacon(requireContext(), GeoUri(it))
            }
        }

        binding.navigate.setOnClickListener {
            location?.let {
                navigator.navigateTo(it, getString(R.string.location), BeaconOwner.Triangulate)
                appNavigation.navigate(R.id.action_navigation)
            }
        }

        binding.shareLocation.setOnClickListener {
            location?.let {
                Share.shareLocation(this, it)
            }
        }

        binding.updateGpsOverride.setOnClickListener {
            location?.let { coord ->
                prefs.locationOverride = coord
                Alerts.toast(requireContext(), getString(R.string.location_override_updated))
            }
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
        pathLayer.setShouldRenderWithDrawLines(true)
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
            location1?.let {
                Beacon.temporary(
                    it,
                    id = 1,
                    name = "1",
                    color = Resources.getPrimaryMarkerColor(requireContext())
                )
            },
            location2?.let {
                Beacon.temporary(
                    it,
                    id = 2,
                    name = "2",
                    color = Resources.getPrimaryMarkerColor(requireContext())
                )
            },
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
            append(getString(R.string.location_number, locationIdx))
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

    private fun getPath(locationIdx: Int): IMappablePath? {
        val destination = location
        val start =
            if (locationIdx == 1) binding.location1.coordinate else binding.location2.coordinate
        val direction = if (locationIdx == 1) binding.bearing1.bearing else binding.bearing2.bearing
        val trueNorth =
            if (locationIdx == 1) binding.bearing1.trueNorth else binding.bearing2.trueNorth

        val end = if (start != null && direction != null) {
            val declination = if (trueNorth) 0f else Geology.getGeomagneticDeclination(start)
            val bearing = Bearing(direction).withDeclination(declination)
            destination ?: start.plus(
                Distance.kilometers(1f),
                if (shouldCalculateMyLocation) bearing.inverse() else bearing
            )
        } else {
            null
        }

        return if (start != null && end != null) {
            val pts = listOf(
                MappableLocation(0, start, Resources.getPrimaryMarkerColor(requireContext()), null),
                MappableLocation(0, end, Resources.getPrimaryMarkerColor(requireContext()), null),
            )
            MappablePath(
                locationIdx.toLong(),
                if (shouldCalculateMyLocation) pts.reversed() else pts,
                Resources.getPrimaryMarkerColor(requireContext()), LineStyle.Arrow
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
        val bearing1 = Bearing(direction1).withDeclination(declination1)
        val bearing2 = Bearing(direction2).withDeclination(declination2)

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
            binding.actions.isVisible = false
        } else {
            binding.triangulateTitle.title.text = formatService.formatLocation(location)
            binding.actions.isVisible = true
        }

        // Update action button visibility
        binding.navigate.isVisible = !shouldCalculateMyLocation
        binding.updateGpsOverride.isVisible = !prefs.useAutoLocation && shouldCalculateMyLocation

        updateMap()
        updateDistances()
    }

    private fun saveState() {
        val preferences = PreferencesSubsystem.getInstance(requireContext()).preferences
        preferences.putBoolean("state_triangulate_self", shouldCalculateMyLocation)
        preferences.putOrRemoveFloat("state_triangulate_bearing1", binding.bearing1.bearing)
        preferences.putOrRemoveFloat("state_triangulate_bearing2", binding.bearing2.bearing)
        preferences.putOrRemoveCoordinate(
            "state_triangulate_location1",
            binding.location1.coordinate
        )
        preferences.putOrRemoveCoordinate(
            "state_triangulate_location2",
            binding.location2.coordinate
        )
        preferences.putBoolean("state_triangulate_true_north1", binding.bearing1.trueNorth)
        preferences.putBoolean("state_triangulate_true_north2", binding.bearing2.trueNorth)
    }

    private fun restoreState() {
        val preferences = PreferencesSubsystem.getInstance(requireContext()).preferences
        shouldCalculateMyLocation = preferences.getBoolean("state_triangulate_self") ?: false
        binding.locationButtonGroup.check(if (shouldCalculateMyLocation) binding.locationButtonSelf.id else binding.locationButtonOther.id)
        binding.bearing1.bearing = preferences.getFloat("state_triangulate_bearing1")
        binding.bearing2.bearing = preferences.getFloat("state_triangulate_bearing2")
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