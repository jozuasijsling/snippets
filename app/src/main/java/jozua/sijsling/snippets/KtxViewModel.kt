package jozua.sijsling.snippets

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel


// [snippet comment]
// This base class sets up coroutines to be dispatched on the Main thread.
// ViewModels are completely decoupled from view lifecycle, but I still want them to respond
// to user interactivity. To do that, underlying views / fragments / activities need to signal
// when they become visible and when they are irrevocably dismissed.


/** ViewModel with coroutine support */
abstract class KtxViewModel : ViewModel(), CoroutineScope by MainScope() {

  open fun becameVisible() {

  }

  open fun dismissed() {
    
  }

  override fun onCleared() {
    cancel()
  }
}
