package com.kylecorry.trail_sense.tools.field_guide.ui

import android.os.Bundle
import android.widget.TextView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.andromeda.alerts.dialog
import com.kylecorry.andromeda.core.ui.useCallback
import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.useBackgroundEffect
import com.kylecorry.andromeda.fragments.useClickCallback
import com.kylecorry.andromeda.views.list.AndromedaListView
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.extensions.useBackPressedCallback
import com.kylecorry.trail_sense.shared.extensions.useNavController
import com.kylecorry.trail_sense.shared.extensions.useResumeEffect
import com.kylecorry.trail_sense.shared.extensions.useShowDisclaimer
import com.kylecorry.trail_sense.shared.extensions.useTrigger
import com.kylecorry.trail_sense.shared.views.SearchView
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePage
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePageTag
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuideService
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideCleanupCommand

class FieldGuideFragment : TrailSenseReactiveFragment(R.layout.fragment_tool_field_guide) {

    override fun update() {
        // Views
        val listView = useView<AndromedaListView>(R.id.list)
        val addButtonView = useView<FloatingActionButton>(R.id.add_btn)
        val searchView = useView<SearchView>(R.id.search)
        val emptyView = useView<TextView>(R.id.empty_text)

        listView.emptyView = emptyView

        // State
        val (reloadKey, triggerReload) = useTrigger()

        // Services
        val service = useService<FieldGuideService>()

        // Callbacks
        val createPage = useCreatePage()
        val editPage = useEditPage()
        val viewPage = useViewPage()
        val deletePage = useDeletePage(triggerReload)

        // One time setup
        useShowDisclaimer(
            getString(R.string.disclaimer_message_title),
            getString(R.string.field_guide_disclaimer),
            "field_guide_disclaimer",
            cancelText = null
        )
        useCleanupPages()

        // List
        val handlePageAction = useCallback<FieldGuidePageListItemActionType, FieldGuidePage, Unit>(
            service,
            viewPage,
            editPage,
            deletePage,
            resetOnResume
        ) { action, page ->
            when (action) {
                FieldGuidePageListItemActionType.View -> viewPage(page)
                FieldGuidePageListItemActionType.Edit -> editPage(page)
                FieldGuidePageListItemActionType.Delete -> deletePage(page)
            }
        }

        val (tagFilter, reload, clearFilters) = useFieldGuidePageList(
            listView,
            searchView,
            handlePageAction
        )
        useResumeEffect(reload, reloadKey) {
            reload()
        }

        // Handle back press
        useBackPressedCallback(clearFilters, searchView) {
            val hadFilters = clearFilters()
            searchView.query = ""
            // If the filter was not set, don't consume the event
            hadFilters
        }

        // Create button
        useClickCallback(addButtonView, createPage, tagFilter) {
            createPage(tagFilter)
        }
    }

    private fun useCleanupPages() {
        val context = useAndroidContext()
        useBackgroundEffect(context, resetOnResume) {
            FieldGuideCleanupCommand(context).execute()
        }
    }

    private fun useDeletePage(triggerReload: () -> Unit): (page: FieldGuidePage) -> Unit {
        val service = useService<FieldGuideService>()
        return useCallback(service, triggerReload) { page ->
            dialog(getString(R.string.delete), page.name) { cancelled ->
                if (!cancelled) {
                    inBackground {
                        service.deletePage(page)
                        triggerReload()
                    }
                }
            }
        }
    }

    private fun useCreatePage(): (tag: FieldGuidePageTag?) -> Unit {
        val navController = useNavController()
        return useCallback { tag ->
            navController.navigate(
                R.id.createFieldGuidePageFragment,
                Bundle().apply {
                    putLong("classification_id", (tag?.id ?: 0L))
                }
            )
        }
    }

    private fun useEditPage(): (page: FieldGuidePage) -> Unit {
        val navController = useNavController()
        return useCallback(navController) { page ->
            navController.navigate(
                R.id.createFieldGuidePageFragment,
                Bundle().apply {
                    putLong("page_id", page.id)
                }
            )
        }
    }

    private fun useViewPage(): (page: FieldGuidePage) -> Unit {
        val navController = useNavController()
        return useCallback(navController) { page ->
            navController.navigate(
                R.id.fieldGuidePageFragment,
                Bundle().apply {
                    putLong("page_id", page.id)
                }
            )
        }
    }

}
