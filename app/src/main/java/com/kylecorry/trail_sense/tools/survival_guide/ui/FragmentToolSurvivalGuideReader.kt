package com.kylecorry.trail_sense.tools.survival_guide.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.trail_sense.databinding.FragmentSurvivalGuideBinding
import com.kylecorry.trail_sense.tools.guide.infrastructure.UserGuideService
import com.kylecorry.trail_sense.tools.guide.infrastructure.UserGuideUtils
import com.kylecorry.trail_sense.tools.survival_guide.domain.Chapters

class FragmentToolSurvivalGuideReader : BoundFragment<FragmentSurvivalGuideBinding>() {

    private var chapterResourceId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chapterResourceId = requireArguments().getInt("chapter_resource_id")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val chapters = Chapters.getChapters(requireContext())
        val chapter = chapters.firstOrNull { it.resource == chapterResourceId } ?: return
        binding.guideName.title.text = chapter.title
        binding.guideName.subtitle.text = chapter.chapter
        inBackground {
            val res = chapterResourceId ?: return@inBackground
            val content = onIO {
                // TODO: Extract this to the shared package
                UserGuideService(requireContext()).load(res)
            }
            if (isBound) {
                binding.guideScroll.removeAllViews()
                // TODO: Extract user guide utils to shared package
                binding.guideScroll.addView(UserGuideUtils.getGuideView(requireContext(), content))
            }
        }
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSurvivalGuideBinding {
        return FragmentSurvivalGuideBinding.inflate(layoutInflater, container, false)
    }
}