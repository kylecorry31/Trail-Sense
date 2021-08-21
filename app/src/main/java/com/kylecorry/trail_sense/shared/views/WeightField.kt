package com.kylecorry.trail_sense.shared.views

import android.content.Context
import com.kylecorry.andromeda.core.units.Weight
import com.kylecorry.andromeda.core.units.WeightUnits
import com.kylecorry.andromeda.forms.Forms

class WeightField(
    context: Context,
    id: String,
    units: List<WeightUnits>,
    defaultValue: Weight? = null,
    defaultUnit: WeightUnits? = null,
    label: CharSequence? = null,
    hint: CharSequence? = null,
    onChanged: (value: Weight?) -> Unit = {}
) : BaseUnitField<Weight, WeightUnits>(
    context,
    id,
    units,
    defaultValue,
    defaultUnit,
    label,
    hint,
    onChanged
) {
    override fun getInputView(context: Context): BaseUnitInputView<Weight, WeightUnits> {
        return WeightInputView(context)
    }

}

fun Forms.Section.weight(
    id: String,
    units: List<WeightUnits>,
    defaultValue: Weight? = null,
    defaultUnit: WeightUnits? = null,
    label: CharSequence? = null,
    hint: CharSequence? = null,
    onChange: (section: Forms.Section, value: Weight?) -> Unit = { _, _ -> }
) {
    add(WeightField(view.context, id, units, defaultValue, defaultUnit, label, hint) {
        onChange(this, it)
    })

}