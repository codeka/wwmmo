package au.com.codeka.warworlds.server;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * This class is similar to BackgroundRunner in the client, except that
 * it doesn't support a "complete" event. Basically just a convenient wrapper
 * around a thread pool.
 */
public abstract class BackgroundRunner {
    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);
        public Thread newThread(Runnable r) {
            return new Thread(r, "BackgroundRunner #" + mCount.getAndIncrement());
        }
    };

    private static final BlockingQueue<Runnable> sPoolWorkQueue =
            new LinkedBlockingQueue<Runnable>(250);

    private static final Executor sExecutor = new ThreadPoolExecutor(
            5, // core pool size
            20, // max pool size
            1, TimeUnit.SECONDS, // keep-alive time
            sPoolWorkQueue, sThreadFactory);

    private Callable<Void> mWorker;
    private FutureTask<Void> mFuture;
    private final AtomicBoolean mTaskInvoked = new AtomicBoolean();
    private boolean mIsFinished;

    public BackgroundRunner() {
        mWorker = new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                mTaskInvoked.set(true);
                Thread.currentThread().setPriority(Thread.NORM_PRIORITY - 1);
                doInBackground();
                return null;
            }
        };

        mFuture = new FutureTask<Void>(mWorker) {
            @Override
            protected void done() {
                mIsFinished = true;
            }
        };
    }

    public void execute() {
        sExecutor.execute(mFuture);
    }

    public boolean isFinished() {
        return mIsFinished;
    }

    protected abstract void doInBackground();
}
