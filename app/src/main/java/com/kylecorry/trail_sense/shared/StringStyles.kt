package com.kylecorry.trail_sense.shared

import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.CharacterStyle
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan

enum class Style {
    Heading,
    Bold,
    Underline,
    Italic,
    BoldItalic,
    Normal
}

object StringStyles {

    fun style(text: List<Pair<String, Style>>): SpannableString {
        val allTxt = text.joinToString("") { it.first }

        val str = SpannableString(allTxt)

        var idx = 0
        for (s in text) {
            val start = idx
            val end = idx + s.first.length

            val style: List<CharacterStyle> = when (s.second) {
                Style.Bold -> listOf(StyleSpan(Typeface.BOLD))
                Style.BoldItalic -> listOf(StyleSpan(Typeface.BOLD_ITALIC))
                Style.Italic -> listOf(StyleSpan(Typeface.ITALIC))
                Style.Underline -> listOf(UnderlineSpan())
                Style.Heading -> listOf(StyleSpan(Typeface.BOLD), RelativeSizeSpan(1.15f))
                else -> emptyList()
            }

            for (characterStyle in style) {
                str.setSpan(
                    characterStyle,
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            idx += s.first.length
        }

        return str
    }

    fun markdown(text: String): SpannableString {
        val lines = text.lines()

        val styles = mutableListOf<Pair<String, Style>>()

        for (line in lines) {
            if (line.startsWith("# ")) {
                styles.add((line.substring(2) + "\n") to Style.Heading)
            } else {
                styles.add((line + "\n") to Style.Normal)
            }
        }

        return style(styles)
    }


}