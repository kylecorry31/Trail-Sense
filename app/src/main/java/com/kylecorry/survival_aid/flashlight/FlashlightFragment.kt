package com.kylecorry.survival_aid.flashlight

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.kylecorry.survival_aid.R
import java.util.*

class FlashlightFragment: Fragment(), Observer {

    private lateinit var flashlight: Flashlight
    private lateinit var strobe: Strobe

    private lateinit var flashlightBtn: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.activity_flashlight, container, false)

        flashlight = Flashlight(context!!)
        strobe = Strobe(flashlight)

        flashlightBtn = view.findViewById(R.id.flashlight_btn)

        flashlightBtn.setOnClickListener {
            if (flashlight.isOn || strobe.isOn) {
                flashlight.off()
                strobe.stop()
            }
            else {
                flashlight.on()
//                strobe.start(20f * 2)
            }
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        flashlight.addObserver(this)
        strobe.addObserver(this)
    }

    override fun onPause() {
        super.onPause()
        strobe.stop()
        flashlight.off()
        flashlight.deleteObserver(this)
        strobe.deleteObserver(this)
    }

    override fun update(o: Observable?, arg: Any?) {
        Handler(Looper.getMainLooper()).post{
            if (o == flashlight) updateFlashlightUI()
            if (o == strobe) updateFlashlightUI()
        }
    }

    private fun updateFlashlightUI(){
        if (flashlight.isOn || strobe.isOn){
            flashlightBtn.text = context?.getString(R.string.turn_flashlight_off)
        } else {
            flashlightBtn.text = context?.getString(R.string.turn_flashlight_on)
        }
    }
}