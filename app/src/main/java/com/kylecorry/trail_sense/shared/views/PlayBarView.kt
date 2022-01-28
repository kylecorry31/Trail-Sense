package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.ViewPlayBarBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils

class PlayBarView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    private val binding: ViewPlayBarBinding

    var title: String
        get() = binding.playBarTitle.title
        set(value) {
            binding.playBarTitle.title = value
        }

    var subtitle: String
        get() = binding.playBarTitle.description
        set(value) {
            binding.playBarTitle.description = value
        }

    init {
        inflate(context, R.layout.view_play_bar, this)
        binding = ViewPlayBarBinding.bind(findViewById(R.id.play_bar))
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.PlayBarView, 0, 0)
        val icon = a.getResourceId(R.styleable.PlayBarView_playBarIcon, -1)
        setImageResource(if (icon == -1) null else icon)
        title = a.getString(R.styleable.PlayBarView_playBarTitle) ?: ""
        subtitle = a.getString(R.styleable.PlayBarView_playBarSubtitle) ?: ""
        a.recycle()
        setState(false)
    }

    fun setImageResource(@DrawableRes res: Int?) {
        binding.playBarTitle.setImageResource(res)
    }

    fun setShowSubtitle(showSubtitle: Boolean) {
        binding.playBarTitle.setShowDescription(showSubtitle)
    }

    fun setState(isOn: Boolean) {
        binding.playBtn.setImageResource(if (isOn) R.drawable.ic_baseline_stop_24 else R.drawable.ic_baseline_play_arrow_24)
        CustomUiUtils.setButtonState(binding.playBtn, true)
    }

    fun setOnPlayButtonClickListener(action: () -> Unit){
        binding.playBtn.setOnClickListener { action() }
    }

}