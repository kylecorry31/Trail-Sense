package com.kylecorry.trail_sense.navigation.beacons.ui.form

import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.kylecorry.andromeda.sense.compass.ICompass
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.databinding.FragmentCreateBeaconBinding
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.extensions.floatValue

class CreateBeaconForm(
    private val units: DistanceUnits,
    private val formatter: FormatService
) {

    private var listener: (data: CreateBeaconData) -> Unit = {}

    var data = CreateBeaconData.empty
        private set

    fun setOnDataChangeListener(listener: (data: CreateBeaconData) -> Unit){
        this.listener = listener
    }

    fun onGroupChanged(groupId: Long?) {
        updateData(data.copy(groupId = groupId))
    }

    fun onColorChanged(color: AppColor) {
        updateData(data.copy(color = color))
    }

    fun bind(binding: FragmentCreateBeaconBinding, compass: ICompass) {
        binding.beaconName.addTextChangedListener { updateData(data.copy(name = getName(binding))) }
        binding.beaconElevation.addTextChangedListener {
            updateData(
                data.copy(
                    elevation = getElevation(
                        binding
                    )
                )
            )
        }
        binding.beaconLocation.setOnCoordinateChangeListener { updateData(data.copy(coordinate = it)) }
        binding.distanceAway.isVisible = binding.createAtDistance.isChecked
        binding.bearingToHolder.isVisible = binding.createAtDistance.isChecked
        binding.createAtDistance.setOnCheckedChangeListener { _, isChecked ->
            binding.distanceAway.isVisible = isChecked
            binding.bearingToHolder.isVisible = isChecked
            updateData(
                data.copy(
                    createAtDistance = isChecked
                )
            )
        }
        binding.distanceAway.setOnValueChangeListener { updateData(data.copy(distanceTo = it)) }
        binding.bearingToBtn.setOnClickListener {
            binding.bearingTo.text = formatter.formatDegrees(compass.bearing.value)
            updateData(data.copy(bearingTo = compass.bearing))
        }
        binding.comment.addTextChangedListener { updateData(data.copy(notes = getNotes(binding))) }
    }

    fun updateData(data: CreateBeaconData) {
        this.data = data
        listener(this.data)
    }

    private fun getElevation(binding: FragmentCreateBeaconBinding): Distance? {
        val rawElevation = binding.beaconElevation.floatValue()
        return rawElevation?.let {
            Distance(
                rawElevation,
                units
            )
        }
    }

    private fun getName(binding: FragmentCreateBeaconBinding): String? {
        return binding.beaconName.text?.toString()
    }

    private fun getNotes(binding: FragmentCreateBeaconBinding): String? {
        return binding.comment.text?.toString()
    }

}