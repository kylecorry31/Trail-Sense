package com.kylecorry.trail_sense.tools.guide.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.fragments.BoundBottomSheetDialogFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.trail_sense.databinding.FragmentGuideBinding
import com.kylecorry.trail_sense.shared.text.TextUtils
import com.kylecorry.trail_sense.tools.guide.domain.UserGuide

class GuideBottomSheetFragment : BoundBottomSheetDialogFragment<FragmentGuideBinding>() {

    private val guide: UserGuide by lazy {
        UserGuide(
            name = requireArguments().getString(ARG_GUIDE_NAME) ?: "",
            contents = requireArguments().getInt(ARG_GUIDE_CONTENTS)
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.guideName.title.text = guide.name

        inBackground {
            val content = onIO {
                TextUtils.loadTextFromResources(requireContext(), guide.contents)
            }
            if (isBound) {
                binding.guideScroll.removeAllViews()
                binding.guideScroll.addView(TextUtils.getMarkdownView(requireContext(), content))
            }
        }
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentGuideBinding {
        return FragmentGuideBinding.inflate(layoutInflater, container, false)
    }

    companion object {
        private const val ARG_GUIDE_NAME = "guide_name"
        private const val ARG_GUIDE_CONTENTS = "guide_contents"

        fun create(guide: UserGuide): GuideBottomSheetFragment {
            return GuideBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_GUIDE_NAME, guide.name)
                    putInt(ARG_GUIDE_CONTENTS, guide.contents)
                }
            }
        }
    }
}
