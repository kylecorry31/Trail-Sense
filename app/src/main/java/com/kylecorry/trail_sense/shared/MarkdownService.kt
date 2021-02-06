package com.kylecorry.trail_sense.shared

import android.content.Context
import android.text.Spanned
import android.widget.TextView
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonSpansFactory
import io.noties.markwon.core.spans.LastLineSpacingSpan
import org.commonmark.node.ListItem

class MarkdownService(private val context: Context) {
    private val markwon by lazy {
        Markwon.builder(context).usePlugin(object : AbstractMarkwonPlugin() {
            override fun configureSpansFactory(builder: MarkwonSpansFactory.Builder) {
                super.configureSpansFactory(builder)
                builder.appendFactory(
                    ListItem::class.java
                ) { _, _ -> LastLineSpacingSpan(16) }
            }
        }).build()
    }

    fun setMarkdown(view: TextView, markdown: String){
        markwon.setMarkdown(view, markdown)
    }

    fun toMarkdown(markdown: String): Spanned {
        return markwon.toMarkdown(markdown)
    }

}