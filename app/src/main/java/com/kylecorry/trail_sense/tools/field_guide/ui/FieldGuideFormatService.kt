package com.kylecorry.trail_sense.tools.field_guide.ui

import android.graphics.Typeface
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePage
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideNameDisplay

class FieldGuideFormatService(private val preferences: UserPreferences) {

    fun formatTitle(page: FieldGuidePage): CharSequence {
        return if (preferences.fieldGuide.nameDisplay == FieldGuideNameDisplay.Scientific) {
            page.scientificName?.takeIf { it.isNotBlank() }?.let(::formatScientificName) ?: page.name
        } else {
            page.name
        }
    }

    fun formatSubtitle(page: FieldGuidePage): CharSequence? {
        val scientificName = page.scientificName?.takeIf { it.isNotBlank() } ?: return null
        return if (preferences.fieldGuide.nameDisplay == FieldGuideNameDisplay.Scientific) {
            page.name
        } else {
            formatScientificName(scientificName)
        }
    }

    fun formatName(page: FieldGuidePage): CharSequence {
        val scientificName = page.scientificName?.takeIf { it.isNotBlank() } ?: return page.name
        return when (preferences.fieldGuide.nameDisplay) {
            FieldGuideNameDisplay.Scientific -> formatScientificName(scientificName)
            FieldGuideNameDisplay.Both -> SpannableStringBuilder(page.name)
                .append(" (")
                .append(formatScientificName(scientificName))
                .append(")")
            else -> page.name
        }
    }

    fun formatScientificName(scientificName: String): SpannableString {
        val suffixStart = scientificName.indexOfFirst { it.isWhitespace() }
        val italicEnd = if (
            suffixStart >= 0 && scientificName.substring(suffixStart).trim() in setOf("sp.", "spp.")
        ) {
            suffixStart
        } else {
            scientificName.length
        }

        return SpannableString(scientificName).apply {
            setSpan(StyleSpan(Typeface.ITALIC), 0, italicEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

}
