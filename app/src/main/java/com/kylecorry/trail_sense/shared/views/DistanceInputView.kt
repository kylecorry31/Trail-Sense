package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.kylecorry.andromeda.core.units.Distance
import com.kylecorry.andromeda.core.units.DistanceUnits
import com.kylecorry.andromeda.forms.UnitInputView
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatServiceV2

class DistanceInputView(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {

    private val formatService by lazy { FormatServiceV2(context) }

    private var _units = DistanceUnits.values().toList()

    var units: List<DistanceUnits>
        get() = _units
        set(value) {
            _units = value
            unitInput.units = value.map {
                UnitInputView.DisplayUnit(
                    it,
                    formatService.getDistanceUnitName(it, true),
                    formatService.getDistanceUnitName(it)
                )
            }
            if (unitInput.unit == null) {
                unitInput.unit = value.firstOrNull()
            }
        }

    var hint: CharSequence?
        get() = unitInput.hint
        set(value) {
            unitInput.hint = value
        }

    var distance: Distance? = null
    private var changeListener: ((distance: Distance?) -> Unit)? = null

    private var unitInput: UnitInputView<DistanceUnits> = UnitInputView(context)

    init {
        val sets = intArrayOf(R.attr.hint)
        val typedArray = context.obtainStyledAttributes(attrs, sets)
        hint = typedArray.getText(0) ?: context.getString(R.string.distance_hint)
        units = DistanceUnits.values().toList()

        unitInput.onChange = { _, _ ->
            onChange()
        }

        typedArray.recycle()

        addView(unitInput)
    }

    private fun onChange() {
        val amount = unitInput.amount
        val unit = unitInput.unit

        if (amount == null || unit == null) {
            distance = null
            changeListener?.invoke(distance)
            return
        }

        distance = Distance(amount.toFloat(), unit)
        changeListener?.invoke(distance)
    }


    fun setOnDistanceChangeListener(listener: ((distance: Distance?) -> Unit)?) {
        changeListener = listener
    }

    fun updateDistance(distance: Distance?) {
        unitInput.amount = distance?.distance
        unitInput.unit = distance?.units
        this.distance = distance
    }

    fun setUnit(unit: DistanceUnits?) {
        unitInput.unit = unit
    }


}