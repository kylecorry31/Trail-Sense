package com.kylecorry.trail_sense.tools.field_guide.ui

import android.widget.TextView
import com.kylecorry.andromeda.core.ui.useCallback
import com.kylecorry.andromeda.fragments.useClickCallback
import com.kylecorry.andromeda.views.list.AndromedaListView
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveBottomSheetFragment
import com.kylecorry.trail_sense.shared.extensions.useBottomSheetBackPressedCallback
import com.kylecorry.trail_sense.shared.extensions.useNavController
import com.kylecorry.trail_sense.shared.openTool
import com.kylecorry.trail_sense.shared.views.SearchView
import com.kylecorry.trail_sense.shared.views.Toolbar
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePage
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class FieldGuidePageListBottomSheetFragment :
    TrailSenseReactiveBottomSheetFragment(R.layout.fragment_field_guide_page_list_bottom_sheet) {

    var onPageSelected: ((FieldGuidePage) -> Unit)? = null

    override fun update() {
        val listView = useView<AndromedaListView>(R.id.list)
        val searchView = useView<SearchView>(R.id.search)
        val emptyView = useView<TextView>(R.id.empty_text)
        val title = useView<Toolbar>(R.id.title)
        val navController = useNavController()
        useEffect(listView, emptyView) {
            listView.emptyView = emptyView
        }

        useClickCallback(title.rightButton) {
            navController.openTool(Tools.FIELD_GUIDE)
        }

        val handleAction = useCallback<FieldGuidePageListItemActionType, FieldGuidePage, Unit> { action, page ->
            if (action == FieldGuidePageListItemActionType.View) {
                onPageSelected?.invoke(page)
            }
        }

        val (_, _, clearFilters) = useFieldGuidePageList(listView, searchView, handleAction, showMenu = false)

        useBottomSheetBackPressedCallback(clearFilters, searchView) {
            val hadFilters = clearFilters()
            searchView.query = ""
            // If the filter was not set, don't consume the event
            hadFilters
        }
    }
}
