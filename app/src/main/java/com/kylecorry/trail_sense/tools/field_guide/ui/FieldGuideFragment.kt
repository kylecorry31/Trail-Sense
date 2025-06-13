package com.kylecorry.trail_sense.tools.field_guide.ui

import android.widget.TextView
import androidx.core.os.bundleOf
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
import com.kylecorry.trail_sense.shared.extensions.useSearch
import com.kylecorry.trail_sense.shared.extensions.useShowDisclaimer
import com.kylecorry.trail_sense.shared.text.TextUtils
import com.kylecorry.trail_sense.shared.views.SearchView
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePage
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePageTag
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideCleanupCommand
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideRepo

class FieldGuideFragment : TrailSenseReactiveFragment(R.layout.fragment_field_guide) {

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

    override fun update() {
        val context = useAndroidContext()

        // Views
        val listView = useView<AndromedaListView>(R.id.list)
        val addButtonView = useView<FloatingActionButton>(R.id.add_btn)
        val searchView = useView<SearchView>(R.id.search)
        val emptyView = useView<TextView>(R.id.empty_text)
        val navController = useNavController()

        listView.emptyView = emptyView

        // State
        val (tagFilter, setTagFilter) = useState<FieldGuidePageTag?>(null)
        val (filter, setFilter) = useState("")
        val (pages, reloadPages) = useLoadPages(filter, tagFilter)

        // Services
        val repo = useService<FieldGuideRepo>()

        // Callbacks
        val createPage = useCreatePage()
        val editPage = useEditPage()
        val viewPage = useViewPage()

        // Handle back press
        useBackPressedCallback(filter, tagFilter, searchView) {
            setTagFilter(null)
            setFilter("")
            searchView.query = ""
            // If the filter was not set, don't consume the event
            tagFilter != null || filter.isNotBlank()
        }

        // One time setup
        useShowDisclaimer(
            getString(R.string.disclaimer_message_title),
            getString(R.string.field_guide_disclaimer),
            "field_guide_disclaimer",
            cancelText = null
        )
        useCleanupPages()

        // Search
        useSearch(searchView, setFilter)

        // Create button
        useClickCallback(addButtonView, createPage, tagFilter) {
            createPage(tagFilter)
        }

        // List
        useEffect(
            pages,
            filter,
            tagFilter,
            listView,
            context,
            viewLifecycleOwner,
            navController,
            resetOnResume
        ) {
            val tagMapper = FieldGuidePageTagListItemMapper(context, setTagFilter)

            val pageMapper = FieldGuidePageListItemMapper(
                context,
                viewLifecycleOwner
            ) { action, page ->
                when (action) {
                    FieldGuidePageListItemActionType.View -> viewPage(page)
                    FieldGuidePageListItemActionType.Edit -> editPage(page)

                    FieldGuidePageListItemActionType.Delete -> {
                        dialog(getString(R.string.delete), page.name) { cancelled ->
                            if (!cancelled) {
                                inBackground {
                                    repo.delete(page)
                                    reloadPages()
                                }
                            }
                        }
                    }
                }
            }

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
    }

    private fun useCleanupPages() {
        val context = useAndroidContext()
        useBackgroundEffect(context, resetOnResume) {
            FieldGuideCleanupCommand(context).execute()
        }
    }

    private fun useLoadPages(
        filter: String,
        tagFilter: FieldGuidePageTag?
    ): Pair<List<FieldGuidePage>, () -> Unit> {
        val repo = useService<FieldGuideRepo>()
        val context = useAndroidContext()
        val (pages, setPages) = useState(listOf<FieldGuidePage>())
        val reload = useCallback<Unit> {
            inBackground {
                setPages(repo.getAllPages().sortedBy { it.name })
            }
        }

        useEffect(resetOnResume) {
            reload()
        }

        val filteredPages = useMemo(pages, filter, tagFilter) {
            val mapper = FieldGuideTagNameMapper(context)
            TextUtils.search(filter, pages) { page ->
                listOf(
                    page.name,
                    page.notes ?: "",
                    page.tags.joinToString { mapper.getName(it) })
            }.filter { pages ->
                tagFilter == null || pages.tags.contains(tagFilter)
            }
        }

        return filteredPages to reload
    }

    private fun useCreatePage(): (tag: FieldGuidePageTag?) -> Unit {
        val navController = useNavController()
        return useCallback { tag ->
            navController.navigate(
                R.id.createFieldGuidePageFragment,
                bundleOf("classification_id" to (tag?.id ?: 0L))
            )
        }
    }

    private fun useEditPage(): (page: FieldGuidePage) -> Unit {
        val navController = useNavController()
        return useCallback(navController) { page ->
            navController.navigate(
                R.id.createFieldGuidePageFragment,
                bundleOf("page_id" to page.id)
            )
        }
    }

    private fun useViewPage(): (page: FieldGuidePage) -> Unit {
        val navController = useNavController()
        return useCallback(navController) { page ->
            navController.navigate(
                R.id.fieldGuidePageFragment,
                bundleOf("page_id" to page.id)
            )
        }
    }

}