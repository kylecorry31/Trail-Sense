package com.kylecorry.trail_sense.shared

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.kylecorry.trail_sense.R

class TextHolderPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet,
    defStyleAttr: Int = 0
) : Preference(context, attrs, defStyleAttr) {

    private lateinit var textView: TextView

    var text: CharSequence
        get() = textView.text
        set(value){
            textView.text = value
        }

    init {
        widgetLayoutResource = R.layout.text_preference
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        with(holder.itemView) {
            textView = findViewById(R.id.textView)
        }
    }

}