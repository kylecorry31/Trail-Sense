package com.kylecorry.trail_sense.tools.field_guide.infrastructure

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
import com.kylecorry.trail_sense.shared.withId
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePage
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePageTag
import java.io.InputStream
import java.util.Base64

class SpeciesImportService(
    private val uriPicker: UriPicker,
    private val uriService: UriService,
    private val files: FileSubsystem
) :
    ImportService<List<FieldGuidePage>> {

    override suspend fun import(): List<FieldGuidePage>? = onIO {
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

    private suspend fun parseZip(stream: InputStream): List<FieldGuidePage>? {
        val root = files.createTempDirectory()
        ZipUtils.unzip(stream, root, MAX_ZIP_FILE_COUNT)

        // Parse each file as a JSON file
        val species = mutableListOf<FieldGuidePage>()

        for (file in root.listFiles() ?: return null) {
            if (file.extension == "json") {
                species.addAll(parseJson(file.inputStream()) ?: emptyList())
            }
        }

        return species
    }

    private suspend fun parseJson(stream: InputStream): List<FieldGuidePage>? {
        val json = stream.bufferedReader().use { it.readText() }
        return try {
            val parsed = JsonConvert.fromJson<SpeciesJson>(json) ?: return null
            val images = parsed.images.map { saveImage(it) }
            listOf(
                FieldGuidePage(
                    0,
                    parsed.name,
                    images,
                    parsed.tags.mapNotNull { FieldGuidePageTag.entries.withId(it) },
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
        var tags: List<Long> = emptyList()
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