package com.kylecorry.survival_aid.flashlight

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.kylecorry.survival_aid.R
import java.util.*

class FlashlightFragment: Fragment(), Observer {

    private lateinit var flashlight: Flashlight

    private lateinit var flashlightBtn: ImageButton
    private lateinit var strobeRadio: RadioButton
    private lateinit var normalRadio: RadioButton
    private lateinit var sosRadio: RadioButton
    private lateinit var normalDesc: TextView
    private lateinit var strobeDesc: TextView
    private lateinit var sosDesc: TextView

    private val modeNormal = NormalFlashlightMode()
    private val modeStrobe = StrobeFlashlightMode(STROBE_FREQUENCY)
    private val modeSos = SosFlashlightMode(SOS_DOT_DURATION)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.activity_flashlight, container, false)

        flashlight = Flashlight(context!!)

        flashlightBtn = view.findViewById(R.id.flashlight_btn)
        normalRadio = view.findViewById(R.id.normal_radio)
        strobeRadio = view.findViewById(R.id.strobe_radio)
        sosRadio = view.findViewById(R.id.sos_radio)

        normalDesc = view.findViewById(R.id.normal_desc)
        strobeDesc = view.findViewById(R.id.strobe_desc)
        sosDesc = view.findViewById(R.id.sos_desc)


        normalDesc.setOnClickListener { normalRadio.isChecked = true }
        strobeDesc.setOnClickListener { strobeRadio.isChecked = true }
        sosDesc.setOnClickListener { sosRadio.isChecked = true }

        normalRadio.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) flashlight.mode = modeNormal
        }

        strobeRadio.setOnCheckedChangeListener {buttonView, isChecked ->
            if (isChecked) flashlight.mode = modeStrobe
        }

        sosRadio.setOnCheckedChangeListener {buttonView, isChecked ->
            if (isChecked) flashlight.mode = modeSos
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
        if (o == flashlight) updateFlashlightUI()
    }

    private fun updateFlashlightUI(){
        val ctx = context
        ctx ?: return
        if (flashlight.isOn){
            flashlightBtn.setImageDrawable(ctx.getDrawable(R.drawable.ic_power_on))
        } else {
            flashlightBtn.setImageDrawable(ctx.getDrawable(R.drawable.ic_power_off))
        }
    }

    companion object {
        const val STROBE_FREQUENCY = 20f //hz
        const val SOS_DOT_DURATION = 250L // ms
    }
}