package com.kylecorry.trail_sense.tools.guide.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kylecorry.andromeda.fragments.BoundBottomSheetDialogFragment
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.trail_sense.databinding.FragmentGuideBinding
import com.kylecorry.trail_sense.tools.guide.domain.UserGuide
import com.kylecorry.trail_sense.tools.guide.infrastructure.UserGuideService

class GuideBottomSheetFragment(private val guide: UserGuide) :
    BoundBottomSheetDialogFragment<FragmentGuideBinding>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val content = UserGuideService(requireContext()).load(guide.contents)
        binding.guideName.title.text = guide.name
        val markdown = MarkdownService(requireContext())
        markdown.setMarkdown(binding.guideContents, content)
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentGuideBinding {
        return FragmentGuideBinding.inflate(layoutInflater, container, false)
    }
}