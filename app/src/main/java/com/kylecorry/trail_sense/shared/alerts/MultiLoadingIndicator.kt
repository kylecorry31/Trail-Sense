package com.kylecorry.trail_sense.shared.alerts

class MultiLoadingIndicator(private val indicators: List<ILoadingIndicator>) :
    ILoadingIndicator {
    override fun show() {
        for (indicator in indicators) {
            indicator.show()
        }
    }

    override fun hide() {
        for (indicator in indicators) {
            indicator.hide()
        }
    }
}