package com.kylecorry.trail_sense.tools.survival_guide.ui

import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.text.buildSpannedString
import androidx.core.text.scale
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.andromeda.fragments.useBackgroundEffect
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.andromeda.views.list.AndromedaListView
import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.andromeda.views.list.ResourceListIcon
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.extensions.useCoroutineQueue
import com.kylecorry.trail_sense.shared.extensions.useNavController
import com.kylecorry.trail_sense.shared.extensions.useSearch
import com.kylecorry.trail_sense.shared.extensions.useShowDisclaimer
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.views.SearchView
import com.kylecorry.trail_sense.tools.survival_guide.domain.Chapter
import com.kylecorry.trail_sense.tools.survival_guide.domain.Chapters
import com.kylecorry.trail_sense.tools.survival_guide.infrastructure.SurvivalGuideFuzzySearch
import com.kylecorry.trail_sense.tools.survival_guide.infrastructure.SurvivalGuideSearchResult
import kotlinx.coroutines.delay

class FragmentToolSurvivalGuideList :
    TrailSenseReactiveFragment(R.layout.fragment_survival_guide_chapters) {

    override fun update() {
        // Views
        val listView = useView<AndromedaListView>(R.id.list)
        val emptyTextView = useView<TextView>(R.id.empty_text)
        val searchView = useView<SearchView>(R.id.search)
        val navController = useNavController()

        // State
        val (query, setQuery) = useState("")
        val chapters = useChapters()
        val searchResults = useSearchResults(query)

        // Services
        val markdown = useService<MarkdownService>()

        useShowDisclaimer(
            getString(R.string.survival_guide),
            getString(R.string.survival_guide_disclaimer),
            "pref_survival_guide_disclaimer_shown",
            cancelText = null
        )

        useSearch(searchView, setQuery)

        listView.emptyView = emptyTextView

        val listItems = useMemo(navController, chapters, query, searchResults, markdown) {
            if (query.isBlank()) {
                chapters.map {
                    ListItem(
                        it.resource.toLong(),
                        it.title,
                        icon = ResourceListIcon(
                            it.icon,
                            Resources.androidTextColorSecondary(requireContext())
                        )
                    ) {
                        navController.navigateWithAnimation(
                            R.id.fragmentToolSurvivalGuideReader,
                            bundleOf("chapter_resource_id" to it.resource)
                        )
                    }
                }
            } else {
                searchResults.map {
                    ListItem(
                        it.chapter.resource.toLong(),
                        it.chapter.title + " > " + it.heading,
                        if (it.snippet.isBlank()) {
                            null
                        } else {
                            buildSpannedString { scale(0.8f) { append(markdown.toMarkdown(it.snippet)) } }
                        },
                        icon = ResourceListIcon(
                            it.chapter.icon,
                            Resources.androidTextColorSecondary(requireContext())
                        ),
                        subtitleMaxLines = 4
                    ) {
                        // TODO: Scroll to the heading
                        navController.navigateWithAnimation(
                            R.id.fragmentToolSurvivalGuideReader,
                            bundleOf("chapter_resource_id" to it.chapter.resource)
                        )
                    }
                }
            }
        }

        useEffect(listView, listItems) {
            listView.setItems(listItems)
        }
    }

    private fun useChapters(): List<Chapter> {
        val context = useAndroidContext()
        return useMemo(context) {
            Chapters.getChapters(context)
        }
    }

    private fun useSearchResults(query: String): List<SurvivalGuideSearchResult> {
        val (results, setResults) = useState(emptyList<SurvivalGuideSearchResult>())
        val context = useAndroidContext()
        val queue = useCoroutineQueue()

        useBackgroundEffect(query, context, cancelWhenRerun = true) {
            // Debounce
            delay(200)
            queue.replace {
                if (query.isBlank()) {
                    setResults(emptyList())
                } else {
                    val search = SurvivalGuideFuzzySearch(context)
                    setResults(search.search(query))
                }
            }
        }

        return results
    }
}