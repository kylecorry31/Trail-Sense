package com.kylecorry.trail_sense.shared.system

import android.content.Context
import android.util.TypedValue
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
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

    fun androidTextColorPrimary(context: Context): Int {
        return getAndroidColorAttr(context, android.R.attr.textColorPrimary)
    }

    fun androidTextColorSecondary(context: Context): Int {
        return getAndroidColorAttr(context, android.R.attr.textColorSecondary)
    }

    @ColorInt
    fun getAndroidColorAttr(context: Context, @AttrRes attrRes: Int): Int {
        val theme = context.theme
        val typedValue = TypedValue()
        theme.resolveAttribute(attrRes, typedValue, true)
        val colorRes = if (typedValue.resourceId != 0) typedValue.resourceId else typedValue.data
        return context.getColor(colorRes)
    }

}