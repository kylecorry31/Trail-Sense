package com.kylecorry.trail_sense.tools.experimentation

import com.kylecorry.andromeda.files.ZipUtils
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
    ImportService<List<Species>> {

    // TODO: Use an enum for tags
    private val idToTag = mapOf(
        "Africa" to 1,
        "Antarctica" to 2,
        "Asia" to 3,
        "Australia" to 4,
        "Europe" to 5,
        "North America" to 6,
        "South America" to 7,
        "Plant" to 8,
        "Animal" to 9,
        "Fungus" to 10,
        "Bird" to 11,
        "Mammal" to 12,
        "Reptile" to 13,
        "Amphibian" to 14,
        "Fish" to 15,
        "Insect" to 16,
        "Arachnid" to 17,
        "Crustacean" to 18,
        "Mollusk" to 19,
        "Forest" to 20,
        "Desert" to 21,
        "Grassland" to 22,
        "Wetland" to 23,
        "Mountain" to 24,
        "Urban" to 25,
        "Marine" to 26,
        "Freshwater" to 27,
        "Cave" to 28,
        "Tundra" to 29,
    ).map { it.value to it.key }.toMap()

    override suspend fun import(): List<Species>? = onIO {
        val uri = uriPicker.open(
            listOf(
                "application/json",
                "application/zip"
            )
        ) ?: return@onIO null
        val stream = uriService.inputStream(uri) ?: return@onIO null
        stream.use {
            if (files.getMimeType(uri) == "application/json") {
                return@use parseJson(it)
            }
            return@use parseZip(it)
        }
    }

    private suspend fun parseZip(stream: InputStream): List<Species>? {
        val root = files.createTempDirectory()
        ZipUtils.unzip(stream, root, MAX_ZIP_FILE_COUNT)

        // Parse each file as a JSON file
        val species = mutableListOf<Species>()

        for (file in root.listFiles() ?: return null) {
            if (file.extension == "json") {
                species.addAll(parseJson(file.inputStream()) ?: emptyList())
            }
        }

        return species
    }

    private suspend fun parseJson(stream: InputStream): List<Species>? {
        val json = stream.bufferedReader().use { it.readText() }
        return try {
            val parsed = JsonConvert.fromJson<SpeciesJson>(json) ?: return null
            val images = parsed.images.map { saveImage(it) }
            listOf(
                Species(
                    0,
                    parsed.name,
                    images,
                    parsed.tags.mapNotNull { idToTag[it] },
                    parsed.notes ?: ""
                )
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
        var tags: List<Int> = emptyList()
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

        private const val MAX_ZIP_FILE_COUNT = 10000
    }
}