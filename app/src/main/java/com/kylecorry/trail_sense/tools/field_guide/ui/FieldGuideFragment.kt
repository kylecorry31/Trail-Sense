package com.kylecorry.trail_sense.tools.field_guide.ui

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.kylecorry.andromeda.alerts.dialog
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.views.list.AsyncListIcon
import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.trail_sense.databinding.FragmentFieldGuideBinding
import com.kylecorry.trail_sense.shared.io.DeleteTempFilesCommand
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.views.Views
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePage
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePageTag
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.BuiltInFieldGuide

class FieldGuideFragment : BoundFragment<FragmentFieldGuideBinding>() {

    private var species by state<List<FieldGuidePage>>(emptyList())
    private var filter by state("")
    private val files by lazy { FileSubsystem.getInstance(requireContext()) }

    private fun loadFromAssets(): List<FieldGuidePage> {
        return BuiltInFieldGuide.getFieldGuide(requireContext())
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater, container: ViewGroup?
    ): FragmentFieldGuideBinding {
        return FragmentFieldGuideBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        inBackground(BackgroundMinimumState.Created) {
            val tagOrder = listOf(
                FieldGuidePageTag.Plant,
                FieldGuidePageTag.Fungus,
                FieldGuidePageTag.Mammal,
                FieldGuidePageTag.Bird,
                FieldGuidePageTag.Reptile,
                FieldGuidePageTag.Amphibian,
                FieldGuidePageTag.Fish,
                FieldGuidePageTag.Insect,
                FieldGuidePageTag.Arachnid,
                FieldGuidePageTag.Crustacean,
                FieldGuidePageTag.Mollusk,
            )
            species = loadFromAssets().sortedWith(
                compareBy(
                    {
                        it.tags.minOfOrNull { tag ->
                            val order = tagOrder.indexOf(tag)
                            if (order == -1) tagOrder.size else order
                        }
                    },
                    { it.name })
            )
        }

        binding.search.setOnSearchListener {
            filter = it
        }
    }

    override fun onUpdate() {
        super.onUpdate()
        effect2(species, filter, lifecycleHookTrigger.onResume()) {
            val filteredSpecies = species.filter {
                it.name.lowercase().contains(filter.trim()) || it.tags.any { tag ->
                    tag.name.lowercase().contains(filter.trim())
                }
            }

            binding.list.setItems(filteredSpecies.map {
                val firstSentence = it.notes?.substringBefore(".")?.plus(".") ?: ""
                ListItem(
                    it.id,
                    it.name,
                    it.tags.joinToString(", ") + "\n\n" + firstSentence.take(200),
                    icon = AsyncListIcon(
                        viewLifecycleOwner,
                        { loadThumbnail(it) },
                        size = 48f,
                        scaleType = ImageView.ScaleType.CENTER_CROP,
                        clearOnPause = true
                    ),
                ) {
                    dialog(
                        it.name,
                        it.notes ?: "",
                        allowLinks = true,
                        contentView = Views.image(
                            requireContext(),
                            files.drawable(it.images.first()),
                            width = ViewGroup.LayoutParams.MATCH_PARENT,
                            height = Resources.dp(requireContext(), 200f).toInt()
                        ),
                        scrollable = true
                    )
                }
            })
        }
    }

    private suspend fun loadThumbnail(species: FieldGuidePage): Bitmap = onIO {
        val size = Resources.dp(requireContext(), 48f).toInt()
        try {
            files.bitmap(species.images.first(), Size(size, size)) ?: getDefaultThumbnail()
        } catch (e: Exception) {
            getDefaultThumbnail()
        }
    }

    private fun getDefaultThumbnail(): Bitmap {
        val size = Resources.dp(requireContext(), 48f).toInt()
        return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    }

    override fun onDestroy() {
        super.onDestroy()
        inBackground {
            DeleteTempFilesCommand(requireContext()).execute()
        }
    }
}