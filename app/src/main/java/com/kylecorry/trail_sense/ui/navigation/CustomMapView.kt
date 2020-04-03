package com.kylecorry.trail_sense.ui.navigation

import android.content.Context
import androidx.preference.PreferenceManager
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.Coordinate
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider

class CustomMapView(private val map: MapView, startingLocation: Coordinate? = null) {

    private var marker: Marker? = null
    private val myLocationMarker: Marker

    init {
        if (startingLocation != null) {
            showLocation(startingLocation, defaultZoom)
        }
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)

        val mCompassOverlay = CompassOverlay(map.context, InternalCompassOrientationProvider(map.context), map)
        mCompassOverlay.enableCompass()
        map.overlays.add(mCompassOverlay)

        myLocationMarker = Marker(map)
        myLocationMarker.icon = map.context.getDrawable(R.drawable.ic_location)?.apply {
            setTint(map.context.getColor(R.color.colorPrimary))
        }
        myLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        map.overlays.add(myLocationMarker)
    }

    fun showLocation(location: Coordinate, zoom: Double = defaultZoom){
        map.controller.setZoom(zoom)
        map.controller.setCenter(GeoPoint(location.latitude, location.longitude))
    }

    fun showBeacon(location: Coordinate){
        hideBeacon()
        marker = Marker(map)
        marker?.icon = map.context.getDrawable(R.drawable.ic_beacon)?.apply {
            setTint(map.context.getColor(R.color.colorPrimary))
        }
        marker?.position = GeoPoint(location.latitude, location.longitude)
        marker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        map.overlays.add(marker)
    }

    fun hideBeacon(){
        if (marker != null){
            map.overlays.remove(marker)
            marker = null
        }
    }

    fun setMyLocation(location: Coordinate){
        myLocationMarker.position = GeoPoint(location.latitude, location.longitude)
    }

    fun downloadCurrentMap(){
        // TODO: Download the current displayed tiles
    }

    fun setVisibility(visibility: Int){
        map.visibility = visibility
    }

    fun onResume(){
        map.onResume()
    }

    fun onPause(){
        map.onPause()
    }

    companion object {
        private const val defaultZoom = 16.0

        fun configure(context: Context?){
            Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
        }

    }

}