package com.ninenine.reactivelocation.sample

import android.app.Application
import com.squareup.leakcanary.LeakCanary

class App : Application() {

  override fun onCreate() {
    super.onCreate()
    if (LeakCanary.isInAnalyzerProcess(this)) {
      return
    }
    LeakCanary.install(this)
  }
}
