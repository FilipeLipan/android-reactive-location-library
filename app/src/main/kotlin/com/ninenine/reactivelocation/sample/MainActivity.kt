package com.ninenine.reactivelocation.sample

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.ninenine.reactivelocation.FusedLocationApiProvider
import com.ninenine.reactivelocation.LocationManager

class MainActivity : AppCompatActivity() {

  val TAG = "Reactive Location"

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    val fusedLocationApi = FusedLocationApiProvider(this)
    fusedLocationApi.connect()

    LocationManager(fusedLocationApi).connect().subscribe(
        { location -> Log.d(TAG, "Location: ${location.latitude}, ${location.longitude}") },
        { error -> Log.e(TAG, "Error: ${error.message}") },
        { Log.d(TAG, "Completed") }
    )
  }
}
