package com.kylecorry.survival_aid.navigation

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import com.kylecorry.survival_aid.R
import java.util.*
import kotlin.math.roundToInt

class NavigationActivity : AppCompatActivity(), Observer {

    private lateinit var compass: Compass
    private lateinit var gps: GPS
    private lateinit var azimuthTxt: TextView
    private lateinit var directionTxt: TextView
    private lateinit var needleImg: ImageView
    private lateinit var locationTxt: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bearing)
        compass = Compass(this)
        azimuthTxt = findViewById(R.id.compass_azimuth)
        directionTxt = findViewById(R.id.compass_direction)
        needleImg = findViewById(R.id.needle)
        locationTxt = findViewById(R.id.location)
        gps = GPS(this)
    }

    override fun onResume() {
        super.onResume()
        gps.start() // Not desired, only run when navigating
        compass.start()
        compass.addObserver(this)
        gps.addObserver(this)
    }

    override fun onPause() {
        super.onPause()
        gps.stop() // Not desired, only run when navigating
        compass.stop()
        compass.deleteObserver(this)
        gps.deleteObserver(this)
    }

    override fun update(o: Observable?, arg: Any?) {
        if (o == compass) updateCompass()
        if (o == gps) updateLocation()
    }

    private fun updateCompass(){
        val azimuthValue = compass.azimuth
        val azimuth = (azimuthValue.roundToInt() % 360).toString().padStart(3, ' ')
        val direction = compass.direction.symbol.toUpperCase().padEnd(2, ' ')
        azimuthTxt.text = "${azimuth}°"
        directionTxt.text = direction
        needleImg.rotation = -azimuthValue
    }

    private fun updateLocation(){
        locationTxt.text = "${gps.location?.latitude}, ${gps.location?.longitude}"
    }

    companion object {
        fun newIntent(ctx: Context): Intent {
            return Intent(ctx, NavigationActivity::class.java)
        }
    }

}
