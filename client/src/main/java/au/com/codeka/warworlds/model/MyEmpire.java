package au.com.codeka.warworlds.model;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;

import com.google.protobuf.ByteString;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import au.com.codeka.common.Log;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.RunnableArg;
import au.com.codeka.warworlds.api.ApiRequest;
import au.com.codeka.warworlds.api.RequestManager;
import au.com.codeka.warworlds.model.billing.IabException;
import au.com.codeka.warworlds.model.billing.IabHelper;
import au.com.codeka.warworlds.model.billing.Purchase;
import au.com.codeka.warworlds.model.billing.SkuDetails;

/**
 * This is a sub-class of {@link Empire} that represents <i>my</i> Empire. We have extra methods
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
   * Colonizes the given planet. We'll call the given {@link ColonizeCompleteHandler} when the
   * operation completes (successfully or not).
   */
  public void colonize(final Planet planet, final ColonizeCompleteHandler callback) {
    log.debug("Colonizing: Star=%s Planet=%d", planet.getStar().getKey(), planet.getIndex());

    if (planet.getStar() == null) {
      log.error("planet.getStar() is null?!");
      return;
    }

    String url = String.format("stars/%s/colonies", planet.getStar().getKey());
    RequestManager.i.sendRequest(new ApiRequest.Builder(url, "POST")
        .body(Messages.ColonizeRequest.newBuilder().setPlanetIndex(planet.getIndex()).build())
        .completeCallback(new ApiRequest.CompleteCallback() {
          @Override
          public void onRequestComplete(ApiRequest request) {
            Colony colony = new Colony();
            colony.fromProtocolBuffer(request.body(Messages.Colony.class));

            if (callback != null) {
              callback.onColonizeComplete(colony);
            }

            // make sure we record the fact that the star is updated as well
            StarManager.i.refreshStar(Integer.parseInt(colony.getStarKey()));
          }
        }).build());
  }

  public void updateFleetStance(final Star star, final Fleet fleet, final Fleet.Stance newStance) {
    String url = String.format("stars/%s/fleets/%s/orders", fleet.getStarKey(), fleet.getKey());
    RequestManager.i.sendRequest(new ApiRequest.Builder(url, "POST")
        .body(Messages.FleetOrder.newBuilder().setOrder(Messages.FleetOrder.FLEET_ORDER.SET_STANCE)
            .setStance(Messages.Fleet.FLEET_STANCE.valueOf(newStance.getValue())).build())
        .completeCallback(new ApiRequest.CompleteCallback() {
          @Override
          public void onRequestComplete(ApiRequest request) {
            // Make sure we record the fact that the star is updated as well.
            EmpireManager.i.refreshEmpire(getID());
            StarManager.i.refreshStar(star.getID());
          }
        }).build());
  }

  public void fetchScoutReports(final Star star, final FetchScoutReportCompleteHandler handler) {
    String url = String.format("stars/%s/scout-reports", star.getKey());
    RequestManager.i.sendRequest(
        new ApiRequest.Builder(url, "GET").completeCallback(new ApiRequest.CompleteCallback() {
          @Override
          public void onRequestComplete(ApiRequest request) {
            Messages.ScoutReports pb = request.body(Messages.ScoutReports.class);
            if (pb == null) {
              return;
            }

            ArrayList<ScoutReport> reports = new ArrayList<>();
            for (Messages.ScoutReport scoutReportPb : pb.getReportsList()) {
              ScoutReport scoutReport = new ScoutReport();
              scoutReport.fromProtocolBuffer(scoutReportPb);
              reports.add(scoutReport);
            }
            handler.onComplete(reports);
          }
        }).build());
  }

  public void fetchCombatReport(final String starKey, final String combatReportKey,
      final FetchCombatReportCompleteHandler handler) {
    String url = String.format("stars/%s/combat-reports/%s", starKey, combatReportKey);
    RequestManager.i.sendRequest(new ApiRequest.Builder(url, "GET")
        .completeCallback(new ApiRequest.CompleteCallback() {
          @Override
          public void onRequestComplete(ApiRequest request) {
            Messages.CombatReport pb = request.body(Messages.CombatReport.class);
            if (pb == null) {
              return;
            }

            CombatReport combatReport = new CombatReport();
            combatReport.fromProtocolBuffer(pb);
            handler.onComplete(combatReport);
          }
        }).build());
  }

  public void attackColony(final Star star, final Colony colony,
      final AttackColonyCompleteHandler callback) {
    String url = "stars/" + star.getKey() + "/colonies/" + colony.getKey() + "/attack";
    RequestManager.i.sendRequest(new ApiRequest.Builder(url, "POST")
        .completeCallback(new ApiRequest.CompleteCallback() {
          @Override
          public void onRequestComplete(ApiRequest request) {
            Messages.Star pb = request.body(Messages.Star.class);
            if (pb == null) {
              return;
            }
            Star star = new Star();
            star.fromProtocolBuffer(pb);
            StarManager.i.refreshStar(star.getID());

            // Refresh my own empire, because we'll have a bunch more cash (assuming we successfully
            // destroyed them, of course).
            EmpireManager.i.refreshEmpire();

            if (callback != null) {
              callback.onComplete();
            }
          }
        }).build());
  }

  //TODO: just return star counts, that's all this is used for anyway.
  public void requestStars(final FetchStarsCompleteHandler callback) {
    String url = "empires/" + getKey() + "/stars";
    RequestManager.i.sendRequest(
        new ApiRequest.Builder(url, "GET").completeCallback(new ApiRequest.CompleteCallback() {
          @Override
          public void onRequestComplete(ApiRequest request) {
            Messages.Stars pb = request.body(Messages.Stars.class);
            if (pb == null) {
              return;
            }

            ArrayList<Star> stars = new ArrayList<>();
            for (Messages.Star star_pb : pb.getStarsList()) {
              Star star = new Star();
              star.fromProtocolBuffer(star_pb);
              StarManager.eventBus.publish(star);
              stars.add(star);
            }
            if (callback != null) {
              callback.onComplete(stars);
            }
          }
        }).build());
  }

  public void rename(final String newName, @Nullable final Purchase purchaseInfo,
      @NotNull final RunnableArg<Boolean> onComplete) {
    String url = "empires/" + getKey() + "/display-name";

    Messages.EmpireRenameRequest renameRequestPb = Messages.EmpireRenameRequest.newBuilder()
        .setKey(getKey())
        .setNewName(newName)
        .setPurchaseInfo(IabHelper.toProtobuf("rename_empire", purchaseInfo))
        .build();

    RequestManager.i.sendRequest(new ApiRequest.Builder(url, "PUT")
        .body(renameRequestPb)
        .completeCallback(new ApiRequest.CompleteCallback() {
          @Override
          public void onRequestComplete(ApiRequest request) {
            EmpireManager.i.refreshEmpire();
            onComplete.run(true);
          }
        }).errorCallback(new ApiRequest.ErrorCallback() {
          @Override
          public void onRequestError(ApiRequest request, Messages.GenericError error) {
            onComplete.run(false);
          }
        }).build());
  }

  /**
   * Call to the server to change the user's shield. Will call the given handler when the change
   * completes, whether successful or not.
   */
  public void changeShieldImage(final Bitmap bmp, final Purchase purchaseInfo,
      @NotNull final RunnableArg<Boolean> onComplete) {
    String url = "empires/" + getKey() + "/shield";

    try {
      PurchaseManager.i.getInventory().getSkuDetails("decorate_empire");
    } catch (IabException e) {
      onComplete.run(false);
      return;
    }

    ByteArrayOutputStream outs = new ByteArrayOutputStream();
    bmp.compress(CompressFormat.PNG, 90, outs);

    Messages.EmpireChangeShieldRequest pb = Messages.EmpireChangeShieldRequest.newBuilder()
        .setKey(getKey())
        .setPngImage(ByteString.copyFrom(outs.toByteArray()))
        .setPurchaseInfo(IabHelper.toProtobuf("decorate_empire", purchaseInfo))
        .build();

    RequestManager.i.sendRequest(new ApiRequest.Builder(url, "PUT").body(pb)
        .completeCallback(new ApiRequest.CompleteCallback() {
          @Override
          public void onRequestComplete(ApiRequest request) {
            Messages.Empire empire_pb = request.body(Messages.Empire.class);
            if (empire_pb == null) {
              return;
            }
            mShieldLastUpdate =
                new DateTime(empire_pb.getShieldImageLastUpdate() * 1000, DateTimeZone.UTC);
            EmpireManager.eventBus.publish(MyEmpire.this);
            onComplete.run(true);
          }
        }).errorCallback(new ApiRequest.ErrorCallback() {
          @Override
          public void onRequestError(ApiRequest request, Messages.GenericError error) {
            onComplete.run(false);
          }
        }).build());
  }

  public void resetEmpire(final String skuName, final Purchase purchaseInfo,
      final EmpireResetCompleteHandler handler) {
    String url = "empires/" + getKey() + "/reset";

    Messages.EmpireResetRequest.Builder pb = Messages.EmpireResetRequest.newBuilder();
    if (purchaseInfo != null) {
      pb.setPurchaseInfo(IabHelper.toProtobuf(skuName, purchaseInfo));
    }

    RequestManager.i.sendRequest(new ApiRequest.Builder(url, "POST")
        .body(pb.build())
        .completeCallback(new ApiRequest.CompleteCallback() {
          @Override
          public void onRequestComplete(ApiRequest request) {
            handler.onEmpireReset();
          }
        })
        .errorCallback(new ApiRequest.ErrorCallback() {
          @Override
          public void onRequestError(ApiRequest request, Messages.GenericError error) {
            handler.onResetFail(error.getErrorMessage());
          }
        }).build());
  }

  public interface ColonizeCompleteHandler {
    void onColonizeComplete(Colony colony);
  }

  public interface FetchScoutReportCompleteHandler {
    void onComplete(List<ScoutReport> reports);
  }

  public interface FetchCombatReportCompleteHandler {
    void onComplete(CombatReport report);
  }

  public interface FetchStarsCompleteHandler {
    void onComplete(List<Star> stars);
  }

  public interface AttackColonyCompleteHandler {
    void onComplete();
  }

  public interface EmpireResetCompleteHandler {
    void onEmpireReset();
    void onResetFail(String reason);
  }
}
