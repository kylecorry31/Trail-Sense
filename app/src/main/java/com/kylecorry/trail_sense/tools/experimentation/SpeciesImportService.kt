package com.kylecorry.trail_sense.tools.experimentation

import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.andromeda.json.JsonConvert
import com.kylecorry.luna.coroutines.onIO
import com.kylecorry.trail_sense.shared.io.ExternalUriService
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.io.ImportService
import com.kylecorry.trail_sense.shared.io.IntentUriPicker
import com.kylecorry.trail_sense.shared.io.UriPicker
import com.kylecorry.trail_sense.shared.io.UriService
import com.kylecorry.trail_sense.tools.species_catalog.Species
import java.io.InputStream
import java.util.Base64

class SpeciesImportService(
    private val uriPicker: UriPicker,
    private val uriService: UriService,
    private val files: FileSubsystem
) :
    ImportService<Species> {
    override suspend fun import(): Species? = onIO {
        val uri = uriPicker.open(
            listOf(
                "application/json"
            )
        ) ?: return@onIO null
        val stream = uriService.inputStream(uri) ?: return@onIO null
        stream.use {
            // TODO: Parse from zip (write images to a temp directory)
            return@use parseJson(it)
        }
    }

    private suspend fun parseJson(stream: InputStream): Species? {
        val json = stream.bufferedReader().use { it.readText() }
        return try {
            val parsed = JsonConvert.fromJson<SpeciesJson>(json) ?: return null
            val images = parsed.images.map { saveImage(it) }
            Species(
                0,
                parsed.name,
                images,
                parsed.tags,
                parsed.notes ?: ""
            )
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun saveImage(b64: String): String {
        val file = files.createTemp("webp")
        val bytes = Base64.getDecoder().decode(b64)
        file.outputStream().use {
            it.write(bytes)
        }
        return files.getLocalPath(file)
    }

    class SpeciesJson {
        var name: String = ""
        var images: List<String> = emptyList()
        var tags: List<String> = emptyList()
        var notes: String? = null
    }

    companion object {
        fun create(fragment: AndromedaFragment): SpeciesImportService {
            return SpeciesImportService(
                IntentUriPicker(fragment, fragment.requireContext()),
                ExternalUriService(fragment.requireContext()),
                FileSubsystem.getInstance(fragment.requireContext())
            )
        }
    }
}