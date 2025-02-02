package com.kylecorry.trail_sense.tools.field_guide.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.luna.coroutines.onMain
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentCreateFieldGuidePageBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.views.MaterialMultiSpinnerView
import com.kylecorry.trail_sense.shared.withId
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePage
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePageTag
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePageTagType
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideRepo

class CreateFieldGuidePageFragment : BoundFragment<FragmentCreateFieldGuidePageBinding>() {

    private val repo by lazy { FieldGuideRepo.getInstance(requireContext()) }
    private val tagNameMapper by lazy { FieldGuideTagNameMapper(requireContext()) }

    private var originalPage by state(FieldGuidePage(0))
    private var page by state(originalPage)

    // Tags
    private val tags =
        FieldGuidePageTag.entries.sortedWith(compareBy({ it.type.ordinal }, { it.ordinal }))

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCreateFieldGuidePageBinding {
        return FragmentCreateFieldGuidePageBinding.inflate(layoutInflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val pageId = it.getLong(ARG_PAGE_ID, 0L)
            if (pageId != 0L) {
                inBackground {
                    repo.getPage(pageId)?.let {
                        originalPage = it
                        page = it
                    }
                }
            }

            val tag = FieldGuidePageTag.entries.withId(it.getLong(ARG_CLASSIFICATION_ID, 0L))

            if (tag != null) {
                page = page.copy(directTags = page.directTags + tag)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        CustomUiUtils.setButtonState(binding.createFieldGuidePageTitle.rightButton, true)
        binding.createFieldGuidePageTitle.rightButton.setOnClickListener {
            save()
        }

        // Fields
        binding.name.addTextChangedListener {
            page = page.copy(name = it.toString())
        }

        binding.notes.addTextChangedListener {
            page = page.copy(notes = it.toString())
        }


        initializeTags(
            binding.tagLocations,
            getString(R.string.location),
            FieldGuidePageTagType.Location
        )

        initializeTags(
            binding.tagHabitats,
            getString(R.string.habitat),
            FieldGuidePageTagType.Habitat
        )

        initializeTags(
            binding.tagClassifications,
            getString(R.string.classification),
            FieldGuidePageTagType.Classification
        )

        initializeTags(
            binding.tagActivityPatterns,
            getString(R.string.activity_pattern),
            FieldGuidePageTagType.ActivityPattern
        )

        initializeTags(
            binding.tagHumanInteractions,
            getString(R.string.human_interaction),
            FieldGuidePageTagType.HumanInteraction
        )

        // TODO: Add dirty checking
    }

    override fun onUpdate() {
        super.onUpdate()

        // Original content
        effect2(originalPage) {
            binding.name.setText(originalPage.name)
            binding.notes.setText(originalPage.notes)
        }

        effect2(page.tags) {
            setTags(binding.tagLocations, page.directTags, FieldGuidePageTagType.Location)
            setTags(binding.tagHabitats, page.directTags, FieldGuidePageTagType.Habitat)
            setTags(
                binding.tagClassifications,
                page.directTags,
                FieldGuidePageTagType.Classification
            )
            setTags(
                binding.tagActivityPatterns,
                page.directTags,
                FieldGuidePageTagType.ActivityPattern
            )
            setTags(
                binding.tagHumanInteractions,
                page.directTags,
                FieldGuidePageTagType.HumanInteraction
            )
        }
    }

    private fun save() {
        inBackground {
            repo.add(page)
            onMain {
                findNavController().navigateUp()
            }
        }
    }

    private fun initializeTags(
        view: MaterialMultiSpinnerView,
        hint: String,
        type: FieldGuidePageTagType
    ) {
        val tagsOfType = tags.filter { it.type == type }
        view.setHint(hint)
        view.setItems(tagsOfType.map { tagNameMapper.getName(it) })
        view.setOnSelectionChangeListener {
            page =
                page.copy(directTags = page.directTags.filter { it.type != type } + it.map { tagsOfType[it] })
        }
    }

    private fun setTags(
        view: MaterialMultiSpinnerView,
        selection: List<FieldGuidePageTag>,
        type: FieldGuidePageTagType
    ) {
        val tagsOfType = tags.filter { it.type == type }
        val selectionOfType = selection.filter { it.type == type }
        view.setSelection(selectionOfType.map { tagsOfType.indexOf(it) })
    }


    companion object {
        private const val ARG_PAGE_ID = "page_id"
        private const val ARG_CLASSIFICATION_ID = "classification_id"
    }
}