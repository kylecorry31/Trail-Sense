package com.kylecorry.trail_sense.tools.guide.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.trail_sense.databinding.FragmentGuideListBinding
import com.kylecorry.trail_sense.tools.guide.domain.UserGuideCategory
import com.kylecorry.trail_sense.tools.guide.infrastructure.Guides

class GuideListFragment : BoundFragment<FragmentGuideListBinding>() {

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentGuideListBinding {
        return FragmentGuideListBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val fragment = GuideListPreferenceFragment()
        val guides = Guides.guides(requireContext())

        binding.searchbox.setOnSearchListener {
            val newGuides = mutableListOf<UserGuideCategory>()

            for (category in guides) {
                if (category.name.contains(it, true)) {
                    newGuides.add(category)
                } else {
                    val newCategory =
                        UserGuideCategory(category.name, category.guides.filter { guide ->
                            guide.name.contains(it, true)
                        })
                    if (newCategory.guides.isNotEmpty()) {
                        newGuides.add(newCategory)
                    }
                }
            }

            fragment.updateList(newGuides)
        }

        setFragment(fragment)
        fragment.updateList(guides)
    }

    private fun setFragment(fragment: Fragment) {
        val fragmentManager = childFragmentManager
        fragmentManager.commit {
            replace(binding.guideFragment.id, fragment)
        }
    }

}