package com.kylecorry.trail_sense.astronomy.ui.fields

import androidx.core.view.isInvisible
import com.kylecorry.trail_sense.databinding.ListItemAstronomyDetailBinding

class SpacerAstroField : AstroField {
    override fun display(binding: ListItemAstronomyDetailBinding) {
        binding.astronomyDetailIcon.isInvisible = true
        binding.astronomyDetailName.isInvisible = true
        binding.astronomyDetailValue.isInvisible = true
    }
}