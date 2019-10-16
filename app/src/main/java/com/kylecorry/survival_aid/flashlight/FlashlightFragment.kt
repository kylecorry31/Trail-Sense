package com.kylecorry.survival_aid.flashlight

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.kylecorry.survival_aid.R
import java.util.*

class FlashlightFragment: Fragment(), Observer {

    private lateinit var flashlight: Flashlight

    private lateinit var flashlightBtn: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.activity_flashlight, container, false)

        flashlight = Flashlight(context!!)

        flashlightBtn = view.findViewById(R.id.flashlight_btn)

        flashlightBtn.setOnClickListener {
            if (flashlight.isOn) flashlight.off()
            else flashlight.on()
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        flashlight.addObserver(this)
    }

    override fun onPause() {
        super.onPause()
        flashlight.off()
    }

    override fun update(o: Observable?, arg: Any?) {
        if (o == flashlight) updateFlashlightUI()
    }

    private fun updateFlashlightUI(){
        if (flashlight.isOn){
            flashlightBtn.text = getString(R.string.turn_flashlight_off)
        } else {
            flashlightBtn.text = getString(R.string.turn_flashlight_on)
        }
    }
}