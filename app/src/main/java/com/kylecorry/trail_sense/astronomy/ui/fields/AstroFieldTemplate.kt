package com.kylecorry.trail_sense.astronomy.ui.fields

import android.content.Context
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.trail_sense.databinding.ListItemAstronomyDetailBinding

abstract class AstroFieldTemplate : AstroField {
    override fun display(binding: ListItemAstronomyDetailBinding) {
        val context = binding.root.context
        binding.astronomyDetailIcon.isVisible = true
        Colors.setImageColor(binding.astronomyDetailIcon, getImageTint(context))
        binding.astronomyDetailIcon.setImageResource(getImage(context))

        binding.astronomyDetailName.isVisible = true
        binding.astronomyDetailName.text = getTitle(context)

        binding.astronomyDetailValue.isVisible = true
        binding.astronomyDetailValue.text = getValue(context)

        binding.root.setOnClickListener {
            onClick(context)
        }
    }


    @ColorInt
    open fun getImageTint(context: Context): Int? {
        return null
    }

    abstract fun getTitle(context: Context): String
    abstract fun getValue(context: Context): String

    @DrawableRes
    abstract fun getImage(context: Context): Int

    open fun onClick(context: Context) {}


}