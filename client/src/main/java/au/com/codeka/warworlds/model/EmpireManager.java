package au.com.codeka.warworlds.model;

import android.net.Uri;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import androidx.collection.LruCache;
import au.com.codeka.common.Log;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.api.ApiRequest;
import au.com.codeka.warworlds.api.RequestManager;
import au.com.codeka.warworlds.eventbus.EventBus;
import au.com.codeka.warworlds.eventbus.EventHandler;

/**
 * Manages stuff about your empire (e.g. colonising planets and what-not).
 */
public class EmpireManager {
  private static final Log log = new Log("EmpireManager");
  public static final EmpireManager i = new EmpireManager();

  public static final EventBus eventBus = new EventBus();

  private LruCache<Integer, Empire> empireCache = new LruCache<>(64);
  private final HashSet<Integer> inProgress = new HashSet<>();
  private MyEmpire myEmpire;
  private NativeEmpire nativeEmpire = new NativeEmpire();

  private EmpireManager() {
    AllianceManager.eventBus.register(eventHandler);
  }

  /**
   * This is called when you first connect to the server. We need to pass in details about
   * the myEmpire and stuff.
   */
  public void setup(MyEmpire empire) {
    this.myEmpire = empire;
  }

  /**
   * Gets a reference to the current empire.
   */
  public MyEmpire getEmpire() {
    return myEmpire;
  }

  public void clearEmpire() {
    myEmpire = null;
  }

  public Empire getEmpire(Integer empireID) {
    if (empireID == null) {
      return nativeEmpire;
    }

    if (empireID == myEmpire.getID()) {
      return myEmpire;
    }

    Empire empire = empireCache.get(empireID);
    if (empire != null) {
      return empire;
    }

    // Refresh the empire from the server, but just return null for now.
    refreshEmpire(empireID, false);
    return null;
  }

  /**
   * Gets all of the empires with the given IDs. If we don't have them all cached, you may not
   * get all of the ones you ask for.
   */
  public List<Empire> getEmpires(Collection<Integer> empireIDs) {
    ArrayList<Empire> empires = new ArrayList<>();
    ArrayList<Integer> missing = new ArrayList<>();

    for (Integer empireID : empireIDs) {
      if (empireID == myEmpire.getID()) {
        empires.add(myEmpire);
        continue;
      }

      Empire empire = empireCache.get(empireID);
      if (empire != null) {
        empires.add(empire);
        continue;
      }

      missing.add(empireID);
    }

    if (missing.size() > 0) {
      refreshEmpires(missing, false);
    }

    return empires;
  }

  public NativeEmpire getNativeEmpire() {
    return nativeEmpire;
  }

  public void refreshEmpire() {
    if (myEmpire == null) {
      return;
    }

    refreshEmpire(myEmpire.getID(), false);
  }

  public void refreshEmpire(final Integer empireID) {
    refreshEmpire(empireID, false);
  }

  public void refreshEmpire(final Integer empireID, boolean skipCache) {
    if (empireID == null) {
      // Nothing to do for native empires
      return;
    }

    ArrayList<Integer> empireIDs = new ArrayList<>();
    empireIDs.add(empireID);
    refreshEmpires(empireIDs, skipCache);
  }

  public void refreshEmpires(final Collection<Integer> empireIDs, boolean skipCache) {
    for (Integer empireID : empireIDs) {
      synchronized (inProgress) {
        if (!inProgress.contains(empireID)) {
          inProgress.add(empireID);
        } else {
          continue;
        }

        String url = String.format("empires/search?ids=%d", empireID);
        RequestManager.i.sendRequest(new ApiRequest.Builder(url, "GET")
            .completeCallback(createSearchCompleteCallback(null))
            .errorCallback(refreshEmpiresErrorCallback)
            .skipCache(skipCache).build());
      }
    }
  }

  private final ApiRequest.ErrorCallback refreshEmpiresErrorCallback =
    new ApiRequest.ErrorCallback() {
      @Override
      public void onRequestError(ApiRequest request, Messages.GenericError error) {
        // TODO: handle errors
      }
    };

  /**
   * Searches empires in the given rank range. This will always include the top three
   * empires as well (since that's usually what you want in addition to the specific
   * range you asked for as well).
   */
  public void searchEmpiresByRank(final int minRank, final int maxRank,
      final SearchCompleteHandler handler) {
    Uri uri = Uri.parse("empires/search")
        .buildUpon()
        .appendQueryParameter("noLeader", "1")
        .appendQueryParameter("minRank", Integer.toString(minRank))
        .appendQueryParameter("maxRank", Integer.toString(maxRank))
        .build();
    RequestManager.i.sendRequest(new ApiRequest.Builder(uri, "GET")
        .completeCallback(createSearchCompleteCallback(handler))
        .build());
  }

  public void searchEmpires(final String nameSearch, final SearchCompleteHandler handler) {
    Uri uri = Uri.parse("empires/search")
        .buildUpon()
        .appendQueryParameter("name", nameSearch)
        .build();
    RequestManager.i.sendRequest(new ApiRequest.Builder(uri, "GET")
        .completeCallback(createSearchCompleteCallback(handler))
        .build());
  }

  private ApiRequest.CompleteCallback createSearchCompleteCallback(
      @Nullable final SearchCompleteHandler handler) {
    return new ApiRequest.CompleteCallback() {
      @Override
      public void onRequestComplete(ApiRequest request) {
        List<Empire> empires = new ArrayList<>();

        Messages.Empires pbs = request.body(Messages.Empires.class);
        if (pbs == null) {
          return;
        }
        for (Messages.Empire pb : pbs.getEmpiresList()) {
          Empire newEmpire = new Empire();
          newEmpire.fromProtocolBuffer(pb);
          empires.add(newEmpire);

          if (myEmpire != null && pb.getKey().equals(myEmpire.getKey())) {
            MyEmpire myEmpire = new MyEmpire();
            myEmpire.fromProtocolBuffer(pb);
            newEmpire = myEmpire;
            if (EmpireManager.this.myEmpire.getAlliance() != null && myEmpire.getAlliance() == null) {
              log.warning("Old myEmpire has an alliance, new myEmpire does not!!");
            }
            EmpireManager.this.myEmpire = myEmpire;
          } else {
            newEmpire.fromProtocolBuffer(pb);
            empireCache.put(newEmpire.getID(), newEmpire);
          }

          empireCache.put(newEmpire.getID(), newEmpire);
          eventBus.publish(newEmpire);
        }

        if (handler != null) {
          handler.onSearchComplete(empires);
        }
      }
    };
  }

  private final Object eventHandler = new Object() {
    @EventHandler
    public void onAllianceUpdated(Alliance alliance) {
      if (myEmpire != null && myEmpire.getAlliance() != null && myEmpire.getAlliance().getKey()
          .equals(alliance.getKey())) {
        myEmpire.updateAlliance(alliance);
        eventBus.publish(myEmpire);
      }

      for (Map.Entry<Integer, Empire> entry : empireCache.snapshot().entrySet()) {
        Empire empire = entry.getValue();
        if (empire != null && empire.getAlliance() != null && empire.getAlliance().getKey()
            .equals(alliance.getKey())) {
          empire.updateAlliance(alliance);
          eventBus.publish(empire);
        }
      }
    }
  };

  public interface SearchCompleteHandler {
    public void onSearchComplete(List<Empire> empires);
  }
}
