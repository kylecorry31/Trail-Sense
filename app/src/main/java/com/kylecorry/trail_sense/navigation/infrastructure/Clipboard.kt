package com.kylecorry.trail_sense.navigation.infrastructure

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.core.content.getSystemService

class Clipboard(private val context: Context) {
    val clipboardManager = context.getSystemService<ClipboardManager>()

    fun copy(text: String, showToast: Boolean = true) {
        clipboardManager?.setPrimaryClip(ClipData.newPlainText(text, text))

        if (showToast) {
            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        }
    }

}