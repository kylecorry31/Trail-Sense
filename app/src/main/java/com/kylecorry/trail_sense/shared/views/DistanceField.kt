package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.units.Distance
import com.kylecorry.andromeda.core.units.DistanceUnits
import com.kylecorry.andromeda.forms.FormField
import com.kylecorry.andromeda.forms.Forms

class DistanceField(
    context: Context,
    id: String,
    units: List<DistanceUnits>,
    defaultValue: Distance? = null,
    defaultUnit: DistanceUnits? = null,
    label: CharSequence? = null,
    hint: CharSequence? = null,
    var onChanged: (value: Distance?) -> Unit = {}
) : FormField<Distance?>(id, LinearLayout(context)) {

    private var labelView: TextView = TextView(context)
    private var unitInputView = DistanceInputView(context)
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

    override var value: Distance?
        get() = unitInputView.distance
        set(value) {
            unitInputView.updateDistance(value)
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
                unitInputView.setUnit(defaultUnit)
            } else {
                unitInputView.setUnit(units.firstOrNull())
            }
        }

        unitInputView.setOnDistanceChangeListener {
            onChanged.invoke(value)
        }
    }
}

fun Forms.Section.distance(
    id: String,
    units: List<DistanceUnits>,
    defaultValue: Distance? = null,
    defaultUnit: DistanceUnits? = null,
    label: CharSequence? = null,
    hint: CharSequence? = null,
    onChange: (section: Forms.Section, value: Distance?) -> Unit = { _, _ -> }
) {
    add(DistanceField(view.context, id, units, defaultValue, defaultUnit, label, hint) {
        onChange(this, it)
    })

}