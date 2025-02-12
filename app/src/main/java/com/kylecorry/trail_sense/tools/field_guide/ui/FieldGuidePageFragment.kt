package com.kylecorry.trail_sense.tools.field_guide.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.text.method.LinkMovementMethodCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.google.android.flexbox.FlexboxLayout
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.views.badge.Badge
import com.kylecorry.luna.coroutines.CoroutineQueueRunner
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentFieldGuidePageBinding
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePage
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePageTag
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePageTagType
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideRepo

class FieldGuidePageFragment : BoundFragment<FragmentFieldGuidePageBinding>() {

    private val repo by lazy { FieldGuideRepo.getInstance(requireContext()) }
    private val files by lazy { FileSubsystem.getInstance(requireContext()) }
    private val tagNameMapper by lazy { FieldGuideTagNameMapper(requireContext()) }

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.notes.movementMethod = LinkMovementMethodCompat.getInstance()
        binding.fieldGuidePageTitle.rightButton.isVisible = false
        binding.fieldGuidePageTitle.rightButton.setOnClickListener {
            findNavController().navigate(
                R.id.createFieldGuidePageFragment,
                bundleOf("page_id" to page?.id)
            )
        }
    }

    override fun onUpdate() {
        super.onUpdate()
        useEffect(pageId) {
            inBackground {
                if (pageId == null) {
                    loader.replace { page = null }
                } else {
                    loader.replace { page = repo.getPage(pageId ?: return@replace) }
                }
            }
        }

        useEffect(page) {
            binding.fieldGuidePageTitle.rightButton.isVisible = page?.isReadOnly == false
            binding.fieldGuidePageTitle.title.text = page?.name
            binding.notes.text = page?.notes
            val image = page?.images?.firstOrNull()
            binding.image.setImageDrawable(
                image?.let { files.drawable(it) }
            )

            displayTags()
        }
    }

    override fun onResume() {
        super.onResume()
        resetHooks()
    }

    private val tagTypeColorMap = mapOf(
        FieldGuidePageTagType.Location to AppColor.Gray,
        FieldGuidePageTagType.Habitat to AppColor.Green,
        FieldGuidePageTagType.Classification to AppColor.Blue,
        FieldGuidePageTagType.ActivityPattern to AppColor.Yellow,
        FieldGuidePageTagType.HumanInteraction to AppColor.Brown
    )

    private fun displayTags() {
        val tags =
            (page?.tags ?: emptyList()).sortedWith(compareBy({ it.type.ordinal }, { it.ordinal }))
        if (tags.isEmpty()) {
            binding.tags.isVisible = false
            return
        }

        binding.tags.isVisible = true
        binding.tags.removeAllViews()

        val margin = Resources.dp(requireContext(), 8f).toInt()

        for (tag in tags) {
            val badgeColor = (tagTypeColorMap[tag.type] ?: AppColor.Gray).color
            val foregroundColor = Colors.mostContrastingColor(Color.WHITE, Color.BLACK, badgeColor)
            val tagView = Badge(requireContext(), null).apply {
                statusImage.isVisible = false
                statusText.textSize = 12f
                setStatusText(tagNameMapper.getName(tag))
                statusText.setTextColor(foregroundColor)
                setBackgroundTint(badgeColor)
                layoutParams = FlexboxLayout.LayoutParams(
                    FlexboxLayout.LayoutParams.WRAP_CONTENT,
                    FlexboxLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, margin, margin)
                }
            }
            tagView.setOnClickListener { onTagClicked(tag) }
            binding.tags.addView(tagView)
        }
    }

    private fun onTagClicked(tag: FieldGuidePageTag) {
        // TODO: Handle tag click
    }

}