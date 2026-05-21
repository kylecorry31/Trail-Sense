package com.kylecorry.trail_sense.shared.views.compose

import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.views.PlayBarView
import java.time.Duration

@Composable
fun PlayBar(
    title: String,
    state: FeatureState,
    modifier: Modifier = Modifier,
    @IdRes id: Int? = null,
    frequency: Duration? = null,
    @DrawableRes icon: Int? = null,
    onSubtitleClick: (() -> Unit)? = null,
    onPlayClick: () -> Unit = {}
) {
    AndroidView(
        factory = { context ->
            PlayBarView(context, null).apply {
                id?.let { this.id = it }
            }
        },
        update = {
            it.title = title
            it.setImageResource(icon)
            it.setState(state, frequency)
            it.setOnSubtitleClickListener(onSubtitleClick)
            it.setOnPlayButtonClickListener(onPlayClick)
        },
        modifier = modifier
    )
}

@Preview
@Composable
private fun PlayBarPreview() {
    MaterialTheme {
        PlayBar(
            title = "Test",
            state = FeatureState.On,
            icon = R.drawable.ic_beacon,
            frequency = Duration.ofHours(3),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
