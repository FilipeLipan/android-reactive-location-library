# reactive-location

Features
--------

* Using FusedLocationApi from Google Play Services.
* Location events will be delivered by stream via RxJava.
* It'll have a single stream for each LocationRequest.
* Support for runtime Location Permission and Location Settings errors.
* Simple errors resolution with startActivityForSolution for both Permission and Settings errors.
* Google Play Services disconnect when all requests are no longer being observed.

Download
--------

Grab via Maven:
```xml
<dependency>
  <groupId>com.ninenine.reactivelocation</groupId>
  <artifactId>reactive-location</artifactId>
  <version>0.0.1</version>
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
