package au.com.codeka.warworlds.model;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.os.AsyncTask;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;

public class BuildQueueManager {
    private static Logger log = LoggerFactory.getLogger(BuildQueueManager.class);
    private static BuildQueueManager sInstance = new BuildQueueManager();

    public static BuildQueueManager getInstance() {
        return sInstance;
    }

    private List<BuildQueueUpdatedListener> mBuildQueueUpdatedListeners =
            new ArrayList<BuildQueueUpdatedListener>();
    private BuildQueueMonitor mBuildQueueMonitor;
    private List<BuildRequest> mCurrentQueue;

    public void addBuildQueueUpdatedListener(BuildQueueUpdatedListener listener) {
        mBuildQueueUpdatedListeners.add(listener);
    }
    public void removeBuildQueueUpdatedListener(BuildQueueUpdatedListener listener) {
        mBuildQueueUpdatedListeners.remove(listener);
    }

    protected void fireBuildQueueUpdatedListeners(List<BuildRequest> queue) {
        for(BuildQueueUpdatedListener listener : mBuildQueueUpdatedListeners) {
            listener.onBuildQueueUpdated(queue);
        }
    }

    public void setup() {
        mBuildQueueMonitor = new BuildQueueMonitor();
        addBuildQueueUpdatedListener(mBuildQueueMonitor);
        fetchBuildQueue(null);
    }

    public List<BuildRequest> getCurrentQueue() {
        return mCurrentQueue;
    }

    public List<BuildRequest> getBuildQueueForColony(Colony colony) {
        List<BuildRequest> colonyQueue = new ArrayList<BuildRequest>();
        for (BuildRequest req : mCurrentQueue) {
            if (req.getColonyKey().equals(colony.getKey())) {
                colonyQueue.add(req);
            }
        }
        return colonyQueue;
    }

    /**
     * This is called by various components when they change something that causes the build
     * queue to potentially change (e.g. if you adjusted a colony's focus, queued a new building,
     * stuff of that nature).
     */
    public void refresh() {
        // TODO: do we have to refresh it every time? At least maybe we can put in a delay so
        // that we don't refresh over and over...
        fetchBuildQueue(null);
    }

    /**
     * This version of \c refresh() is specially called when you've queued a new build request. We
     * just add it to the list without re-querying the server again.
     */
    public void refresh(BuildRequest request) {
        mCurrentQueue.add(request);
        fireBuildQueueUpdatedListeners(mCurrentQueue);
    }

    /**
     * Fetches the current list of \c BuildRequest objects for the current user, representing
     * all of the currently in-progress build operations.
     */
    private void fetchBuildQueue(final BuildQueueUpdatedListener callback) {
        new AsyncTask<Void, Void, List<BuildRequest>>() {
            @Override
            protected List<BuildRequest> doInBackground(Void... arg0) {
                warworlds.Warworlds.BuildQueue buildQueue;
                try {
                    buildQueue = ApiClient.getProtoBuf("buildqueue",
                                                       warworlds.Warworlds.BuildQueue.class);
                } catch (ApiException e) {
                    log.error("Could not fetch current build queue!", e);
                    return null;
                }

                List<BuildRequest> queue = new ArrayList<BuildRequest>();
                for(int i = 0; i < buildQueue.getRequestsCount(); i++) {
                    warworlds.Warworlds.BuildRequest pb = buildQueue.getRequests(i);
                    queue.add(BuildRequest.fromProtocolBuffer(pb));
                }
                return queue;
            }
            @Override
            protected void onPostExecute(List<BuildRequest> queue) {
                if (queue != null) {
                    mCurrentQueue = queue;
                    fireBuildQueueUpdatedListeners(queue);
                    if (callback != null) {
                        callback.onBuildQueueUpdated(queue);
                    }
                }
            }
        }.execute();
    }

    public interface BuildQueueUpdatedListener {
        void onBuildQueueUpdated(List<BuildRequest> queue);
    }

    /**
     * This is a global listener that monitors the build queue and fires the BuildCompletedListener
     * event handlers that are waiting for builds to complete.
     */
    private static class BuildQueueMonitor implements BuildQueueUpdatedListener {
        /**
         * This is called when the build queue changes (usually because you've changed a parameter
         * that affects build times, built a new structure, or whatever).
         */
        @Override
        public void onBuildQueueUpdated(List<BuildRequest> queue) {
            for (BuildRequest req : queue) {
                // TODO: figue out when it finishes, find the one coming up and wait for that
                // to happen...
            }
        }
    }
}
