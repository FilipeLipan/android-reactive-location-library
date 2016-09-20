package com.ninenine.reactivelocation

import android.location.Location
import android.os.Bundle
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import rx.Observable
import rx.subjects.PublishSubject
import java.util.concurrent.atomic.AtomicInteger

class LocationManager(
    private val locationApi: LocationApiProvider
) {

  companion object {
    val DEFAULT_REQUEST: LocationRequest = LocationRequest.create()
        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        .setInterval(1000)
  }

  private val registeredRequests: MutableMap<Int, LocationConnectionHandler> = mutableMapOf()

  fun connect(
      request: LocationRequest = DEFAULT_REQUEST
  ): Observable<Location> {
    val cachedAdapter = registeredRequests[request.hashCode()]
    val adapter = cachedAdapter ?: createAdapter(request)

    return adapter.observable()
        .doOnSubscribe {
          registerAdapter(request, adapter)
        }
        .doOnError {
          disposeAdapter(request)
        }
        .doOnCompleted {
          disposeAdapter(request)
        }
  }

  private fun registerAdapter(request: LocationRequest, adapter: LocationConnectionHandler) {
    registeredRequests.put(request.hashCode(), adapter)
  }

  private fun disposeAdapter(request: LocationRequest) {
    registeredRequests.remove(request.hashCode())
    if (registeredRequests.isEmpty()) {
      locationApi.disconnect()
    }
  }

  private fun createAdapter(request: LocationRequest): LocationConnectionHandler {
    return LocationConnectionHandler(locationApi, request)
  }

  private class LocationConnectionHandler(
      private val locationApi: LocationApiProvider,
      private val request: LocationRequest
  ) : LocationListener,
      GoogleApiClient.OnConnectionFailedListener,
      GoogleApiClient.ConnectionCallbacks {

    private val subject: PublishSubject<Location>
    private val connections = AtomicInteger(0)

    init {
      subject = PublishSubject.create<Location>()
      locationApi.registerConnectionCallbacks(this)
      locationApi.registerConnectionFailedListener(this)
    }

    fun observable(): Observable<Location> {
      return subject.onBackpressureLatest()
          .doOnSubscribe {
            newConnection()
          }
          .doOnUnsubscribe {
            connectionClosed()
          }
    }

    private fun newConnection() {
      connections.incrementAndGet()

      connect()
    }

    private fun connectionClosed() {
      if (connections.decrementAndGet() == 0) {
        subject.onCompleted()
        disconnect()
      }
    }

    private fun connect() {
      if (!locationApi.isConnected()) {
        locationApi.connect()
      } else {
        locationApiConnected()
      }
    }

    private fun disconnect() {
      locationApi.unregisterConnectionCallbacks(this)
      locationApi.unregisterConnectionFailedListener(this)
      stopLocationUpdates()
    }

    override fun onLocationChanged(location: Location?) {
      location?.let { subject.onNext(location) }
    }

    override fun onConnected(connectionHint: Bundle?) {
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

    private fun dispatchError(error: LocationConnectionException) {
      subject.onError(error)
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
