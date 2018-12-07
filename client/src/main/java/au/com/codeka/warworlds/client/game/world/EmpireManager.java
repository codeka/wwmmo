package au.com.codeka.warworlds.client.game.world;

import androidx.annotation.Nullable;

import com.google.common.collect.Lists;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import au.com.codeka.warworlds.client.App;
import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.concurrency.Threads;
import au.com.codeka.warworlds.client.store.ProtobufStore;
import au.com.codeka.warworlds.client.util.eventbus.EventHandler;
import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.EmpireDetailsPacket;
import au.com.codeka.warworlds.common.proto.Packet;
import au.com.codeka.warworlds.common.proto.RequestEmpirePacket;

import static com.google.common.base.Preconditions.checkState;

/** Manages empires. */
public class EmpireManager {
  public static final EmpireManager i = new EmpireManager();
  private static final Log log = new Log("EmpireManager");

  /**
   * The number of milliseconds to delay sending the empire request to the server, so that we can
   * batch them up a bit in case of a burst of requests (happens when you open chat for example).
   */
  private static final long EMPIRE_REQUEST_DELAY_MS = 150;

  private final ProtobufStore<Empire> empires;

  /** The list of pending empire requests. */
  private final Set<Long> pendingEmpireRequests = new HashSet<>();

  /** An object to synchronize on when updating {@link #pendingEmpireRequests}. */
  private final Object pendingRequestLock = new Object();

  /** Whether a request for empires is currently pending. */
  private boolean requestPending;

  /** Our current empire, will be null before we're connected. */
  @Nullable private Empire myEmpire;

  /** A placeholder {@link Empire} for native empires. */
  @Nullable private Empire nativeEmpire;

  private EmpireManager() {
    empires = App.i.getDataStore().empires();
    nativeEmpire = new Empire.Builder()
        .display_name(App.i.getString(R.string.native_colony))
        .build();
    App.i.getEventBus().register(eventListener);
  }

  /** Called by the server when we get the 'hello', and lets us know the empire. */
  public void onHello(Empire empire) {
    empires.put(empire.id, empire);
    myEmpire = empire;
    App.i.getEventBus().publish(empire);
  }

  /** Returns {@link true} if my empire has been set, or false if it's not ready yet. */
  public boolean hasMyEmpire() {
    return myEmpire != null;
  }

  /** Gets my empire, if my empire hasn't been set yet, IllegalStateException is thrown. */
  @Nonnull
  public Empire getMyEmpire() {
    checkState(myEmpire != null);
    return myEmpire;
  }

  public boolean isEnemy(@Nullable Empire empire) {
    if (empire == null) {
      return false;
    }
    if (empire.id == null) {
      return true;
    }
    if (myEmpire != null && !empire.id.equals(myEmpire.id)) {
      return true;
    }
    return false;
  }

  /**
   * Gets the {@link Empire} with the given id. If the id is null, returns a pseudo-Empire that
   * can be used for native colonies/fleets.
   */
  public Empire getEmpire(Long id) {
    if (id == null) {
      return nativeEmpire;
    }
    if (myEmpire != null && myEmpire.id.equals(id)) {
      return myEmpire;
    }

    Empire empire = empires.get(id);
    if (empire == null) {
      requestEmpire(id);
    }

    return empire;
  }

  /**
   * Request the {@link Empire} with the given ID from the server. To save a storm of requests when
   * showing the chat screen (and others), we delay sending the request by a couple hundred
   * milliseconds.
   */
  private void requestEmpire(long id) {
    synchronized (pendingRequestLock) {
      pendingEmpireRequests.add(id);
      if (!requestPending) {
        App.i.getTaskRunner().runTask(
            this::sendPendingEmpireRequests,
            Threads.BACKGROUND,
            EMPIRE_REQUEST_DELAY_MS);
      }
    }
  }

  /** Called on a background thread to actually send the request empire request to the server. */
  private void sendPendingEmpireRequests() {
    List<Long> empireIds;
    synchronized (pendingRequestLock) {
      empireIds = Lists.newArrayList(pendingEmpireRequests);
      pendingEmpireRequests.clear();
      requestPending = false;
    }

    App.i.getServer().send(new Packet.Builder()
        .request_empire(new RequestEmpirePacket.Builder()
            .empire_id(empireIds)
            .build())
        .build());
  }

  private final Object eventListener = new Object() {
    @EventHandler(thread = Threads.BACKGROUND)
    public void handleEmpireUpdatedPacket(EmpireDetailsPacket pkt) {
      for (Empire empire : pkt.empires) {
        long startTime = System.nanoTime();
        empires.put(empire.id, empire);
        App.i.getEventBus().publish(empire);
        long endTime = System.nanoTime();

        log.debug("Refreshed empire %d [%s] in %dms.",
            empire.id, empire.display_name, (endTime - startTime) / 1000000L);
      }
    }
  };
}

