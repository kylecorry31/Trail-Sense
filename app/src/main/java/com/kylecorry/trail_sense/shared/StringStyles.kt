package com.kylecorry.trail_sense.shared

import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.CharacterStyle
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan

enum class Style {
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

            val style: CharacterStyle? = when (s.second) {
                Style.Bold -> StyleSpan(Typeface.BOLD)
                Style.BoldItalic -> StyleSpan(Typeface.BOLD_ITALIC)
                Style.Italic -> StyleSpan(Typeface.ITALIC)
                Style.Underline -> UnderlineSpan()
                else -> null
            }

            if (style != null) {
                str.setSpan(
                    style,
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
                styles.add((line.substring(2) + "\n") to Style.Bold)
            } else {
                styles.add((line + "\n") to Style.Normal)
            }
        }

        return style(styles)
    }


}