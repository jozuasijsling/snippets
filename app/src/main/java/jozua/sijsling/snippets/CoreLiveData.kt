package jozua.sijsling.snippets

import android.net.Uri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import jozua.sijsling.snippets.ext.assign


// [snippet comments]
// This class uses a LiveData queue to run a command on the main activity, when it is ready for it.
// I created this for a project where I needed to strictly decouple everything from Activity code.
// It became the foundation for navigation logic. You can think of it as an event bus that the
// Activity subscribes to.

class CoreLiveData {

  private val activityCommand = MutableLiveData<ActivityCommand?>()
  private val deepLinkResolver = MutableLiveData<DeepLinkResolver?>()

  fun submitActivityCommand(command: ActivityCommand) = activityCommand.assign(command)
  fun submitDeepLinkResolver(resolver: DeepLinkResolver) = deepLinkResolver.assign(resolver)

  fun observeActivityCommands(activity: FragmentActivity) {
    activityCommand.observe(activity, Observer { command ->
      if (command != null) {
        activityCommand.value = null
        command.execute(activity)
      }
    })
  }


  fun observeDeepLinkResolvers(
    activity: FragmentActivity,
    function: (resolver: DeepLinkResolver) -> Unit
  ) {
    deepLinkResolver.observe(activity, Observer { resolver ->
      resolver?.resetLiveDataOnResolve()?.apply(function)
    })
  }

  private fun DeepLinkResolver.resetLiveDataOnResolve(): DeepLinkResolver = object :
    DeepLinkResolver {
    override fun resolve(link: Uri) {
      deepLinkResolver.value = null
      this@resetLiveDataOnResolve.resolve(link)
    }
  }

  inline fun submitActivityCommand(crossinline command: (FragmentActivity) -> Unit) {
    submitActivityCommand(object : ActivityCommand {
      override fun execute(activity: FragmentActivity) {
        command.invoke(activity)
      }
    })
  }

  inline fun submitDeepLinkResolver(crossinline resolver: (link: Uri) -> Unit) {
    submitDeepLinkResolver(object : DeepLinkResolver {
      override fun resolve(link: Uri) {
        resolver.invoke(link)
      }
    })
  }
}

interface ActivityCommand {
  fun execute(activity: FragmentActivity)
}

interface DeepLinkResolver {
  fun resolve(link: Uri)
}
