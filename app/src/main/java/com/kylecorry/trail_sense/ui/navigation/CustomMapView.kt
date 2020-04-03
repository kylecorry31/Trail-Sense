package com.kylecorry.trail_sense.ui.navigation

import android.content.Context
import android.widget.ImageView
import androidx.preference.PreferenceManager
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.Coordinate
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

class CustomMapView(private val map: MapView, private val compass: ImageView, startingLocation: Coordinate? = null) {

    private var marker: Marker? = null
    private var line: Polyline? = null
    private val myLocationMarker: Marker
    private var myLocation: Coordinate? = null

    init {
        if (startingLocation != null) {
            showLocation(startingLocation, defaultZoom)
        }

        map.setMultiTouchControls(true)
        map.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

        myLocationMarker = Marker(map)
        myLocationMarker.icon = map.context.getDrawable(R.drawable.ic_location)?.apply {
            setTint(map.context.getColor(R.color.colorPrimary))
        }
        myLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        map.overlays.add(myLocationMarker)
    }

    fun setTileSource(type: MapType){
        val source = when(type){
            MapType.Topographical -> TileSourceFactory.OpenTopo
            MapType.USGSTopographical -> TileSourceFactory.USGS_TOPO
            MapType.Satellite -> TileSourceFactory.USGS_SAT
            MapType.Street -> TileSourceFactory.MAPNIK
        }
        map.setTileSource(source)
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

        val myLoc = myLocation
        if (myLoc != null) {
            line = Polyline()
            line?.setPoints(listOf(GeoPoint(myLoc.latitude, myLoc.longitude), GeoPoint(location.latitude, location.longitude)))
            line?.outlinePaint?.color = map.context.getColor(R.color.colorPrimary)
            map.overlays.add(line)
        }

    }

    fun hideBeacon(){
        if (marker != null){
            map.overlays.remove(marker)
            marker = null
        }

        if (line != null){
            map.overlays.remove(line)
            line = null
        }
    }

    fun setMapAzimuth(azimuth: Float){
        map.mapOrientation = -azimuth
    }

    fun setCompassAzimuth(azimuth: Float){
        compass.rotation = -azimuth
    }

    fun setMyLocationAzimuth(azimuth: Float?){
        if (azimuth != null) {
            myLocationMarker.rotation = -azimuth
            myLocationMarker.icon = map.context.getDrawable(R.drawable.ic_navigation_arrow)?.apply {
                setTint(map.context.getColor(R.color.colorPrimary))
            }
        } else {
            myLocationMarker.icon = map.context.getDrawable(R.drawable.ic_location)?.apply {
                setTint(map.context.getColor(R.color.colorPrimary))
            }
        }
    }

    fun setMyLocation(location: Coordinate){
        myLocationMarker.position = GeoPoint(location.latitude, location.longitude)
        myLocation = location
    }

    fun downloadCurrentMap(){
        // TODO: Download the current displayed tiles
    }

    fun setVisibility(visibility: Int){
        map.visibility = visibility
        compass.visibility = visibility
    }

    fun onResume(){
        map.onResume()
    }

    fun onPause(){
        map.onPause()
    }

    companion object {
        private const val defaultZoom = 15.0

        fun configure(context: Context?){
            Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
        }

    }

}