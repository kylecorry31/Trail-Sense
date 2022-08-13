package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.ViewPlayBarBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.FormatService
import java.time.Duration

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
            useDefaultSubtitle = false
            binding.playBarTitle.description = value
        }

    private val formatter = FormatService.getInstance(context)

    private var useDefaultSubtitle: Boolean = true

    init {
        inflate(context, R.layout.view_play_bar, this)
        binding = ViewPlayBarBinding.bind(findViewById(R.id.play_bar))
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.PlayBarView, 0, 0)
        val icon = a.getResourceId(R.styleable.PlayBarView_playBarIcon, -1)
        setImageResource(if (icon == -1) null else icon)
        title = a.getString(R.styleable.PlayBarView_playBarTitle) ?: ""
        val subtitleText = a.getString(R.styleable.PlayBarView_playBarSubtitle) ?: ""
        if (subtitleText.isNotEmpty()) {
            subtitle = subtitleText
        }
        a.recycle()
        setState(false)
    }

    fun setImageResource(@DrawableRes res: Int?) {
        binding.playBarTitle.setImageResource(res)
    }

    fun setShowSubtitle(showSubtitle: Boolean) {
        binding.playBarTitle.setShowDescription(showSubtitle)
    }

    fun setState(state: FeatureState, frequency: Duration? = null){
        setState(state == FeatureState.On, frequency)
    }

    fun setState(isOn: Boolean, frequency: Duration? = null) {
        binding.playBtn.setImageResource(if (isOn) R.drawable.ic_baseline_stop_24 else R.drawable.ic_baseline_play_arrow_24)
        if (useDefaultSubtitle) {
            binding.playBarTitle.description = if (isOn) {
                context.getString(R.string.on)
            } else {
                context.getString(R.string.off)
            } + (frequency?.let {
                " ${context.getString(R.string.dash)} ${formatter.formatDuration(
                    frequency,
                    short = true,
                    includeSeconds = true
                )}"
            } ?: "")
        }
        CustomUiUtils.setButtonState(binding.playBtn, true)
    }

    fun setOnSubtitleClickListener(action: (() -> Unit)?) {
        binding.playBarTitle.setOnDescriptionClickListener(action)
    }

    fun setOnPlayButtonClickListener(action: () -> Unit) {
        binding.playBtn.setOnClickListener { action() }
    }

}