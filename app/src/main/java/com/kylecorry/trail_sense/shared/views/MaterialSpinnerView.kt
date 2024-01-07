package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.Spinner
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.kylecorry.trail_sense.R

class MaterialSpinnerView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    private val spinner: Spinner
    private val edittext: TextInputEditText
    private val holder: TextInputLayout
    private var listener: ((Int?) -> Unit)? = null

    val selectedItemPosition: Int
        get() = spinner.selectedItemPosition

    val selectedItem: String?
        get() = spinner.selectedItem?.toString()

    init {
        inflate(context, R.layout.view_material_spinner, this)
        spinner = findViewById(R.id.material_spinner_spinner)
        edittext = findViewById(R.id.material_spinner_edittext)
        holder = findViewById(R.id.material_spinner_holder)

        edittext.setOnClickListener {
            spinner.performClick()
        }

        spinner.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                listener?.invoke(position)
                edittext.setText(spinner.selectedItem.toString())
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                listener?.invoke(null)
                edittext.setText("")
            }
        }

    }

    fun setItems(items: List<String>) {
        val adapter = ArrayAdapter(
            context,
            R.layout.spinner_item_plain,
            R.id.item_name,
            items
        )
        spinner.adapter = adapter
    }

    fun setSelection(position: Int) {
        spinner.setSelection(position)
    }

    fun setHint(hint: String) {
        holder.hint = hint
        spinner.prompt = hint
    }

    fun setOnItemSelectedListener(listener: (Int?) -> Unit) {
        this.listener = listener
    }

}