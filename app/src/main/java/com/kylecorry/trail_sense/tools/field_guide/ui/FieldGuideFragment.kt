package com.kylecorry.trail_sense.tools.field_guide.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.dialog
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.onBackPressed
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentFieldGuideBinding
import com.kylecorry.trail_sense.shared.text.TextUtils
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePage
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePageTag
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideCleanupCommand
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideRepo

class FieldGuideFragment : BoundFragment<FragmentFieldGuideBinding>() {

    private var pages by state<List<FieldGuidePage>>(emptyList())
    private var filter by state("")
    private var tagFilter by state<FieldGuidePageTag?>(null)
    private val repo by lazy { FieldGuideRepo.getInstance(requireContext()) }
    private val tagNameMapper by lazy { FieldGuideTagNameMapper(requireContext()) }
    private lateinit var pageMapper: FieldGuidePageListItemMapper
    private val tagMapper by lazy {
        FieldGuidePageTagListItemMapper(requireContext()) { action, tag ->
            if (action == FieldGuidePageTagListItemActionType.View) {
                tagFilter = tag
            }
        }
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater, container: ViewGroup?
    ): FragmentFieldGuideBinding {
        return FragmentFieldGuideBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        inBackground(BackgroundMinimumState.Created) {
            reloadPages()
        }

        binding.list.emptyView = binding.emptyText

        binding.search.setOnSearchListener {
            filter = it
        }

        onBackPressed {
            when {
                tagFilter != null -> {
                    tagFilter = null
                }

                else -> {
                    remove()
                    findNavController().navigateUp()
                }
            }
        }

        binding.addBtn.setOnClickListener {
            findNavController().navigate(
                R.id.createFieldGuidePageFragment, bundleOf(
                    "classification_id" to (tagFilter?.id ?: 0L)
                )
            )
        }
    }

    override fun onUpdate() {
        super.onUpdate()
        useEffect(pages, filter, tagFilter, lifecycleHookTrigger.onResume()) {
            val tags = listOf(
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
                FieldGuidePageTag.Other
            )

            val filteredPages = TextUtils.search(filter, pages) { page ->
                listOf(
                    page.name,
                    page.notes ?: "",
                    page.tags.joinToString { tagNameMapper.getName(it) })
            }.filter { pages ->
                tagFilter == null || pages.tags.contains(tagFilter)
            }

            if (tagFilter == null && filter.isBlank()) {
                val items = tags.map { tag ->
                    val numPages = pages.count { it.tags.contains(tag) }
                    tag to numPages
                }
                binding.list.setItems(items, tagMapper)
            } else {
                binding.list.setItems(filteredPages, pageMapper)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        pageMapper = FieldGuidePageListItemMapper(
            requireContext(),
            viewLifecycleOwner,
            this::onPageAction
        )

        inBackground {
            FieldGuideCleanupCommand(requireContext()).execute()
        }
    }

    private fun onPageAction(action: FieldGuidePageListItemActionType, page: FieldGuidePage) {
        when (action) {
            FieldGuidePageListItemActionType.View -> {
                findNavController().navigate(
                    R.id.fieldGuidePageFragment,
                    bundleOf("page_id" to page.id)
                )
            }

            FieldGuidePageListItemActionType.Edit -> {
                findNavController().navigate(
                    R.id.createFieldGuidePageFragment,
                    bundleOf("page_id" to page.id)
                )
            }

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

    private suspend fun reloadPages() {
        pages = repo.getAllPages().sortedBy { it.name }
    }
}