package com.kylecorry.trail_sense.tools.experimentation

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.trail_sense.databinding.FragmentExperimentationBinding
import com.kylecorry.trail_sense.shared.io.DeleteTempFilesCommand
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.species_catalog.Species

class ExperimentationFragment : BoundFragment<FragmentExperimentationBinding>() {

    private var species by state<Species?>(null)
    private val importer by lazy { SpeciesImportService.create(this) }
    private val files by lazy { FileSubsystem.getInstance(requireContext()) }

    override fun generateBinding(
        layoutInflater: LayoutInflater, container: ViewGroup?
    ): FragmentExperimentationBinding {
        return FragmentExperimentationBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.text.movementMethod = LinkMovementMethod.getInstance()
        binding.text.autoLinkMask = Linkify.WEB_URLS
        inBackground(BackgroundMinimumState.Created) {
            species = importer.import()
        }
        binding.title.setOnClickListener {
            inBackground(BackgroundMinimumState.Created) {
                species = importer.import()
                // TODO: Ask the user if they want to import them, if they do copy the files to local storage and delete temp files
            }
        }
    }

    override fun onUpdate() {
        super.onUpdate()
        effect2(species) {
            binding.title.title.text = species?.name
            binding.text.text = species?.notes
            binding.title.subtitle.text = species?.tags?.joinToString(", ")
            if (species?.images?.isNotEmpty() == true) {
                binding.image.setImageURI(files.uri(species?.images?.firstOrNull() ?: ""))
            } else {
                binding.image.setImageBitmap(null)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        inBackground {
            DeleteTempFilesCommand(requireContext()).execute()
        }
    }

}