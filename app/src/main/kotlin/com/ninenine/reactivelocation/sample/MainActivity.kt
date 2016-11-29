package com.ninenine.reactivelocation.sample

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.TextView
import com.ninenine.reactivelocation.FusedLocationApiProvider
import com.ninenine.reactivelocation.LocationConnectionException
import com.ninenine.reactivelocation.LocationManager
import rx.Subscription

class MainActivity : AppCompatActivity() {

  val TAG = "Reactive Location"
  val REQUEST_CODE_LOCATION_EXCEPTION = 1

  val fusedLocationApi by lazy { FusedLocationApiProvider() }

  var subscription: Subscription? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
  }

  override fun onStart() {
    super.onStart()
    startListeningToLocation()
  }

  override fun onStop() {
    super.onStop()
    subscription?.unsubscribe()
  }

  fun startListeningToLocation() {
    val manager = LocationManager(fusedLocationApi)
    subscription = manager.streamForRequest(this).subscribe(
        { location ->
          val text = "Location: ${location.latitude}, ${location.longitude}"
          val lastLocation = findViewById(R.id.lastLocation) as TextView
          lastLocation.text = text

          Log.d(TAG, text)
        },
        { error ->
          if (error is LocationConnectionException && error.hasSolution()) {
            error.startActivityForSolution(this, REQUEST_CODE_LOCATION_EXCEPTION)
          }
          Log.e(TAG, "Error: ${error.message}")
        },
        {
          Log.d(TAG, "Completed")
        }
    )
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (requestCode == REQUEST_CODE_LOCATION_EXCEPTION) {
      permissions.forEachIndexed { index, permission ->
        if (permission == Manifest.permission.ACCESS_FINE_LOCATION && grantResults[index] == PackageManager.PERMISSION_GRANTED) {
          startListeningToLocation()
        }
      }
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == REQUEST_CODE_LOCATION_EXCEPTION && resultCode == Activity.RESULT_OK) {
      startListeningToLocation()
    }
  }
}
