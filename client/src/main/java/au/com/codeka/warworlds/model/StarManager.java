package au.com.codeka.warworlds.model;

import android.support.v4.util.LruCache;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.Nullable;

import au.com.codeka.common.Log;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.api.ApiRequest;
import au.com.codeka.warworlds.api.RequestManager;
import au.com.codeka.warworlds.eventbus.EventBus;
import au.com.codeka.warworlds.model.billing.IabException;
import au.com.codeka.warworlds.model.billing.Purchase;
import au.com.codeka.warworlds.model.billing.SkuDetails;

public class StarManager extends BaseManager {
  private static final Log log = new Log("StarManager");
  public static final StarManager i = new StarManager();
  public static final EventBus eventBus = new EventBus();

  /** We keep an in-memory cache for instant retrieval as well. */
  private final LruCache<Integer, Star> stars = new LruCache<>(100);
  private final SparseArray<ApiRequest> inProgress = new SparseArray<>();

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
        refreshStar(starID, false);
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
    refreshStar(starID, false);
  }

  /**
   * Refresh the star with the given from the server. An event will be posted to notify when the
   * star is updated.
   *
   * @param onlyIfCached If true, we'll only refresh the star if we already have a cached version,
   *                     otherwise this will do nothing.
   */
  public boolean refreshStar(final int starID, boolean onlyIfCached) {
    if (onlyIfCached && stars.get(starID) == null) {
      log.debug("Not updating star, onlyIfCached = true and star is not cached.");
      return false;
    }

    synchronized (inProgress) {
      if (inProgress.get(starID) != null) {
        log.debug("Star is already being refreshed, not calling again.");
        return true;
      }
    }

    RequestManager.i.sendRequest(new ApiRequest.Builder(String.format("stars/%s", starID), "GET")
        .completeCallback(requestCompleteCallback).build());
    return true;
  }

  private final ApiRequest.CompleteCallback requestCompleteCallback =
      new ApiRequest.CompleteCallback() {
    @Override
    public void onRequestComplete(ApiRequest request) {
      Messages.Star starPb = request.body(Messages.Star.class);
      if (starPb != null) {
        Star star = new Star();
        star.fromProtocolBuffer(starPb);
        stars.put(star.getID(), star);

        log.debug("Star %s refreshed, publishing event...", star.getName());
        eventBus.publish(star);
        inProgress.remove(star.getID());
      }
    }
  };

  public void renameStar(final Purchase purchase, final Star star, final String newName,
      final StarRenameCompleteHandler onCompleteHandler) {
    String price = "???";
    SkuDetails sku = null;
    if (purchase != null) {
      try {
        sku = PurchaseManager.i.getInventory().getSkuDetails(purchase.getSku());
      } catch (IabException e1) {
        // Just ignore.
      }
    }
    if (sku != null) {
      price = sku.getPrice();
    }

    Messages.StarRenameRequest.Builder pb =
        Messages.StarRenameRequest.newBuilder().setStarKey(star.getKey())
            .setOldName(star.getName()).setNewName(newName);
    if (purchase != null) {
      pb.setPurchaseInfo(Messages.PurchaseInfo.newBuilder().setSku(purchase.getSku())
          .setOrderId(purchase.getOrderId()).setPrice(price).setToken(purchase.getToken())
          .setDeveloperPayload(purchase.getDeveloperPayload()));
    }

    ApiRequest request = new ApiRequest.Builder(String.format("stars/%d", star.getID()), "PUT")
        .body(pb.build())
        .completeCallback(new ApiRequest.CompleteCallback() {
          @Override
          public void onRequestComplete(ApiRequest request) {
            // if failure() {
            //  onCompleteHandler.onStarRename(null, false, errorMessage);
            // }
            Messages.Star starPb = request.body(Messages.Star.class);
            Star star = new Star();
            star.fromProtocolBuffer(starPb);

            notifyStarUpdated(star);
            if (onCompleteHandler != null) {
              onCompleteHandler.onStarRename(star, true, null);
            }
          }
        })
        .build();
    RequestManager.i.sendRequest(request);
  }

  public interface StarRenameCompleteHandler {
    void onStarRename(Star star, boolean successful, String errorMessage);
  }
}
