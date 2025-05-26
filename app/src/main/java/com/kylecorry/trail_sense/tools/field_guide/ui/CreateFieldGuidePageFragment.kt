package com.kylecorry.trail_sense.tools.field_guide.ui

import android.net.Uri
import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.net.toFile
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.exifinterface.media.ExifInterface
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.bitmaps.BitmapUtils.rotate
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.luna.coroutines.onIO
import com.kylecorry.luna.coroutines.onMain
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentCreateFieldGuidePageBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.extensions.promptIfUnsavedChanges
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.io.IntentUriPicker
import com.kylecorry.trail_sense.shared.views.MaterialMultiSpinnerView
import com.kylecorry.trail_sense.shared.withId
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePage
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePageTag
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePageTagType
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideRepo
import java.util.UUID

class CreateFieldGuidePageFragment : BoundFragment<FragmentCreateFieldGuidePageBinding>() {

    private val repo by lazy { FieldGuideRepo.getInstance(requireContext()) }
    private val tagNameMapper by lazy { FieldGuideTagNameMapper(requireContext()) }
    private val files by lazy { FileSubsystem.getInstance(requireContext()) }
    private val uriPicker by lazy { IntentUriPicker(this, requireContext()) }

    private var originalPage by state(FieldGuidePage(0))
    private var page by state(originalPage)

    private var backCallback: OnBackPressedCallback? = null

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
        CustomUiUtils.setButtonState(binding.createFieldGuidePageTitle.rightButton, true)
        binding.createFieldGuidePageTitle.rightButton.setOnClickListener {
            save()
        }

        // Fields
        binding.name.setOnTextChangeListener {
            page = page.copy(name = it.toString())
        }

        binding.notes.addTextChangedListener {
            page = page.copy(notes = it.toString())
        }

        CustomUiUtils.setButtonState(binding.deleteImageButton, false)
        binding.deleteImageButton.setOnClickListener {
            inBackground {
                deleteImage()
            }
        }

        binding.takePhotoButton.setOnClickListener {
            inBackground {
                val uri = CustomUiUtils.takePhoto(this@CreateFieldGuidePageFragment)
                uploadPhoto(uri)
            }
        }

        binding.selectPhotoButton.setOnClickListener {
            inBackground(BackgroundMinimumState.Created) {
                val uri = uriPicker.open(listOf("image/*"))
                uploadPhoto(uri)
            }
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

        val image = page.images.firstOrNull()
        useEffect(image) {
            binding.imageHolder.isVisible = image != null
            if (image != null) {
                binding.image.setImageURI(files.uri(image))
            } else {
                binding.image.setImageURI(null)
            }
        }
    }

    private fun save() {
        inBackground {
            repo.add(page)
            onMain {
                backCallback?.remove()
                findNavController().navigateUp()
            }
        }
    }

    private fun hasChanges(): Boolean {
        return originalPage != page
    }

    private suspend fun uploadPhoto(uri: Uri?) = onIO {
        uri ?: return@onIO

        // Delete unsaved images
        val originalImages = originalPage.images
        val imagesToDelete = page.images.filter { it !in originalImages }
        imagesToDelete.forEach { files.delete(it) }

        // Copy over the new image
        val file = files.copyToLocal(uri, "field_guide", "${UUID.randomUUID()}.webp") ?: return@onIO
        val path = files.getLocalPath(file)

        // Reduce the resolution
        var rotation = 0
        tryOrLog {
            val exif = ExifInterface(uri.toFile())
            rotation = exif.rotationDegrees
        }
        val bmp = files.bitmap(path, Size(500, 500)) ?: return@onIO
        val rotated = if (rotation != 0) {
            bmp.rotate(rotation.toFloat())
        } else {
            bmp
        }

        if (rotated != bmp) {
            bmp.recycle()
        }

        files.save(path, rotated, 75, true)

        // Update the page
        page = page.copy(images = listOf(path))
    }

    private suspend fun deleteImage() = onIO {
        val originalImages = originalPage.images
        val imagesToDelete = page.images.filter { it !in originalImages }
        imagesToDelete.forEach { files.delete(it) }
        page = page.copy(images = emptyList())
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