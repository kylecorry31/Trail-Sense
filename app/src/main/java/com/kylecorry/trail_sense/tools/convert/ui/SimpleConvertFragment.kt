package com.kylecorry.trail_sense.tools.convert.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.widget.addTextChangedListener
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolSimpleConvertBinding
import com.kylecorry.trailsensecore.infrastructure.view.BoundFragment

abstract class SimpleConvertFragment<T>(private val defaultFrom: T, private val defaultTo: T) : BoundFragment<FragmentToolSimpleConvertBinding>() {

    abstract val units: List<T>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val fromAdapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_item_plain,
            R.id.item_name,
            units.map { getUnitName(it) })
        binding.fromUnits.prompt = getString(R.string.distance_from)
        binding.fromUnits.adapter = fromAdapter
        binding.fromUnits.setSelection(units.indexOf(defaultFrom))

        val toAdapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_item_plain,
            R.id.item_name,
            units.map { getUnitName(it) })
        binding.toUnits.prompt = getString(R.string.distance_to)
        binding.toUnits.adapter = toAdapter
        binding.toUnits.setSelection(units.indexOf(defaultTo))

        binding.swapBtn.setOnClickListener {
            val fromCurrent = binding.fromUnits.selectedItemPosition
            val toCurrent = binding.toUnits.selectedItemPosition
            binding.fromUnits.setSelection(toCurrent)
            binding.toUnits.setSelection(fromCurrent)
            update()
        }

        binding.unitEdit.addTextChangedListener {
            update()
        }

        binding.fromUnits.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
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

        binding.toUnits.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
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

        update()
    }

    fun update(){
        val amount = binding.unitEdit.text?.toString()?.toFloatOrNull() ?: 0.0f
        val from = units[binding.fromUnits.selectedItemPosition]
        val to = units[binding.toUnits.selectedItemPosition]
        binding.result.text = convert(amount, from, to)
    }

    abstract fun convert(amount: Float, from: T, to: T): String

    abstract fun getUnitName(unit: T): String

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentToolSimpleConvertBinding {
        return FragmentToolSimpleConvertBinding.inflate(layoutInflater, container, false)
    }

}