package com.kylecorry.trail_sense.shared.io

import com.kylecorry.andromeda.gpx.GPXData
import com.kylecorry.andromeda.gpx.GPXParser

class GpxIOService(private val uriPicker: UriPicker, private val uriService: UriService) :
    IOService<GPXData> {
    override suspend fun export(data: GPXData, filename: String): Boolean {
        val uri = uriPicker.create(filename, "application/gpx+xml") ?: return false
        val gpxString = GPXParser.toGPX(data, "Trail Sense")
        return uriService.write(uri, gpxString)
    }

    override suspend fun import(): GPXData? {
        val uri = uriPicker.open(listOf("*/*")) ?: return null
        val stream = uriService.inputStream(uri) ?: return null
        return GPXParser.parse(stream)
    }

}