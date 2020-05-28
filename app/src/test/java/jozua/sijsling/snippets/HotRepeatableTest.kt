package jozua.sijsling.snippets

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.Executor


// [snippet comments]
// I was asked not to write tests and focus on speed of development instead. This is one of those
// cases where I wrote tests anyway. The class is complex and a bug will lead to multi-threading
// issues that are very hard to spot. The time spent writing these tests is more than worth it.


class HotRepeatableTest {

  private val counter = Counter()
  private val testExecutor = TestExecutor()
  private val testDispatcher = testExecutor.asCoroutineDispatcher()
  private val hotRepeatable = HotRepeatable(counter::inc, testDispatcher)

  @Test
  fun `jobs are not started without calling trigger()`() {
    assertEquals(0, counter.jobCount)
  }

  @Test
  fun `job from trigger() does nothing if not started`() {
    val job = hotRepeatable.trigger()
    testExecutor.triggerActions()
    assertFalse(job.isActive)
  }

  @Test
  fun `calling trigger() while ongoing does not start a new job`() {
    repeat(10) { hotRepeatable.trigger().start() }
    testExecutor.triggerActions()
    assertEquals(1, counter.jobCount)
  }

  @Test
  fun `calling trigger() after job ends does start new jobs`() {
    repeat(10) {
      hotRepeatable.trigger().start()
      testExecutor.triggerActions()
    }
    assertEquals(10, counter.jobCount)
  }

  @Test(expected = TimeoutCancellationException::class)
  fun `timeout test - trigger is timeout cancellable`() {
    runBlocking {
      withTimeout(50) {
        val job = hotRepeatable.trigger()
        job.start()
        job.await()
        Unit
      }
      Unit
    }
  }

  @Test
  fun `timeout test - remaining observers can still observe result`() {
    val job = hotRepeatable.trigger()
    try {
      runBlocking {
        withTimeout(50) {
          job.start()
          job.await()
          Unit
        }
        Unit
      }
    } catch (_: TimeoutCancellationException) {

    }

    val sameJob = hotRepeatable.trigger()
    sameJob.start()
    testExecutor.triggerActions()
    runBlocking {
      assertEquals(1, sameJob.await())
    }
  }

  @Test
  fun `exceptions are shared with all observers`() {
    val preparedException = RuntimeException("Oops!")
    val crashingRepeatable = HotRepeatable({
      throw preparedException
    }, testDispatcher)

    val jobs = (1..10).map {
      crashingRepeatable.trigger()
    }
    jobs.forEach { it.start() }
    testExecutor.triggerActions()

    val errors: List<Exception> = jobs.map {
      try {
        runBlocking { it.await() }
        throw IllegalStateException()
      } catch (error: Exception) {
        error
      }
    }

    assertEquals(10, errors.size)
    assertTrue(errors.all { it is RuntimeException })
    assertTrue(errors.all { it.message == preparedException.message })
  }

  class TestExecutor : Executor {
    private val commands = mutableListOf<Runnable>()

    override fun execute(command: Runnable) {
      commands += command
    }

    fun triggerActions() {
      while (commands.isNotEmpty()) {
        val commandsCopy = ArrayList(commands)
        commands.clear()
        commandsCopy.forEach { it.run() }
      }
    }
  }

  class Counter {
    var jobCount: Int = 0
    fun inc(): Int {
      jobCount++
      return jobCount
    }
  }
}
