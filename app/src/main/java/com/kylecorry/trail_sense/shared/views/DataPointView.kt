package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.ViewDataPointBinding

class DataPointView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    private val binding: ViewDataPointBinding
    private var tint: Int

    var title: String
        get() = binding.dataPointTitle.text.toString()
        set(value) {
            binding.dataPointTitle.text = value
        }

    var description: String
        get() = binding.dataPointDesc.text.toString()
        set(value) {
            binding.dataPointDesc.text = value
        }

    init {
        inflate(context, R.layout.view_data_point, this)
        binding = ViewDataPointBinding.bind(findViewById(R.id.data_point))
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.DataPointView, 0, 0)
        tint = a.getColor(
            R.styleable.DataPointView_dataPointTint,
            Resources.androidTextColorPrimary(context)
        )
        val icon = a.getResourceId(R.styleable.DataPointView_dataPointIcon, -1)
        setImageResource(if (icon == -1) null else icon)
        title = a.getString(R.styleable.DataPointView_dataPointText) ?: ""
        description = a.getString(R.styleable.DataPointView_dataPointDescription) ?: ""
        binding.root.layoutParams =
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).also {
                it.gravity = a.getInt(R.styleable.DataPointView_android_gravity, Gravity.START)
            }
        a.recycle()
    }


    fun setImageResource(@DrawableRes res: Int?) {
        if (res != null) {
            binding.dataPointImage.setImageResource(res)
            Colors.setImageColor(
                binding.dataPointImage,
                if (tint == -1) Resources.androidTextColorPrimary(context) else tint
            )
        }
        binding.dataPointImage.isVisible = res != null
    }

    fun setShowDescription(showDescription: Boolean) {
        binding.dataPointDesc.isVisible = showDescription
    }

    fun setOnDescriptionClickListener(listener: (() -> Unit)?) {
        if (listener != null) {
            binding.dataPointDesc.setOnClickListener { listener() }
        } else {
            binding.dataPointDesc.setOnClickListener(null)
        }
    }

}