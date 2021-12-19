package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.forms.FormField
import com.kylecorry.andromeda.forms.Forms

@Suppress("LeakingThis")
abstract class BaseMultipartUnitField<T, Units : Enum<*>>(
    context: Context,
    id: String,
    units: List<Units>,
    defaultValue: T? = null,
    defaultUnit: Units? = null,
    label: CharSequence? = null,
    hint: CharSequence? = null,
    var onChanged: (value: T?) -> Unit = {}
) : FormField<T?>(id, LinearLayout(context)) {

    protected abstract fun getInputView(context: Context): BaseMultipartUnitInputView<T, Units>

    private var labelView: TextView = TextView(context)
    private var unitInputView = getInputView(context)
    private var linearLayout = view as LinearLayout

    var label: CharSequence?
        get() = labelView.text
        set(value) {
            labelView.text = value
            labelView.isVisible = value != null
        }

    var isEnabled: Boolean
        get() = unitInputView.isEnabled
        set(value) {
            unitInputView.isEnabled = value
        }

    override var value: T?
        get() = unitInputView.value
        set(value) {
            unitInputView.value = value
        }

    init {
        Forms.setDefaultLinearLayoutStyle(linearLayout)
        Forms.setDefaultFieldPadding(linearLayout)
        val labelSpacing = Resources.dp(context, 8f).toInt()
        labelView.setPadding(0, 0, 0, labelSpacing)

        linearLayout.addView(labelView)
        linearLayout.addView(unitInputView)

        this.label = label
        unitInputView.units = units
        unitInputView.hint = hint
        value = defaultValue
        if (defaultValue == null) {
            if (defaultUnit != null) {
                unitInputView.unit = defaultUnit
            } else {
                unitInputView.unit = units.firstOrNull()
            }
        }

        unitInputView.setOnValueChangeListener {
            onChanged.invoke(value)
        }
    }
}