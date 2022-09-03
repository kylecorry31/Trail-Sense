package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.core.view.setPadding
import com.google.android.flexbox.FlexboxLayout
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.beacons.domain.BeaconIcon
import com.kylecorry.trail_sense.shared.CustomUiUtils

class BeaconIconPickerView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    private val flex: FlexboxLayout
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
        for (icon in BeaconIcon.values()) {
            addButton(icon)
        }

    }

    private fun selectButton(icon: BeaconIcon?) {
        val view = if (icon == null) {
            flex.getChildAt(0)
        } else {
            flex.getChildAt(icon.ordinal + 1)
        } as ImageButton
        CustomUiUtils.setButtonState(view, true)
    }

    private fun deselectButton(icon: BeaconIcon?) {
        val view = if (icon == null) {
            flex.getChildAt(0)
        } else {
            flex.getChildAt(icon.ordinal + 1)
        } as ImageButton
        CustomUiUtils.setButtonState(view, false)
    }

    private fun addButton(icon: BeaconIcon?) {
        val button = ImageButton(context, null)
        button.setImageResource(icon?.icon ?: R.drawable.bubble)
        CustomUiUtils.setButtonState(button, false)
        button.setPadding(Resources.dp(context, 16f).toInt())
        button.setOnClickListener {
            this.icon = icon
            changeListener?.invoke(icon)
        }
        flex.addView(button)
    }

    fun setOnIconChangeListener(listener: ((icon: BeaconIcon?) -> Unit)?) {
        changeListener = listener
    }
}