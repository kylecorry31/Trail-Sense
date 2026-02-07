package com.kylecorry.trail_sense.tools.field_guide.map_layers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.Bundle
import android.util.Size
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonFeatureCollection
import com.kylecorry.andromeda.geojson.GeoJsonObject
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.point
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.sources.GeoJsonSource
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconIcon
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePage
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePageTag
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePageTagType
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideRepo

class FieldGuideSightingGeoJsonSource : GeoJsonSource {

    private val repo = AppServiceRegistry.get<FieldGuideRepo>()
    private val files = AppServiceRegistry.get<FileSubsystem>()
    var nameFormat = ""
    var context: Context? = null
    private val size = 12f
    private val imageSize = size * 1.5f
    private val bitmapCache = mutableMapOf<Long, Bitmap?>()

    private val tagIconMap = mapOf(
        FieldGuidePageTag.Plant to BeaconIcon.Plant,
        FieldGuidePageTag.Fungus to BeaconIcon.Fungus,
        FieldGuidePageTag.Animal to BeaconIcon.Animal,
        FieldGuidePageTag.Mammal to BeaconIcon.Mammal,
        FieldGuidePageTag.Bird to BeaconIcon.Bird,
        FieldGuidePageTag.Reptile to BeaconIcon.Reptile,
        FieldGuidePageTag.Amphibian to BeaconIcon.Amphibian,
        FieldGuidePageTag.Fish to BeaconIcon.Fish,
        FieldGuidePageTag.Invertebrate to BeaconIcon.Invertebrate,
        FieldGuidePageTag.Rock to BeaconIcon.Rock,
        FieldGuidePageTag.Weather to BeaconIcon.Weather,
        FieldGuidePageTag.Other to BeaconIcon.Information
    )

    override suspend fun load(
        bounds: CoordinateBounds,
        zoom: Int,
        params: Bundle
    ): GeoJsonObject {
        val preferences = params.getBundle(GeoJsonSource.PARAM_PREFERENCES)
        val showImages =
            preferences?.getBoolean(FieldGuideSightingLayer.PREFERENCE_SHOW_IMAGES, false) ?: false
        if (nameFormat.isEmpty()) {
            nameFormat = context?.getString(R.string.sighting_label) ?: ""
        }
        val pages = repo.getAllPages()

        val allSightings = pages
            .flatMap { page ->
                page.sightings.map { sighting ->
                    Pair(sighting, page)
                }
            }
            .filter { (sighting, _) -> sighting.location != null && sighting.showOnMap }

        // Cache pruning
        val currentPageIds = allSightings.map { it.second.id }.toSet()
        val keysToRemove = bitmapCache.keys.filter { it !in currentPageIds }
        keysToRemove.forEach { bitmapCache.remove(it) }

        val collection = GeoJsonFeatureCollection(
            allSightings.map { (sighting, page) ->
                val icon = getIconForPage(page)
                val bitmap = if (showImages) getBitmapForPage(page) else null
                val point = GeoJsonFeature.point(
                    sighting.location!!,
                    sighting.id,
                    nameFormat.format(page.name),
                    color = Color.BLACK,
                    icon = if (bitmap == null) icon.id else null,
                    iconSize = if (bitmap == null) size * 0.75f else imageSize,
                    iconColor = if (bitmap == null) Color.WHITE else null,
                    markerShape = if (bitmap == null) "circle" else null,
                    size = size,
                    isClickable = true,
                    layerId = FieldGuideSightingLayer.LAYER_ID,
                    bitmap = bitmap,
                    additionalProperties = mapOf(
                        FieldGuideSightingLayer.PROPERTY_PAGE_ID to page.id
                    )
                )
                point
            }
        )

        return collection
    }

    private fun getBitmapForPage(page: FieldGuidePage): Bitmap? {
        if (page.images.isEmpty()) {
            return null
        }

        if (bitmapCache.containsKey(page.id)) {
            return bitmapCache[page.id]
        }

        // Double the size for increased resolution
        val sizePixels = Resources.dp(context ?: return null, imageSize * 2).toInt()

        val sourceBitmap = tryOrDefault(null) {
            files.bitmap(page.images.first(), Size(sizePixels, sizePixels))
        }

        val circularBitmap = sourceBitmap?.let { cropToCircle(it, sizePixels) }
        bitmapCache[page.id] = circularBitmap
        return circularBitmap
    }

    private fun cropToCircle(source: Bitmap, size: Int): Bitmap {
        val output = createBitmap(size, size)
        val canvas = Canvas(output)

        val path = Path()
        path.addCircle(size / 2f, size / 2f, size / 2f, Path.Direction.CW)
        canvas.clipPath(path)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val scaledSource = source.scale(size, size)
        canvas.drawBitmap(scaledSource, 0f, 0f, paint)
        scaledSource.recycle()

        return output
    }

    private fun getIconForPage(page: FieldGuidePage): BeaconIcon {
        val classificationTags = page.tags.filter {
            it.type == FieldGuidePageTagType.Classification
        }

        val lowestTag = classificationTags
            .filter { tagIconMap.containsKey(it) }
            .maxByOrNull { it.parentId != null }

        return tagIconMap[lowestTag] ?: BeaconIcon.Information
    }
}
