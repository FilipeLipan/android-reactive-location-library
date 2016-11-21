# reactive-location
A simple RxJava wrapper for location requests in Android written in Kotlin

### Features
--------

* Using FusedLocationApi from Google Play Services.
* Location events will be delivered by stream via RxJava.
* It'll have a single stream for each LocationRequest.
* Support for runtime Location Permission and Location Settings errors.
* Simple errors resolution with startActivityForSolution for both Permission and Settings errors.
* Google Play Services disconnect when all requests are no longer being observed.

### Usage
```kotlin
val REQUEST_CODE_LOCATION_EXCEPTION = 1
var subscription: Subscription? = null

override fun onStart() {
  super.onStart()
  startListeningForLocations()
}

override fun onStop() {
  super.onStop()
  subscription?.unsubscribe()
}

private fun startListeningForLocations() {
  val locationApiProvider = FusedLocationApiProvider()
  val locationManager = LocationManager(locationApiProvider)
  val locationRequest = LocationRequest.create()
        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        .setInterval(1000)

  subscription = locationManager.streamForRequest(this, locationRequest).subscribe(
    { location ->
        // use locations as pleased
    },
    { error ->
       // Check if the error has solution. Settings or Permission exceptions
       if (error is LocationConnectionException && error.hasSolution()) {
         error.startActivityForSolution(this, REQUEST_CODE_LOCATION_EXCEPTION)
       }
    }
  )
}

override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
  super.onRequestPermissionsResult(requestCode, permissions, grantResults)
  if (requestCode == REQUEST_CODE_LOCATION_EXCEPTION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      startListeningToLocation()
  }
}

override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {  super.onActivityResult(requestCode, resultCode, data)
  if (requestCode == REQUEST_CODE_LOCATION_EXCEPTION && resultCode == Activity.RESULT_OK) {
    startListeningToLocation()
  }
}
```

### Download
--------

Grab via Maven:
```xml
<dependency>
  <groupId>com.ninenine.reactivelocation</groupId>
  <artifactId>reactive-location</artifactId>
  <version>0.0.2</version>
  <type>pom</type>
</dependency>
```
or Gradle:
```groovy
repositories {
  maven { url 'http://dl.bintray.com/99/android' }
}

compile 'com.ninenine.reactivelocation:reactive-location:0.0.1'
```

Reactive-location requires at minimum Android 2.3.
