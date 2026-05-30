package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.view.setPadding
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.button.MaterialButton
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconIcon

class BeaconIconPickerView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    private val flex: FlexboxLayout
    private val selectableIcons = BeaconIcon.values().filter { it.isUserSelectable }
    private val buttons = mutableMapOf<BeaconIcon?, MaterialButton>()
    private var changeListener: ((icon: BeaconIcon?) -> Unit)? = null

    var icon: BeaconIcon? = null
        set(value) {
            deselectButton(field)
            selectButton(value)
            field = value
        }


    init {
        inflate(context, R.layout.view_color_picker, this)
        flex = findViewById(R.id.color_picker_flex)

        addButton(null)
        for (icon in selectableIcons) {
            addButton(icon)
        }

    }

    private fun selectButton(icon: BeaconIcon?) {
        buttons[icon]?.let { it.isChecked = true }
    }

    private fun deselectButton(icon: BeaconIcon?) {
        buttons[icon]?.let { it.isChecked = false }
    }

    private fun addButton(icon: BeaconIcon?) {
        val button = MaterialButton(context, null, com.google.android.material.R.attr.materialIconButtonOutlinedStyle)
        button.setIconResource(icon?.icon ?: R.drawable.bubble)
        button.isCheckable = true
        button.isToggleCheckedStateOnClick = false
        button.setPadding(Resources.dp(context, 16f).toInt())
        button.setOnClickListener {
            this.icon = icon
            changeListener?.invoke(icon)
        }
        buttons[icon] = button
        flex.addView(button)
    }

    fun setOnIconChangeListener(listener: ((icon: BeaconIcon?) -> Unit)?) {
        changeListener = listener
    }
}
