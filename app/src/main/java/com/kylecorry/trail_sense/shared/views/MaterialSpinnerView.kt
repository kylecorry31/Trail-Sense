package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.trail_sense.R

class MaterialSpinnerView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    private val edittext: TextInputEditText
    private val holder: TextInputLayout
    private var listener: ((Int?) -> Unit)? = null
    private var items: List<String> = listOf()

    var selectedItemPosition: Int = 0
        private set

    val selectedItem: String?
        get() = items.getOrNull(selectedItemPosition)

    init {
        inflate(context, R.layout.view_material_spinner, this)
        edittext = findViewById(R.id.material_spinner_edittext)
        holder = findViewById(R.id.material_spinner_holder)

        edittext.setOnClickListener {
            Pickers.item(context, holder.hint ?: "", items, selectedItemPosition) {
                it ?: return@item
                setSelection(it)
            }
        }
    }

    fun setItems(items: List<String>) {
        this.items = items
    }

    fun setSelection(position: Int) {
        selectedItemPosition = position
        listener?.invoke(position)
        edittext.setText(items.getOrNull(position) ?: "")
    }

    fun setHint(hint: String) {
        holder.hint = hint
    }

    fun setOnItemSelectedListener(listener: (Int?) -> Unit) {
        this.listener = listener
    }

}