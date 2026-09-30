package com.kylecorry.trail_sense.tools.convert.ui

import android.content.Context
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import com.google.android.material.textfield.TextInputEditText
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment

abstract class SimpleConvertFragment<T>(private val defaultFrom: T, private val defaultTo: T) :
    TrailSenseReactiveFragment(R.layout.fragment_tool_simple_convert) {

    abstract val units: List<T>

    override fun update() {
        val context = useAndroidContext()

        // Views
        val fromUnitsView = useView<Spinner>(R.id.from_units)
        val toUnitsView = useView<Spinner>(R.id.to_units)
        val swapButtonView = useView<ImageButton>(R.id.swap_btn)
        val unitEditView = useView<TextInputEditText>(R.id.unit_edit)
        val resultView = useView<TextView>(R.id.result)

        // State
        val (amountText, setAmountText) = useState(unitEditView.text?.toString() ?: "")
        val (fromIndex, setFromIndex) = useState(units.indexOf(defaultFrom))
        val (toIndex, setToIndex) = useState(units.indexOf(defaultTo))

        val result = useMemo(amountText, fromIndex, toIndex) {
            convert(amountText.toFloatOrNull() ?: 0.0f, units[fromIndex], units[toIndex])
        }

        // View - Unit spinners
        useEffect(fromUnitsView, context) {
            bindUnitSpinner(fromUnitsView, context, getString(R.string.distance_from), fromIndex) {
                setFromIndex(it)
            }
        }

        useEffect(toUnitsView, context) {
            bindUnitSpinner(toUnitsView, context, getString(R.string.distance_to), toIndex) {
                setToIndex(it)
            }
        }

        // Keeps the spinners in sync when the units are swapped
        useEffect(fromUnitsView, fromIndex) {
            if (fromUnitsView.selectedItemPosition != fromIndex) {
                fromUnitsView.setSelection(fromIndex)
            }
        }

        useEffect(toUnitsView, toIndex) {
            if (toUnitsView.selectedItemPosition != toIndex) {
                toUnitsView.setSelection(toIndex)
            }
        }

        useEffect(swapButtonView, fromIndex, toIndex) {
            swapButtonView.setOnClickListener {
                setFromIndex(toIndex)
                setToIndex(fromIndex)
            }
        }

        // View - Amount and result
        useEffect(unitEditView) {
            setAmountText(unitEditView.text?.toString() ?: "")
            unitEditView.addTextChangedListener {
                setAmountText(it?.toString() ?: "")
            }
        }

        useEffect(resultView, result) {
            resultView.text = result
        }
    }

    private fun bindUnitSpinner(
        spinner: Spinner,
        context: Context,
        prompt: String,
        selectedIndex: Int,
        onSelected: (Int) -> Unit
    ) {
        spinner.prompt = prompt
        spinner.adapter = ArrayAdapter(
            context,
            R.layout.spinner_item_plain,
            R.id.item_name,
            units.map { getUnitName(it) }
        )
        spinner.setSelection(selectedIndex)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                onSelected(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                onSelected(spinner.selectedItemPosition)
            }
        }
    }

    abstract fun convert(amount: Float, from: T, to: T): String

    abstract fun getUnitName(unit: T): String
}
