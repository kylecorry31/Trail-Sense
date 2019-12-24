package com.kylecorry.survival_aid

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.kylecorry.survival_aid.flashlight.FlashlightFragment
import com.kylecorry.survival_aid.navigator.NavigatorFragment
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import com.kylecorry.survival_aid.blueprints.BlueprintListFragment
import com.kylecorry.survival_aid.weather.BarometerAlarmManager
import com.kylecorry.survival_aid.weather.BarometerAlarmReceiver
import com.kylecorry.survival_aid.weather.WeatherFragment


class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!hasPermissions()){
            getPermission()
        }

        BarometerAlarmManager.startRecording(this)

        bottomNavigation = findViewById(R.id.bottom_navigation)

        syncFragmentWithSelection(bottomNavigation.selectedItemId)

        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            syncFragmentWithSelection(item.itemId)
            true
        }

    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        bottomNavigation.selectedItemId = savedInstanceState.getInt("page", R.id.action_navigation)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("page", bottomNavigation.selectedItemId)
    }

    private fun syncFragmentWithSelection(selection: Int){
        when (selection) {
            R.id.action_navigation -> {
                switchFragment(NavigatorFragment())
            }
            R.id.action_weather -> {
                switchFragment(WeatherFragment())
            }
            R.id.action_light -> {
                switchFragment(FlashlightFragment())
            }
            R.id.action_blueprint -> {
                switchFragment(BlueprintListFragment())
            }
        }
    }

    private fun switchFragment(fragment: Fragment){
        supportFragmentManager.doTransaction {
            this.replace(R.id.fragment_holder, fragment)
        }
    }

    override fun onBackPressed() {
        val count = supportFragmentManager.backStackEntryCount

        if (count == 0) {
            super.onBackPressed()
            //additional code
        } else {
            supportFragmentManager.popBackStack()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        val granted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        if (granted){
            // Do nothing yet
        } else {
            Toast.makeText(this, "Location permission is required, some features may be broken", Toast.LENGTH_LONG).show()
        }
    }

    private fun hasPermissions(): Boolean {
        val permissions = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

        for (permission in permissions){
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                return false
            }
        }

        return true
    }

    private fun getPermission(){
        ActivityCompat.requestPermissions(this,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION),
            1
        )
    }

    companion object {
        fun newIntent(ctx: Context): Intent {
            return Intent(ctx, MainActivity::class.java)
        }
    }
}
