package com.kylecorry.trail_sense.tools.photo_maps.ui

import android.content.Context
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.toCoordinate

class PhotoMapView : BasePhotoMapView {

    var onMapLongClick: ((coordinate: Coordinate) -> Unit)? = null

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    override fun onLongPress(e: MotionEvent) {
        super.onLongPress(e)
        val viewNoRotation = toViewNoRotation(PointF(e.x, e.y)) ?: return
        val coordinate = toCoordinate(toPixel(viewNoRotation))
        onMapLongClick?.invoke(coordinate)
    }

    override fun onSinglePress(e: MotionEvent) {
        super.onSinglePress(e)
        val viewNoRotation = toViewNoRotation(PointF(e.x, e.y))

        // TODO: Pass in a coordinate rather than a pixel (convert radius to meters)
        if (viewNoRotation != null) {
            for (layer in layers.reversed()) {
                val handled = layer.onClick(
                    drawer,
                    this@PhotoMapView,
                    PixelCoordinate(viewNoRotation.x, viewNoRotation.y)
                )
                if (handled) {
                    break
                }
            }
        }
    }
}