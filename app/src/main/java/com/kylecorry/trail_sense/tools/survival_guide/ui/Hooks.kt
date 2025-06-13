package com.kylecorry.trail_sense.tools.survival_guide.ui

import androidx.lifecycle.LifecycleOwner
import com.kylecorry.andromeda.core.ui.ReactiveComponent
import com.kylecorry.andromeda.fragments.useBackgroundEffect
import com.kylecorry.andromeda.fragments.useCoroutineQueue
import com.kylecorry.trail_sense.tools.survival_guide.infrastructure.GuideDetails
import com.kylecorry.trail_sense.tools.survival_guide.infrastructure.GuideLoader
import com.kylecorry.trail_sense.tools.survival_guide.infrastructure.SurvivalGuideSearch
import com.kylecorry.trail_sense.tools.survival_guide.infrastructure.SurvivalGuideSearchResult
import kotlinx.coroutines.delay

fun <T> T.useSurvivalGuideChapters(): List<GuideDetails> where T : LifecycleOwner, T : ReactiveComponent {
    val context = useAndroidContext()
    val (chapters, setChapters) = useState(emptyList<GuideDetails>())

    useBackgroundEffect(context) {
        val loader = GuideLoader(context)
        setChapters(loader.load(includeContent = false))
    }

    return chapters
}


fun <T> T.useSearchSurvivalGuide(query: String): Pair<List<SurvivalGuideSearchResult>, String>
        where T : LifecycleOwner, T : ReactiveComponent {
    val (results, setResults) = useState(emptyList<SurvivalGuideSearchResult>())
    val (summary, setSummary) = useState("")
    val context = useAndroidContext()
    val queue = useCoroutineQueue()
    val search = useMemo(context) {
        SurvivalGuideSearch(context)
    }

    useBackgroundEffect(query, context, search, cancelWhenRerun = true) {
        // Debounce
        delay(200)
        queue.replace {
            if (query.isBlank()) {
                setResults(emptyList())
                setSummary("")
            } else {
                val searchResults = search.search(query)
                setResults(searchResults)
                val firstResult = searchResults.firstOrNull()
                if (firstResult != null && firstResult.score >= 0.5f) {
                    setSummary(search.getSummary(query, firstResult))
                } else {
                    setSummary("")
                }
            }
        }
    }

    return results to summary
}