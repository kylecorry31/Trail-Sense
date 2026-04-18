package com.kylecorry.trail_sense.tools.guide.infrastructure

import android.os.Bundle
import androidx.annotation.RawRes
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kylecorry.andromeda.fragments.show
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.guide.ui.GuideBottomSheetFragment

object UserGuideUtils {

    fun showGuide(fragment: Fragment, @RawRes guideId: Int): BottomSheetDialogFragment? {
        val guides = Guides.guides(fragment.requireContext())
        val guide =
            guides.flatMap { it.guides }.firstOrNull { it.contents == guideId } ?: return null
        val sheet = GuideBottomSheetFragment.create(guide)
        sheet.show(fragment)
        return sheet
    }

    fun openGuide(fragment: Fragment, @RawRes guideId: Int) {
        val navController = fragment.findNavController()
        val guides = Guides.guides(fragment.requireContext())

        val guide = guides.flatMap { it.guides }.firstOrNull { it.contents == guideId }

        if (guide != null) {
            navController.navigate(
                R.id.guideFragment, Bundle().apply {
                    putString("guide_name", guide.name)
                    putInt("guide_contents", guide.contents)
                }
            )
        }
    }

}
