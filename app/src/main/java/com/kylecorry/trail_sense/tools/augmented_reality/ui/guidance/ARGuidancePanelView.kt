package com.kylecorry.trail_sense.tools.augmented_reality.ui.guidance

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.trail_sense.databinding.ViewArGuidancePanelBinding

class ARGuidancePanelView(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {

    private val binding = ViewArGuidancePanelBinding.inflate(LayoutInflater.from(context), this, true)

    var onCancel: (() -> Unit)? = null

    init {
        isVisible = false
        binding.arGuideCancel.setOnClickListener {
            onCancel?.invoke()
        }
    }

    fun setState(state: ARGuidanceDisplayState?) {
        isVisible = state != null
        if (state == null) {
            return
        }

        binding.arGuideName.text = state.name
        if (state.iconBitmap != null) {
            binding.arGuideIcon.setImageBitmap(state.iconBitmap)
        } else {
            binding.arGuideIcon.setImageResource(state.icon)
        }
        binding.arGuideIcon.rotation = state.iconRotation
        binding.arGuideIcon.backgroundTintList =
            ColorStateList.valueOf(state.iconBackgroundTint ?: Color.TRANSPARENT)
        Colors.setImageColor(binding.arGuideIcon, state.iconTint)
    }
}
