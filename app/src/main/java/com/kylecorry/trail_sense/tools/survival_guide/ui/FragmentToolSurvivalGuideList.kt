package com.kylecorry.trail_sense.tools.survival_guide.ui

import android.graphics.Color
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.andromeda.core.ui.setCompoundDrawables
import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.andromeda.fragments.useArgument
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.andromeda.views.badge.Badge
import com.kylecorry.andromeda.views.list.AndromedaListView
import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.andromeda.views.list.ListItemTag
import com.kylecorry.andromeda.views.list.ResourceListIcon
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.extensions.useBackPressedCallback
import com.kylecorry.trail_sense.shared.extensions.useNavController
import com.kylecorry.trail_sense.shared.extensions.useSearch
import com.kylecorry.trail_sense.shared.extensions.useShowDisclaimer
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.views.SearchView
import com.kylecorry.trail_sense.tools.survival_guide.infrastructure.SurvivalGuideSearch

class FragmentToolSurvivalGuideList :
    TrailSenseReactiveFragment(R.layout.fragment_survival_guide_chapters) {

    override fun update() {
        // Views
        val listView = useView<AndromedaListView>(R.id.list)
        val emptyTextView = useView<TextView>(R.id.empty_text)
        val searchView = useView<SearchView>(R.id.search)
        val summaryView = useView<TextView>(R.id.summary)
        val summaryHolderView = useView<View>(R.id.summary_holder)
        val summaryTitleView = useView<TextView>(R.id.summary_title)
        val summaryScrollView = useView<ScrollView>(R.id.summary_scroll)
        val summaryChapterBadgeView = useView<Badge>(R.id.summary_chapter_title)
        val navController = useNavController()

        // Arguments
        val searchArgumentQuery = useArgument<String>("search_query") ?: ""

        // State
        val (query, setQuery) = useState(searchArgumentQuery)
        val chapters = useSurvivalGuideChapters()
        val (searchResults, summary) = useSearchSurvivalGuide(query)

        // Services
        val context = useAndroidContext()
        val markdown = useService<MarkdownService>()

        useShowDisclaimer(
            getString(R.string.survival_guide),
            getString(R.string.survival_guide_disclaimer),
            "pref_survival_guide_disclaimer_shown",
            cancelText = null
        )

        useBackPressedCallback(query, searchView) {
            if (query.isNotBlank()) {
                setQuery("")
                searchView.query = ""
                true
            } else {
                false
            }
        }

        useSearch(searchView, setQuery)

        useEffect(searchView, searchArgumentQuery) {
            if (searchArgumentQuery.isNotEmpty()) {
                searchView.query = searchArgumentQuery
                searchView.setCursorPosition(searchArgumentQuery.length)
            }
        }

        listView.emptyView = emptyTextView

        val listItems = useMemo(navController, chapters, query, searchResults, markdown) {
            if (query.isBlank() || searchResults.isEmpty()) {
                chapters.map {
                    ListItem(
                        it.chapter.resource.toLong(),
                        it.chapter.title,
                        it.sections.firstOrNull()?.summary,
                        icon = ResourceListIcon(
                            it.chapter.icon,
                            Resources.androidTextColorSecondary(requireContext())
                        )
                    ) {
                        navController.navigateWithAnimation(
                            R.id.fragmentToolSurvivalGuideReader,
                            bundleOf("chapter_resource_id" to it.chapter.resource)
                        )
                    }
                }
            } else {
                val textColor = Resources.androidTextColorSecondary(requireContext())
                searchResults.map {
                    ListItem(
                        it.chapter.resource.toLong(),
                        it.heading ?: getString(R.string.overview),
                        it.summary,
                        icon = ResourceListIcon(it.chapter.icon, textColor),
                        tags = listOf(
                            ListItemTag(
                                it.chapter.title,
                                ResourceListIcon(it.chapter.icon, size = 12f),
                                textColor
                            )
                        )
                    ) {
                        // TODO: Scroll to the heading
                        navController.navigateWithAnimation(
                            R.id.fragmentToolSurvivalGuideReader,
                            bundleOf(
                                "chapter_resource_id" to it.chapter.resource,
                                "header_index" to it.headingIndex
                            )
                        )
                    }
                }
            }
        }

        useEffect(listView, listItems) {
            listView.setItems(listItems)
        }

        useEffect(
            summaryHolderView,
            summaryTitleView,
            summaryView,
            summaryChapterBadgeView,
            summary,
            markdown,
            searchResults
        ) {
            summaryHolderView.isVisible = summary.isNotBlank()
            markdown.setMarkdown(summaryView, summary)
            val result = searchResults.firstOrNull()
            if (result != null) {
                summaryTitleView.text = listOfNotNull(
                    result.heading,
                    if (SurvivalGuideSearch.shouldUseSubsection(result)) result.bestSubsection?.heading else null
                ).joinToString(" > ")
                summaryChapterBadgeView.setStatusText(result.chapter.title)
                ResourceListIcon(
                    result.chapter.icon,
                    size = 12f
                ).apply(summaryChapterBadgeView.statusImage)
                val backgroundColor = Resources.androidTextColorSecondary(context)
                summaryChapterBadgeView.setBackgroundTint(backgroundColor)
                summaryChapterBadgeView.setForegroundTint(
                    Colors.mostContrastingColor(
                        Color.WHITE,
                        Color.BLACK,
                        backgroundColor
                    )
                )
                summaryTitleView.setCompoundDrawables(
                    size = Resources.dp(context, 16f).toInt(),
                    right = R.drawable.ic_keyboard_arrow_right
                )
                summaryHolderView.setOnClickListener {
                    navController.navigateWithAnimation(
                        R.id.fragmentToolSurvivalGuideReader,
                        bundleOf(
                            "chapter_resource_id" to result.chapter.resource,
                            "header_index" to result.headingIndex
                        )
                    )
                }
            }
        }

        useEffect(summaryScrollView, summary) {
            summaryScrollView.scrollTo(0, 0)
        }
    }
}