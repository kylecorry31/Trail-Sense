package com.kylecorry.trail_sense.tools.survival_guide.ui

import android.graphics.Color
import android.view.View
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.andromeda.core.ui.setCompoundDrawables
import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.andromeda.views.badge.Badge
import com.kylecorry.andromeda.views.list.ResourceListIcon
import com.kylecorry.andromeda.views.toolbar.Toolbar
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveBottomSheetFragment
import com.kylecorry.trail_sense.shared.extensions.useNavController
import com.kylecorry.trail_sense.shared.extensions.useSearch
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.views.SearchView
import com.kylecorry.trail_sense.tools.survival_guide.infrastructure.SurvivalGuideSearch

class SurvivalGuideBottomSheetFragment :
    TrailSenseReactiveBottomSheetFragment(R.layout.fragment_survival_guide_bottom_sheet) {

    override fun update() {
        // Views
        val searchView = useView<SearchView>(R.id.search)
        val summaryView = useView<TextView>(R.id.summary)
        val summaryHolderView = useView<View>(R.id.summary_holder)
        val summaryTitleView = useView<TextView>(R.id.summary_title)
        val summaryScrollView = useView<NestedScrollView>(R.id.summary_scroll)
        val summaryChapterBadgeView = useView<Badge>(R.id.summary_chapter_title)
        val emptyView = useView<View>(R.id.empty_view_description)
        val titleView = useView<Toolbar>(R.id.title)
        val navController = useNavController()

        // State
        val (query, setQuery) = useState("")
        val (searchResults, summary) = useSearchSurvivalGuide(query)

        // Services
        val context = useAndroidContext()
        val markdown = useService<MarkdownService>()

        // Navigation
        useEffect(titleView, query, navController) {
            titleView.rightButton.setOnClickListener {
                dismiss()
                navController.navigateWithAnimation(
                    R.id.fragmentToolSurvivalGuideList,
                    bundleOf("search_query" to query)
                )
            }
        }

        useSearch(searchView, setQuery)

        useEffect(
            summaryHolderView,
            summaryTitleView,
            summaryView,
            summaryChapterBadgeView,
            emptyView,
            summary,
            markdown,
            searchResults
        ) {
            summaryHolderView.isVisible = summary.isNotBlank()
            emptyView.isVisible = summary.isBlank()
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
                    dismiss()
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
