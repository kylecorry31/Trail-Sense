package com.kylecorry.trail_sense.tools.guide.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.trail_sense.databinding.FragmentGuideBinding
import com.kylecorry.trail_sense.tools.guide.infrastructure.UserGuideService

class GuideFragment : BoundFragment<FragmentGuideBinding>() {

    private lateinit var name: String

    private var resource: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        name = requireArguments().getString("guide_name", "")
        resource = requireArguments().getInt("guide_contents")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.guideName.title.text = name
        inBackground {
            val res = resource ?: return@inBackground
            val content = onIO {
                UserGuideService(requireContext()).load(res)
            }
            val markdown = MarkdownService(requireContext())
            val spanned = onDefault {
                markdown.toMarkdown(content)
            }
            if (isBound) {
                binding.guideContents.text = spanned
            }
        }
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentGuideBinding {
        return FragmentGuideBinding.inflate(layoutInflater, container, false)
    }
}