package com.kylecorry.trail_sense.shared.text

import android.content.Context
import android.content.res.ColorStateList
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RawRes
import androidx.core.text.method.LinkMovementMethodCompat
import androidx.core.view.setPadding
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.ExpansionLayout
import com.kylecorry.andromeda.core.ui.setCompoundDrawables
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.text.nlp.processors.EnglishContractionSplitter
import com.kylecorry.trail_sense.shared.text.nlp.processors.EnglishStopWordRemover
import com.kylecorry.trail_sense.shared.text.nlp.processors.LowercaseProcessor
import com.kylecorry.trail_sense.shared.text.nlp.processors.PorterStemmer
import com.kylecorry.trail_sense.shared.text.nlp.processors.SequentialProcessor
import com.kylecorry.trail_sense.shared.text.nlp.tokenizers.PostProcessedTokenizer
import com.kylecorry.trail_sense.shared.text.nlp.tokenizers.SimpleWordTokenizer
import com.kylecorry.trail_sense.shared.views.TrailSenseTextView
import com.kylecorry.trail_sense.shared.views.Views

object TextUtils {

    fun <T> search(query: String, values: List<T>, propertySelector: (T) -> List<String>): List<T> {
        val modifiedQuery = query.trim().lowercase()
        return values.filter { value ->
            propertySelector(value).any { it.lowercase().contains(modifiedQuery) }
        }
    }

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
        val markdown = AppServiceRegistry.get<MarkdownService>()
        val sections = groupSections(getSections(text), null)
        val children = sections.mapNotNull { section ->
            val first = section.firstOrNull() ?: return@mapNotNull null
            if (first.level != null && first.title != null) {
                // Create an expandable section
                val expandable = expandable(
                    context, first.title
                ) {
                    markdown.setMarkdown(
                        it,
                        removeMarkdownComments(first.content) + "\n" + section.drop(1)
                            .joinToString("\n") { it.toMarkdown(shouldUppercaseSubheadings) })
                    it.movementMethod = LinkMovementMethodCompat.getInstance()
                }
                expandable
            } else {
                // Only text nodes
                val t = defaultTextView(context)
                markdown.setMarkdown(t, section.joinToString("\n") { it.toMarkdown() })
                t.movementMethod = LinkMovementMethodCompat.getInstance()
                t
            }
        }

        return Views.linear(children, padding = Resources.dp(context, 16f).toInt())
    }

    fun getMarkdownKeywords(text: String): Set<String> {
        val regex = Regex("<!-- K: (.*?)-->")
        return regex.findAll(text)
            .take(1)
            .flatMap { it.groupValues[1].split(",") }
            .map { it.trim() }
            .toSet()
    }

    fun getMarkdownSummary(text: String): String? {
        val regex = Regex("<!-- S: (.*?)-->")
        return regex.find(text)?.groupValues?.get(1)
    }

    fun getKeywords(
        text: String,
        preservedWords: Set<String> = emptySet(),
        additionalContractions: Map<String, List<String>> = emptyMap(),
        additionalStopWords: Set<String> = emptySet(),
        additionalStemWords: Map<String, String> = emptyMap()
    ): Set<String> {
        val tokenizer = PostProcessedTokenizer(
            SimpleWordTokenizer(preservedWords),
            SequentialProcessor(
                LowercaseProcessor(),
                EnglishContractionSplitter(additionalContractions),
                EnglishStopWordRemover(additionalStopWords),
                PorterStemmer(additionalStemWords + preservedWords.associateWith { it })
            )
        )
        return tokenizer.tokenize(text).toSet()
    }

    fun getQueryMatchPercent(
        query: String,
        text: String,
        preservedWords: Set<String> = emptySet(),
        synonyms: List<Set<String>> = emptyList(),
        additionalContractions: Map<String, List<String>> = emptyMap(),
        additionalStopWords: Set<String> = emptySet(),
        additionalStemWords: Map<String, String> = emptyMap()
    ): Float {
        val queryKeywords =
            getKeywords(
                query,
                preservedWords,
                additionalContractions,
                additionalStopWords,
                additionalStemWords
            )
        val textKeywords =
            getKeywords(
                text,
                preservedWords,
                additionalContractions,
                additionalStopWords,
                additionalStemWords
            ).toMutableSet()

        // Add the synonyms to the text keywords (but not query keywords, since that will skew the results)
        val toAdd = mutableSetOf<String>()
        for (keyword in textKeywords) {
            for (synonymSet in synonyms) {
                if (keyword in synonymSet) {
                    toAdd.addAll(synonymSet)
                }
            }
        }
        textKeywords.addAll(toAdd)

        val distanceMetric = LevenshteinDistance()
        val scores = mutableMapOf<String, Float>()

        for (qWord in queryKeywords) {
            if (qWord in textKeywords) {
                scores[qWord] = 1f
                continue
            }

            for (lWord in textKeywords) {
                val distance = distanceMetric.percentSimilarity(qWord, lWord)
                if (qWord !in scores) {
                    scores[qWord] = distance
                } else {
                    scores[qWord] = maxOf(scores[qWord] ?: 0f, distance)
                }
            }
        }

        var total = 0f
        for (word in queryKeywords) {
            if (word in scores) {
                total += scores[word] ?: 0f
            }
        }

        return total / queryKeywords.size
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
            defaultTextView(context).also {
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

    fun removeMarkdownComments(text: String): String {
        val commentRegex = Regex("<!--.*?-->")
        return text.replace(commentRegex, "").trim()
    }

    private fun defaultTextView(context: Context): TextView {
        return TrailSenseTextView(context).also {
            it.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            it.setTextIsSelectable(true)
        }
    }


    data class TextSection(val title: String?, val level: Int?, val content: String) {
        fun toMarkdown(
            shouldUppercaseTitle: Boolean = false,
            removeComments: Boolean = true
        ): String {
            val contentWithoutComments = if (removeComments) {
                removeMarkdownComments(content)
            } else {
                content
            }

            if (title == null || level == null) {
                return contentWithoutComments
            }

            return "#".repeat(level) + " ${if (shouldUppercaseTitle) title.uppercase() else title}\n$contentWithoutComments"
        }
    }
}