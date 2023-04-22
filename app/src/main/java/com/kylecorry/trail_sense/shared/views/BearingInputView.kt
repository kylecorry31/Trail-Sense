package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.widget.addTextChangedListener
import com.kylecorry.sol.units.Bearing
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.extensions.floatValue
import com.kylecorry.trail_sense.shared.sensors.SensorService
import kotlin.math.roundToInt

class BearingInputView(context: Context, attrs: AttributeSet? = null) :
    LinearLayout(context, attrs) {

    private val formatter by lazy { FormatService.getInstance(getContext()) }
    private val compass by lazy { SensorService(getContext()).getCompass() }

    var bearing: Bearing?
        get() = _bearing
        set(value) {
            _bearing = value
            if (value == null) {
                bearingEdit.setText("")
            } else {
                val degrees = value.value.roundToInt() % 360
                bearingEdit.setText(degrees.toString())
            }
        }

    @Suppress("MemberVisibilityCanBePrivate")
    var trueNorth: Boolean
        get() = trueNorthSwitch.isChecked
        set(value) {
            trueNorthSwitch.isChecked = value
        }

    private var _bearing: Bearing? = null

    private var changeListener: ((bearing: Bearing?, isTrueNorth: Boolean) -> Unit)? = null

    private val bearingEdit: EditText
    private val compassBtn: ImageButton
    private val compassText: TextView
    private val trueNorthSwitch: SwitchCompat


    init {
        inflate(context, R.layout.view_bearing_input, this)

        bearingEdit = findViewById(R.id.bearing)
        compassBtn = findViewById(R.id.compass_btn)
        compassText = findViewById(R.id.compass_bearing)
        trueNorthSwitch = findViewById(R.id.true_north)

        bearingEdit.addTextChangedListener {
            onChange()
        }

        trueNorthSwitch.setOnCheckedChangeListener { _, _ -> onChange() }

        compassBtn.setOnClickListener {
            bearing = compass.bearing
            trueNorth = false
        }
    }

    fun start() {
        stop()
        compass.start {
            compassText.text = formatter.formatDegrees(compass.bearing.value, replace360 = true)
            true
        }
    }

    fun stop() {
        compass.stop(null)
    }

    private fun onChange() {
        val degrees = bearingEdit.floatValue()
        _bearing = degrees?.let { Bearing(it) }
        changeListener?.invoke(_bearing, trueNorthSwitch.isChecked)
    }

    fun setOnBearingChangeListener(listener: ((bearing: Bearing?, isTrueNorth: Boolean) -> Unit)?) {
        changeListener = listener
    }

}