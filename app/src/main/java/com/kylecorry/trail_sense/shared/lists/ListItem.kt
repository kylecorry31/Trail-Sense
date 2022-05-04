package com.kylecorry.trail_sense.shared.lists

import android.graphics.Bitmap
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import com.kylecorry.trail_sense.shared.database.Identifiable

data class ListItem(
    override val id: Long,
    val title: CharSequence,
    val subtitle: CharSequence? = null,
    val icon: ListIcon? = null,
    val checkbox: ListItemCheckbox? = null,
    val tags: List<ListItemTag> = emptyList(),
    val data: List<ListItemData> = emptyList(),
    val trailingText: CharSequence? = null,
    val trailingIcon: ListIcon? = null,
    val trailingIconAction: () -> Unit = {},
    val menu: List<ListMenuItem> = emptyList(),
    val longClickAction: () -> Unit = {},
    val action: () -> Unit = {}
) : Identifiable

data class ListMenuItem(val text: String, val action: () -> Unit)

data class ListItemTag(val text: String, val icon: ListIcon?, @ColorInt val color: Int)

data class ListItemData(val text: CharSequence, val icon: ListIcon?)

data class ListItemCheckbox(val checked: Boolean, val onClick: () -> Unit)

interface ListIcon

data class ResourceListIcon(@DrawableRes val id: Int, @ColorInt val tint: Int? = null) : ListIcon
data class AsyncBitmapListIcon(val provider: suspend () -> Bitmap) : ListIcon