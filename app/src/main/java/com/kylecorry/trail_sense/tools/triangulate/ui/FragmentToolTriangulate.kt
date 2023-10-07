package com.kylecorry.trail_sense.tools.triangulate.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.system.GeoUri
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
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.extensions.from

class FragmentToolTriangulate : BoundFragment<FragmentToolTriangulateBinding>() {

    private val formatService by lazy { FormatService.getInstance(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }

    private var direction1: Bearing? = null
    private var trueNorth1: Boolean = false
    private var trueNorth2: Boolean = false
    private var direction2: Bearing? = null
    private var location: Coordinate? = null

    private var shouldCalculateMyLocation = false

    private val beaconLayer = BeaconLayer()
    private val pathLayer = PathLayer()

    private val radius = Distance.meters(100f)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.bearing1.setOnBearingChangeListener { bearing, isTrueNorth ->
            direction1 = bearing
            trueNorth1 = isTrueNorth
            update()
        }

        binding.bearing2.setOnBearingChangeListener { bearing, isTrueNorth ->
            direction2 = bearing
            trueNorth2 = isTrueNorth
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
            // TODO: Set the dropdown icon
        }

        binding.location2Expansion.setOnExpandStateChangedListener {
            // TODO: Set the dropdown icon
        }

        // Expand the first location by default (this will change once it loads the last recorded values)
        binding.location1Expansion.expand()

        // TODO: Add an status indicator for the location
        // TODO: Display the distance to the location in the title
        beaconLayer.setOutlineColor(Color.WHITE)
        binding.map.setLayers(listOf(pathLayer, beaconLayer))
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

        binding.triangulateTitle.subtitle.text = if (shouldCalculateMyLocation) {
            null
        } else {
            getDistanceToDestination()?.let {
                getString(
                    R.string.distance_away,
                    formatService.formatDistance(it, Units.getDecimalPlaces(it.units))
                )
            }
        }

        // TODO: Display distance between locations
    }

    private fun getDistanceToDestination(): Distance? {
        // TODO: Maybe use the GPS for this
        val location2 = binding.location2.coordinate ?: return null
        val destination = location ?: return null
        return Distance.meters(location2.distanceTo(destination)).convertTo(prefs.baseDistanceUnits)
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
        val direction = if (locationIdx == 1) direction1 else direction2
        val trueNorth = if (locationIdx == 1) trueNorth1 else trueNorth2

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
    }

    override fun onPause() {
        super.onPause()
        binding.bearing1.stop()
        binding.bearing2.stop()
    }

    private fun update() {
        if (!isBound) {
            return
        }

        val location1 = binding.location1.coordinate
        val location2 = binding.location2.coordinate
        val direction1 = direction1
        val direction2 = direction2

        if (location1 == null || location2 == null || direction1 == null || direction2 == null) {
            setLocation(null)
            return
        }

        // All information is available to triangulate
        val declination1 = if (trueNorth1) 0f else Geology.getGeomagneticDeclination(location1)
        val declination2 = if (trueNorth2) 0f else Geology.getGeomagneticDeclination(location2)
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

    override fun generateBinding(
        layoutInflater: LayoutInflater, container: ViewGroup?
    ): FragmentToolTriangulateBinding {
        return FragmentToolTriangulateBinding.inflate(layoutInflater, container, false)
    }
}