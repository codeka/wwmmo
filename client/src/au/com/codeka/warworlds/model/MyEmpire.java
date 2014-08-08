package au.com.codeka.warworlds.model;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import au.com.codeka.BackgroundRunner;
import au.com.codeka.common.Log;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.model.billing.Purchase;
import au.com.codeka.warworlds.model.billing.SkuDetails;

import com.google.protobuf.ByteString;

/**
 * This is a sub-class of \c Empire that represents \em my Empire. We have extra methods
 * and such that only the current user can perform.
 */
public class MyEmpire extends Empire {
    private static final Log log = new Log("MyEmpire");

    public MyEmpire() {
    }

    public void addCash(float amount) {
        mCash += amount;
    }

    public void updateCash(float cash) {
        mCash = cash;
    }

    /**
     * Colonizes the given planet. We'll call the given \c ColonizeCompleteHandler when the
     * operation completes (successfully or not).
     */
    public void colonize(final Planet planet, final ColonizeCompleteHandler callback) {
        log.debug("Colonizing: Star=%s Planet=%d",
                planet.getStar().getKey(), planet.getIndex());
        new BackgroundRunner<Colony>() {
            @Override
            protected Colony doInBackground() {
                try {
                    if (planet.getStar() == null) {
                        log.warning("planet.getStarSummary() returned null!");
                        return null;
                    }

                    Messages.ColonizeRequest request = Messages.ColonizeRequest.newBuilder()
                            .setPlanetIndex(planet.getIndex())
                            .build();

                    String url = String.format("stars/%s/colonies", planet.getStar().getKey());
                    Messages.Colony pb = ApiClient.postProtoBuf(url, request,
                            Messages.Colony.class);
                    if (pb == null)
                        return null;

                    Colony colony = new Colony();
                    colony.fromProtocolBuffer(pb);
                    return colony;
                } catch(Exception e) {
                    log.error("Error issuing colonize request.", e);
                    return null;
                }
            }

            @Override
            protected void onComplete(Colony colony) {
                if (colony == null) {
                    return; // BAD!
                }

                if (callback != null) {
                    callback.onColonizeComplete(colony);
                }

                // make sure we record the fact that the star is updated as well
                StarManager.i.refreshStar(Integer.parseInt(colony.getStarKey()));
            }
        }.execute();
    }

    public void updateFleetStance(final Star star, final Fleet fleet,
                                  final Fleet.Stance newStance) {
        new BackgroundRunner<Boolean>() {
            @Override
            protected Boolean doInBackground() {
                try {
                    String url = String.format("stars/%s/fleets/%s/orders",
                            fleet.getStarKey(),
                            fleet.getKey());
                    Messages.FleetOrder fleetOrder = Messages.FleetOrder.newBuilder()
                                    .setOrder(Messages.FleetOrder.FLEET_ORDER.SET_STANCE)
                                    .setStance(Messages.Fleet.FLEET_STANCE.valueOf(newStance.getValue()))
                                    .build();
                    ApiClient.postProtoBuf(url, fleetOrder);
                    return true;
                } catch(Exception e) {
                    log.error("Error issuing update fleet stance request.", e);
                    return false;
                }
            }

            @Override
            protected void onComplete(Boolean success) {
                if (!success) {
                    return; // BAD!
                }

                // make sure we record the fact that the star is updated as well
                EmpireManager.i.refreshEmpire(getID());
                StarManager.i.refreshStar(star.getID());
            }
        }.execute();

    }

    public void fetchScoutReports(final Star star,
                                  final FetchScoutReportCompleteHandler handler) {
        new BackgroundRunner<List<ScoutReport>>() {
            @Override
            protected List<ScoutReport> doInBackground() {
                try {
                    String url = String.format("stars/%s/scout-reports", star.getKey());
                    Messages.ScoutReports pb = ApiClient.getProtoBuf(url, Messages.ScoutReports.class);
                    if (pb == null)
                        return null;

                    ArrayList<ScoutReport> reports = new ArrayList<ScoutReport>();
                    for (Messages.ScoutReport srpb : pb.getReportsList()) {
                        ScoutReport scoutReport = new ScoutReport();
                        scoutReport.fromProtocolBuffer(srpb);
                        reports.add(scoutReport);
                    }
                    return reports;
                } catch(Exception e) {
                    log.error("Error issuing fetch scout reports request.", e);
                    return null;
                }
            }

            @Override
            protected void onComplete(List<ScoutReport> reports) {
                handler.onComplete(reports);
            }
        }.execute();
    }

    public void fetchCombatReport(final String starKey,
            final String combatReportKey, final FetchCombatReportCompleteHandler handler ) {
        new BackgroundRunner<CombatReport>() {
            @Override
            protected CombatReport doInBackground() {
                try {
                    String url = String.format("stars/%s/combat-reports/%s",
                                               starKey, combatReportKey);
                    Messages.CombatReport pb = ApiClient.getProtoBuf(url, Messages.CombatReport.class);
                    if (pb == null)
                        return null;

                    CombatReport combatReport = new CombatReport();
                    combatReport.fromProtocolBuffer(pb);
                    return combatReport;
                } catch(Exception e) {
                    log.error("Uh oh!", e);
                    return null;
                }
            }

            @Override
            protected void onComplete(CombatReport report) {
                if (report != null) {
                    handler.onComplete(report);
                }
            }
        }.execute();
    }

    public void attackColony(final Star star,
                             final Colony colony,
                             final AttackColonyCompleteHandler callback) {
        new BackgroundRunner<Star>() {
            @Override
            protected Star doInBackground() {
                try {
                    String url = "stars/"+star.getKey()+"/colonies/"+colony.getKey()+"/attack";

                    Messages.Star pb = ApiClient.postProtoBuf(url, null, Messages.Star.class);
                    if (pb == null)
                        return null;

                    Star star = new Star();
                    star.fromProtocolBuffer(pb);
                    return star;
                } catch(Exception e) {
                    log.error("Uh oh!", e);
                    return null;
                }
            }

            @Override
            protected void onComplete(Star star) {
                if (star != null) {
                    StarManager.i.refreshStar(star.getID());
                }

                if (callback != null) {
                    callback.onComplete();
                }
            }
        }.execute();
    }

    public void requestStars(final FetchStarsCompleteHandler callback) {
        new BackgroundRunner<List<Star>>() {
            @Override
            protected List<Star> doInBackground() {
                try {
                    String url = "empires/"+getKey()+"/stars";

                    Messages.Stars pb = ApiClient.getProtoBuf(url, Messages.Stars.class);
                    if (pb == null)
                        return null;

                    ArrayList<Star> stars = new ArrayList<Star>();
                    for (Messages.Star star_pb : pb.getStarsList()) {
                        Star star = new Star();
                        star.fromProtocolBuffer(star_pb);
                        stars.add(star);
                    }

                    return stars;
                } catch(Exception e) {
                    log.error("Uh oh!", e);
                    return null;
                }
            }

            @Override
            protected void onComplete(List<Star> stars) {
                if (stars != null) {
                    for (Star star : stars) {
                        StarManager.eventBus.publish(star);
                    }
                }

                if (callback != null) {
                    callback.onComplete(stars);
                }
            }
        }.execute();
    }

    public void rename(final String newName, final Purchase purchaseInfo) {
        new BackgroundRunner<String>() {
            @Override
            protected String doInBackground() {
                String url = "empires/"+getKey()+"/display-name";

                try {
                    SkuDetails sku = PurchaseManager.i.getInventory().getSkuDetails("rename_empire");

                    Messages.EmpireRenameRequest pb = Messages.EmpireRenameRequest.newBuilder()
                            .setKey(getKey())
                            .setNewName(newName)
                            .setPurchaseInfo(Messages.PurchaseInfo.newBuilder()
                                    .setSku("rename_empire")
                                    .setToken(purchaseInfo.getToken())
                                    .setOrderId(purchaseInfo.getOrderId())
                                    .setPrice(sku.getPrice())
                                    .setDeveloperPayload(purchaseInfo.getDeveloperPayload()))
                            .build();
                    Messages.Empire empire_pb = ApiClient.putProtoBuf(url, pb, Messages.Empire.class);
                    return empire_pb.getDisplayName();
                } catch(Exception e) {
                    log.error("Uh oh!", e);
                    return null;
                }
            }

            @Override
            protected void onComplete(String name) {
                mDisplayName = name;
                EmpireManager.eventBus.publish(MyEmpire.this);
            }
        }.execute();
    }

    public void changeShieldImage(final Bitmap bmp, final Purchase purchaseInfo) {
        new BackgroundRunner<DateTime>() {
            @Override
            protected DateTime doInBackground() {
                String url = "empires/"+getKey()+"/shield";

                try {
                    SkuDetails sku = PurchaseManager.i.getInventory().getSkuDetails("decorate_empire");

                    ByteArrayOutputStream outs = new ByteArrayOutputStream();
                    bmp.compress(CompressFormat.PNG, 90, outs);

                    Messages.EmpireChangeShieldRequest pb = Messages.EmpireChangeShieldRequest.newBuilder()
                            .setKey(getKey())
                            .setPngImage(ByteString.copyFrom(outs.toByteArray()))
                            .setPurchaseInfo(Messages.PurchaseInfo.newBuilder()
                                    .setSku("decorate_empire")
                                    .setToken(purchaseInfo.getToken())
                                    .setOrderId(purchaseInfo.getOrderId())
                                    .setPrice(sku.getPrice())
                                    .setDeveloperPayload(purchaseInfo.getDeveloperPayload()))
                            .build();
                    Messages.Empire empire_pb = ApiClient.putProtoBuf(url, pb, Messages.Empire.class);
                    return new DateTime(empire_pb.getShieldImageLastUpdate() * 1000, DateTimeZone.UTC);
                } catch(Exception e) {
                    log.error("Uh oh!", e);
                    return null;
                }
            }

            @Override
            protected void onComplete(DateTime lastUpdateTime) {
                if (lastUpdateTime != null) {
                    mShieldLastUpdate = lastUpdateTime;
                    EmpireManager.eventBus.publish(MyEmpire.this);
                }
            }
        }.execute();
    }

    public void resetEmpire(final String skuName, final Purchase purchaseInfo, final EmpireResetCompleteHandler handler) {
        new BackgroundRunner<Boolean>() {
            @Override
            protected Boolean doInBackground() {
                String url = "empires/"+getKey()+"/reset";

                try {
                    Messages.EmpireResetRequest.Builder pbuilder = Messages.EmpireResetRequest.newBuilder();
                    if (purchaseInfo != null) {
                        SkuDetails sku = PurchaseManager.i.getInventory().getSkuDetails(skuName);
                        pbuilder.setPurchaseInfo(Messages.PurchaseInfo.newBuilder()
                                    .setSku(skuName)
                                    .setToken(purchaseInfo.getToken())
                                    .setOrderId(purchaseInfo.getOrderId())
                                    .setPrice(sku.getPrice())
                                    .setDeveloperPayload(purchaseInfo.getDeveloperPayload()));
                    }
                    ApiClient.postProtoBuf(url, pbuilder.build(), null);
                    return true;
                } catch(Exception e) {
                    log.error("Uh oh!", e);
                    return false;
                }
            }

            @Override
            protected void onComplete(Boolean success) {
                if (success) {
                    handler.onEmpireReset();
                }
            }
        }.execute();

    }

    public static interface ColonizeCompleteHandler {
        public void onColonizeComplete(Colony colony);
    }

    public static interface FetchScoutReportCompleteHandler {
        public void onComplete(List<ScoutReport> reports);
    }

    public static interface FetchCombatReportCompleteHandler {
        public void onComplete(CombatReport report);
    }

    public static interface FetchStarsCompleteHandler {
        public void onComplete(List<Star> stars);
    }

    public static interface AttackColonyCompleteHandler {
        public void onComplete();
    }

    public static interface EmpireResetCompleteHandler {
        public void onEmpireReset();
    }
}
