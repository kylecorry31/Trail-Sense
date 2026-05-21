package com.kylecorry.trail_sense.shared.views.compose

import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.isVisible
import com.kylecorry.andromeda.core.ui.flatten
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.andromeda.views.toolbar.Toolbar as AndromedaToolbar

@Composable
fun Toolbar(
    title: CharSequence?,
    modifier: Modifier = Modifier,
    subtitle: CharSequence? = null,
    @IdRes id: Int? = null,
    @DrawableRes leftButtonIcon: Int? = null,
    @DrawableRes rightButtonIcon: Int? = null,
    isLeftButtonVisible: Boolean = leftButtonIcon != null,
    isRightButtonVisible: Boolean = rightButtonIcon != null,
    flattenLeftButton: Boolean = false,
    flattenRightButton: Boolean = false,
    onLeftButtonClick: (() -> Unit)? = null,
    onRightButtonClick: (() -> Unit)? = null,
    onLeftButtonViewClick: ((View) -> Unit)? = null,
    onRightButtonViewClick: ((View) -> Unit)? = null
) {
    AndroidView(
        factory = { context ->
            AndromedaToolbar(context, null).apply {
                id?.let { this.id = it }
                CustomUiUtils.setButtonState(leftButton, false)
                CustomUiUtils.setButtonState(rightButton, false)
                if (flattenLeftButton) {
                    leftButton.flatten()
                }
                if (flattenRightButton) {
                    rightButton.flatten()
                }
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        },
        update = {
            it.title.text = title
            it.subtitle.text = subtitle
            it.subtitle.isVisible = subtitle != null
            it.leftButton.isVisible = isLeftButtonVisible
            it.rightButton.isVisible = isRightButtonVisible
            leftButtonIcon?.let(it.leftButton::setImageResource)
            rightButtonIcon?.let(it.rightButton::setImageResource)
            it.leftButton.setOnClickListener { view ->
                onLeftButtonViewClick?.invoke(view) ?: onLeftButtonClick?.invoke()
            }
            it.rightButton.setOnClickListener { view ->
                onRightButtonViewClick?.invoke(view) ?: onRightButtonClick?.invoke()
            }
        },
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun ToolbarPreview() {
    MaterialTheme {
        Toolbar(
            title = "Trail Sense",
            subtitle = "Navigation",
            leftButtonIcon = R.drawable.ic_beacon,
            rightButtonIcon = R.drawable.ic_menu_dots,
            flattenRightButton = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
