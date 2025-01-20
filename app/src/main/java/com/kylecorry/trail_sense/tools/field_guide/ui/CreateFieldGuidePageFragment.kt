package com.kylecorry.trail_sense.tools.field_guide.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.trail_sense.databinding.FragmentCreateFieldGuidePageBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.withId
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePage
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePageTag
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideRepo

class CreateFieldGuidePageFragment : BoundFragment<FragmentCreateFieldGuidePageBinding>() {

    private var existingPage by state<FieldGuidePage?>(null)
    private var tags by state<List<FieldGuidePageTag>>(emptyList())
    private val repo by lazy { FieldGuideRepo.getInstance(requireContext()) }

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
                    existingPage = repo.getPage(pageId)
                }
            }

            val tag = it.getLong(ARG_CLASSIFICATION_ID, 0L)
                .takeIf { id -> id != 0L }
                ?.let { id -> FieldGuidePageTag.entries.withId(id) }

            if (tag != null) {
                tags += listOf(tag)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        CustomUiUtils.setButtonState(binding.createFieldGuidePageTitle.rightButton, true)
    }

    companion object {
        private const val ARG_PAGE_ID = "page_id"
        private const val ARG_CLASSIFICATION_ID = "classification_id"
    }
}