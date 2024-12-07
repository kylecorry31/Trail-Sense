package com.kylecorry.trail_sense.tools.flashlight.widgets

import android.os.Bundle
import com.kylecorry.andromeda.fragments.AndromedaActivity
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightSubsystem

class FlashlightWidgetActivityWorkaround : AndromedaActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FlashlightSubsystem.getInstance(this).toggle()
        finishAffinity()
    }

}