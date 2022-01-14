package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.ViewErrorBannerBinding
import com.kylecorry.trail_sense.shared.ErrorBannerReason
import com.kylecorry.trail_sense.shared.UserPreferences

class ErrorBannerView(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {

    private val binding: ViewErrorBannerBinding

    private val errors: MutableList<UserError> = mutableListOf()

    private val prefs by lazy { UserPreferences(this.context) }

    private var onAction: (() -> Unit)? = null

    init {
        inflate(context, R.layout.view_error_banner, this)
        binding = ViewErrorBannerBinding.bind(this)
        binding.errorAction.setOnClickListener {
            onAction?.invoke()
        }
        binding.errorClose.setOnClickListener {
            val reason = synchronized(this) {
                errors.firstOrNull()?.reason
            }
            if (reason != null) {
                dismiss(reason)
            }
        }
    }

    fun report(error: UserError) {
        if (!prefs.errors.canShowError(error.reason)) {
            return
        }
        synchronized(this) {
            errors.removeAll { it.reason == error.reason }
            errors.add(error)
            errors.sortBy { it.reason.id }
        }
        displayNextError()
        show()
    }

    fun dismiss(reason: ErrorBannerReason) {
        synchronized(this) {
            errors.removeAll { it.reason == reason }
        }
        displayNextError()
    }

    fun dismissAll() {
        synchronized(this) {
            errors.clear()
        }
        displayNextError()
    }

    private fun displayNextError() {
        val first = synchronized(this) {
            errors.firstOrNull()
        }
        if (first != null) {
            displayError(first)
        } else {
            onAction = null
            hide()
        }
    }

    private fun displayError(error: UserError) {
        binding.errorText.text = error.title
        binding.errorAction.text = error.action
        binding.errorIcon.setImageResource(error.icon)
        onAction = error.onAction
        if (error.action.isNullOrEmpty()) {
            binding.errorAction.visibility = View.GONE
        } else {
            binding.errorAction.visibility = View.VISIBLE
        }
    }

    fun show() {
        visibility = View.VISIBLE
    }

    fun hide() {
        visibility = View.GONE
    }

}