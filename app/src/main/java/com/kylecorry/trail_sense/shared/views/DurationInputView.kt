package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.TextView
import androidx.core.view.isVisible
import com.kylecorry.trail_sense.R
import java.time.Duration


class DurationInputView(context: Context?, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    var duration: Duration? = null
    private var changeListener: ((duration: Duration?) -> Unit)? = null

    private lateinit var hours: NumberPicker
    private lateinit var minutes: NumberPicker
    private lateinit var seconds: NumberPicker
    private lateinit var secondsLabel: TextView

    var showSeconds: Boolean = true
        set(value) {
            seconds.isVisible = value
            secondsLabel.isVisible = value
            field = value
        }

    init {
        context?.let {
            inflate(it, R.layout.view_duration_input, this)
            hours = findViewById(R.id.hours)
            minutes = findViewById(R.id.minutes)
            seconds = findViewById(R.id.seconds)
            secondsLabel = findViewById(R.id.seconds_label)

            hours.minValue = 0
            hours.maxValue = 23
            minutes.minValue = 0
            minutes.maxValue = 59
            seconds.minValue = 0
            seconds.maxValue = 59

            hours.setOnValueChangedListener { _, _, _ ->
                onChange()
            }

            minutes.setOnValueChangedListener { _, _, _ ->
                onChange()
            }

            seconds.setOnValueChangedListener { _, _, _ ->
                onChange()
            }
        }
    }

    private fun onChange() {
        val h = hours.value
        val m = minutes.value
        val s = seconds.value
        duration = Duration.ofHours(h.toLong()).plusMinutes(m.toLong()).plusSeconds(s.toLong())
        changeListener?.invoke(duration)
    }

    fun setOnDurationChangeListener(listener: ((duration: Duration?) -> Unit)?) {
        changeListener = listener
    }

    fun updateDuration(duration: Duration?) {
        val nonNull = duration?.abs() ?: Duration.ZERO
        val clamped = if (nonNull > MAX_DURATION) {
            MAX_DURATION
        } else {
            nonNull
        }
        val h = clamped.toHours().toInt()
        val m = clamped.toMinutes().toInt() % 60
        val s = clamped.seconds.toInt() % 60
        hours.value = h
        minutes.value = m
        seconds.value = s

        this.duration = Duration.ofHours(h.toLong()).plusMinutes(m.toLong()).plusSeconds(s.toLong())
    }


    companion object {
        private val MAX_DURATION = Duration.ofHours(23).plusMinutes(59).plusSeconds(59)
    }

}