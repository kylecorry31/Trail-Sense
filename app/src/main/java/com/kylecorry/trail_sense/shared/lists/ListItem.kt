package com.kylecorry.trail_sense.shared.lists

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes

data class ListItem(
    val title: String,
    val subtitle: String? = null,
    val icon: ListIcon? = null,
    val trailingText: String? = null,
    val trailingIcon: ListIcon? = null,
    val menu: List<ListMenuItem> = emptyList(),
    val action: () -> Unit = {}
)

data class ListMenuItem(val text: String, val action: () -> Unit)

interface ListIcon

data class ResourceListIcon(@DrawableRes val id: Int, @ColorInt val tint: Int? = null): ListIcon