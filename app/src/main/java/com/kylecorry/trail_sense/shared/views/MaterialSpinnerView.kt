package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.scale
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.trail_sense.R

class MaterialSpinnerView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    private val edittext: TextInputEditText
    private val holder: TextInputLayout
    private var listener: ((Int?) -> Unit)? = null
    private var items: List<String> = listOf()
    private var descriptions: List<String> = listOf()

    var selectedItemPosition: Int = 0
        private set

    val selectedItem: String?
        get() = items.getOrNull(selectedItemPosition)

    init {
        inflate(context, R.layout.view_material_spinner, this)
        edittext = findViewById(R.id.material_spinner_edittext)
        holder = findViewById(R.id.material_spinner_holder)

        edittext.setOnClickListener {
            val pickerItems = items.mapIndexed { index, item ->
                buildSpannedString {
                    val hasDescription = descriptions.getOrNull(index)?.isNotBlank() == true
                    if (hasDescription) {
                        bold {
                            append(item)
                        }
                    } else {
                        append(item)
                    }
                    if (hasDescription) {
                        append("\n")
                        scale(0.2f) {
                            append("\n")
                        }
                        scale(0.65f) {
                            append(descriptions[index])
                        }
                        scale(0.2f) {
                            append("\n")
                        }
                    }
                }
            }

            Pickers.item(context, holder.hint ?: "", pickerItems, selectedItemPosition) {
                it ?: return@item
                setSelection(it)
            }
        }
    }

    fun setItems(items: List<String>) {
        this.items = items
    }

    fun setDescriptions(descriptions: List<String>) {
        this.descriptions = descriptions
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