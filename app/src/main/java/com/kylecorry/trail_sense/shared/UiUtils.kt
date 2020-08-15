package com.kylecorry.trail_sense.shared

import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.kylecorry.trail_sense.R

object UiUtils {

    fun alert(context: Context, title: String, content: String, onClose: (() -> Unit)? = null): AlertDialog {
        val builder = AlertDialog.Builder(context)
        builder.apply {
            setMessage(content)
            setTitle(title)
            setPositiveButton(
                R.string.dialog_ok
            ) { dialog, _ ->
                onClose?.invoke()
                dialog.dismiss()
            }
        }

        val dialog = builder.create()
        dialog.show()
        return dialog
    }

    fun longToast(context: Context, text: String){
        Toast.makeText(context, text, Toast.LENGTH_LONG).show()
    }

    fun shortToast(context: Context, text: String){
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }

}