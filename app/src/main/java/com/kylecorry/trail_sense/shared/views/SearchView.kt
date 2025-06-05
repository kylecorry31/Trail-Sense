package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.widget.addTextChangedListener
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.ViewSearchBinding

class SearchView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {
    private val binding: ViewSearchBinding
    private var onSearch: ((String) -> Unit)? = null

    var query: String
        get() = binding.searchViewEditText.text.toString()
        set(value) {
            binding.searchViewEditText.setText(value)
        }

    init {
        inflate(context, R.layout.view_search, this)
        binding = ViewSearchBinding.bind(this)
        binding.searchViewEditText.addTextChangedListener {
            onSearch?.invoke(binding.searchViewEditText.text.toString())
        }
    }

    fun setOnSearchListener(listener: ((String) -> Unit)?) {
        onSearch = listener
    }

    fun setCursorPosition(position: Int) {
        binding.searchViewEditText.setSelection(position)
    }
}

