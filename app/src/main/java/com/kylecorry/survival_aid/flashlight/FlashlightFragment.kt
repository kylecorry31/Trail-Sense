package com.kylecorry.survival_aid.flashlight

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.RadioButton
import androidx.fragment.app.Fragment
import com.kylecorry.survival_aid.R
import java.util.*

class FlashlightFragment: Fragment(), Observer {

    private lateinit var flashlight: Flashlight

    private lateinit var flashlightBtn: ImageButton
    private lateinit var strobeRadio: RadioButton
    private lateinit var normalRadio: RadioButton

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.activity_flashlight, container, false)

        flashlight = Flashlight(context!!)

        flashlightBtn = view.findViewById(R.id.flashlight_btn)
        normalRadio = view.findViewById(R.id.normal_radio)
        strobeRadio = view.findViewById(R.id.strobe_radio)


        normalRadio.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) flashlight.mode = NormalFlashlightMode()
        }

        strobeRadio.setOnCheckedChangeListener {buttonView, isChecked ->
            if (isChecked) flashlight.mode = StrobeFlashlightMode(STROBE_FREQUENCY)
        }

        flashlightBtn.setOnClickListener {
            if (flashlight.isOn) {
                flashlight.off()
            } else {
                flashlight.on()
            }
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
        flashlight.deleteObserver(this)
    }

    override fun update(o: Observable?, arg: Any?) {
        Handler(Looper.getMainLooper()).post{
            if (o == flashlight) updateFlashlightUI()
        }
    }

    private fun updateFlashlightUI(){
        if (flashlight.isOn){
            flashlightBtn.setImageDrawable(context!!.getDrawable(R.drawable.ic_power_on))
        } else {
            flashlightBtn.setImageDrawable(context!!.getDrawable(R.drawable.ic_power_off))
        }
    }

    companion object {
        const val STROBE_FREQUENCY = 40f //hz
    }
}