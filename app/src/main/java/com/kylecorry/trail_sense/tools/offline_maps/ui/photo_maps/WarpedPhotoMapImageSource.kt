package com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Point
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.util.Size
import com.kylecorry.andromeda.bitmaps.operations.CorrectPerspective
import com.kylecorry.andromeda.bitmaps.operations.Resize
import com.kylecorry.andromeda.bitmaps.operations.applyOperationsOrNull
import com.kylecorry.andromeda.views.subscaleview.decoder.ImageDecoder
import com.kylecorry.andromeda.views.subscaleview.decoder.ImageRegionDecoder
import com.kylecorry.sol.math.geometry.Size as SolSize
import com.kylecorry.trail_sense.shared.extensions.toAndroidSize
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.PhotoMapWarp
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PercentBounds as DomainPercentBounds
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object WarpedPhotoMapImageSource {
    private const val SCHEME = "trail-sense-photo-map-warp"
    private val sources = ConcurrentHashMap<String, Source>()

    fun register(filename: String, rawSize: SolSize, warpedSize: SolSize, bounds: DomainPercentBounds): Uri {
        val id = UUID.randomUUID().toString()
        sources[id] = Source(filename, rawSize, warpedSize, bounds)
        return Uri.parse("$SCHEME://$id")
    }

    fun get(uri: Uri): Source? {
        if (uri.scheme != SCHEME) {
            return null
        }
        return sources[uri.host]
    }

    data class Source(
        val filename: String,
        val rawSize: SolSize,
        val warpedSize: SolSize,
        val bounds: DomainPercentBounds
    )
}

class WarpedPhotoMapImageRegionDecoder : ImageRegionDecoder {

    private var source: WarpedPhotoMapImageSource.Source? = null
    private var decoder: BitmapRegionDecoder? = null
    private var sourceTransform: Matrix? = null

    override fun init(context: Context, uri: Uri): Point {
        val source = WarpedPhotoMapImageSource.get(uri) ?: error("Unknown warped photo map URI")
        this.source = source

        val files = FileSubsystem.getInstance(context)
        decoder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            BitmapRegionDecoder.newInstance(files.get(source.filename).path)
        } else {
            @Suppress("DEPRECATION")
            BitmapRegionDecoder.newInstance(files.get(source.filename).path, false)
        }
        sourceTransform = getSourceTransform(source)
        return Point(source.warpedSize.width.toInt(), source.warpedSize.height.toInt())
    }

    override fun decodeRegion(sRect: Rect, sampleSize: Int): Bitmap {
        val source = source ?: error("Decoder not initialized")
        val decoder = decoder ?: error("Decoder not initialized")
        val transform = sourceTransform ?: error("Decoder not initialized")

        val virtualCorners = PhotoMapWarp.corners(sRect)
        val sourceCorners = PhotoMapWarp.map(transform, virtualCorners)

        val boundedRegion = PhotoMapWarp.exactRegion(
            PhotoMapWarp.boundingRect(sourceCorners),
            source.rawSize.toAndroidSize()
        )
        if (boundedRegion.width() <= 0 || boundedRegion.height() <= 0) {
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).also {
                it.eraseColor(Color.TRANSPARENT)
            }
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.RGB_565
        }

        val bitmap = decoder.decodeRegion(boundedRegion, options)
            ?: error("Unable to decode warped photo map region")

        val width = (sRect.width() / sampleSize).coerceAtLeast(1)
        val height = (sRect.height() / sampleSize).coerceAtLeast(1)
        return bitmap.applyOperationsOrNull(
            CorrectPerspective(
                PhotoMapWarp.perspectiveBounds(sourceCorners, boundedRegion),
                outputSize = Size(width, height)
            ),
            Resize(Size(width, height), true)
        ) ?: bitmap
    }

    override fun isReady(): Boolean {
        return decoder?.isRecycled == false
    }

    override fun recycle() {
        decoder?.recycle()
        decoder = null
    }
}

class WarpedPhotoMapImageDecoder : ImageDecoder {
    override fun decode(context: Context, uri: Uri): Bitmap {
        val source = WarpedPhotoMapImageSource.get(uri) ?: error("Unknown warped photo map URI")
        val decoder = WarpedPhotoMapImageRegionDecoder()
        return try {
            decoder.init(context, uri)
            decoder.decodeRegion(
                Rect(0, 0, source.warpedSize.width.toInt(), source.warpedSize.height.toInt()),
                1
            )
        } finally {
            decoder.recycle()
        }
    }
}

private fun getSourceTransform(source: WarpedPhotoMapImageSource.Source): Matrix {
    return PhotoMapWarp.sourceTransform(source.rawSize, source.warpedSize, source.bounds)
        ?: error("Unable to invert photo map warp")
}
