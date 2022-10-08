package com.kylecorry.trail_sense.navigation.beacons.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isInvisible
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.core.capitalizeWords
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.setCompoundDrawables
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentCreateBeaconBinding
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.BeaconPickers
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.navigation.beacons.ui.form.CreateBeaconData
import com.kylecorry.trail_sense.navigation.beacons.ui.form.CreateBeaconForm
import com.kylecorry.trail_sense.navigation.beacons.ui.form.DoesBeaconFormDataHaveChanges
import com.kylecorry.trail_sense.navigation.beacons.ui.form.IsBeaconFormDataComplete
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.inBackground
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.extensions.onMain
import com.kylecorry.trail_sense.shared.extensions.promptIfUnsavedChanges
import com.kylecorry.trail_sense.shared.sensors.SensorService

class PlaceBeaconFragment : BoundFragment<FragmentCreateBeaconBinding>() {

    private val beaconService by lazy { BeaconService(requireContext()) }

    private val altimeter by lazy { sensorService.getAltimeter() }

    private val sensorService by lazy { SensorService(requireContext()) }
    private val formatter by lazy { FormatService(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val units by lazy { prefs.baseDistanceUnits }

    private lateinit var backCallback: OnBackPressedCallback

    private var editingBeaconId: Long? = null
    private var initialLocation: GeoUri? = null

    private val isComplete = IsBeaconFormDataComplete()
    private var hasChanges = DoesBeaconFormDataHaveChanges(CreateBeaconData.empty)

    private val form = CreateBeaconForm()

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
        hasChanges = DoesBeaconFormDataHaveChanges(data)
        fill(data)
    }

    private fun loadExistingBeacon() {
        // TODO: Prevent interaction until loaded
        editingBeaconId?.let {
            inBackground {
                val beacon = onIO {
                    beaconService.getBeacon(it)
                }

                onMain {
                    beacon?.let { it ->
                        setEditingBeaconValues(it)
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        form.bind(binding)
        binding.beaconElevation.defaultHint = getString(R.string.elevation)
        binding.beaconElevation.hint = getString(R.string.elevation)
        CustomUiUtils.setButtonState(binding.createBeaconTitle.rightButton, true)
        binding.createBeaconTitle.title.text = getString(R.string.create_beacon).capitalizeWords()

        // TODO: Prevent interaction until loaded
        updateIcon()
        updateColor()
        updateBeaconGroupName()
        loadExistingBeacon()
        updateElevationUnits()

        // Fill in the initial location information
        initialLocation?.let {
            val data = CreateBeaconData.from(it).copy(groupId = form.data.groupId)
            fill(data)
            updateSubmitButton()
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
            updateSubmitButton()
        }

        binding.beaconColorPicker.setOnClickListener {
            CustomUiUtils.pickColor(
                requireContext(),
                form.data.color,
                getString(R.string.color)
            ) {
                if (it != null) {
                    form.onColorChanged(it)
                    updateColor()
                }
            }
        }

        binding.beaconIconPicker.setOnClickListener {
            CustomUiUtils.pickBeaconIcon(
                requireContext(),
                form.data.icon,
                getString(R.string.icon)
            ) {
                if (it != null) {
                    form.onIconChanged(it)
                    updateIcon()
                }
            }
        }

        binding.beaconGroupPicker.setOnClickListener {
            inBackground {
                val result = BeaconPickers.pickGroup(
                    requireContext(),
                    initialGroup = form.data.groupId
                )
                if (result.first) {
                    return@inBackground
                }
                form.onGroupChanged(result.second?.id)
                updateBeaconGroupName()
            }
        }

        backCallback = promptIfUnsavedChanges { hasChanges.isSatisfiedBy(form.data) }

        binding.createBeaconTitle.rightButton.setOnClickListener { onSubmit() }

        binding.distanceAway.units = formatter.sortDistanceUnits(
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
        binding.bearingTo.start()
    }

    override fun onPause() {
        altimeter.stop(this::setElevationFromAltimeter)
        binding.beaconElevation.isEnabled = true
        binding.beaconLocation.pause()
        binding.bearingTo.stop()
        super.onPause()
    }

    private fun setElevationFromAltimeter(): Boolean {
        binding.beaconElevation.isEnabled = true
        val elevation =
            Distance.meters(altimeter.altitude).convertTo(units)
        binding.beaconElevation.value = elevation
        return false
    }

    private fun updateSubmitButton() {
        binding.createBeaconTitle.rightButton.isInvisible =
            !isComplete.isSatisfiedBy(form.data)
    }


    private fun hasValidName(): Boolean {
        return form.data.name.isNotBlank()
    }

    private fun updateColor() {
        CustomUiUtils.setImageColor(binding.beaconColorPicker, form.data.color.color)
    }

    private fun updateIcon() {
        binding.beaconIconPicker.setCompoundDrawables(
            Resources.dp(requireContext(), 24f).toInt(),
            left = form.data.icon?.icon ?: R.drawable.bubble
        )
    }

    private fun updateBeaconGroupName() {
        val parent = form.data.groupId
        inBackground {
            val name = onIO {
                if (parent == null) {
                    getString(R.string.no_group)
                } else {
                    beaconService.getGroup(parent)?.name ?: ""
                }
            }

            onMain {
                binding.beaconGroupPicker.text = name
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
        form.updateData(data)
        binding.beaconName.setText(data.name)
        binding.beaconLocation.coordinate = data.coordinate
        binding.beaconElevation.value = data.elevation?.convertTo(units)
        updateElevationUnits()
        binding.comment.setText(data.notes)
        updateBeaconGroupName()
        updateColor()
        updateIcon()
    }

    private fun updateElevationUnits() {
        binding.beaconElevation.units =
            formatter.sortDistanceUnits(listOf(DistanceUnits.Meters, DistanceUnits.Feet))
    }

    private fun onSubmit() {
        val beacon = form.data.toBeacon() ?: return
        inBackground {
            onIO {
                beaconService.add(beacon)
            }

            onMain {
                backCallback.remove()
                findNavController().navigateUp()
            }
        }
    }
}