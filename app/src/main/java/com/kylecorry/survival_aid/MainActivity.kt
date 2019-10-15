package com.kylecorry.survival_aid

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import com.kylecorry.survival_aid.navigation.NavigationActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (!hasPermissions()){
            getPermission()
        } else {
            startApp()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        val granted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        if (granted){
            startApp()
        }
    }

    private fun startApp(){
        startActivity(NavigationActivity.newIntent(this))
    }

    private fun hasPermissions(): Boolean {
        val permissionAccessCoarseLocationApproved = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

        val permissionAccessFineLocationApproved = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

        return permissionAccessCoarseLocationApproved && permissionAccessFineLocationApproved
    }

    private fun getPermission(){
        ActivityCompat.requestPermissions(this,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION),
            1
        )
    }
}
