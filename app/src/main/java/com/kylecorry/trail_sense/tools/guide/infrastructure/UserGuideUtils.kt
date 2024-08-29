package com.kylecorry.trail_sense.tools.guide.infrastructure

import android.content.Context
import android.content.res.ColorStateList
import android.text.Layout
import android.text.style.AlignmentSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RawRes
import androidx.core.os.bundleOf
import androidx.core.text.method.LinkMovementMethodCompat
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.ExpansionLayout
import com.kylecorry.andromeda.core.ui.setCompoundDrawables
import com.kylecorry.andromeda.fragments.show
import com.kylecorry.andromeda.markdown.MarkdownExtension
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.text.TextUtils
import com.kylecorry.trail_sense.shared.views.Views
import com.kylecorry.trail_sense.tools.guide.ui.GuideBottomSheetFragment

object UserGuideUtils {

    fun showGuide(fragment: Fragment, @RawRes guideId: Int): BottomSheetDialogFragment? {
        val guides = Guides.guides(fragment.requireContext())
        val guide =
            guides.flatMap { it.guides }.firstOrNull { it.contents == guideId } ?: return null
        val sheet = GuideBottomSheetFragment(guide)
        sheet.show(fragment)
        return sheet
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

    fun getGuideView(context: Context, text: String, shouldUppercaseSubheadings: Boolean = false): View {
        val markdown = MarkdownService(context, extensions = listOf(
            MarkdownExtension(1, '+') { AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER) }
        ))
        val sections = TextUtils.groupSections(TextUtils.getSections(text), null)
        val children = sections.mapNotNull { section ->
            val first = section.firstOrNull() ?: return@mapNotNull null
            if (first.level != null && first.title != null) {
                // Create an expandable section
                val expandable = expandable(
                    context, first.title
                ) {
                    markdown.setMarkdown(it,
                        first.content + "\n" + section.drop(1)
                            .joinToString("\n") { it.toMarkdown(shouldUppercaseSubheadings) })
                }
                expandable
            } else {
                // Only text nodes
                val t = com.kylecorry.andromeda.core.ui.Views.text(context, null).also {
                    (it as TextView).movementMethod = LinkMovementMethodCompat.getInstance()
                }
                markdown.setMarkdown(t as TextView, section.joinToString("\n") { it.toMarkdown() })
                t
            }
        }

        return Views.linear(children, padding = Resources.dp(context, 16f).toInt())
    }

    private fun expandable(
        context: Context,
        title: String,
        setContent: (TextView) -> Unit
    ): ExpansionLayout {
        val expandable = ExpansionLayout(context, null)

        val titleView = Views.text(context, title) as TextView
        titleView.setCompoundDrawables(right = R.drawable.ic_drop_down)
        CustomUiUtils.setImageColor(titleView, Resources.androidTextColorSecondary(context))
        titleView.compoundDrawablePadding = Resources.dp(context, 8f).toInt()
        val padding = Resources.dp(context, 16f).toInt()
        val margin = Resources.dp(context, 8f).toInt()
        titleView.setPadding(padding)
        titleView.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).also {
            it.setMargins(0, margin, 0, margin)
            it.gravity = Gravity.CENTER_VERTICAL
        }
        titleView.setBackgroundResource(R.drawable.rounded_rectangle)
        titleView.backgroundTintList = ColorStateList.valueOf(
            Resources.getAndroidColorAttr(
                context,
                android.R.attr.colorBackgroundFloating
            )
        )

        expandable.addView(titleView)

        expandable.addView(
            Views.text(context, null).also {
                (it as TextView).movementMethod = LinkMovementMethodCompat.getInstance()
                it.setPadding(margin)
                setContent(it)
            }
        )

        expandable.setOnExpandStateChangedListener { isExpanded ->
            titleView.setCompoundDrawables(right = if (isExpanded) R.drawable.ic_drop_down_expanded else R.drawable.ic_drop_down)
            CustomUiUtils.setImageColor(titleView, Resources.androidTextColorSecondary(context))
        }

        return expandable
    }

}