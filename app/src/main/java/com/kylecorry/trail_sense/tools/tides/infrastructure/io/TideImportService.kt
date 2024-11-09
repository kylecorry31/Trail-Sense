package com.kylecorry.trail_sense.tools.tides.infrastructure.io

import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.luna.coroutines.onIO
import com.kylecorry.trail_sense.shared.io.ExternalUriService
import com.kylecorry.trail_sense.shared.io.ImportService
import com.kylecorry.trail_sense.shared.io.IntentUriPicker
import com.kylecorry.trail_sense.shared.io.UriPicker
import com.kylecorry.trail_sense.shared.io.UriService
import com.kylecorry.trail_sense.tools.tides.domain.TideTable

class TideImportService(private val uriPicker: UriPicker, private val uriService: UriService) :
    ImportService<TideTable> {
    override suspend fun import(): TideTable? = onIO {
        val uri = uriPicker.open(
            listOf(
                "application/xml",
                // TODO: Add support for JSON
//            "application/json",
                // TODO: Add support for CSV
//            "text/csv",
//            "application/csv",
//            "text/comma-separated-values",
//            "text/plain"

            )
        ) ?: return@onIO null
        val stream = uriService.inputStream(uri) ?: return@onIO null
        val parsers: List<TideTableParser> = listOf(NoaaHcAndMetadataTideTableConverter())
        stream.use {
            return@use parsers.firstNotNullOf { parser ->
                parser.parse(it)
            }
        }
    }

    companion object {
        fun create(fragment: AndromedaFragment): TideImportService {
            return TideImportService(
                IntentUriPicker(fragment, fragment.requireContext()),
                ExternalUriService(fragment.requireContext())
            )
        }
    }
}