package com.ninenine.reactivelocation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResult

interface LocationApiProvider {
  fun connect()
  fun disconnect()
  fun registerConnectionCallbacks(callback: GoogleApiClient.ConnectionCallbacks)
  fun registerConnectionFailedListener(listener: GoogleApiClient.OnConnectionFailedListener)
  fun unregisterConnectionCallbacks(callback: GoogleApiClient.ConnectionCallbacks)
  fun unregisterConnectionFailedListener(listener: GoogleApiClient.OnConnectionFailedListener)

  fun checkHasLocationPermission(): Boolean
  fun checkHasLocationSettings(request: LocationRequest, callback: (LocationSettingsResult) -> Unit)

  fun isConnected(): Boolean
  fun requestLocationUpdates(request: LocationRequest, listener: LocationListener)
  fun removeLocationUpdates(listener: LocationListener)
}

class FusedLocationApiProvider(
    private val context: Context
) : LocationApiProvider {

  private val googleApiClient: GoogleApiClient = GoogleApiClient.Builder(context).addApi(LocationServices.API).build()

  override fun connect() {
    googleApiClient.connect()
  }

  override fun disconnect() {
    googleApiClient.disconnect()
  }

  override fun isConnected(): Boolean {
    return googleApiClient.isConnected
  }

  override fun registerConnectionCallbacks(callback: GoogleApiClient.ConnectionCallbacks) {
    googleApiClient.registerConnectionCallbacks(callback)
  }

  override fun registerConnectionFailedListener(listener: GoogleApiClient.OnConnectionFailedListener) {
    googleApiClient.registerConnectionFailedListener(listener)
  }

  override fun unregisterConnectionCallbacks(callback: GoogleApiClient.ConnectionCallbacks) {
    googleApiClient.unregisterConnectionCallbacks(callback)
  }

  override fun unregisterConnectionFailedListener(listener: GoogleApiClient.OnConnectionFailedListener) {
    googleApiClient.unregisterConnectionFailedListener(listener)
  }

  override fun checkHasLocationPermission(): Boolean {
    val permission = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
    return permission == PackageManager.PERMISSION_GRANTED
  }

  override fun checkHasLocationSettings(request: LocationRequest, callback: (LocationSettingsResult) -> Unit) {
    val settingsBuilder = LocationSettingsRequest.Builder().addLocationRequest(request)
    LocationServices.SettingsApi.checkLocationSettings(googleApiClient, settingsBuilder.build()).setResultCallback(callback)
  }

  override fun requestLocationUpdates(request: LocationRequest, listener: LocationListener) {
    LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, request, listener)
  }

  override fun removeLocationUpdates(listener: LocationListener) {
    LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, listener)
  }
}
