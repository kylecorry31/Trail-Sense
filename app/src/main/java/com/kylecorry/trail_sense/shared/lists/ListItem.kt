package com.kylecorry.trail_sense.shared.lists

import android.graphics.Bitmap
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import com.kylecorry.trail_sense.shared.database.Identifiable

data class ListItem(
    override val id: Long,
    val title: String,
    val subtitle: String? = null,
    val description: String? = null,
    val icon: ListIcon? = null,
    val tag: ListItemTag? = null,
    val trailingText: String? = null,
    val trailingIcon: ListIcon? = null,
    val trailingIconAction: () -> Unit = {},
    val menu: List<ListMenuItem> = emptyList(),
    val longClickAction: () -> Unit = {},
    val action: () -> Unit = {}
) : Identifiable

data class ListMenuItem(val text: String, val action: () -> Unit)

data class ListItemTag(val text: String, val icon: ListIcon?, @ColorInt val color: Int)

interface ListIcon

data class ResourceListIcon(@DrawableRes val id: Int, @ColorInt val tint: Int? = null) : ListIcon
data class AsyncBitmapListIcon(val provider: suspend () -> Bitmap) : ListIcon