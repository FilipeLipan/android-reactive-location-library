# reactive-location

Features

* Using FusedLocationApi from Google Play Services.
* Location events will be delivered by stream via RxJava.
* It'll have a single stream for each LocationRequest.
* Support for runtime Location Permission and Location Settings errors.
* Simple errors resolution with startActivityForSolution for both Permission and Settings errors.
* Google Play Services disconnect when all requests are no longer being observed.
