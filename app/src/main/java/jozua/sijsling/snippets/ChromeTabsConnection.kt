package jozua.sijsling.snippets

import android.app.Application
import android.content.ComponentName
import android.os.Bundle
import android.os.RemoteException
import android.util.Log
import androidx.browser.customtabs.CustomTabsCallback
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession


// [snippet comments]
// As usual the API from Google is just awful. This class wraps around it giving a single
// function to setup the connection and receive a callback when connected. This class assumes
// that navigation is decoupled from the Android Lifecycle. Not many projects can actually use
// a class like this.


/** Simplifies connecting to Custom Tabs */
class ChromeTabsConnection(private val application: Application) : CustomTabsServiceConnection() {

  private var connectionRequested = false
  private var clientAndSession: Pair<CustomTabsClient, CustomTabsSession>? = null
  private val taskQueue = mutableListOf<(CustomTabsClient, CustomTabsSession) -> Unit>()

  override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
    val session = client.newSession(object : CustomTabsCallback() {
      override fun onNavigationEvent(navigationEvent: Int, extras: Bundle?) {
        super.onNavigationEvent(navigationEvent, extras)
        Log.i("Chrome", "Navigation event received: code=$navigationEvent")
      }
    }) ?: throw RemoteException("Chrome browser not available.")
    taskQueue.forEach { task -> task.invoke(client, session) }
    taskQueue.clear()
    this.clientAndSession = client to session
  }

  override fun onServiceDisconnected(name: ComponentName) {
    this.clientAndSession = null // the library will try to reconnect
  }

  fun enqueueTask(task: (client: CustomTabsClient, session: CustomTabsSession?) -> Unit) {
    val clientAndSessionCopy = clientAndSession
    if (clientAndSessionCopy != null) {
      task.invoke(clientAndSessionCopy.first, clientAndSessionCopy.second)
    } else {
      taskQueue += task

      if (!connectionRequested) {
        connect(application)
        connectionRequested = true
      }
    }
  }

  private fun connect(application: Application) {
    val bound = CustomTabsClient.bindCustomTabsService(application, "com.android.chrome", this)
    check(bound) { "Unable to connect to Chrome" }
  }
}
