package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.textfield.TextInputEditText

class EndOnlyEditText(context: Context, attrs: AttributeSet?) : TextInputEditText(context, attrs) {
    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        setSelection(text?.length ?: 0)
    }

    override fun isTextSelectable(): Boolean {
        return false
    }

    override fun getDefaultEditable(): Boolean {
        return false
    }

    override fun performLongClick(): Boolean {
        return false
    }
}