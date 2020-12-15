package au.com.codeka.warworlds.model;

import android.util.SparseArray;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.Future;

import javax.annotation.Nullable;

import androidx.collection.LruCache;

import com.android.billingclient.api.Purchase;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import au.com.codeka.common.Log;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.api.ApiRequest;
import au.com.codeka.warworlds.api.RequestManager;
import au.com.codeka.warworlds.eventbus.EventBus;

public class StarManager extends BaseManager {
  private static final Log log = new Log("StarManager");
  public static final StarManager i = new StarManager();
  public static final EventBus eventBus = new EventBus();

  /** We keep an in-memory cache for instant retrieval as well. */
  private final LruCache<Integer, Star> stars = new LruCache<>(100);
  private final SparseArray<InProgressRequestInfo> inProgress = new SparseArray<>();

  public void clearCache() {
    stars.evictAll();
  }

  /**
   * Gets a single star from our in-memory cache. If no star is cached, we'll return null and
   * request the star from the server. Listen to the event bus to find out when the star is fetched.
   */
  @Nullable
  public Star getStar(int starID) {
    Star star = stars.get(starID);
    if (star != null) {
      return star;
    }

    refreshStar(starID);
    return null;
  }

  /**
   * Gets a star either from our in-memory cache, or the server. Returns a future that you can wait
   * on if you need it immediately.
   */
  public ListenableFuture<Star> requireStar(int starID) {
    Star star = stars.get(starID);
    if (star != null) {
      return Futures.immediateFuture(star);
    }

    SettableFuture<Star> future = SettableFuture.create();
    refreshStar(starID, future);
    return future;
  }

  /**
   * Gets all the stars we have with the given identifier from our in-memory cache, and initiates a
   * request to the server to fetch the remainder.
   */
  public SparseArray<Star> getStars(Collection<Integer> starIDs) {
    SparseArray<Star> result = new SparseArray<>();

    ArrayList<Integer> toFetch = null;
    for (Integer starID : starIDs) {
      Star star = stars.get(starID);
      if (star != null) {
        result.put(star.getID(), star);
      } else {
        if (toFetch == null) {
          toFetch = new ArrayList<>();
        }
        toFetch.add(starID);
      }
    }

    if (toFetch != null) {
      for (Integer starID : toFetch) {
        refreshStar(starID, false, null);
      }
    }
    return result;
  }

  /**
   * This can be called when a star is updated outside the context of StarManager. It will notify
   * the rest of the system that the star is updated.
   */
  public void notifyStarUpdated(Star star) {
    stars.put(star.getID(), star);
    eventBus.publish(star);
  }

  /**
   * Refresh the star with the given from the server. An event will be posted to notify when the
   * star is updated.
   */
  public void refreshStar(int starID) {
    refreshStar(starID, false, null);
  }

  /** Refresh the star, passing a future where we'll populate the value once it's been refreshed. */
  public void refreshStar(int starID, SettableFuture<Star> future) {
    refreshStar(starID, false, future);
  }

  /**
   * Refresh the star with the given from the server. An event will be posted to notify when the
   * star is updated.
   *
   * @param onlyIfCached If true, we'll only refresh the star if we already have a cached version,
   *                     otherwise this will do nothing.
   * @param future Optional Future that we'll populate with the value of the star when it's
   *              refreshed.
   */
  public boolean refreshStar(
      final int starID, boolean onlyIfCached, @Nullable SettableFuture<Star> future) {
    if (onlyIfCached && stars.get(starID) == null) {
      log.debug("Not updating star, onlyIfCached = true and star is not cached.");
      return false;
    }

    ApiRequest apiRequest;
    synchronized (inProgress) {
      InProgressRequestInfo info = inProgress.get(starID);
      if (info != null) {
        log.debug("Star is already being refreshed, not calling again.");
        if (future != null) {
          info.ensureFutures().add(future);
        }
        return true;
      }

      apiRequest = new ApiRequest.Builder(String.format("stars/%s", starID), "GET")
          .completeCallback(requestCompleteCallback)
          .skipCache(true)
          .build();
      inProgress.put(starID, new InProgressRequestInfo(apiRequest, future));
    }

    RequestManager.i.sendRequest(apiRequest);
    return true;
  }

  private final ApiRequest.CompleteCallback requestCompleteCallback =
      request -> {
        Messages.Star starPb = request.body(Messages.Star.class);
        if (starPb != null) {
          Star star = new Star();
          star.fromProtocolBuffer(starPb);
          stars.put(star.getID(), star);

          InProgressRequestInfo info = inProgress.get(star.getID());
          inProgress.remove(star.getID());

          Runnable completeCallback = null;
          if (info != null && info.futures != null) {
            completeCallback = () -> {
              for (SettableFuture<Star> future : info.futures) {
                future.set(star);
              }
            };
          }

          // Enqueue the star so that it get simulated, this will post to the event bus when
          // simulation finishes, so we don't need to do that here.
          log.debug("Star %d %s fetched from server, simulating...", star.getID(), star.getName());
          StarSimulationQueue.i.simulate(star, true, completeCallback);
        }
      };

  public void renameStar(final Purchase purchase, final Star star, final String newName,
                         final StarRenameCompleteHandler onCompleteHandler) {
    Messages.StarRenameRequest.Builder pb =
        Messages.StarRenameRequest.newBuilder().setStarKey(star.getKey())
            .setOldName(star.getName()).setNewName(newName);
    if (purchase != null) {
      pb.setPurchaseInfo(PurchaseManager.i.toProtobuf(purchase.getSku(), purchase));
    }

    ApiRequest request =
        new ApiRequest.Builder(String.format(Locale.ENGLISH, "stars/%d", star.getID()), "PUT")
            .body(pb.build())
            .completeCallback(request1 -> {
              Messages.Star starPb = request1.body(Messages.Star.class);
              Star star1 = new Star();
              star1.fromProtocolBuffer(starPb);

              notifyStarUpdated(star1);
              if (onCompleteHandler != null) {
                onCompleteHandler.onStarRename(star1, true, null);
              }
            })
            .build();
    RequestManager.i.sendRequest(request);
  }

  public interface StarRenameCompleteHandler {
    void onStarRename(Star star, boolean successful, String errorMessage);
  }

  private class InProgressRequestInfo {
    public ApiRequest request;
    ArrayList<SettableFuture<Star>> futures;

    public InProgressRequestInfo(ApiRequest request, @Nullable SettableFuture<Star> future) {
      this.request = request;
      if (future != null) {
        ensureFutures().add(future);
      }
    }

    public ArrayList<SettableFuture<Star>> ensureFutures() {
      if (futures == null) {
        futures = new ArrayList<>();
      }
      return futures;
    }
  }
}
