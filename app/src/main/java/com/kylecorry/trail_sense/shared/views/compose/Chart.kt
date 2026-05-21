package com.kylecorry.trail_sense.shared.views.compose

import androidx.annotation.IdRes
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.kylecorry.andromeda.views.chart.Chart as AndromedaChart

@Composable
fun Chart(
    modifier: Modifier = Modifier,
    @IdRes id: Int? = null,
    onChartCreated: (AndromedaChart) -> Unit = {},
    update: (AndromedaChart) -> Unit = {}
) {
    AndroidView(
        factory = { context ->
            AndromedaChart(context).apply {
                id?.let { this.id = it }
                onChartCreated(this)
            }
        },
        update = {
            update(it)
        },
        modifier = modifier
    )
}

@Preview
@Composable
private fun ChartPreview() {
    Chart(modifier = Modifier.size(200.dp))
}
