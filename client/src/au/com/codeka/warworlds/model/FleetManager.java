package au.com.codeka.warworlds.model;

import au.com.codeka.BackgroundRunner;
import au.com.codeka.common.Log;
import au.com.codeka.common.model.BaseFleet.State;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;

public class FleetManager {
    private static final Log log = new Log("FleetManager");
    public static FleetManager i = new FleetManager();

    private FleetManager() {
    }

    public void updateNotes(final Fleet fleet) {
        new BackgroundRunner<Boolean>() {
            @Override
            protected Boolean doInBackground() {
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

                    return true;
                } catch(Exception e) {
                    log.error("Error updating notes.", e);
                    return null;
                }
            }

            @Override
            protected void onComplete(Boolean success) {
                // do we need to refresh anything? I don't think so...
            }
        }.execute();
    }

    public void boostFleet(final Fleet fleet, final FleetBoostedHandler handler) {
        if (fleet.getState() != State.MOVING) {
            // don't call the handler...
            return;
        }

        new BackgroundRunner<Boolean>() {
            @Override
            protected Boolean doInBackground() {
                String url = String.format("stars/%s/fleets/%s/orders",
                                           fleet.getStarKey(),
                                           fleet.getKey());
                Messages.FleetOrder fleetOrder = Messages.FleetOrder.newBuilder()
                               .setOrder(Messages.FleetOrder.FLEET_ORDER.BOOST)
                               .build();

                try {
                    return ApiClient.postProtoBuf(url, fleetOrder);
                } catch (ApiException e) {
                    // TODO: do something..?
                    return false;
                }
            }

            @Override
            protected void onComplete(Boolean success) {
                if (success) {
                    // the star this fleet is attached to needs to be refreshed...
                    StarManager.i.refreshStar(Integer.parseInt(fleet.getStarKey()));

                    if (handler != null) {
                        handler.onFleetBoosted(fleet);
                    }
                }
            }
        }.execute();
    }

    public void enterWormhole(final Star star, final Fleet fleet, final FleetEnteredWormholeHandler handler) {
        if (fleet.getState() != State.IDLE) {
            // don't call the handler...
            return;
        }

        new BackgroundRunner<Boolean>() {
            @Override
            protected Boolean doInBackground() {
                String url = String.format("stars/%s/fleets/%s/orders",
                                           fleet.getStarKey(),
                                           fleet.getKey());
                Messages.FleetOrder fleetOrder = Messages.FleetOrder.newBuilder()
                               .setOrder(Messages.FleetOrder.FLEET_ORDER.ENTER_WORMHOLE)
                               .build();

                try {
                    return ApiClient.postProtoBuf(url, fleetOrder);
                } catch (ApiException e) {
                    // TODO: do something..?
                    return false;
                }
            }

            @Override
            protected void onComplete(Boolean success) {
                if (success) {
                    // we need to refresh both this star and the destination star
                    if (star.getWormholeExtra() != null) {
                        StarManager.i.refreshStar(star.getWormholeExtra().getDestWormholeID());
                    }
                    StarManager.i.refreshStar(star.getID());

                    if (handler != null) {
                        handler.onFleetEnteredWormhole(fleet);
                    }
                }
            }
        }.execute();
    }

    public interface FleetBoostedHandler {
        void onFleetBoosted(Fleet fleet);
    }
    public interface FleetEnteredWormholeHandler {
        void onFleetEnteredWormhole(Fleet fleet);
    }
}
