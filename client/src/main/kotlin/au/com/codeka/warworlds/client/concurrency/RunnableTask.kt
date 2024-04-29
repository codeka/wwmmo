package au.com.codeka.warworlds.client.concurrency

import au.com.codeka.warworlds.common.Log

typealias RunnablePR<P, R> = (P) -> R

/**
 * A [Task] that encapsulates a [Runnable] or [RunnablePR] that you want to run on a particular
 * thread.
 *
 * @param <P> The parameter type. Depending on the type of runnable you're using, this may or may
 * not be ignored.
 * @param <R> The result type. Depending on the type of runnable you're using, this may or may
 * not be ignored.
 */
class RunnableTask<P, R> : Task<P, R> {
  private val runnable: Runnable?
  private val runnablePR: RunnablePR<P, R>?
  private val thread: Threads

  constructor(taskRunner: TaskRunner, runnable: Runnable, thread: Threads): super(taskRunner) {
    this.runnable = runnable
    runnablePR = null
    this.thread = thread
  }

  constructor(taskRunner: TaskRunner, runnable: RunnablePR<P, R>, thread: Threads):
      super(taskRunner) {
    this.runnable = null
    runnablePR = runnable
    this.thread = thread
  }

  override fun run(param: P?) {
    thread.run(Runnable {
      try {
        var result: R? = null
        if (runnable != null) {
          runnable.run()
        } else if (runnablePR != null && param != null) {
          result = runnablePR.invoke(param)
        }
        onComplete(result)
      } catch (e: Exception) {
        log.error("Unexpected.", e)
        onError(e)
      }
    })
  }

  companion object {
    private val log = Log("RunnableTask")
  }
}