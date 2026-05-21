package com.kylecorry.trail_sense.shared.views.compose

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.R

@Composable
fun DataPoint(
    title: String?,
    description: String?,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    onClick: (() -> Unit)? = null
) {
    Column(modifier = modifier.clickable(enabled = onClick != null) { onClick?.invoke() }) {
        Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
            icon?.let {
                Icon(
                    painter = painterResource(it),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(20.dp),
                )
            }
            Column {
                title?.let {
                    Text(title, style = MaterialTheme.typography.bodyMedium)
                }
                description?.let {
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DataPointPreview() {
    MaterialTheme {
        DataPoint(
            title = "Temperature",
            description = "72°F",
            icon = R.drawable.thermometer
        )
    }
}
