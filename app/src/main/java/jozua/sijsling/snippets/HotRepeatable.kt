package jozua.sijsling.snippets

import jozua.sijsling.snippets.ext.Background
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean


// [snippet comments]
// This class takes lessons from RxJava and applies them to coroutines. It lets multiple events
// trigger a job, that must only run once at a time. Neither RxJava nor coroutines have a simple
// solution for this. We use an atomic boolean to signal when it is possible to queue a new job.
// When the job completes all callers get the same result and the signal flips so that a new job
// can be queued again. Note that it's easy to introduce bugs because we are dealing with
// multiple threads here, flipping the atomic boolean must be done at EXACTLY the right time.
// Check the test class for more details.


/** Runs function purely sequentially regardless of number of subscribers. */
internal class HotRepeatable<T : Any>(
  private val function: () -> T,
  override val coroutineContext: CoroutineDispatcher = Dispatchers.Background
) : CoroutineScope {

  private var ongoing = AtomicBoolean(true)
  private var job: Deferred<T> = asOngoingAsync(function)

  /** Returns ongoing job or creates a new one. */
  fun trigger(): Deferred<T> {
    if (ongoing.compareAndSet(false, true)) {
      job = asOngoingAsync(function)
    }
    return job
  }

  private fun asOngoingAsync(fn: () -> T): Deferred<T> = async(start = CoroutineStart.LAZY) {
    fn().also {
      ongoing.set(false)
    }
  }
}
