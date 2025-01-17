package com.kylecorry.trail_sense.tools.field_guide.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import com.google.android.flexbox.FlexboxLayout
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.views.badge.Badge
import com.kylecorry.luna.coroutines.CoroutineQueueRunner
import com.kylecorry.trail_sense.databinding.FragmentFieldGuidePageBinding
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.readableName
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePage
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePageTag
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePageTagType
import com.kylecorry.trail_sense.tools.field_guide.domain.getMostSpecific
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideRepo

class FieldGuidePageFragment : BoundFragment<FragmentFieldGuidePageBinding>() {

    private val repo by lazy { FieldGuideRepo.getInstance(requireContext()) }
    private val files by lazy { FileSubsystem.getInstance(requireContext()) }

    private var pageId by state<Long?>(null)
    private var page by state<FieldGuidePage?>(null)

    private val loader = CoroutineQueueRunner()

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentFieldGuidePageBinding {
        return FragmentFieldGuidePageBinding.inflate(layoutInflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageId = arguments?.getLong("page_id")
    }

    override fun onUpdate() {
        super.onUpdate()
        effect2(pageId) {
            inBackground {
                if (pageId == null) {
                    loader.replace { page = null }
                } else {
                    loader.replace { page = repo.getPage(pageId ?: return@replace) }
                }
            }
        }

        effect2(page) {
            binding.fieldGuidePageTitle.title.text = page?.name
            val tags = page?.tags ?: emptyList()
            binding.notes.text = page?.notes
            val image = page?.images?.firstOrNull()
            binding.image.setImageDrawable(
                image?.let { files.drawable(it) }
            )

            binding.fieldGuidePageTitle.subtitle.text = tags
                .filter { it.type == FieldGuidePageTagType.Classification }
                .getMostSpecific()
                ?.readableName()

            displayTags(
                tags.filter { it.type == FieldGuidePageTagType.Location },
                binding.locationsLabel,
                binding.locations
            )

            displayTags(
                tags.filter { it.type == FieldGuidePageTagType.Habitat },
                binding.habitatsLabel,
                binding.habitats
            )

            displayTags(
                tags.filter { it.type == FieldGuidePageTagType.ActivityPattern },
                binding.behaviorsLabel,
                binding.behaviors
            )

            displayTags(
                tags.filter { it.type == FieldGuidePageTagType.HumanInteraction },
                binding.humanInteractionsLabel,
                binding.humanInteractions
            )
        }
    }

    private fun displayTags(tags: List<FieldGuidePageTag>, label: TextView, layout: FlexboxLayout) {
        if (tags.isEmpty()) {
            label.isVisible = false
            layout.isVisible = false
            return
        }

        label.isVisible = true
        layout.isVisible = true
        layout.removeAllViews()

        val badgeColor =
            Resources.getAndroidColorAttr(requireContext(), android.R.attr.colorBackgroundFloating)
        val foregroundColor = Resources.androidTextColorPrimary(requireContext())
        val margin = Resources.dp(requireContext(), 8f).toInt()

        for (tag in tags) {
            val tagView = Badge(requireContext(), null).apply {
                statusImage.isVisible = false
                statusText.textSize = 12f
                setStatusText(tag.readableName())
                statusText.setTextColor(foregroundColor)
                setBackgroundTint(badgeColor)
                layoutParams = FlexboxLayout.LayoutParams(
                    FlexboxLayout.LayoutParams.WRAP_CONTENT,
                    FlexboxLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, margin, margin)
                }
            }
            layout.addView(tagView)
        }
    }

}