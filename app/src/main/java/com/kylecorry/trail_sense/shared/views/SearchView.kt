package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import androidx.core.content.getSystemService
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
            onSearch?.invoke(it?.toString().orEmpty())
        }

        binding.searchViewEditText.setOnEditorActionListener { view, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                view.context
                    .getSystemService<InputMethodManager>()
                    ?.hideSoftInputFromWindow(view.windowToken, 0)

                true
            } else {
                false
            }
        }
    }

    fun setOnSearchListener(listener: ((String) -> Unit)?) {
        onSearch = listener
    }

    fun setCursorPosition(position: Int) {
        binding.searchViewEditText.setSelection(position)
    }
}

