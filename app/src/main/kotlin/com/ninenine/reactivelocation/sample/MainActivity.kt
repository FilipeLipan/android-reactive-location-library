package com.ninenine.reactivelocation.sample

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import com.google.android.gms.location.LocationRequest
import com.ninenine.reactivelocation.FusedLocationApiProvider
import com.ninenine.reactivelocation.LocationConnectionException
import com.ninenine.reactivelocation.LocationManager
import rx.Subscription
import java.text.SimpleDateFormat
import java.util.Date

class MainActivity : AppCompatActivity() {

  val TAG = "Reactive Location"
  val REQUEST_CODE_LOCATION_EXCEPTION = 1
  val REQUEST_CODE_SETTINGS = 2

  val fusedLocationApi by lazy { FusedLocationApiProvider() }
  val lastLocationTextView by lazy { findViewById(R.id.lastLocation) as TextView }
  val lastTimestampTextView by lazy { findViewById(R.id.lastTimestamp) as TextView }
  val dateFormatter by lazy { SimpleDateFormat("HH:mm:ss")}

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
    stopListeningToLocation()
  }

  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    super.onCreateOptionsMenu(menu)
    menuInflater.inflate(R.menu.menu_main, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem?): Boolean {
    if (item?.itemId == R.id.menu_action_request_settings) {
      navigateToRequestSettingsScreen()
      return true
    }
    return super.onOptionsItemSelected(item)
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
    } else if (requestCode == REQUEST_CODE_SETTINGS && resultCode == Activity.RESULT_OK) {
      stopListeningToLocation()
      startListeningToLocation()
    }
  }

  fun startListeningToLocation() {
    val manager = LocationManager(fusedLocationApi)
    val locationRequest = createLocationRequest()

    subscription = manager.streamForRequest(this, locationRequest).subscribe(
        { location ->
          val text = "Location: ${location.latitude}, ${location.longitude}"
          val date = Date(location.time)

          lastLocationTextView.text = text
          lastTimestampTextView.text = dateFormatter.format(date)
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

  fun stopListeningToLocation() {
    subscription?.unsubscribe()
  }

  private fun createLocationRequest(): LocationRequest {
    return LocationRequest().setInterval(getInterval())
  }

  private fun getInterval(): Long {
    return getSharedPreferences(packageName, Context.MODE_PRIVATE).getInt(RequestSettingsActivity.SETTINGS_INTERVAL, 1) * 1000L
  }

  private fun navigateToRequestSettingsScreen() {
    startActivityForResult(Intent(this, RequestSettingsActivity::class.java), REQUEST_CODE_SETTINGS)
  }
}
