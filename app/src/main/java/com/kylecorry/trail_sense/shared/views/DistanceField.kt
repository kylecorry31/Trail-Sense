package com.kylecorry.trail_sense.shared.views

import android.content.Context
import com.kylecorry.andromeda.forms.Forms
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits

class DistanceField(
    context: Context,
    id: String,
    units: List<DistanceUnits>,
    defaultValue: Distance? = null,
    defaultUnit: DistanceUnits? = null,
    label: CharSequence? = null,
    hint: CharSequence? = null,
    onChanged: (value: Distance?) -> Unit = {}
) : BaseMultipartUnitField<Distance, DistanceUnits>(
    context,
    id,
    units,
    defaultValue,
    defaultUnit,
    label,
    hint,
    onChanged
) {
    override fun getInputView(context: Context): BaseMultipartUnitInputView<Distance, DistanceUnits> {
        return DistanceInputView(context)
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