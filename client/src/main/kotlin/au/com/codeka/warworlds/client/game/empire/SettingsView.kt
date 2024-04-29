package au.com.codeka.warworlds.client.game.empire

import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import au.com.codeka.warworlds.client.App
import au.com.codeka.warworlds.client.R
import au.com.codeka.warworlds.client.concurrency.Threads

class SettingsView(context: Context?, private val callback: Callback) : ScrollView(context) {
  interface Callback {
    fun onPatreonConnectClick(completeCallback: PatreonConnectCompleteCallback)
  }

  interface PatreonConnectCompleteCallback {
    fun onPatreonConnectComplete(msg: String?)
  }

  init {
    View.inflate(context, R.layout.empire_settings, this)
    val patreonBtn = findViewById<Button>(R.id.patreon_btn)
    patreonBtn.setOnClickListener {
      patreonBtn.isEnabled = false
      callback.onPatreonConnectClick(
        object : PatreonConnectCompleteCallback {
          override fun onPatreonConnectComplete(msg: String?) {
            App.taskRunner.runOn(Threads.UI) {
                patreonBtn.isEnabled = true
                val msgView = findViewById<TextView>(R.id.patreon_complete)
                msgView.visibility = View.VISIBLE
                msgView.text = msg
            }
          }
        }
      )
    }
  }
}
