package au.com.codeka;

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

/**
 * This class is similar to AsyncTask, except we get to control all the parameters.
 */
public abstract class BackgroundRunner<Result> {

    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);
        public Thread newThread(Runnable r) {
            return new Thread(r, "BackgroundRunner #" + mCount.getAndIncrement());
        }
    };

    private static final BlockingQueue<Runnable> sPoolWorkQueue = new LinkedBlockingQueue<Runnable>(250);

    private static final Executor sExecutor = new ThreadPoolExecutor(
            5, // core pool size
            20, // max pool size
            1, TimeUnit.SECONDS, // keep-alive time
            sPoolWorkQueue, sThreadFactory);

    private static InternalHandler sHandler = new InternalHandler();

    private Callable<Result> mWorker;
    private FutureTask<Result> mFuture;
    private final AtomicBoolean mTaskInvoked = new AtomicBoolean();

    public BackgroundRunner() {
        mWorker = new Callable<Result>() {
            @Override
            public Result call() throws Exception {
                mTaskInvoked.set(true);
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                return postResult(doInBackground());
            }
        };

        mFuture = new FutureTask<Result>(mWorker) {
            @Override
            protected void done() {
                try {
                    postResultIfNotInvoked(get());
                } catch (InterruptedException e) {
                    //ignore?
                } catch (ExecutionException e) {
                    throw new RuntimeException("An error occured while executing doInBackground()",
                            e.getCause());
                } catch (CancellationException e) {
                    postResultIfNotInvoked(null);
                }
            }
        };
    }

    public void execute() {
        sExecutor.execute(mFuture);
    }

    protected abstract Result doInBackground();
    protected abstract void onComplete(Result result);

    private void postResultIfNotInvoked(Result result) {
        final boolean wasTaskInvoked = mTaskInvoked.get();
        if (!wasTaskInvoked) {
            postResult(result);
        }
    }

    private Result postResult(Result result) {
        @SuppressWarnings("unchecked")
        Message message = sHandler.obtainMessage(0,
                new BackgroundRunnerResult<Result>(this, result));
        message.sendToTarget();
        return result;
    }

    private static class InternalHandler extends Handler {
        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override
        public void handleMessage(Message msg) {
            BackgroundRunnerResult result = (BackgroundRunnerResult) msg.obj;
            result.mRunner.onComplete(result.mData[0]);
        }
    }

    @SuppressWarnings({"rawtypes"})
    private static class BackgroundRunnerResult<Data> {
        final BackgroundRunner mRunner;
        final Data[] mData;

        BackgroundRunnerResult(BackgroundRunner runner, Data... data) {
            mRunner = runner;
            mData = data;
        }
    }
}
