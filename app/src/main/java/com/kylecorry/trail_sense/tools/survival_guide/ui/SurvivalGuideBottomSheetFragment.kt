package com.kylecorry.trail_sense.tools.survival_guide.ui

import android.graphics.Color
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.andromeda.core.ui.setCompoundDrawables
import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.andromeda.views.badge.Badge
import com.kylecorry.andromeda.views.list.AndromedaListView
import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.andromeda.views.list.ListItemTag
import com.kylecorry.andromeda.views.list.ResourceListIcon
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveBottomSheetFragment
import com.kylecorry.trail_sense.shared.views.SearchView
import com.kylecorry.trail_sense.tools.survival_guide.infrastructure.SurvivalGuideSearch

class SurvivalGuideBottomSheetFragment :
    TrailSenseReactiveBottomSheetFragment(R.layout.fragment_survival_guide_section) {

    override fun update() {
        // Views
        val listView = useView<AndromedaListView>(R.id.list)
        val emptyTextView = useView<TextView>(R.id.empty_text)
        val searchView = useView<SearchView>(R.id.search)
        val summaryView = useView<TextView>(R.id.summary)
        val summaryHolderView = useView<View>(R.id.summary_holder)
        val summaryTitleView = useView<TextView>(R.id.summary_title)
        val summaryScrollView = useView<NestedScrollView>(R.id.summary_scroll)
        val summaryChapterBadgeView = useView<Badge>(R.id.summary_chapter_title)
        val navController = useNavController()

        // State
        val (query, setQuery) = useState("")
        val chapters = useSurvivalGuideChapters()
        val (searchResults, summary) = useSearchSurvivalGuide(query)

        // Services
        val context = useAndroidContext()
        val markdown = useService<MarkdownService>()

        useSearch(searchView, setQuery)

        listView.emptyView = emptyTextView

        val listItems = useMemo(chapters, query, searchResults, markdown) {
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
                        // Do nothing
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
                        // Do nothing
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
//                    navController.navigateWithAnimation(
//                        R.id.fragmentToolSurvivalGuideReader,
//                        bundleOf(
//                            "chapter_resource_id" to result.chapter.resource,
//                            "header_index" to result.headingIndex
//                        )
//                    )
                }
            }
        }

        useEffect(summaryScrollView, summary) {
            summaryScrollView.scrollTo(0, 0)
        }
    }
}