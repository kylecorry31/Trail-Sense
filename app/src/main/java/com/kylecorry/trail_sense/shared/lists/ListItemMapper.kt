package com.kylecorry.trail_sense.shared.lists

interface ListItemMapper<T> {
    fun map(value: T): ListItem
}