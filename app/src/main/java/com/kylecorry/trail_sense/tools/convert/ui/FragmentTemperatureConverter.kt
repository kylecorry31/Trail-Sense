package com.kylecorry.trail_sense.tools.convert.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolTemperatureConvertBinding
import com.kylecorry.trail_sense.shared.FormatServiceV2
import com.kylecorry.trailsensecore.domain.units.*
import com.kylecorry.trailsensecore.infrastructure.view.BoundFragment

class FragmentTemperatureConverter : BoundFragment<FragmentToolTemperatureConvertBinding>() {

    private val formatService by lazy { FormatServiceV2(requireContext()) }

    private var toUnits = TemperatureUnits.F
    private var fromUnits = TemperatureUnits.C

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setToFromUnits()

        binding.swapBtn.setOnClickListener {
            val temp = toUnits
            toUnits = fromUnits
            fromUnits = temp
            setToFromUnits()
            update()
        }

        binding.temperatureEdit.addTextChangedListener {
            update()
        }
    }

    fun update(){
        val temperature = binding.temperatureEdit.text?.toString()?.toFloatOrNull() ?: 0.0f
        val converted = Temperature(temperature, fromUnits).convertTo(toUnits)
        binding.result.text = formatService.formatTemperature(converted, 4, false)
    }

    private fun setToFromUnits(){
        binding.fromUnits.text = getUnitName(fromUnits)
        binding.toUnits.text = getUnitName(toUnits)
    }

    private fun getUnitName(unit: TemperatureUnits): String {
        return when(unit){
            TemperatureUnits.F -> getString(R.string.fahrenheit)
            TemperatureUnits.C -> getString(R.string.celsius)
        }
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentToolTemperatureConvertBinding {
        return FragmentToolTemperatureConvertBinding.inflate(layoutInflater, container, false)
    }

}