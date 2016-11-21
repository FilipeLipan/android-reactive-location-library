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
import java.util.concurrent.atomic.AtomicReference

interface LocationApiProvider {
  fun connect(context: Context)
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

class FusedLocationApiProvider : LocationApiProvider {

  private val context: AtomicReference<Context> = AtomicReference()
  private val googleApiClient: AtomicReference<GoogleApiClient> = AtomicReference()

  override fun connect(context: Context) {
    val client = GoogleApiClient.Builder(context).addApi(LocationServices.API).build()
    client.connect()

    this.context.set(context.applicationContext)
    this.googleApiClient.set(client)
  }

  override fun disconnect() {
    getGoogleClient()?.disconnect()

    this.context.set(null)
    this.googleApiClient.set(null)
  }

  override fun isConnected(): Boolean {
    return getGoogleClient()?.isConnected ?: false
  }

  override fun registerConnectionCallbacks(callback: GoogleApiClient.ConnectionCallbacks) {
    val googleApiClient = getGoogleClient() ?: throw IllegalStateException("Location API not connected")

    googleApiClient.registerConnectionCallbacks(callback)
  }

  override fun registerConnectionFailedListener(listener: GoogleApiClient.OnConnectionFailedListener) {
    val googleApiClient = getGoogleClient() ?: throw IllegalStateException("Location API not connected")

    googleApiClient.registerConnectionFailedListener(listener)
  }

  override fun unregisterConnectionCallbacks(callback: GoogleApiClient.ConnectionCallbacks) {
    val googleApiClient = getGoogleClient() ?: throw IllegalStateException("Location API not connected")

    googleApiClient.unregisterConnectionCallbacks(callback)
  }

  override fun unregisterConnectionFailedListener(listener: GoogleApiClient.OnConnectionFailedListener) {
    val googleApiClient = getGoogleClient() ?: throw IllegalStateException("Location API not connected")

    googleApiClient.unregisterConnectionFailedListener(listener)
  }

  override fun checkHasLocationPermission(): Boolean {
    val context = this.context.get() ?: throw IllegalArgumentException("API Provider was already disposed")

    val permission = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
    return permission == PackageManager.PERMISSION_GRANTED
  }

  override fun checkHasLocationSettings(request: LocationRequest, callback: (LocationSettingsResult) -> Unit) {
    val googleApiClient = getGoogleClient() ?: throw IllegalStateException("Location API not connected")

    val settingsBuilder = LocationSettingsRequest.Builder().addLocationRequest(request)
    LocationServices.SettingsApi.checkLocationSettings(googleApiClient, settingsBuilder.build()).setResultCallback(callback)
  }

  override fun requestLocationUpdates(request: LocationRequest, listener: LocationListener) {
    val googleApiClient = getGoogleClient() ?: throw IllegalStateException("Location API not connected")

    LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, request, listener)
  }

  override fun removeLocationUpdates(listener: LocationListener) {
    val googleApiClient = getGoogleClient() ?: throw IllegalStateException("Location API not connected")

    LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, listener)
  }

  private fun getGoogleClient(): GoogleApiClient? {
    return googleApiClient.get()
  }

}
