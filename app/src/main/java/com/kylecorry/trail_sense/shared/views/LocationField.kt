package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.units.Coordinate
import com.kylecorry.andromeda.forms.FormField
import com.kylecorry.andromeda.forms.Forms

class LocationField(
    context: Context,
    id: String,
    defaultValue: Coordinate? = null,
    label: CharSequence? = null,
    var onChanged: (value: Coordinate?) -> Unit = {}
) :
    FormField<Coordinate?>(id, LinearLayout(context)) {

    private var labelView: TextView = TextView(context)
    private var coordinateInput = CoordinateInputView(context)
    private var linearLayout = view as LinearLayout

    var label: CharSequence?
        get() = labelView.text
        set(value) {
            labelView.text = value
            labelView.isVisible = value != null
        }


    var isEnabled: Boolean
        get() = coordinateInput.isEnabled
        set(value) {
            coordinateInput.isEnabled = value
        }

    override var value: Coordinate?
        get() = coordinateInput.coordinate
        set(value) {
            coordinateInput.coordinate = value
        }

    fun pause() {
        coordinateInput.pause()
    }

    init {
        Forms.setDefaultLinearLayoutStyle(linearLayout)
        Forms.setDefaultFieldPadding(linearLayout)
        val labelSpacing = Resources.dp(context, 8f).toInt()
        labelView.setPadding(0, 0, 0, labelSpacing)
//        coordinateInput.width = LinearLayout.LayoutParams.MATCH_PARENT

        linearLayout.addView(labelView)
        linearLayout.addView(coordinateInput)

        this.label = label
        value = defaultValue
        coordinateInput.setOnCoordinateChangeListener {
            onChanged.invoke(value)
        }
    }
}

fun Forms.Section.location(
    id: String,
    defaultValue: Coordinate? = null,
    label: CharSequence? = null,
    onChange: (section: Forms.Section, value: Coordinate?) -> Unit = { _, _ -> }
) {
    add(LocationField(view.context, id, defaultValue, label) {
        onChange.invoke(this, it)
    })

}