package com.kylecorry.trail_sense.shared.text

import android.content.Context
import android.content.res.ColorStateList
import android.text.Layout
import android.text.style.AlignmentSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RawRes
import androidx.core.text.method.LinkMovementMethodCompat
import androidx.core.view.setPadding
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.ExpansionLayout
import com.kylecorry.andromeda.core.ui.setCompoundDrawables
import com.kylecorry.andromeda.markdown.MarkdownExtension
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.views.Views

object TextUtils {

    fun getSections(markdown: String): List<TextSection> {
        val sections = mutableListOf<TextSection>()
        val lines = markdown.split("\n")
        var currentContent = ""
        var currentTitle: String? = null
        var currentLevel: Int? = null
        for (line in lines) {
            if (line.startsWith("#")) {
                if (currentTitle != null || currentContent.isNotBlank()) {
                    sections.add(TextSection(currentTitle, currentLevel, currentContent.trim()))
                }
                currentLevel = line.count { it == '#' }
                currentTitle = line.substringAfter("#".repeat(currentLevel)).trim()
                currentContent = ""
            } else {
                currentContent += line + "\n"
            }
        }

        if (currentTitle != null || currentContent.isNotBlank()) {
            sections.add(TextSection(currentTitle, currentLevel, currentContent.trim()))
        }

        return sections
    }

    /**
     * Groups sections by their level. Nested sections are grouped together.
     */
    fun groupSections(sections: List<TextSection>, splitLevel: Int?): List<List<TextSection>> {
        val grouped = mutableListOf<MutableList<TextSection>>()
        var currentGroup = mutableListOf<TextSection>()
        for (section in sections) {
            val first = currentGroup.firstOrNull()
            val isLowerLevel = if (splitLevel != null) {
                section.level == null || splitLevel < section.level
            } else if (first?.level == null) {
                false
            } else {
                section.level == null || first.level < section.level
            }


            if (currentGroup.isEmpty() || isLowerLevel) {
                currentGroup.add(section)
            } else {
                grouped.add(currentGroup)
                currentGroup = mutableListOf(section)
            }
        }

        if (currentGroup.isNotEmpty()) {
            grouped.add(currentGroup)
        }

        return grouped
    }

    fun loadTextFromResources(context: Context, @RawRes resource: Int): String {
        return context.resources.openRawResource(resource).use {
            it.bufferedReader().readText()
        }
    }

    fun getMarkdownView(
        context: Context,
        text: String,
        shouldUppercaseSubheadings: Boolean = false
    ): View {
        val markdown = MarkdownService(context, extensions = listOf(
            MarkdownExtension(1, '+') { AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER) }
        ))
        val sections = groupSections(getSections(text), null)
        val children = sections.mapNotNull { section ->
            val first = section.firstOrNull() ?: return@mapNotNull null
            if (first.level != null && first.title != null) {
                // Create an expandable section
                val expandable = expandable(
                    context, first.title
                ) {
                    markdown.setMarkdown(it,
                        first.content + "\n" + section.drop(1)
                            .joinToString("\n") { it.toMarkdown(shouldUppercaseSubheadings) })
                }
                expandable
            } else {
                // Only text nodes
                val t = Views.text(context, null).also {
                    (it as TextView).movementMethod = LinkMovementMethodCompat.getInstance()
                }
                markdown.setMarkdown(t as TextView, section.joinToString("\n") { it.toMarkdown() })
                t
            }
        }

        return Views.linear(children, padding = Resources.dp(context, 16f).toInt())
    }

    private fun expandable(
        context: Context,
        title: String,
        setContent: (TextView) -> Unit
    ): ExpansionLayout {
        val expandable = ExpansionLayout(context, null)

        val titleView = Views.text(context, title) as TextView
        titleView.setCompoundDrawables(right = R.drawable.ic_drop_down)
        CustomUiUtils.setImageColor(titleView, Resources.androidTextColorSecondary(context))
        titleView.compoundDrawablePadding = Resources.dp(context, 8f).toInt()
        val padding = Resources.dp(context, 16f).toInt()
        val margin = Resources.dp(context, 8f).toInt()
        titleView.setPadding(padding)
        titleView.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).also {
            it.setMargins(0, margin, 0, margin)
            it.gravity = Gravity.CENTER_VERTICAL
        }
        titleView.setBackgroundResource(R.drawable.rounded_rectangle)
        titleView.backgroundTintList = ColorStateList.valueOf(
            Resources.getAndroidColorAttr(
                context,
                android.R.attr.colorBackgroundFloating
            )
        )

        expandable.addView(titleView)

        expandable.addView(
            Views.text(context, null).also {
                (it as TextView).movementMethod = LinkMovementMethodCompat.getInstance()
                it.setPadding(margin)
                setContent(it)
            }
        )

        expandable.setOnExpandStateChangedListener { isExpanded ->
            titleView.setCompoundDrawables(right = if (isExpanded) R.drawable.ic_drop_down_expanded else R.drawable.ic_drop_down)
            CustomUiUtils.setImageColor(titleView, Resources.androidTextColorSecondary(context))
        }

        return expandable
    }

    data class TextSection(val title: String?, val level: Int?, val content: String) {
        fun toMarkdown(shouldUppercaseTitle: Boolean = false): String {
            if (title == null || level == null) {
                return content
            }

            return "#".repeat(level) + " ${if (shouldUppercaseTitle) title.uppercase() else title}\n$content"
        }
    }
}