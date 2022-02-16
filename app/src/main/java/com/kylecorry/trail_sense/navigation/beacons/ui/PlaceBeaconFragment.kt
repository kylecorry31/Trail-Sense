package com.kylecorry.trail_sense.navigation.beacons.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isInvisible
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.core.capitalizeWords
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.sol.science.geology.GeologyService
import com.kylecorry.sol.units.*
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentCreateBeaconBinding
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.extensions.onMain
import com.kylecorry.trail_sense.shared.extensions.promptIfUnsavedChanges
import com.kylecorry.trail_sense.shared.sensors.SensorService

class PlaceBeaconFragment : BoundFragment<FragmentCreateBeaconBinding>() {

    private val beaconService by lazy { BeaconService(requireContext()) }
    private lateinit var navController: NavController

    private val altimeter by lazy { sensorService.getAltimeter() }

    private lateinit var units: UserPreferences.DistanceUnits
    private val sensorService by lazy { SensorService(requireContext()) }
    private val compass by lazy { sensorService.getCompass() }
    private val formatService by lazy { FormatService(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }

    private lateinit var backCallback: OnBackPressedCallback

    private var editingBeacon: Beacon? = null
    private var editingBeaconId: Long? = null
    private var initialLocation: GeoUri? = null
    private val geoService = GeologyService()

    private val form by lazy { CreateBeaconForm(prefs.baseDistanceUnits, formatService) }

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

        form.updateData(
            form.data.copy(
                groupId = if (groupId == 0L) {
                    null
                } else {
                    groupId
                }
            )
        )
    }

    private fun setEditingBeaconValues(beacon: Beacon) {
        val data = CreateBeaconData.from(beacon)
        form.updateData(data)
        fill(data)
    }

    private fun loadExistingBeacon() {
        // TODO: Prevent interaction until loaded
        editingBeaconId?.let {
            runInBackground {
                onIO {
                    editingBeacon = beaconService.getBeacon(it)
                }

                onMain {
                    editingBeacon?.let { beacon ->
                        setEditingBeaconValues(beacon)
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        units = prefs.distanceUnits

        form.bind(binding, compass)

        navController = findNavController()

        binding.createBeaconTitle.title.text = getString(R.string.create_beacon).capitalizeWords()
        binding.beaconColor.imageTintList = ColorStateList.valueOf(AppColor.Orange.color)

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

        form.setOnDataChangeListener {
            updateDoneButtonState()
        }

        binding.beaconColorPicker.setOnClickListener {
            CustomUiUtils.pickColor(
                requireContext(),
                form.data.color,
                getString(R.string.color)
            ) {
                if (it != null) {
                    form.onColorChanged(it)
                    binding.beaconColor.imageTintList = ColorStateList.valueOf(it.color)
                }
            }
        }

        binding.beaconGroupPicker.setOnClickListener {
            CustomUiUtils.pickBeaconGroup(requireContext(), null) { cancelled, groupId ->
                if (!cancelled) {
                    form.onGroupChanged(groupId)
                    updateBeaconGroupName()
                }
            }
        }
        
        backCallback = promptIfUnsavedChanges(this::hasChanges)

        binding.createBeaconTitle.rightQuickAction.setOnClickListener { onSubmit() }

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
        val elevation =
            Distance.meters(altimeter.altitude).convertTo(prefs.baseDistanceUnits).distance
        binding.beaconElevation.setText(DecimalFormatter.format(elevation, 2))
        return false
    }

    private fun updateDoneButtonState() {
        binding.createBeaconTitle.rightQuickAction.isInvisible = !isComplete()
    }

    private fun isComplete(): Boolean {
        val data = form.data
        return !data.name.isNullOrBlank() &&
                data.coordinate != null &&
                hasValidDistanceTo(data)
    }

    private fun hasValidDistanceTo(data: CreateBeaconData): Boolean {
        if (!data.createAtDistance) {
            return true
        }

        if (data.distanceTo == null) {
            return false
        }

        return data.bearingTo != null
    }

    private fun hasValidName(): Boolean {
        return !form.data.name.isNullOrBlank()
    }

    private fun hasChanges(): Boolean {
        val original = editingBeacon?.let { CreateBeaconData.from(it) } ?: CreateBeaconData.empty
        return original != form.data
    }

    private fun updateBeaconGroupName() {
        val parent = form.data.groupId
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

    private fun fill(data: CreateBeaconData) {
        binding.beaconName.setText(data.name)
        binding.beaconLocation.coordinate = data.coordinate
        binding.beaconElevation.setText(
            data.elevation?.let {
                DecimalFormatter.format(it.convertTo(prefs.baseDistanceUnits).distance, 2)
            })
        binding.createAtDistance.isChecked = data.createAtDistance
        binding.distanceAway.value = data.distanceTo
        binding.bearingTo.text = data.bearingTo?.let { formatService.formatDegrees(it.value) } ?: ""
        binding.beaconColor.imageTintList = ColorStateList.valueOf(data.color.color)
        binding.comment.setText(data.notes)
        updateBeaconGroupName()
    }

    private fun getRealCoordinate(data: CreateBeaconData): Coordinate? {
        if (data.coordinate == null) return null
        return if (data.createAtDistance) {
            val distanceTo = data.distanceTo?.meters()?.distance?.toDouble() ?: 0.0
            val bearingTo = data.bearingTo ?: Bearing.from(CompassDirection.North)
            val declination = geoService.getGeomagneticDeclination(
                data.coordinate,
                data.elevation?.meters()?.distance
            )
            data.coordinate.plus(distanceTo, bearingTo.withDeclination(declination))
        } else {
            data.coordinate
        }
    }

    private fun onSubmit() {
        val data = form.data

        if (!isComplete()) return

        val beacon = if (editingBeacon == null) {
            Beacon(
                0,
                data.name!!,
                getRealCoordinate(data)!!,
                true,
                data.notes,
                data.groupId,
                data.elevation?.meters()?.distance,
                color = data.color.color
            )
        } else {
            Beacon(
                editingBeacon!!.id,
                data.name!!,
                getRealCoordinate(data)!!,
                editingBeacon!!.visible,
                data.notes,
                data.groupId,
                data.elevation?.meters()?.distance,
                color = data.color.color
            )
        }
        runInBackground {
            onIO {
                beaconService.add(beacon)
            }

            onMain {
                backCallback.remove()
                navController.navigateUp()
            }
        }
    }
}