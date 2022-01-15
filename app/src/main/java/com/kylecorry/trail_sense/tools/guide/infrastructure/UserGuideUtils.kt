package com.kylecorry.trail_sense.tools.guide.infrastructure

import androidx.annotation.RawRes
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.trail_sense.R

object UserGuideUtils {

    fun showGuide(fragment: Fragment, @RawRes guideId: Int): AlertDialog? {
        val guides = Guides.guides(fragment.requireContext())
        val guide =
            guides.flatMap { it.guides }.firstOrNull { it.contents == guideId } ?: return null

        val content = UserGuideService(fragment.requireContext()).load(guide.contents)
        val markdown = MarkdownService(fragment.requireContext())
        val text = markdown.toMarkdown(content)
        return Alerts.dialog(
            fragment.requireContext(),
            guide.name,
            text,
            allowLinks = true,
            cancelText = null
        )
    }

    fun openGuide(fragment: Fragment, @RawRes guideId: Int) {
        val navController = fragment.findNavController()
        val guides = Guides.guides(fragment.requireContext())

        val guide = guides.flatMap { it.guides }.firstOrNull { it.contents == guideId }

        if (guide != null) {
            navController.navigate(
                R.id.guideFragment, bundleOf(
                    "guide_name" to guide.name,
                    "guide_contents" to guide.contents
                )
            )
        }

    }

}