package com.kylecorry.trail_sense.shared.sharing

data class ActionItem(
    val name: String,
    val icon: Int,
    val action: () -> Unit
) {
}