package com.kylecorry.trail_sense.tools.coordinateconvert.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolCoordinateConvertBinding
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trailsensecore.domain.geo.CoordinateFormat

class FragmentToolCoordinateConvert: Fragment() {

    private var _binding: FragmentToolCoordinateConvertBinding? = null
    private val binding get() = _binding!!

    private val formatService by lazy { FormatService(requireContext()) }

    private val formats = listOf(
        CoordinateFormat.DecimalDegrees,
        CoordinateFormat.DegreesDecimalMinutes,
        CoordinateFormat.DegreesMinutesSeconds,
        CoordinateFormat.UTM
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentToolCoordinateConvertBinding.inflate(inflater, container, false)

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

        return binding.root
    }

    override fun onPause() {
        super.onPause()
        binding.coordinateEdit.pause()
    }


    fun update(){
        val coordinate = binding.coordinateEdit.coordinate
        val to = formats[binding.toUnits.selectedItemPosition]

        if (coordinate == null){
            binding.result.text = ""
            return
        }

        binding.result.text = formatService.formatLocation(coordinate, to)
    }

}