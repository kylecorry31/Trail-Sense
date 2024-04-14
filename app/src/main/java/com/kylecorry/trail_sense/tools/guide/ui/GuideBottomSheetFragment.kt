package com.kylecorry.trail_sense.tools.guide.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.fragments.BoundBottomSheetDialogFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.trail_sense.databinding.FragmentGuideBinding
import com.kylecorry.trail_sense.tools.guide.domain.UserGuide
import com.kylecorry.trail_sense.tools.guide.infrastructure.UserGuideService
import com.kylecorry.trail_sense.tools.guide.infrastructure.UserGuideUtils

class GuideBottomSheetFragment(private val guide: UserGuide) :
    BoundBottomSheetDialogFragment<FragmentGuideBinding>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.guideName.title.text = guide.name

        inBackground {
            val content = onIO {
                UserGuideService(requireContext()).load(guide.contents)
            }
            if (isBound) {
                binding.guideScroll.removeAllViews()
                binding.guideScroll.addView(UserGuideUtils.getGuideView(requireContext(), content))
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