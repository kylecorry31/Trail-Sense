package com.kylecorry.trail_sense.tools.field_guide.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentCreateFieldGuidePageBinding
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.extensions.promptIfUnsavedChanges
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.views.MaterialMultiSpinnerView
import com.kylecorry.andromeda.views.list.ListMenuItem
import com.kylecorry.trail_sense.shared.views.PhotoUploadPagerAdapter
import com.kylecorry.trail_sense.shared.withId
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePage
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuideService
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePageTag
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePageTagType
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideRepo

class CreateFieldGuidePageFragment : BoundFragment<FragmentCreateFieldGuidePageBinding>() {

    private val repo by lazy { FieldGuideRepo.getInstance(requireContext()) }
    private val service by lazy { getAppService<FieldGuideService>() }
    private val tagNameMapper by lazy { FieldGuideTagNameMapper(requireContext()) }
    private val files by lazy { FileSubsystem.getInstance(requireContext()) }

    private var originalPage by state(FieldGuidePage(0))
    private var page by state(originalPage)

    private var backCallback: OnBackPressedCallback? = null

    private val photoAdapter by lazy {
        PhotoUploadPagerAdapter("field_guide", this::onPhotoAdded, this::getPhotoMenuItems)
    }
    private var pendingPhotoPosition = 0

    // Tags
    private val tags =
        FieldGuidePageTag.entries.sortedWith(compareBy({ it.type.ordinal }, { it.ordinal }))

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCreateFieldGuidePageBinding {
        return FragmentCreateFieldGuidePageBinding.inflate(layoutInflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val pageId = it.getLong(ARG_PAGE_ID, 0L)
            if (pageId != 0L) {
                inBackground {
                    repo.getPage(pageId)?.let {
                        originalPage = it
                        page = it
                    }
                }
            }

            val tag = FieldGuidePageTag.entries.withId(it.getLong(ARG_CLASSIFICATION_ID, 0L))

            if (tag != null) {
                page = page.copy(directTags = page.directTags + tag)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.createFieldGuidePageTitle.rightButton.setOnClickListener {
            save()
        }

        // Fields
        binding.name.setOnTextChangeListener {
            page = page.copy(name = it.toString())
        }

        binding.scientificName.setOnTextChangeListener {
            page = page.copy(scientificName = it?.toString()?.takeIf(String::isNotBlank))
        }

        binding.notes.addTextChangedListener {
            page = page.copy(notes = it.toString())
        }

        binding.photoUploadPager.adapter = photoAdapter
        binding.photoUploadPager.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updatePhotoArrows()
            }
        })

        binding.previousPhotoButton.setOnClickListener {
            binding.photoUploadPager.setCurrentItem(binding.photoUploadPager.currentItem - 1, true)
        }

        binding.nextPhotoButton.setOnClickListener {
            binding.photoUploadPager.setCurrentItem(binding.photoUploadPager.currentItem + 1, true)
        }

        initializeTags(
            binding.tagLocations,
            getString(R.string.location),
            FieldGuidePageTagType.Location
        )

        initializeTags(
            binding.tagHabitats,
            getString(R.string.habitat),
            FieldGuidePageTagType.Habitat
        )

        initializeTags(
            binding.tagClassifications,
            getString(R.string.classification),
            FieldGuidePageTagType.Classification
        )

        initializeTags(
            binding.tagActivityPatterns,
            getString(R.string.activity_pattern),
            FieldGuidePageTagType.ActivityPattern
        )

        initializeTags(
            binding.tagHumanInteractions,
            getString(R.string.human_interaction),
            FieldGuidePageTagType.HumanInteraction
        )

        backCallback = promptIfUnsavedChanges(this::hasChanges)
    }

    override fun onUpdate() {
        super.onUpdate()

        // Original content
        useEffect(originalPage) {
            binding.name.text = originalPage.name
            binding.scientificName.text = originalPage.scientificName
            binding.notes.setText(originalPage.notes)
        }

        useEffect(page.tags) {
            setTags(binding.tagLocations, page.directTags, FieldGuidePageTagType.Location)
            setTags(binding.tagHabitats, page.directTags, FieldGuidePageTagType.Habitat)
            setTags(
                binding.tagClassifications,
                page.directTags,
                FieldGuidePageTagType.Classification
            )
            setTags(
                binding.tagActivityPatterns,
                page.directTags,
                FieldGuidePageTagType.ActivityPattern
            )
            setTags(
                binding.tagHumanInteractions,
                page.directTags,
                FieldGuidePageTagType.HumanInteraction
            )
        }

        useEffect(page.images) {
            photoAdapter.setPhotos(page.images)
            binding.photoUploadPager.setCurrentItem(pendingPhotoPosition, false)
            updatePhotoArrows()
        }
    }

    private fun getPhotoMenuItems(position: Int): List<ListMenuItem> {
        return listOfNotNull(
            // The first photo is already the default
            if (position > 0) {
                ListMenuItem(getString(R.string.set_as_default_photo)) {
                    val images = page.images.toMutableList()
                    images.add(0, images.removeAt(position))
                    onPhotosChanged(images)
                }
            } else {
                null
            },
            ListMenuItem(getString(R.string.delete)) {
                Alerts.dialog(
                    requireContext(),
                    getString(R.string.delete)
                ) { cancelled ->
                    if (!cancelled) {
                        onPhotosChanged(page.images.filterIndexed { index, _ -> index != position })
                    }
                }
            }
        )
    }

    private fun onPhotoAdded(path: String) {
        onPhotosChanged(page.images + path)
    }

    private fun onPhotosChanged(images: List<String>) {
        val existing = page.images
        pendingPhotoPosition = when {
            // Show the photo which was just added
            images.size > existing.size -> images.lastIndex
            // Show the photo which took the place of the deleted one
            images.size < existing.size -> binding.photoUploadPager.currentItem.coerceAtMost(images.size)
            // Show the new default photo
            else -> 0
        }

        inBackground {
            deleteUnsavedImages(images)
            page = page.copy(images = images)
        }
    }

    private fun updatePhotoArrows() {
        val position = binding.photoUploadPager.currentItem
        // There is always a page for adding a photo, so there is nothing to page through until
        // there is at least one photo
        val hasMultiplePages = photoAdapter.itemCount > 1
        binding.previousPhotoButton.isVisible = hasMultiplePages
        binding.nextPhotoButton.isVisible = hasMultiplePages
        binding.previousPhotoButton.isEnabled = position > 0
        binding.nextPhotoButton.isEnabled = position < photoAdapter.itemCount - 1

        // The last page is used to add a photo rather than to view one
        val photoCount = photoAdapter.itemCount - 1
        binding.photoPosition.isVisible = hasMultiplePages && position < photoCount
        binding.photoPosition.text = getString(R.string.image_index, position + 1, photoCount)
    }

    private fun save() {
        inBackground {
            service.savePage(page)
            onMain {
                backCallback?.remove()
                findNavController().navigateUp()
            }
        }
    }

    private fun hasChanges(): Boolean {
        return originalPage != page
    }

    private suspend fun deleteUnsavedImages(newImages: List<String>) = onIO {
        val originalImages = originalPage.images
        val imagesToDelete = page.images.filter { it !in newImages && it !in originalImages }
        imagesToDelete.forEach { files.delete(it) }
    }

    private fun initializeTags(
        view: MaterialMultiSpinnerView,
        hint: String,
        type: FieldGuidePageTagType
    ) {
        val tagsOfType = tags.filter { it.type == type }
        view.setHint(hint)
        view.setItems(tagsOfType.map { tagNameMapper.getName(it) })
        view.setOnSelectionChangeListener {
            page =
                page.copy(directTags = page.directTags.filter { it.type != type } + it.map { tagsOfType[it] })
        }
    }

    private fun setTags(
        view: MaterialMultiSpinnerView,
        selection: List<FieldGuidePageTag>,
        type: FieldGuidePageTagType
    ) {
        val tagsOfType = tags.filter { it.type == type }
        val selectionOfType = selection.filter { it.type == type }
        view.setSelection(selectionOfType.map { tagsOfType.indexOf(it) })
    }

    companion object {
        private const val ARG_PAGE_ID = "page_id"
        private const val ARG_CLASSIFICATION_ID = "classification_id"
    }
}
