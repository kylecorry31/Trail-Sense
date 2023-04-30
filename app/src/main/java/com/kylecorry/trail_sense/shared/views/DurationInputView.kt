package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.widget.LinearLayout
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.text.scale
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.Colors.withAlpha
import com.kylecorry.trail_sense.R
import java.time.Duration


class DurationInputView(context: Context?, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    var duration: Duration? = null
    private var changeListener: ((duration: Duration?) -> Unit)? = null

    private lateinit var input: TextInputEditText
    private lateinit var inputHolder: TextInputLayout

    private var durationText = "000000"

    private val PLACES_HOURS = 1
    private val PLACES_MINUTES = 3
    private val PLACES_SECONDS = 5

    var hint: CharSequence?
        get() = inputHolder.hint
        set(value) {
            inputHolder.hint = value
        }

    var showSeconds: Boolean = true
        set(value) {
            if (!value && field) {
                // Clear the seconds
                removeDigit(PLACES_SECONDS)
                removeDigit(PLACES_SECONDS)
                appendDigit(0, PLACES_SECONDS)
                appendDigit(0, PLACES_SECONDS)
            }
            field = value
        }

    init {
        context?.let {
            inflate(it, R.layout.view_duration_input, this)
            input = findViewById(R.id.duration)
            inputHolder = findViewById(R.id.duration_holder)
            
            inputHolder.setEndIconOnClickListener {
                // Clear
                durationText = "000000"
                onDurationTextChanged()
            }

            hint = context.getString(R.string.duration)

            input.setOnKeyListener { _, keyCode, event ->

                // Only handle key press
                if (event.action != KeyEvent.ACTION_DOWN) {
                    return@setOnKeyListener true
                }

                // Remove digit when backspace is pressed
                if (keyCode == 67) {
                    removeDigit(if (showSeconds) PLACES_SECONDS else PLACES_MINUTES)
                }

                // Add digit if a number is pressed
                if (keyCode in 7..16) {
                    appendDigit(keyCode - 7, if (showSeconds) PLACES_SECONDS else PLACES_MINUTES)
                }

                true
            }

            updateDuration(null)
        }
    }

    private fun removeDigit(place: Int = PLACES_SECONDS) {

        // Remove the digit at the given position and insert a 0 at the leftmost position
        durationText = ("0" + durationText.substring(0, place)).padEnd(6, '0')

        onDurationTextChanged()
    }

    private fun appendDigit(digit: Int, place: Int = PLACES_SECONDS) {
        // Only apply if the leftmost digit is 0
        if (durationText[0] != '0') {
            return
        }


        // Insert the digit at the given position and remove the leftmost digit, fill remaining digits with 0
        durationText = (durationText.substring(1, place + 1) + digit.toString()).padEnd(6, '0')

        onDurationTextChanged()
    }

    private fun onDurationTextChanged(shouldEvent: Boolean = true) {
        val h = durationText.substring(0, 2).toInt()
        val m = durationText.substring(2, 4).toInt()
        val s = durationText.substring(4, 6).toInt()
        duration = Duration.ofHours(h.toLong()).plusMinutes(m.toLong()).plusSeconds(s.toLong())
        updateTextView()
        if (shouldEvent) {
            changeListener?.invoke(duration)
        }
    }

    fun setOnDurationChangeListener(listener: ((duration: Duration?) -> Unit)?) {
        changeListener = listener
    }

    private fun updateTextView() {
        val h = durationText.substring(0, 2).toInt()
        val m = durationText.substring(2, 4).toInt()
        val s = durationText.substring(4, 6).toInt()

        val hoursSymbol = context.getString(R.string.hours_format, "").trim()
        val minutesSymbol = context.getString(R.string.minutes_format, "").trim()
        val secondsSymbol = context.getString(R.string.seconds_format, "").trim()

        val setColor = Resources.androidTextColorPrimary(context)
        val unsetColor = setColor.withAlpha(50)
        val numberScale = 1.5f

        val text = buildSpannedString {
            color(if (h > 0) setColor else unsetColor) {
                scale(numberScale) {
                    append(h.toString().padStart(2, '0'))
                }
                append(hoursSymbol)
            }

            append(" ")

            color(if (h > 0 || m > 0 || !showSeconds) setColor else unsetColor) {
                scale(numberScale) {
                    append(m.toString().padStart(2, '0'))
                }
                append(minutesSymbol)
            }

            if (showSeconds) {
                append(" ")
                color(setColor) {
                    scale(numberScale) {
                        append(s.toString().padStart(2, '0'))
                    }
                    append(secondsSymbol)
                }
            }
        }

        input.setText(text)
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


        // Set the duration text
        durationText = "%02d%02d%02d".format(h, m, s)

        // Update text view
        onDurationTextChanged(false)
    }


    companion object {
        private val MAX_DURATION = Duration.ofHours(99).plusMinutes(99).plusSeconds(99)
    }

}