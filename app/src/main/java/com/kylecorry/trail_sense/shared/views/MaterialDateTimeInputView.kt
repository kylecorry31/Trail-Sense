package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import java.time.LocalDateTime

class MaterialDateTimeInputView(context: Context, attrs: AttributeSet?) :
    FrameLayout(context, attrs) {

    private val edittext: TextInputEditText
    private val holder: TextInputLayout
    private var listener: ((LocalDateTime?) -> Unit)? = null

    var datetime: LocalDateTime = LocalDateTime.now()
        private set

    init {
        inflate(context, R.layout.view_date_time_input, this)
        edittext = findViewById(R.id.datetime_input)
        holder = findViewById(R.id.datetime_input_holder)

        setValue(datetime)

        edittext.setOnClickListener {
            val prefs = AppServiceRegistry.get<UserPreferences>()
            Pickers.datetime(context, prefs.use24HourTime, datetime) {
                setValue(it ?: return@datetime)
            }
        }
    }

    fun setValue(datetime: LocalDateTime) {
        this.datetime = datetime
        listener?.invoke(datetime)
        val formatter = AppServiceRegistry.get<FormatService>()
        edittext.setText(formatter.formatDateTime(datetime.toZonedDateTime(), relative = true))
    }

    fun setHint(hint: CharSequence?) {
        holder.hint = hint
    }

    fun setOnItemSelectedListener(listener: (LocalDateTime?) -> Unit) {
        this.listener = listener
    }

}