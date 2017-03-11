package au.com.codeka.warworlds.server.util;

import com.google.common.util.concurrent.AbstractFuture;
import com.google.firebase.tasks.Task;

/**
 * A simple future that wraps a firebase {@link Task}.
 */
public class TaskFuture<T> extends AbstractFuture<T> {
  public TaskFuture(Task<T> task) {
    task.addOnCompleteListener(t -> set(t.getResult()));
    task.addOnFailureListener(this::setException);
  }
}
