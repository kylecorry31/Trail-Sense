package com.kylecorry.trail_sense.tools.field_guide.ui

import com.kylecorry.andromeda.core.ui.ReactiveComponent
import com.kylecorry.andromeda.core.ui.useCallback
import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.andromeda.fragments.useBackgroundCallback
import com.kylecorry.andromeda.views.list.AndromedaListView
import com.kylecorry.trail_sense.shared.extensions.useSearch
import com.kylecorry.trail_sense.shared.views.SearchView
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePage
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePageTag
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuideService

fun ReactiveComponent.useLoadPages(
    filter: String,
    tagFilter: FieldGuidePageTag?
): Pair<List<FieldGuidePage>, () -> Unit> {
    val service = useService<FieldGuideService>()
    val (pages, setPages) = useState(listOf<FieldGuidePage>())
    val reloadCallback = useBackgroundCallback {
        setPages(service.getAllPages())
    }
    val reload = useCallback<Unit>(reloadCallback) {
        reloadCallback()
    }

    val filteredPages = useMemo(pages, filter, tagFilter) {
        service.filterPages(pages, filter, tagFilter)
    }

    useEffect(reload) {
        reload()
    }

    return filteredPages to reload
}

private val headingTags = listOf(
    FieldGuidePageTag.Plant,
    FieldGuidePageTag.Fungus,
    FieldGuidePageTag.Animal,
    FieldGuidePageTag.Mammal,
    FieldGuidePageTag.Bird,
    FieldGuidePageTag.Reptile,
    FieldGuidePageTag.Amphibian,
    FieldGuidePageTag.Fish,
    FieldGuidePageTag.Invertebrate,
    FieldGuidePageTag.Rock,
    FieldGuidePageTag.Weather,
    FieldGuidePageTag.Other
)

data class FieldGuidePageListResult(
    val tagFilter: FieldGuidePageTag?,
    val reload: () -> Unit,
    val clearFilters: () -> Boolean
)

fun ReactiveComponent.useFieldGuidePageList(
    listView: AndromedaListView,
    searchView: SearchView,
    handleAction: (FieldGuidePageListItemActionType, FieldGuidePage) -> Unit,
    showMenu: Boolean = true
): FieldGuidePageListResult {
    val context = useAndroidContext()
    val viewLifecycleOwner = useLifecycleOwner()

    val (tagFilter, setTagFilter) = useState<FieldGuidePageTag?>(null)
    val (filter, setFilter) = useState("")
    val (pages, reloadPages) = useLoadPages(filter, tagFilter)

    useSearch(searchView, setFilter)

    useEffect(
        pages,
        filter,
        tagFilter,
        listView,
        context,
        viewLifecycleOwner,
        handleAction,
        showMenu
    ) {
        val tagMapper = FieldGuidePageTagListItemMapper(context, setTagFilter)

        val pageMapper = FieldGuidePageListItemMapper(
            context,
            viewLifecycleOwner,
            action = handleAction,
            showMenu = showMenu
        )

        if (tagFilter == null && filter.isBlank()) {
            val items = headingTags.map { tag ->
                val numPages = pages.count { it.tags.contains(tag) }
                tag to numPages
            }
            listView.setItems(items, tagMapper)
        } else {
            listView.setItems(pages, pageMapper)
        }
    }

    val clearFilters = useCallback<Boolean>(tagFilter, filter) {
        setTagFilter(null)
        setFilter("")
        tagFilter != null || filter.isNotBlank()
    }

    return useMemo(reloadPages, clearFilters, tagFilter) {
        FieldGuidePageListResult(tagFilter, reloadPages, clearFilters)
    }
}
