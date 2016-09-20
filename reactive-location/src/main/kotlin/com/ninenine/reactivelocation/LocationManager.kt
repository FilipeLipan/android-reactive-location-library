package com.ninenine.reactivelocation

import android.location.Location
import android.os.Bundle
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import rx.AsyncEmitter
import rx.Observable
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
      request: LocationRequest = DEFAULT_REQUEST
  ): Observable<Location> {
    return Observable.fromEmitter<Location>({ asyncEmitter ->
      val emitterHandler = LocationEmitterHandler.startEmitting(locationApi, asyncEmitter, request)

      asyncEmitter.setCancellation {
        emitterHandler.stopEmitting()
      }

    }, AsyncEmitter.BackpressureMode.LATEST)
        .doOnSubscribe { addingObserver() }
        .doOnUnsubscribe { removingObserver() }
  }

  private fun addingObserver() {
    if (observers.incrementAndGet() >= 0 && !locationApi.isConnected()) {
      locationApi.connect()
    }
  }

  private fun removingObserver() {
    if (observers.decrementAndGet() <= 0) {
      locationApi.disconnect()
    }
  }

  private class LocationEmitterHandler private constructor(
      private val locationApi: LocationApiProvider,
      private val asyncEmitter: AsyncEmitter<Location>,
      private val request: LocationRequest
  ) : LocationListener,
      GoogleApiClient.OnConnectionFailedListener,
      GoogleApiClient.ConnectionCallbacks {

    companion object {
      fun startEmitting(
          locationApi: LocationApiProvider,
          asyncEmitter: AsyncEmitter<Location>,
          request: LocationRequest
      ) = LocationEmitterHandler(locationApi, asyncEmitter, request).apply {
        locationApi.registerConnectionCallbacks(this)
        locationApi.registerConnectionFailedListener(this)
      }
    }

    fun stopEmitting() {
      locationApi.unregisterConnectionCallbacks(this)
      locationApi.unregisterConnectionFailedListener(this)

      if (locationApi.isConnected()) {
        stopLocationUpdates()
      }
    }

    private fun dispatchNext(location: Location) {
      asyncEmitter.onNext(location)
    }

    private fun dispatchError(error: LocationConnectionException) {
      asyncEmitter.onError(error)
    }

    override fun onLocationChanged(location: Location?) {
      location?.let { dispatchNext(it) }
    }

    override fun onConnected(hint: Bundle?) {
      locationApiConnected()
    }

    override fun onConnectionSuspended(cause: Int) {
      dispatchError(GoogleApiConnectionSuspendedException())
    }

    override fun onConnectionFailed(result: ConnectionResult) {
      dispatchError(GoogleApiConnectException(result))
    }

    private fun locationApiConnected() {
      checkLocationSettings()
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
  }

}
