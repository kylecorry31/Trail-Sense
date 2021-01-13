package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.*
import androidx.core.widget.addTextChangedListener
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trailsensecore.infrastructure.sensors.gps.IGPS
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import com.kylecorry.trailsensecore.infrastructure.time.Intervalometer
import java.time.Duration

class CoordinateInputView(context: Context?, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    private val formatService by lazy { FormatService(getContext()) }
    private val sensorService by lazy { SensorService(getContext()) }
    lateinit var gps: IGPS

    private val errorHandler = Intervalometer {
        locationEdit.error = getContext().getString(R.string.coordinate_input_invalid_location)
    }

    var coordinate: Coordinate?
        get() = _coordinate
        set(value) {
            _coordinate = value
            if (value == null) {
                locationEdit.setText("")
            } else {
                val formatted = formatService.formatLocation(value)
                locationEdit.setText(formatted)
            }
        }
    private var _coordinate: Coordinate? = null

    private var changeListener: ((coordinate: Coordinate?) -> Unit)? = null
    private var autofillListener: (() -> Unit)? = null

    private lateinit var locationEdit: EditText
    private lateinit var gpsBtn: ImageButton
    private lateinit var helpBtn: ImageButton
    private lateinit var gpsLoadingIndicator: ProgressBar

    init {
        context?.let {
            inflate(it, R.layout.view_coordinate_input, this)
            gps = sensorService.getGPS()
            locationEdit = findViewById(R.id.utm)
            gpsLoadingIndicator = findViewById(R.id.gps_loading)
            helpBtn = findViewById(R.id.coordinate_input_help_btn)
            gpsBtn = findViewById(R.id.gps_btn)

            gpsBtn.visibility = View.VISIBLE
            gpsLoadingIndicator.visibility = View.GONE

            locationEdit.addTextChangedListener {
                onChange()
            }

            helpBtn.setOnClickListener {
                UiUtils.alert(
                    getContext(),
                    getContext().getString(R.string.location_input_help_title),
                    getContext().getString(R.string.location_input_help),
                    getContext().getString(R.string.dialog_ok)
                )
            }

            gpsBtn.setOnClickListener {
                autofillListener?.invoke()
                gpsBtn.visibility = View.GONE
                gpsLoadingIndicator.visibility = View.VISIBLE
                locationEdit.isEnabled = false
                gps.start(this::onGPSUpdate)
            }
        }
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility != View.VISIBLE) {
            pause()
        }
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        if (!isVisible) {
            pause()
        }
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (!hasWindowFocus) {
            pause()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pause()
    }

    private fun onGPSUpdate(): Boolean {
        coordinate = gps.location
        gpsBtn.visibility = View.VISIBLE
        gpsLoadingIndicator.visibility = View.GONE
        locationEdit.isEnabled = true
        return false
    }

    private fun onChange() {
        val locationText = locationEdit.text.toString()
        _coordinate = Coordinate.parse(locationText)
        errorHandler.stop()
        if (_coordinate == null && locationText.isNotEmpty()) {
            errorHandler.once(Duration.ofSeconds(2))
        } else {
            locationEdit.error = null
        }
        changeListener?.invoke(_coordinate)
    }

    private fun pause() {
        gps.stop(this::onGPSUpdate)
        gpsBtn.visibility = View.VISIBLE
        gpsLoadingIndicator.visibility = View.GONE
        locationEdit.isEnabled = true
        errorHandler.stop()
    }

    fun setOnAutoLocationClickListener(listener: (() -> Unit)?) {
        autofillListener = listener
    }

    fun setOnCoordinateChangeListener(listener: ((coordinate: Coordinate?) -> Unit)?) {
        changeListener = listener
    }

}