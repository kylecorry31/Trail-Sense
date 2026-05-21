package com.kylecorry.trail_sense.shared.views.compose

import androidx.annotation.IdRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.shared.views.DistanceInputView

@Composable
fun DistanceInput(
    units: List<DistanceUnits>,
    modifier: Modifier = Modifier,
    value: Distance? = null,
    initialValue: Distance? = null,
    hint: String? = null,
    @IdRes id: Int? = null,
    onValueChange: (Distance?) -> Unit
) {
    AndroidView(
        factory = { ctx ->
            DistanceInputView(ctx).also { view ->
                id?.let { view.id = it }
                initialValue?.let { view.value = it }
            }
        },
        update = {
            it.value = value
            it.hint = hint
            it.units = units
            it.setOnValueChangeListener(onValueChange)
        },
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun DistanceInputPreview() {
    DistanceInput(
        units = listOf(DistanceUnits.Meters, DistanceUnits.Feet),
        hint = "Distance",
        value = Distance.meters(10f),
        onValueChange = {},
        modifier = Modifier.fillMaxWidth()
    )
}
