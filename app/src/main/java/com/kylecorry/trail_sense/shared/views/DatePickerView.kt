package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.google.android.material.button.MaterialButton
import com.kylecorry.andromeda.pickers.material.AndromedaDayViewDecorator
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import java.time.LocalDate

class DatePickerView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    var date: LocalDate = LocalDate.now()
        set(value) {
            val changed = field != value
            field = value
            if (changed) {
                calendar.text = formatter.formatRelativeDate(value)
                onDateChange(value)
            }
        }

    var searchEnabled: Boolean = false
        set(value) {
            field = value
            search.isVisible = value
        }

    private val formatter = FormatService.getInstance(context)
    private val calendar: MaterialButton
    private val search: MaterialButton
    private val next: MaterialButton
    private val previous: MaterialButton
    private var onDateChange: (LocalDate) -> Unit = {}
    private var onCalendarLongPress: () -> Unit = {
        date = LocalDate.now()
    }
    private var onSearch: () -> Unit = {}
    private var dayViewDecorator: AndromedaDayViewDecorator? = null

    init {
        inflate(context, R.layout.view_date_picker, this)
        calendar = findViewById(R.id.date_btn)
        search = findViewById(R.id.search_btn)
        next = findViewById(R.id.next_date)
        previous = findViewById(R.id.prev_date)

        calendar.text = formatter.formatRelativeDate(date)

        calendar.setOnClickListener {
            CustomUiUtils.pickDate(context, date, dayViewDecorator) {
                if (it != null) {
                    date = it
                }
            }
        }

        calendar.setOnLongClickListener {
            onCalendarLongPress()
            true
        }

        next.setOnClickListener {
            date = date.plusDays(1)
        }
        CustomUiUtils.setRepeatingLongPressAction(next) {
            date = date.plusDays(1)
        }

        previous.setOnClickListener {
            date = date.minusDays(1)
        }
        CustomUiUtils.setRepeatingLongPressAction(previous) {
            date = date.minusDays(1)
        }

        search.setOnClickListener {
            onSearch()
        }

    }

    fun setOnDateChangeListener(listener: (LocalDate) -> Unit) {
        onDateChange = listener
    }

    fun setOnCalendarLongPressListener(listener: () -> Unit) {
        onCalendarLongPress = listener
    }

    fun setOnSearchListener(listener: () -> Unit) {
        onSearch = listener
    }

    fun setDayViewDecorator(decorator: AndromedaDayViewDecorator?) {
        dayViewDecorator = decorator
    }

    fun reset() {
        date = LocalDate.now()
    }

}
