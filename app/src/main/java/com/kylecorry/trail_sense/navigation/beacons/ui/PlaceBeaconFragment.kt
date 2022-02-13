package com.kylecorry.trail_sense.navigation.beacons.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.core.capitalizeWords
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.sol.math.SolMath.roundPlaces
import com.kylecorry.sol.science.geology.GeologyService
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.CompassDirection
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentCreateBeaconBinding
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.BeaconEntity
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.BeaconRepo
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.navigation.domain.LocationMath
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.extensions.onMain
import com.kylecorry.trail_sense.shared.extensions.promptIfUnsavedChanges
import com.kylecorry.trail_sense.shared.sensors.SensorService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaceBeaconFragment : BoundFragment<FragmentCreateBeaconBinding>() {

    private val beaconRepo by lazy { BeaconRepo.getInstance(requireContext()) }
    private val beaconService by lazy { BeaconService(requireContext()) }
    private lateinit var navController: NavController

    private val altimeter by lazy { sensorService.getAltimeter() }

    private lateinit var units: UserPreferences.DistanceUnits
    private val sensorService by lazy { SensorService(requireContext()) }
    private val compass by lazy { sensorService.getCompass() }
    private val formatService by lazy { FormatService(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }

    private lateinit var backCallback: OnBackPressedCallback

    private var color = AppColor.Orange

    private var editingBeacon: Beacon? = null
    private var editingBeaconId: Long? = null
    private var initialLocation: GeoUri? = null
    private val geoService = GeologyService()

    private var bearingTo: Bearing? = null

    private var parent: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val beaconId = arguments?.getLong("edit_beacon") ?: 0L
        val groupId = arguments?.getLong("initial_group") ?: 0L
        initialLocation = arguments?.getParcelable("initial_location")

        editingBeaconId = if (beaconId == 0L) {
            null
        } else {
            beaconId
        }

        parent = if (groupId == 0L) {
            null
        } else {
            groupId
        }
    }

    private fun setEditingBeaconValues(beacon: Beacon) {
        parent = beacon.parentId
        updateBeaconGroupName()

        binding.createBeaconTitle.title.text = getString(R.string.edit_beacon).capitalizeWords()
        color = AppColor.values().firstOrNull { it.color == beacon.color } ?: AppColor.Orange
        binding.beaconColor.imageTintList = ColorStateList.valueOf(beacon.color)
        binding.beaconName.setText(beacon.name)
        binding.beaconLocation.coordinate = beacon.coordinate
        binding.beaconElevation.setText(
            if (beacon.elevation != null) {
                val dist = Distance.meters(beacon.elevation)
                val userUnits =
                    dist.convertTo(if (prefs.distanceUnits == UserPreferences.DistanceUnits.Meters) DistanceUnits.Meters else DistanceUnits.Feet)
                userUnits.distance.toString()
            } else {
                ""
            }
        )
        binding.comment.setText(beacon.comment ?: "")
        updateDoneButtonState()
    }

    private fun loadExistingBeacon() {
        // TODO: Prevent interaction until loaded
        editingBeaconId?.let {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    editingBeacon = beaconRepo.getBeacon(it)?.toBeacon()
                }

                withContext(Dispatchers.Main) {
                    editingBeacon?.let { beacon ->
                        setEditingBeaconValues(beacon)
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val prefs = UserPreferences(requireContext())
        units = prefs.distanceUnits

        navController = findNavController()

        binding.createBeaconTitle.title.text = getString(R.string.create_beacon).capitalizeWords()
        binding.beaconColor.imageTintList = ColorStateList.valueOf(color.color)

        // TODO: Prevent interaction until loaded
        updateBeaconGroupName()
        loadExistingBeacon()

        // Fill in the initial location information
        if (initialLocation != null) {
            binding.beaconName.setText(initialLocation!!.queryParameters.getOrDefault("label", ""))
            binding.beaconLocation.coordinate = initialLocation!!.coordinate
            val altitude =
                initialLocation!!.altitude ?: initialLocation!!.queryParameters.getOrDefault(
                    "ele",
                    ""
                ).toFloatOrNull()
            binding.beaconElevation.setText(
                if (altitude != null) {
                    val dist = Distance.meters(altitude)
                    val userUnits = dist.convertTo(prefs.baseDistanceUnits)
                    userUnits.distance.toString()
                } else {
                    ""
                }
            )
            updateDoneButtonState()
        }

        binding.beaconName.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && !hasValidName()) {
                binding.beaconName.error = getString(R.string.beacon_invalid_name)
            } else if (!hasFocus) {
                binding.beaconName.error = null
            }
        }

        binding.beaconLocation.setOnAutoLocationClickListener {
            binding.beaconElevation.isEnabled = false
            altimeter.start(this::setElevationFromAltimeter)
        }

        binding.beaconLocation.setOnCoordinateChangeListener {
            updateDoneButtonState()
        }

        binding.beaconElevation.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && !hasValidElevation()) {
                binding.beaconElevation.error = getString(R.string.beacon_invalid_elevation)
            } else if (!hasFocus) {
                binding.beaconElevation.error = null
            }
        }

        binding.beaconName.addTextChangedListener {
            updateDoneButtonState()
        }

        binding.beaconElevation.addTextChangedListener {
            updateDoneButtonState()
        }

        binding.beaconColorPicker.setOnClickListener {
            CustomUiUtils.pickColor(
                requireContext(),
                color,
                getString(R.string.color)
            ) {
                if (it != null) {
                    color = it
                    binding.beaconColor.imageTintList = ColorStateList.valueOf(it.color)
                }
            }
        }

        binding.createAtDistance.setOnCheckedChangeListener { _, isChecked ->
            binding.distanceAway.visibility = if (isChecked) View.VISIBLE else View.GONE
            binding.bearingToHolder.visibility = if (isChecked) View.VISIBLE else View.GONE
            updateDoneButtonState()
        }

        binding.distanceAway.setOnValueChangeListener {
            updateDoneButtonState()
        }

        binding.beaconGroupPicker.setOnClickListener {
            CustomUiUtils.pickBeaconGroup(requireContext(), null) { cancelled, groupId ->
                if (!cancelled) {
                    parent = groupId
                    updateBeaconGroupName()
                }
            }
        }

        binding.bearingToBtn.setOnClickListener {
            bearingTo = compass.bearing
            binding.bearingTo.text = formatService.formatDegrees(bearingTo?.value ?: 0f)
            updateDoneButtonState()
        }

        backCallback = promptIfUnsavedChanges(this::hasChanges)

        binding.placeBeaconBtn.setOnClickListener {
            val name = binding.beaconName.text.toString()
            val createAtDistance = binding.createAtDistance.isChecked
            val distanceTo =
                binding.distanceAway.value?.convertTo(DistanceUnits.Meters)?.distance?.toDouble()
                    ?: 0.0
            val bearingTo = bearingTo ?: Bearing.from(CompassDirection.North)
            val comment = binding.comment.text.toString()
            val rawElevation = binding.beaconElevation.text.toString().toFloatOrNull()
            val elevation = if (rawElevation == null) {
                null
            } else {
                LocationMath.convertToMeters(rawElevation, units)
            }

            val coordinate = if (createAtDistance) {
                val coord = binding.beaconLocation.coordinate
                val declination = if (coord != null) {
                    geoService.getGeomagneticDeclination(coord, elevation)
                } else {
                    0f
                }
                coord?.plus(distanceTo, bearingTo.withDeclination(declination))
            } else {
                binding.beaconLocation.coordinate
            }

            if (name.isNotBlank() && coordinate != null) {
                val beacon = if (editingBeacon == null) {
                    Beacon(
                        0,
                        name,
                        coordinate,
                        true,
                        comment,
                        parent,
                        elevation,
                        color = color.color
                    )
                } else {
                    Beacon(
                        editingBeacon!!.id,
                        name,
                        coordinate,
                        editingBeacon!!.visible,
                        comment,
                        parent,
                        elevation,
                        color = color.color
                    )
                }
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        beaconRepo.addBeacon(BeaconEntity.from(beacon))
                    }

                    withContext(Dispatchers.Main) {
                        backCallback.remove()
                        navController.navigateUp()
                    }
                }
            }
        }

        if (units == UserPreferences.DistanceUnits.Feet) {
            binding.beaconElevationHolder.hint = getString(R.string.beacon_elevation_hint_feet)
        } else {
            binding.beaconElevationHolder.hint = getString(R.string.beacon_elevation_hint_meters)
        }

        binding.bearingToBtn.text =
            getString(R.string.beacon_set_bearing_btn, formatService.formatDegrees(0f))
        binding.distanceAway.units = formatService.sortDistanceUnits(
            listOf(
                DistanceUnits.Meters,
                DistanceUnits.Kilometers,
                DistanceUnits.Feet,
                DistanceUnits.Miles,
                DistanceUnits.NauticalMiles
            )
        )
    }

    override fun onResume() {
        super.onResume()
        compass.start(this::onCompassUpdate)
    }

    override fun onPause() {
        compass.stop(this::onCompassUpdate)
        altimeter.stop(this::setElevationFromAltimeter)
        binding.beaconElevation.isEnabled = true
        binding.beaconLocation.pause()
        super.onPause()
    }

    private fun onCompassUpdate(): Boolean {
        binding.bearingToBtn.text = getString(
            R.string.beacon_set_bearing_btn,
            formatService.formatDegrees(compass.bearing.value)
        )
        return true
    }

    private fun setElevationFromAltimeter(): Boolean {
        binding.beaconElevation.isEnabled = true
        if (units == UserPreferences.DistanceUnits.Meters) {
            binding.beaconElevation.setText(altimeter.altitude.roundPlaces(1).toString())
        } else {
            binding.beaconElevation.setText(
                LocationMath.convertToBaseUnit(altimeter.altitude, units).roundPlaces(1).toString()
            )
        }
        return false
    }

    private fun updateDoneButtonState() {
        binding.placeBeaconBtn.visibility =
            if (hasValidName() && binding.beaconLocation.coordinate != null && hasValidElevation() && hasValidDistanceTo()) View.VISIBLE else View.GONE
    }

    private fun hasValidDistanceTo(): Boolean {
        if (!binding.createAtDistance.isChecked) {
            return true
        }

        if (binding.distanceAway.value == null) {
            return false
        }

        return bearingTo != null
    }


    private fun hasValidElevation(): Boolean {
        return binding.beaconElevation.text.isNullOrBlank() || binding.beaconElevation.text.toString()
            .toFloatOrNull() != null
    }

    private fun hasValidName(): Boolean {
        return binding.beaconName.text.toString().isNotBlank()
    }

    private fun hasChanges(): Boolean {
        val name = binding.beaconName.text.toString()
        val createAtDistance = binding.createAtDistance.isChecked
        val distanceTo =
            binding.distanceAway.value?.convertTo(DistanceUnits.Meters)?.distance?.toDouble()
                ?: 0.0
        val bearingTo = bearingTo ?: Bearing.from(CompassDirection.North)
        val comment = binding.comment.text.toString()
        val rawElevation = binding.beaconElevation.text.toString().toFloatOrNull()
        val elevation = if (rawElevation == null) {
            null
        } else {
            LocationMath.convertToMeters(rawElevation, units)
        }

        val coordinate = if (createAtDistance) {
            val coord = binding.beaconLocation.coordinate
            val declination = if (coord != null) {
                geoService.getGeomagneticDeclination(coord, elevation)
            } else {
                0f
            }
            coord?.plus(distanceTo, bearingTo.withDeclination(declination))
        } else {
            binding.beaconLocation.coordinate
        }

        return !nothingEntered() && (name != editingBeacon?.name || coordinate != editingBeacon?.coordinate ||
                comment != editingBeacon?.comment || elevation != editingBeacon?.elevation ||
                parent != editingBeacon?.parentId)
    }

    private fun nothingEntered(): Boolean {
        if (editingBeacon != null) {
            return false
        }

        val name = binding.beaconName.text.toString()
        val createAtDistance = binding.createAtDistance.isChecked
        val comment = binding.comment.text.toString()
        val elevation = binding.beaconElevation.text.toString()
        val location = binding.beaconLocation.coordinate

        return name.isBlank() && !createAtDistance && comment.isBlank() && elevation.isBlank() && location == null && parent == null

    }

    private fun updateBeaconGroupName() {
        val parent = parent
        runInBackground {
            val name = onIO {
                if (parent == null) {
                    getString(R.string.no_group)
                } else {
                    beaconService.getGroup(parent)?.name ?: ""
                }
            }

            onMain {
                binding.beaconGroup.text = name
            }
        }
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCreateBeaconBinding {
        return FragmentCreateBeaconBinding.inflate(layoutInflater, container, false)
    }

}