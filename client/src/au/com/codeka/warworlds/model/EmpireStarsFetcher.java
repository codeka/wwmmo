package au.com.codeka.warworlds.model;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import android.os.Handler;
import android.util.SparseArray;
import au.com.codeka.BackgroundRunner;
import au.com.codeka.common.Log;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.eventbus.EventBus;

/**
 * Manages fetching and caching of the stars "owned" by your empire.
 *
 * Stars that the empire owns are conceptually kept in a sorted array, ordered alphabetically by
 * the star's name. You can call {@link getStars} to get the stars we currently have cached in
 * a given range. If needed, this will make a request to the server to fetch new stars, which
 * will then be cached.
 */
public class EmpireStarsFetcher {
    private static final Log log = new Log("EmpireStarManager");

    public final EventBus eventBus = new EventBus();

    private SparseArray<WeakReference<Star>> cache = new SparseArray<WeakReference<Star>>();

    private Handler handler = new Handler();
    private ArrayList<Integer> indicesToFetch;
    private Object indicesToFetchLock = new Object();
    private Filter filter;
    private String search;
    private int numStars;

    public EmpireStarsFetcher(Filter filter, String search) {
        this.filter = filter;
        this.search = search;
    }

    /** Gets the number of stars we own. */
    public int getNumStars() {
        return numStars;
    }

    /**
     * Gets the star at the given index. If we don't have the star cached, we'll wait a couple
     * of milliseconds before fetching all the stars we've been asked for.
     */
    public Star getStar(int index) {
        WeakReference<Star> ref = cache.get(index);
        Star star = (ref != null ? ref.get() : null);
        if (star == null) {
            synchronized(indicesToFetchLock) {
                if (indicesToFetch == null) {
                    indicesToFetch = new ArrayList<Integer>();
                    handler.postDelayed(starFetchRunnable, 150);
                }
                indicesToFetch.add(index);
            }
        }

        return star;
    }

    private Runnable starFetchRunnable = new Runnable() {
        @Override
        public void run() {
            ArrayList<Integer> indices;
            synchronized(indicesToFetchLock) {
                if (indicesToFetch == null) {
                    return;
                }

                indices = new ArrayList<Integer>(indicesToFetch);
                indicesToFetch = null;
            }
            Collections.sort(indices);
            fetchStars(indices);
        }
    };

    /**
     * Attempts to fetch the stars in the empire's list of stars between the given start and end
     * index (inclusive). Stars may be null if we haven't refreshed them from the server yet, in
     * which case you should subscribe to the {@link StarFetchedEvent} to be notified when it has
     * been refreshed from the server.
     *
     * @param startIndex
     * @param endIndex
     * @return
     */
    public SparseArray<Star> getStars(int startIndex, int endIndex) {
        ArrayList<Integer> missing = null;
        SparseArray<Star> stars = new SparseArray<Star>();
        for (int i = startIndex; i <= endIndex; i++) {
            WeakReference<Star> ref = cache.get(i);
            Star star = (ref != null ? ref.get() : null);
            if (star == null) {
                if (missing == null) {
                    missing = new ArrayList<Integer>();
                }
                missing.add(i);
            } else {
                stars.put(i, star);
            }
        }

        if (missing != null) {
            fetchStars(missing);
        }

        return stars;
    }

    /** Sends a request to the server to fetch the given stars. We assume the collection is
        sorted. */
    private void fetchStars(Collection<Integer> indices) {
        final StringBuilder url = new StringBuilder();
        url.append("empires/");
        url.append(Integer.toString(EmpireManager.i.getEmpire().getID()));
        url.append("/stars?indices=");
        int lastIndex = -1;
        for (Integer index : indices) {
            if (lastIndex < 0 || lastIndex < (index - 1)) {
                if (lastIndex < 0) {
                    url.append(Integer.toString(index));
                    url.append("-");
                } else {
                    url.append(Integer.toString(lastIndex));
                    url.append(",");
                    url.append(Integer.toString(index));
                    url.append("-");
                }
            }
            lastIndex = index;
        }
        lastIndex += 5; // fetch 5 more stars than we actually need
        if (lastIndex >= numStars) {
            lastIndex = numStars - 1;
        }
        url.append(Integer.toString(lastIndex + 5));
        url.append("&filter=");
        url.append(filter.toString().toLowerCase());
        if (search != null) {
            url.append("&search=");
            try {
                url.append(URLEncoder.encode(search, "utf-8"));
            } catch (UnsupportedEncodingException e) {
                return;
            }
        }
        log.info("Fetching: %s", url);

        new BackgroundRunner<SparseArray<Star>>() {
            @Override
            protected SparseArray<Star> doInBackground() {
                try {
                    Messages.EmpireStars pb = ApiClient.getProtoBuf(url.toString(),
                            Messages.EmpireStars.class);
                    if (pb == null)
                        return null;
    
                    SparseArray<Star> stars = new SparseArray<Star>();
                    for (Messages.EmpireStar empire_star_pb : pb.getStarsList()) {
                        Star star = new Star();
                        star.fromProtocolBuffer(empire_star_pb.getStar());
                        stars.put(empire_star_pb.getIndex(), star);
                    }
    
                    numStars = pb.getTotalStars();

                    return stars;
                } catch (ApiException e) {
                    log.error("Error fetching stars!", e);
                    return null;
                }
            }

            @Override
            protected void onComplete(SparseArray<Star> result) {
                if (result != null) {
                    for (int i = 0; i < result.size(); i++) {
                        cache.put(result.keyAt(i), new WeakReference<Star>(result.valueAt(i)));
                    }

                    eventBus.publish(new StarsFetchedEvent(result));
                }
            }
        }.execute();
    }

    public static class StarsFetchedEvent {
        public SparseArray<Star> stars;

        public StarsFetchedEvent(SparseArray<Star> stars) {
            this.stars = stars;
        }
    }

    public enum Filter {
        Everything,
        Colonies,
        Fleets,
        Building,
        NotBuilding
    }
}
