package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.LifecycleObserver
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.andromeda.core.units.Bearing


class BearingInputView(context: Context?, attrs: AttributeSet?) : LinearLayout(context, attrs), LifecycleObserver {

    private val sensorService by lazy { SensorService(getContext()) }
    private val compass by lazy { sensorService.getCompass() }

    private var changeListener: ((bearing: Bearing?) -> Unit)? = null
    private var bearing: Bearing? = null


    var declination: Float = 0f
        set(value) {
            compass.declination = value
            field = value
        }

    private lateinit var compassBtn: ImageButton
    private lateinit var bearingEdit: EditText

    private var started = false

    init {
        context?.let {
            inflate(it, R.layout.view_bearing_input, this)
            compassBtn = findViewById(R.id.compass_btn)
            bearingEdit = findViewById(R.id.bearing)

            compassBtn.setOnClickListener {
                onCompassClick()
            }

            bearingEdit.addTextChangedListener {
                onChange()
            }
        }
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == View.VISIBLE){
            start()
        } else {
            stop()
        }
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (hasWindowFocus){
            start()
        } else {
            stop()
        }
    }

    private fun start(){
        if (!started){
            started = true
            compass.declination = declination
            compass.start(this::onCompassUpdate)
        }
    }

    private fun stop(){
        if (started){
            started = false
            compass.stop(this::onCompassUpdate)
        }
    }

    private fun onCompassUpdate(): Boolean {
        return true
    }

    private fun onCompassClick(){
        bearing = compass.bearing
        bearingEdit.setText(bearing?.value?.toString())
        onChange()
    }

    private fun onChange() {
        val bearingTxt = bearingEdit.text.toString().toFloatOrNull()

        bearing = if (bearingTxt == null){
            null
        } else {
            Bearing(bearingTxt)
        }

        changeListener?.invoke(bearing)
    }

    fun setOnBearingChangeListener(listener: ((bearing: Bearing?) -> Unit)?) {
        changeListener = listener
    }


}