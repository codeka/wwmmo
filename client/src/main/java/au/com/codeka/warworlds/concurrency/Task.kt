package au.com.codeka.warworlds.concurrency

import kotlin.collections.ArrayList

/**
 * Wrapper for a "task", which allows chaining of following tasks (via [.then]) and handling
 * of errors (via [.error]).
 *
 * @param <P> the type of the input parameter to this task. Can be Void if you want nothing.
 * @param <R> the type of the return value from the task. Can be Void if you want nothing.
 */
open class Task<P, R> internal constructor(private val taskRunner: TaskRunner) {
  private var thenTasks: MutableList<Task<R, *>>? = null
  private var errorTasks: MutableList<Task<Exception?, Void?>>? = null
  private val lock = Any()
  private var finished = false
  private var result: R? = null
  private var error: Exception? = null

  open fun run(param: P?) {}

  protected fun onComplete(result: R?) {
    synchronized(lock) {
      finished = true
      this.result = result

      val remaining = thenTasks ?: return
      thenTasks = null
      for (task in remaining) {
        taskRunner.runTask(task, result)
      }
    }
  }

  protected fun onError(error: Exception?) {
    synchronized(lock) {
      finished = true
      this.error = error
      if (errorTasks != null) {
        for (task in errorTasks!!) {
          taskRunner.runTask(task, null)
        }
        errorTasks = null
      }
    }
  }

  /**
   * Queues up the given [Task] to run after this task. It will be handed this task's result
   * as it's parameter.
   * @param task The task to queue after this task completes.
   * @return The new task (so you can chain .then().then().then() calls to get tasks to run one
   * after the other.
   */
  fun <TR> then(task: Task<R, TR>): Task<R, TR> {
    synchronized(lock) {
      if (finished) {
        if (error == null) {
          taskRunner.runTask(task, result)
        }
        return task
      }
      val remaining = thenTasks ?: ArrayList()
      remaining.add(task)
      thenTasks = remaining
    }
    return task
  }

  /**
   * Queues the given runnable to run after this task. If this task returns a result, obviously the
   * runnable will not know what it was.
   *
   * @param runnable The runnable to run after this task completes.
   * @param thread The [Threads] on which to run the runnable.
   * @return The new task (so you can chain .then().then().then() calls to get tasks to run one
   * after the other.
   */
  fun then(runnable: Runnable?, thread: Threads): Task<R, Void> {
    return then(RunnableTask(taskRunner, runnable, thread))
  }

  /**
   * Queues the given runnable to run after this task. If this task returns a result, obviously the
   * runnable will not know what it was.
   *
   * @param runnable The runnable to run after this task completes.
   * @param thread The [Threads] on which to run the runnable.
   * @return The new task (so you can chain .then().then().then() calls to get tasks to run one
   * after the other.
   */
  fun then(runnable: RunnableTask.RunnableP<R>?, thread: Threads): Task<R, Void> {
    return then(RunnableTask(taskRunner, runnable, thread))
  }

  /**
   * Queues the given runnable to run after this task. If this task returns a result, obviously the
   * runnable will not know what it was.
   *
   * @param runnable The runnable to run after this task completes.
   * @param thread The [Threads] on which to run the runnable.
   * @return The new task (so you can chain .then().then().then() calls to get tasks to run one
   * after the other.
   */
  fun <RR> then(runnable: RunnableTask.RunnableR<RR>?, thread: Threads): Task<R, RR> {
    return then(RunnableTask(taskRunner, runnable, thread))
  }

  /**
   * Queues the given runnable to run after this task. If this task returns a result, obviously the
   * runnable will not know what it was.
   *
   * @param runnable The runnable to run after this task completes.
   * @param thread The [Threads] on which to run the runnable.
   * @return The new task (so you can chain .then().then().then() calls to get tasks to run one
   * after the other.
   */
  fun <RR> then(runnable: RunnableTask.RunnablePR<R, RR>?, thread: Threads): Task<R, RR> {
    return then(RunnableTask(taskRunner, runnable, thread))
  }

  /**
   * Queue a task to run in case there's an exception running the current task.
   *
   * @param task The task to run if there's an error.
   * @return The current task, so you can queue up calls like task.error().then() to handle both
   * the error case and the 'next' case.
   */
  fun error(task: Task<Exception?, Void?>): Task<P, R> {
    synchronized(lock) {
      if (finished) {
        if (error != null) {
          taskRunner.runTask(task, error)
        }
        return this
      }
      if (errorTasks == null) {
        errorTasks = ArrayList()
      }
      errorTasks!!.add(task)
    }
    return this
  }

  fun error(runnable: Runnable?, thread: Threads): Task<P, R> {
    return error(RunnableTask(taskRunner, runnable, thread))
  }

  fun <E : java.lang.Exception> error(runnable: RunnableTask.RunnableP<E?>?, thread: Threads): Task<P, R> {
    error(RunnableTask<E?, Void?>(taskRunner, runnable, thread))
  }
}