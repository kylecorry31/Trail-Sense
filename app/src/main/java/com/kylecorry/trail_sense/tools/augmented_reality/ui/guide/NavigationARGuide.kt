package com.kylecorry.trail_sense.tools.augmented_reality.ui.guide

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.kylecorry.andromeda.core.coroutines.onMain
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.ViewArNavigationGuideBinding
import com.kylecorry.trail_sense.tools.augmented_reality.domain.position.GeographicARPoint
import com.kylecorry.trail_sense.tools.augmented_reality.ui.AugmentedRealityView
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class NavigationARGuide(private val navigator: Navigator) : ARGuide {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var job: Job? = null

    override fun start(arView: AugmentedRealityView, panel: FrameLayout) {
        job?.cancel()
        val layoutInflater = LayoutInflater.from(panel.context)
        val binding = ViewArNavigationGuideBinding.inflate(layoutInflater, panel, true)
        binding.arGuideCancel.setOnClickListener {
            navigator.cancelNavigation()
        }
        job = scope.launch {
            navigator.destination.collect {
                if (it == null) {
                    onMain {
                        panel.isVisible = false
                    }
                    arView.clearGuide()
                } else {
                    onMain {
                        panel.isVisible = true
                        binding.arGuideIcon.setImageResource(
                            it.icon?.icon ?: R.drawable.ic_location
                        )
                        binding.arGuideIcon.backgroundTintList = ColorStateList.valueOf(it.color)
                        Colors.setImageColor(
                            binding.arGuideIcon,
                            Colors.mostContrastingColor(Color.WHITE, Color.BLACK, it.color)
                        )
                        binding.arGuideName.text = it.name
                    }
                    arView.guideTo(GeographicARPoint(it.coordinate, it.elevation)) {
                        // Do nothing when reached
                    }
                }
            }
        }
    }

    override fun stop(arView: AugmentedRealityView, panel: FrameLayout) {
        panel.removeAllViews()
        job?.cancel()
        arView.clearGuide()
    }
}