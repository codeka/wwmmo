package au.com.codeka.warworlds.client.concurrency;

/** Simple wrapper around GmsCore tasks API. */
public class GmsTask<R> extends Task<Void, R> {
  public GmsTask(TaskRunner taskRunner, com.google.android.gms.tasks.Task<R> gmsTask) {
    super(taskRunner);
    gmsTask.addOnFailureListener(this::onError);
    gmsTask.addOnCompleteListener(task -> {
      try {
        this.onComplete(task.getResult());
      } catch (Exception e) {
        onError(e);
      }
    });
  }
}
