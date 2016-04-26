package au.com.codeka.warworlds.client.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.support.annotation.NonNull;

import com.google.common.base.Throwables;

import au.com.codeka.warworlds.common.Log;

/**
 * This class is similar to AsyncTask, except we get to control all the parameters.
 */
public abstract class BackgroundRunner<Result> {
  private static final Log log = new Log("BackgroundRunner");
  private String creatorStackTrace;

  private static final boolean DEBUG = false;

  private static final ThreadFactory THREAD_FACTORY = new ThreadFactory() {
    private final AtomicInteger count = new AtomicInteger(1);

    public Thread newThread(@NonNull Runnable r) {
      return new Thread(r, "BackgroundRunner #" + count.getAndIncrement());
    }
  };

  /** The maximum number of items we'll allow to be queued. */
  private static final int WORK_QUEUE_MAX_ITEMS = 750;

  /** The minimum number of threads in the thread pool. */
  private static final int MIN_THREAD_POOL_THREADS = 5;

  /** The maximum number of threads in the thread pool. */
  private static final int MAX_THREAD_POOL_THREADS = 20;

  /** The number of milliseconds to keep an idle thread in the thread pool. */
  private static final int THREAD_POOL_KEEP_ALIVE_MS = 1000;

  private static final BlockingQueue<Runnable> WORK_QUEUE =
      new LinkedBlockingQueue<>(WORK_QUEUE_MAX_ITEMS);

  private static final Executor EXECUTOR = new ThreadPoolExecutor(
      MIN_THREAD_POOL_THREADS, MAX_THREAD_POOL_THREADS, THREAD_POOL_KEEP_ALIVE_MS,
      TimeUnit.MILLISECONDS, WORK_QUEUE, THREAD_FACTORY);

  private static InternalHandler internalHandler = new InternalHandler();

  private FutureTask<Result> future;
  private final AtomicBoolean taskInvoked = new AtomicBoolean();
  private boolean isFinished;

  public BackgroundRunner() {
    if (DEBUG) {
      creatorStackTrace = Throwables.getStackTraceAsString(new Throwable());
    }

    Callable<Result> worker = new Callable<Result>() {
      @Override
      public Result call() throws Exception {
        taskInvoked.set(true);
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        return postResult(doInBackground());
      }
    };

    future = new FutureTask<Result>(worker) {
      @Override
      protected void done() {
        try {
          postResultIfNotInvoked(get());
        } catch (InterruptedException e) {
          //ignore?
        } catch (ExecutionException e) {
          throw new RuntimeException(
              "An error occured while executing doInBackground()", e.getCause());
        } catch (CancellationException e) {
          postResultIfNotInvoked(null);
        }
      }
    };
  }

  public void execute() {
    EXECUTOR.execute(future);
  }

  public boolean isFinished() {
    return isFinished;
  }

  protected abstract Result doInBackground();

  protected abstract void onComplete(Result result);

  private void postResultIfNotInvoked(Result result) {
    final boolean wasTaskInvoked = taskInvoked.get();
    if (!wasTaskInvoked) {
      postResult(result);
    }
  }

  private Result postResult(Result result) {
    @SuppressWarnings("unchecked")
    Message message = internalHandler.obtainMessage(0, new BackgroundRunnerResult<>(this, result));

    if (creatorStackTrace != null) {
      log.info("Posting message back to target from original stack trace:\r\n" + creatorStackTrace);
    }
    message.sendToTarget();
    isFinished = true;
    return result;
  }

  private static class InternalHandler extends Handler {
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void handleMessage(Message msg) {
      BackgroundRunnerResult result = (BackgroundRunnerResult) msg.obj;
      result.runner.onComplete(result.data[0]);
    }
  }

  @SuppressWarnings({"rawtypes"})
  private static class BackgroundRunnerResult<Data> {
    final BackgroundRunner runner;
    final Data[] data;

    @SafeVarargs
    BackgroundRunnerResult(BackgroundRunner runner, Data... data) {
      this.runner = runner;
      this.data = data;
    }
  }
}
