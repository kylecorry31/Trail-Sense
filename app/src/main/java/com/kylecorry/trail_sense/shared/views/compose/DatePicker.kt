package com.kylecorry.trail_sense.shared.views.compose

import androidx.annotation.IdRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.kylecorry.andromeda.pickers.material.AndromedaDayViewDecorator
import com.kylecorry.trail_sense.shared.views.DatePickerView
import java.time.LocalDate

@Composable
fun DatePicker(
    date: LocalDate,
    onDateChanged: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    @IdRes id: Int? = null,
    searchEnabled: Boolean = false,
    dayViewDecorator: AndromedaDayViewDecorator? = null,
    onSearch: () -> Unit = {}
) {
    AndroidView(
        factory = { context ->
            DatePickerView(context, null).apply {
                id?.let { this.id = it }
            }
        },
        update = {
            it.searchEnabled = searchEnabled
            it.setOnDateChangeListener(onDateChanged)
            it.setOnSearchListener(onSearch)
            it.setDayViewDecorator(dayViewDecorator)
            if (it.date != date) {
                it.date = date
            }
        },
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun DatePickerPreview() {
    MaterialTheme {
        DatePicker(
            date = LocalDate.of(2026, 5, 1),
            onDateChanged = {}
        )
    }
}
