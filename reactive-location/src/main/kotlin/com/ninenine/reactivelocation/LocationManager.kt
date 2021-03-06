package com.ninenine.reactivelocation

import android.content.Context
import android.location.Location
import android.os.Bundle
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import rx.AsyncEmitter
import rx.Observable
import java.lang.ref.SoftReference
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class LocationManager(
    private val locationApi: LocationApiProvider
) {

  companion object {
    val DEFAULT_REQUEST: LocationRequest = LocationRequest.create()
        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        .setInterval(5000)
  }

  private val observers = AtomicInteger(0)

  fun streamForRequest(
      context: Context,
      request: LocationRequest = DEFAULT_REQUEST
  ): Observable<Location> {
    return Observable.fromEmitter<Location>({ asyncEmitter ->
      val emitterHandler = LocationEmitterHandler.startEmitting(locationApi, asyncEmitter, request)

      asyncEmitter.setCancellation {
        emitterHandler.stopEmitting()
      }

    }, AsyncEmitter.BackpressureMode.LATEST)
        .doOnSubscribe { addingObserver(context) }
        .doOnUnsubscribe { removingObserver() }
  }

  private fun addingObserver(context: Context) {
    if (observers.incrementAndGet() >= 0 && !locationApi.isConnected()) {
      locationApi.connect(context)
    }
  }

  private fun removingObserver() {
    if (observers.decrementAndGet() <= 0) {
      locationApi.disconnect()
    }
  }

  private class LocationEmitterHandler private constructor(
      private val locationApi: LocationApiProvider,
      private val request: LocationRequest,
      asyncEmitter: AsyncEmitter<Location>
  ) : LocationListener,
      GoogleApiClient.OnConnectionFailedListener,
      GoogleApiClient.ConnectionCallbacks {

    private val alive: AtomicBoolean = AtomicBoolean(true)
    private val asyncEmitter: SoftReference<AsyncEmitter<Location>> = SoftReference(asyncEmitter)

    fun stopEmitting() {
      alive.set(false)

      locationApi.unregisterConnectionCallbacks(this)
      locationApi.unregisterConnectionFailedListener(this)

      if (locationApi.isConnected()) {
        stopLocationUpdates()
      }

      clearResources()
    }

    override fun onLocationChanged(location: Location?) {
      if (isAlive()) {
        location?.let { dispatchNext(it) }
      }
    }

    override fun onConnected(hint: Bundle?) {
      if (isAlive()) {
        locationApiConnected()
      }
    }

    override fun onConnectionSuspended(cause: Int) {
      if (isAlive()) {
        dispatchError(GoogleApiConnectionSuspendedException())
      }
    }

    override fun onConnectionFailed(result: ConnectionResult) {
      if (isAlive()) {
        dispatchError(GoogleApiConnectException(result))
      }
    }

    private fun locationApiConnected() {
      checkLocationSettings()
    }

    private fun dispatchNext(location: Location) {
      asyncEmitter.get()?.onNext(location)
    }

    private fun dispatchError(error: LocationConnectionException) {
      asyncEmitter.get()?.onError(error)
    }

    private fun checkLocationSettings() {
      if (!locationApi.checkHasLocationPermission()) {
        dispatchError(LocationPermissionException())
        return
      }

      locationApi.checkHasLocationSettings(request) { result ->
        val status = result.status
        when (status.statusCode) {
          LocationSettingsStatusCodes.SUCCESS -> locationSettingsApproved()
          else -> dispatchError(LocationSettingsException(result))
        }
      }
    }

    private fun locationSettingsApproved() {
      startLocationsUpdates()
    }

    private fun startLocationsUpdates() {
      locationApi.requestLocationUpdates(request, this)
    }

    private fun stopLocationUpdates() {
      locationApi.removeLocationUpdates(this)
    }

    private fun isAlive(): Boolean {
      return alive.get()
    }

    private fun clearResources() {
      asyncEmitter.clear()
    }

    companion object {
      fun startEmitting(
          locationApi: LocationApiProvider,
          asyncEmitter: AsyncEmitter<Location>,
          request: LocationRequest
      ) = LocationEmitterHandler(locationApi, request, asyncEmitter).apply {
        locationApi.registerConnectionCallbacks(this)
        locationApi.registerConnectionFailedListener(this)
      }
    }
  }

}
