package com.kylecorry.trail_sense.tools.survival_guide.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.andromeda.views.list.ListItemMapper
import com.kylecorry.andromeda.views.list.ResourceListIcon
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentSurvivalGuideChaptersBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.tools.survival_guide.domain.Chapter
import com.kylecorry.trail_sense.tools.survival_guide.domain.Chapters

class FragmentToolSurvivalGuideList : BoundFragment<FragmentSurvivalGuideChaptersBinding>() {

    private val itemMapper = object : ListItemMapper<Chapter> {
        override fun map(value: Chapter): ListItem {
            return ListItem(
                value.resource.toLong(),
                value.title,
                value.chapter,
                icon = ResourceListIcon(
                    value.icon,
                    Resources.androidTextColorSecondary(requireContext())
                )
            ) {
                findNavController().navigateWithAnimation(
                    R.id.fragmentToolSurvivalGuideReader,
                    bundleOf("chapter_resource_id" to value.resource)
                )
            }
        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.survivalGuideChaptersList.setItems(
            Chapters.getChapters(requireContext()),
            itemMapper
        )
    }

    override fun onResume() {
        super.onResume()
        CustomUiUtils.disclaimer(
            requireContext(),
            getString(R.string.survival_guide),
            getString(R.string.survival_guide_disclaimer),
            "pref_survival_guide_disclaimer_shown",
            cancelText = null
        )
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSurvivalGuideChaptersBinding {
        return FragmentSurvivalGuideChaptersBinding.inflate(layoutInflater, container, false)
    }
}