package com.kylecorry.trail_sense.tools.survival_guide.ui

import android.widget.TextView
import androidx.core.os.bundleOf
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.andromeda.fragments.useBackgroundEffect
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.andromeda.views.list.AndromedaListView
import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.andromeda.views.list.ListItemTag
import com.kylecorry.andromeda.views.list.ResourceListIcon
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.extensions.useBackPressedCallback
import com.kylecorry.trail_sense.shared.extensions.useCoroutineQueue
import com.kylecorry.trail_sense.shared.extensions.useNavController
import com.kylecorry.trail_sense.shared.extensions.useSearch
import com.kylecorry.trail_sense.shared.extensions.useShowDisclaimer
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.views.SearchView
import com.kylecorry.trail_sense.tools.survival_guide.infrastructure.EnglishSurvivalGuideFuzzySearch
import com.kylecorry.trail_sense.tools.survival_guide.infrastructure.GuideDetails
import com.kylecorry.trail_sense.tools.survival_guide.infrastructure.GuideLoader
import com.kylecorry.trail_sense.tools.survival_guide.infrastructure.MultilingualSurvivalGuideFuzzySearch
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
    }

    private fun useChapters(): List<GuideDetails> {
        val context = useAndroidContext()
        val (chapters, setChapters) = useState(emptyList<GuideDetails>())

        useBackgroundEffect(context) {
            val loader = GuideLoader(context)
            setChapters(loader.load(includeContent = false))
        }

        return chapters
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
                    val language = Resources.getLocale(context).language
                    val search = if (language.startsWith("en")) {
                        EnglishSurvivalGuideFuzzySearch(context)
                    } else {
                        MultilingualSurvivalGuideFuzzySearch(context)
                    }
                    setResults(search.search(query))
                }
            }
        }

        return results
    }
}