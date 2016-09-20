package com.ninenine.reactivelocation

import android.Manifest
import android.app.Activity
import android.support.v4.app.ActivityCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.location.LocationSettingsResult

abstract class LocationConnectionException : Throwable() {
  abstract fun hasSolution(): Boolean
  abstract fun startActivityForSolution(activity: Activity, requestCode: Int)
}

class LocationSettingsException(
    private val locationSettingsResult: LocationSettingsResult
) : LocationConnectionException() {

  override fun hasSolution() = locationSettingsResult.status.hasResolution()

  override fun startActivityForSolution(activity: Activity, requestCode: Int) {
    locationSettingsResult.status.startResolutionForResult(activity, requestCode)
  }
}

class GoogleApiConnectException(
    private val connectionResult: ConnectionResult? = null
) : LocationConnectionException() {

  override fun hasSolution() = connectionResult?.hasResolution() ?: false

  override fun startActivityForSolution(activity: Activity, requestCode: Int) {
    connectionResult?.startResolutionForResult(activity, requestCode)
  }
}

class LocationPermissionException : LocationConnectionException() {
  override fun hasSolution() = true

  override fun startActivityForSolution(activity: Activity, requestCode: Int) {
    ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), requestCode)
  }
}

class GoogleApiConnectionSuspendedException : LocationConnectionException() {
  override fun hasSolution() = false

  override fun startActivityForSolution(activity: Activity, requestCode: Int) {
    // Don't have solution
  }
}
