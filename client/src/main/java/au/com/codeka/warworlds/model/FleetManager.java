package au.com.codeka.warworlds.model;

import au.com.codeka.common.Log;
import au.com.codeka.common.model.BaseFleet.State;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.App;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.concurrency.Threads;

public class FleetManager {
  private static final Log log = new Log("FleetManager");
  public static FleetManager i = new FleetManager();

  private FleetManager() {
  }

  public void updateNotes(final Fleet fleet) {
    App.i.getTaskRunner().runTask(() -> {
      try {
        Messages.Fleet.Builder fleet_pb = Messages.Fleet.newBuilder()
            .setKey(fleet.getKey())
            .setEmpireKey(EmpireManager.i.getEmpire().getKey())
            .setStarKey(fleet.getStarKey());
        if (fleet.getNotes() != null) {
          fleet_pb.setNotes(fleet.getNotes());
        }

        String url = String.format("stars/%s/fleets/%s", fleet.getStarKey(), fleet.getKey());
        ApiClient.putProtoBuf(url, fleet_pb.build(), null);
      } catch (Exception e) {
        log.error("Error updating notes.", e);
      }
    }, Threads.BACKGROUND);
  }

  public void boostFleet(final Fleet fleet, final FleetBoostedHandler handler) {
    if (fleet.getState() != State.MOVING) {
      // don't call the handler...
      return;
    }

    App.i.getTaskRunner().runTask(() -> {
      String url = String.format("stars/%s/fleets/%s/orders",
          fleet.getStarKey(),
          fleet.getKey());
      Messages.FleetOrder fleetOrder = Messages.FleetOrder.newBuilder()
          .setOrder(Messages.FleetOrder.FLEET_ORDER.BOOST)
          .build();

      try {
        ApiClient.postProtoBuf(url, fleetOrder);

        // the star this fleet is attached to needs to be refreshed...
        StarManager.i.refreshStar(Integer.parseInt(fleet.getStarKey()));

        if (handler != null) {
          App.i.getTaskRunner().runTask(() -> handler.onFleetBoosted(fleet), Threads.UI);
        }
      } catch (ApiException e) {
        // TODO: do something..?
      }
    }, Threads.BACKGROUND);
  }

  public void enterWormhole(final Star star, final Fleet fleet, final FleetEnteredWormholeHandler handler) {
    if (fleet.getState() != State.IDLE) {
      // don't call the handler...
      log.warning("Fleet state isn't IDLE, can't enter the wormhole.");
      return;
    }

    App.i.getTaskRunner().runTask(() -> {
      String url = String.format("stars/%s/fleets/%s/orders", fleet.getStarKey(), fleet.getKey());
      log.info(url);
      Messages.FleetOrder fleetOrder = Messages.FleetOrder.newBuilder()
          .setOrder(Messages.FleetOrder.FLEET_ORDER.ENTER_WORMHOLE)
          .build();
      try {
        ApiClient.postProtoBuf(url, fleetOrder);
        // we need to refresh both this star and the destination star
        if (star.getWormholeExtra() != null) {
          StarManager.i.refreshStar(star.getWormholeExtra().getDestWormholeID());
        }
        StarManager.i.refreshStar(star.getID());

        if (handler != null) {
          App.i.getTaskRunner().runTask(() -> handler.onFleetEnteredWormhole(fleet), Threads.UI);
        }
      } catch (ApiException e) {
        // TODO: do something..?
      }
    }, Threads.BACKGROUND);
  }

  public interface FleetBoostedHandler {
    void onFleetBoosted(Fleet fleet);
  }

  public interface FleetEnteredWormholeHandler {
    void onFleetEnteredWormhole(Fleet fleet);
  }
}
