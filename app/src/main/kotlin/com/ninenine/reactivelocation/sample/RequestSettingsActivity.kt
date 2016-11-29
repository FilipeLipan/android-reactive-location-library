package com.ninenine.reactivelocation.sample

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView

class RequestSettingsActivity : AppCompatActivity(), SeekBar.OnSeekBarChangeListener {

  companion object {
    const val SETTINGS_INTERVAL = "s_interval"
  }

  val seekBar by lazy { findViewById(R.id.secondsSeekBar) as SeekBar }
  val intervalSeconds by lazy { findViewById(R.id.intervalSeconds) as TextView }
  val saveButton by lazy { findViewById(R.id.saveButton) as Button }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_request_settings)

    intervalSeconds.text = getSharedPreferences(packageName, Context.MODE_PRIVATE).getInt(SETTINGS_INTERVAL, 1).toString()
    seekBar.setOnSeekBarChangeListener(this)
    saveButton.setOnClickListener {
      saveSecondsInterval()
      returnToMain()
    }
  }

  private fun returnToMain() {
    setResult(Activity.RESULT_OK)
    finish()
  }

  private fun saveSecondsInterval() {
    getSharedPreferences(packageName, Context.MODE_PRIVATE)
        .edit()
        .putInt(SETTINGS_INTERVAL, seekBar.progress)
        .apply()
  }

  override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
    intervalSeconds.text = progress.toString()
  }

  override fun onStartTrackingTouch(seekBar: SeekBar?) {
    // Do nothing
  }

  override fun onStopTrackingTouch(seekBar: SeekBar?) {
    // Do nothing
  }

}
