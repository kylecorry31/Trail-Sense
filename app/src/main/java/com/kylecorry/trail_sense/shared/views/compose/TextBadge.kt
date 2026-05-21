package com.kylecorry.trail_sense.shared.views.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun TextBadge(
    text: String,
    containerColor: Color,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    shape: Shape = MaterialTheme.shapes.small,
    textStyle: TextStyle = LocalTextStyle.current,
    onClick: (() -> Unit)? = null
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = shape,
        modifier = if (onClick == null) {
            modifier
        } else {
            modifier.clickable { onClick() }
        }
    ) {
        Text(
            text = text,
            style = textStyle,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Preview
@Composable
private fun TextBadgePreview() {
    MaterialTheme {
        TextBadge(
            text = "Badge",
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
