package com.kylecorry.trail_sense.shared.views

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.colors.AppColor

@Composable
fun ColorCircle(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    outline: Color? = null,
    outlineWidth: Dp = 4.dp
) {
    Box(contentAlignment = Alignment.Center, modifier = modifier) {

        outline?.let {
            Surface(
                color = outline,
                shape = CircleShape,
                modifier = Modifier.size(size + outlineWidth * 2)
            ) {

            }
        }

        Surface(
            color = color,
            shape = CircleShape,
            modifier = Modifier.size(size)
        ) {}
    }

}

@Preview
@Composable
private fun ShowColorCircle() {
    ColorCircle(
        Color(AppColor.Red.color), outline = Color.Black,
        modifier = Modifier.padding(16.dp)
    )
}