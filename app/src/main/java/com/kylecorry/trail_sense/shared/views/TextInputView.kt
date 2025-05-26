package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.widget.addTextChangedListener
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.kylecorry.trail_sense.R

class TextInputView(context: Context, attrs: AttributeSet?) :
    FrameLayout(context, attrs) {

    private val edittext: TextInputEditText
    private val holder: TextInputLayout

    init {
        inflate(context, R.layout.view_text_input, this)
        edittext = findViewById(R.id.text_input)
        holder = findViewById(R.id.text_input_holder)

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.TextInputView)
        try {
            val hint = typedArray.getString(R.styleable.TextInputView_android_hint)
            setHint(hint)
        } finally {
            typedArray.recycle()
        }
    }

    fun setHint(hint: String?) {
        holder.hint = hint
    }

    fun setOnTextChangeListener(callback: (text: CharSequence?) -> Unit) {
        edittext.addTextChangedListener { text ->
            callback(text)
        }
    }

    var text: CharSequence?
        get() = edittext.text
        set(value) {
            edittext.setText(value)
        }
}