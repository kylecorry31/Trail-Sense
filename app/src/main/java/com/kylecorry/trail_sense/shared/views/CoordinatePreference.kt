package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.kylecorry.trail_sense.R
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trailsensecore.infrastructure.sensors.gps.IGPS

class CoordinatePreference(context: Context, attributeSet: AttributeSet) : Preference(context, attributeSet) {

    private var coordinateInputView: CoordinateInputView? = null
    private var titleTextView: TextView? = null
    private var gps: IGPS? = null
    private var initialCoordinate: Coordinate? = null
    private var listener: ((coordinate: Coordinate?) -> Unit)? = null
    private var title: String? = null

    init {
        layoutResource = R.layout.preference_coordinate
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        holder.itemView.isClickable = false
        coordinateInputView = holder.findViewById(R.id.preference_coordinate_input) as CoordinateInputView
        titleTextView = holder.findViewById(R.id.preference_coordinate_input_title) as TextView

        title?.let {
            titleTextView?.text = it
        }

        gps?.let {
            coordinateInputView?.gps = it
        }

        initialCoordinate?.let {
            coordinateInputView?.coordinate = it
        }

        listener?.let {
            coordinateInputView?.setOnCoordinateChangeListener(it)
        }

        title = null
        gps = null
        initialCoordinate = null
        listener = null
    }

    fun setTitle(title: String){
        this.title = title
        titleTextView?.text = title
    }

    fun setGPS(gps: IGPS){
        coordinateInputView?.gps = gps
        this.gps = gps
    }

    fun setLocation(coordinate: Coordinate?){
        coordinateInputView?.coordinate = coordinate
        initialCoordinate = coordinate
    }

    fun setOnLocationChangeListener(listener: ((coordinate: Coordinate?) -> Unit)?){
        coordinateInputView?.setOnCoordinateChangeListener(listener)
        this.listener = listener
    }

    fun pause(){
        coordinateInputView?.pause()
    }

}