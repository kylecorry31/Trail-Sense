package com.kylecorry.trail_sense.tools.field_guide.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.luna.coroutines.CoroutineQueueRunner
import com.kylecorry.trail_sense.databinding.FragmentFieldGuidePageBinding
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePage
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideRepo

class FieldGuidePageFragment : BoundFragment<FragmentFieldGuidePageBinding>() {

    private val repo by lazy { FieldGuideRepo.getInstance(requireContext()) }
    private val files by lazy { FileSubsystem.getInstance(requireContext()) }

    private var pageId by state<Long?>(null)
    private var page by state<FieldGuidePage?>(null)

    private val loader = CoroutineQueueRunner()

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentFieldGuidePageBinding {
        return FragmentFieldGuidePageBinding.inflate(layoutInflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageId = arguments?.getLong("page_id")
    }

    override fun onUpdate() {
        super.onUpdate()
        effect2(pageId) {
            inBackground {
                if (pageId == null) {
                    loader.replace { page = null }
                } else {
                    loader.replace { page = repo.getPage(pageId ?: return@replace) }
                }
            }
        }

        effect2(page) {
            binding.fieldGuidePageTitle.title.text = page?.name
            val tags = page?.tags?.joinToString(", ") ?: ""
            binding.notes.text = tags + "\n\n" + page?.notes
            val image = page?.images?.firstOrNull()
            binding.image.setImageDrawable(
                image?.let { files.drawable(it) }
            )
        }
    }


}