package com.kylecorry.trail_sense.shared.sensors

import android.content.Context
import android.os.Handler
import androidx.preference.PreferenceManager
import com.kylecorry.trail_sense.shared.AltitudeCorrection
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.domain.Accuracy
import com.kylecorry.trail_sense.shared.domain.Coordinate


class FakeGPS(private val context: Context) : AbstractSensor(), IGPS {

    override val satellites: Int = 0
    override val accuracy: Accuracy = Accuracy.Unknown
    override val horizontalAccuracy: Float? = null
    override val verticalAccuracy: Float? = null

    override val location: Coordinate
        get() = _location

    override val speed: Float = 0f

    override val altitude: Float
        get() = _altitude

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    private val userPrefs = UserPreferences(context)

    private var _altitude = prefs.getFloat(LAST_ALTITUDE, 0f)
    private var _location = Coordinate(
        prefs.getFloat(LAST_LATITUDE, 0f).toDouble(),
        prefs.getFloat(LAST_LONGITUDE, 0f).toDouble()
    )

    private lateinit var handler: Handler
    private lateinit var runnable: Runnable

    override fun startImpl() {
       handler = Handler()

        runnable = Runnable {

            // TODO: Get fake location
            _location = Coordinate(
                prefs.getFloat(LAST_LATITUDE, 0f).toDouble(),
                prefs.getFloat(LAST_LONGITUDE, 0f).toDouble()
            )
            _altitude = if (userPrefs.useAutoAltitude) prefs.getFloat(LAST_ALTITUDE, 0f) else userPrefs.altitudeOverride

            if (userPrefs.useAutoAltitude && userPrefs.useAltitudeOffsets){
                _altitude -= AltitudeCorrection.getOffset(this._location, context)
            }

            notifyListeners()
            handler.postDelayed(runnable, 100)
        }

        handler.post(runnable)
    }

    override fun stopImpl() {
        handler.removeCallbacks(runnable)
    }


    companion object {
        private const val LAST_LATITUDE = "last_latitude"
        private const val LAST_LONGITUDE = "last_longitude"
        private const val LAST_ALTITUDE = "last_altitude"
    }
}