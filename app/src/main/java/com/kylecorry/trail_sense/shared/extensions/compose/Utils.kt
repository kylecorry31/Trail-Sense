package com.kylecorry.trail_sense.shared.extensions.compose

import android.util.Patterns
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink

fun annotateWithLinks(text: String, textLinkStyle: TextLinkStyles = TextLinkStyles()): AnnotatedString {
    return buildAnnotatedString {
        var lastIndex = 0
        val matcher = Patterns.WEB_URL.matcher(text)
        while (matcher.find()) {
            append(text.substring(lastIndex, matcher.start()))
            val url = matcher.group().orEmpty()
            withLink(
                LinkAnnotation.Url(
                    url = url,
                    styles = textLinkStyle
                )
            ) {
                append(url)
            }
            lastIndex = matcher.end()
        }
        append(text.substring(lastIndex))
    }
}
