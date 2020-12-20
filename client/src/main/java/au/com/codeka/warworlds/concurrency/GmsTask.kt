package au.com.codeka.warworlds.concurrency

/** Simple wrapper around GmsCore tasks API.  */
class GmsTask<R>(taskRunner: TaskRunner, gmsTask: com.google.android.gms.tasks.Task<R>):
    Task<Void, R>(taskRunner) {
  init {
    gmsTask.addOnFailureListener { error: Exception? -> onError(error) }
    gmsTask.addOnCompleteListener { task: com.google.android.gms.tasks.Task<R> ->
      try {
        onComplete(task.result)
      } catch (e: Exception) {
        onError(e)
      }
    }
  }
}
