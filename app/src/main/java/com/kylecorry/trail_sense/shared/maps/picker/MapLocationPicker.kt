package com.kylecorry.trail_sense.shared.maps.picker

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.button.MaterialButton
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.tools.map.ui.MapView

class MapLocationPicker {

    private val formatter = getAppService<FormatService>()

    fun pickLocation(
        context: Context,
        owner: LifecycleOwner,
        defaultLocation: Coordinate,
        userLocation: Coordinate? = null,
        onLocationPicked: (Coordinate?) -> Unit
    ) {
        val view = View.inflate(context, R.layout.view_map_location_picker_prompt, null)
        val mapView = view.findViewById<MapView>(R.id.map)
        val locationView = view.findViewById<TextView>(R.id.location)

        // Zoom buttons
        val zoomInButton = view.findViewById<MaterialButton>(R.id.zoom_in_btn)
        val zoomOutButton = view.findViewById<MaterialButton>(R.id.zoom_out_btn)
        zoomInButton.setOnClickListener { mapView.zoom(2f) }
        zoomOutButton.setOnClickListener { mapView.zoom(0.5f) }

        val manager = MapLocationPickerViewManager()
        var selectedLocation = defaultLocation
        manager.setSelectedLocation(defaultLocation)
        locationView.text = formatter.formatLocation(defaultLocation)

        val start = {
            manager.start(mapView, owner)
        }

        val stop = {
            manager.stop(mapView)
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> start()
                Lifecycle.Event.ON_PAUSE -> stop()
                else -> {}
            }
        }

        mapView.backgroundColorOverride = Color.rgb(127, 127, 127)
        mapView.resolution = 2f
        // Rendering in widget mode prevents the map from showing the location marker as an arrow which may cause confusion
        mapView.isWidget = true
        userLocation?.let { mapView.userLocation = it }
        mapView.mapCenter = defaultLocation
        mapView.setOnSingleTapListener {
            selectedLocation = it
            manager.setSelectedLocation(it)
            locationView.text = formatter.formatLocation(it)
        }

        owner.lifecycle.addObserver(observer)

        Alerts.dialog(
            context,
            "",
            contentView = view
        ) { cancelled ->
            stop()
            owner.lifecycle.removeObserver(observer)
            if (cancelled) {
                onLocationPicked.invoke(null)
            } else {
                onLocationPicked.invoke(selectedLocation)
            }
        }
    }

}
