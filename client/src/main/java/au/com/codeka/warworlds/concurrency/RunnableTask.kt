package au.com.codeka.warworlds.concurrency

import au.com.codeka.common.Log

/**
 * A [Task] that encapsulates a [Runnable], [RunnableP], [RunnableR] or [RunnablePR] that you want
 * to run on a particular thread.
 *
 * @param <P> The parameter type. Depending on the type of runnable you're using, this may or may
 * not be ignored.
 * @param <R> The result type. Depending on the type of runnable you're using, this may or may
 * not be ignored.
 */
class RunnableTask<P, R> : Task<P, R> {
  /** A runnable that takes a parameter.  */
  interface RunnableP<P> {
    fun run(param: P)
  }

  /** A runnable that returns a value.  */
  interface RunnableR<R> {
    fun run(): R
  }

  /** A runnable that takes a parameter and returns a value.  */
  interface RunnablePR<P, R> {
    fun run(param: P): R
  }

  private val runnable: Runnable?
  private val runnableP: RunnableP<P>?
  private val runnableR: RunnableR<R>?
  private val runnablePR: RunnablePR<P, R>?
  private val thread: Threads

  constructor(taskRunner: TaskRunner, runnable: Runnable?, thread: Threads): super(taskRunner) {
    this.runnable = runnable
    runnableP = null
    runnableR = null
    runnablePR = null
    this.thread = thread
  }

  constructor(taskRunner: TaskRunner, runnable: RunnableP<P>?, thread: Threads): super(taskRunner) {
    this.runnable = null
    runnableP = runnable
    runnableR = null
    runnablePR = null
    this.thread = thread
  }

  constructor(taskRunner: TaskRunner, runnable: RunnableR<R>?, thread: Threads): super(taskRunner) {
    this.runnable = null
    runnableP = null
    runnableR = runnable
    runnablePR = null
    this.thread = thread
  }

  constructor(taskRunner: TaskRunner, runnable: RunnablePR<P, R>?, thread: Threads):
      super(taskRunner) {
    this.runnable = null
    runnableP = null
    runnableR = null
    runnablePR = runnable
    this.thread = thread
  }

  override fun run(param: P?) {
    thread.run(Runnable {
      try {
        var result: R? = null
        if (runnable != null) {
          runnable.run()
        } else if (runnableP != null && param != null) {
          runnableP.run(param)
        } else if (runnableR != null) {
          result = runnableR.run()
        } else if (runnablePR != null && param != null) {
          result = runnablePR.run(param)
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