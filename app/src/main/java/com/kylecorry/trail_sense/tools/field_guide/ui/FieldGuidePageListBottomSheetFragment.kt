package com.kylecorry.trail_sense.tools.field_guide.ui

import android.widget.TextView
import com.kylecorry.andromeda.core.ui.useCallback
import com.kylecorry.andromeda.views.list.AndromedaListView
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveBottomSheetFragment
import com.kylecorry.trail_sense.shared.views.SearchView
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePage

class FieldGuidePageListBottomSheetFragment :
    TrailSenseReactiveBottomSheetFragment(R.layout.fragment_field_guide_page_list_bottom_sheet) {

    var onPageSelected: ((FieldGuidePage) -> Unit)? = null

    override fun update() {
        val listView = useView<AndromedaListView>(R.id.list)
        val searchView = useView<SearchView>(R.id.search)
        val emptyView = useView<TextView>(R.id.empty_text)
        useEffect(listView, emptyView) {
            listView.emptyView = emptyView
        }

        val handleAction = useCallback<FieldGuidePageListItemActionType, FieldGuidePage, Unit> { action, page ->
            if (action == FieldGuidePageListItemActionType.View) {
                onPageSelected?.invoke(page)
            }
        }

        val (_, _, clearFilters) = useFieldGuidePageList(listView, searchView, handleAction, showMenu = false)

        // TODO: Need to support clearing filters - back button does not work with bottom sheet dialog, maybe add a clear button
//        useBackPressedCallback(clearFilters, searchView) {
//            val hadFilters = clearFilters()
//            searchView.query = ""
//            // If the filter was not set, don't consume the event
//            hadFilters
//        }
    }
}
