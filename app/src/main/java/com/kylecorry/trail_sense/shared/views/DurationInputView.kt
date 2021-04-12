package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import android.widget.*
import com.kylecorry.trail_sense.R
import java.time.Duration


class DurationInputView(context: Context?, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    var duration: Duration? = null
    private var changeListener: ((duration: Duration?) -> Unit)? = null

    private lateinit var hours: NumberPicker
    private lateinit var minutes: NumberPicker

    init {
        context?.let {
            inflate(it, R.layout.view_duration_input, this)
            hours = findViewById(R.id.hours)
            minutes = findViewById(R.id.minutes)

            hours.minValue = 0
            hours.maxValue = 23
            minutes.minValue = 0
            minutes.maxValue = 59

            hours.setOnValueChangedListener { picker, oldVal, newVal ->
                onChange()
            }

            minutes.setOnValueChangedListener { picker, oldVal, newVal ->
                onChange()
            }
        }
    }

    private fun onChange() {
        val h = hours.value
        val m = minutes.value
        duration = Duration.ofHours(h.toLong()).plusMinutes(m.toLong())
        changeListener?.invoke(duration)
    }

    fun setOnDurationChangeListener(listener: ((duration: Duration?) -> Unit)?) {
        changeListener = listener
    }

    fun updateDuration(duration: Duration?){
        val nonNull = duration?.abs() ?: Duration.ZERO
        val clamped = if (nonNull > MAX_DURATION){
            MAX_DURATION
        } else {
            nonNull
        }
        hours.value = clamped.toHours().toInt()
        minutes.value = clamped.toMinutes().toInt() % 60
        this.duration = clamped
    }


    companion object {
        private val MAX_DURATION = Duration.ofHours(23).plusMinutes(59)
    }

}