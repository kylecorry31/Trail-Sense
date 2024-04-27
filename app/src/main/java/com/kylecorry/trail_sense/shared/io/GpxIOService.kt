package com.kylecorry.trail_sense.shared.io

import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.gpx.GPXData
import com.kylecorry.andromeda.gpx.GPXParser

class GpxIOService(private val uriPicker: UriPicker, private val uriService: UriService) :
    IOService<GPXData> {
    override suspend fun export(data: GPXData, filename: String): Boolean = onIO {
        val uri = uriPicker.create(filename, "application/gpx+xml") ?: return@onIO false
        val gpxString = GPXParser.toGPX(data, "Trail Sense")
        uriService.write(uri, gpxString)
    }

    override suspend fun import(): GPXData? = onIO {
        val uri = uriPicker.open(listOf("*/*")) ?: return@onIO null
        val stream = uriService.inputStream(uri) ?: return@onIO null
        GPXParser.parse(stream)
    }

}