package com.kylecorry.trail_sense.tools.convert.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolCoordinateConvertBinding
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trailsensecore.domain.geo.CoordinateFormat
import com.kylecorry.trailsensecore.infrastructure.view.BoundFragment

class FragmentToolCoordinateConvert : BoundFragment<FragmentToolCoordinateConvertBinding>() {

    private val formatService by lazy { FormatService(requireContext()) }

    private val formats = CoordinateFormat.values()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toUnits.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                update()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                update()
            }
        }

        binding.coordinateEdit.setOnCoordinateChangeListener {
            update()
        }

        val toAdapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_item_plain,
            R.id.item_name,
            formats.map { formatService.coordinateFormatString(it) })
        binding.toUnits.prompt = getString(R.string.distance_from)
        binding.toUnits.adapter = toAdapter
        binding.toUnits.setSelection(0)
    }

    override fun onPause() {
        super.onPause()
        binding.coordinateEdit.pause()
    }


    fun update() {
        val coordinate = binding.coordinateEdit.coordinate
        val to = formats[binding.toUnits.selectedItemPosition]

        if (coordinate == null) {
            binding.result.text = ""
            return
        }

        binding.result.text = formatService.formatLocation(coordinate, to)
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentToolCoordinateConvertBinding {
        return FragmentToolCoordinateConvertBinding.inflate(layoutInflater, container, false)
    }

}