package com.kylecorry.trail_sense.ui

import android.annotation.SuppressLint
import android.hardware.SensorManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.sensors.gps.GPS
import java.util.*
import com.kylecorry.trail_sense.Constants
import com.kylecorry.trail_sense.altimeter.*
import com.kylecorry.trail_sense.database.PressureHistoryRepository
import com.kylecorry.trail_sense.models.AltitudeReading
import com.kylecorry.trail_sense.models.Coordinate
import com.kylecorry.trail_sense.navigator.LocationMath
import com.kylecorry.trail_sense.sensors.barometer.Barometer
import com.kylecorry.trail_sense.models.PressureAltitudeReading
import com.kylecorry.trail_sense.toZonedDateTime
import kotlinx.android.synthetic.main.activity_weather.*
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt

class AstronomyFragment : Fragment(), Observer {

    private lateinit var gps: GPS
    private lateinit var location: Coordinate
    private var gotLocation = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.activity_astronomy, container, false)

        gps = GPS(context!!)
        location = gps.location

        return view
    }

    override fun onResume() {
        super.onResume()
        if (!gotLocation) {
            gps.addObserver(this)
            gps.start()
        }
    }

    override fun onPause() {
        super.onPause()
        gps.stop()
        gps.deleteObserver(this)
    }

    override fun update(o: Observable?, arg: Any?) {
        if (context == null) return
        if (o == gps){
            gotLocation = true
            gps.stop()
        }
    }

    fun updateUI(){
        // TODO: Calculate sunrise / sunset times
        // TODO: Calculate moon phase
    }

}
