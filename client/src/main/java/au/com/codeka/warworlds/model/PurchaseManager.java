package au.com.codeka.warworlds.model;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.google.common.collect.Lists;

import java.util.List;

import au.com.codeka.common.Log;
import au.com.codeka.common.model.BaseEmpire;
import au.com.codeka.common.protobuf.Messages;

public class PurchaseManager implements PurchasesUpdatedListener {
  private static final Log log = new Log("PurchaseManager");
  public static PurchaseManager i = new PurchaseManager();

  private boolean isSetupComplete = false;

  private BillingClient billingClient;

  // A purchase that we have pending, may be null.
  @Nullable private PendingPurchase pendingPurchase;

  public interface PurchaseHandler {
    /**
     * Called when a purchase completes successfully.
     *
     * @param purchase The completed purchase. May be null if we bypassed actually making the
     *                 purchase (e.g. if you're a top-tier patron).
     */
    void onPurchaseComplete(@Nullable Purchase purchase);
  }

  public interface ConsumeHandler {
    void onPurchaseConsumed();
  }

  private PurchaseManager() {
  }

  public void setup(Context context) {
    isSetupComplete = false;
    billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build();
    billingClient.startConnection(new BillingClientStateListener() {
      @Override
      public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
        log.info("onBillingSetupFinished: %s", billingResult);

        if (pendingPurchase != null) {
          launchPurchaseFlow(
              pendingPurchase.activity, pendingPurchase.sku, pendingPurchase.handler);
        }
        isSetupComplete = true;
      }

      @Override
      public void onBillingServiceDisconnected() {
        log.info("onBillingServiceDisconnected");
        isSetupComplete = false;
      }
    });
  }

  public void querySkus(List<String> skuNames, SkuDetailsResponseListener listener) {
    SkuDetailsParams params = SkuDetailsParams.newBuilder()
        .setSkusList(skuNames)
        .setType(BillingClient.SkuType.INAPP)
        .build();
    billingClient.querySkuDetailsAsync(params, listener);
  }

  @Nullable
  public Messages.PurchaseInfo toProtobuf(String sku, @Nullable Purchase purchase) {
    if (purchase == null) {
      return null;
    }

    return Messages.PurchaseInfo.newBuilder()
        .setDeveloperPayload(purchase.getDeveloperPayload())
        .setOrderId(purchase.getOrderId())
        .setPrice("??")
        .setSku(sku)
        .setToken(purchase.getPurchaseToken())
        .build();
  }

  @Override
  public void onPurchasesUpdated(
      @NonNull BillingResult billingResult, @Nullable List<Purchase> purchases) {
    log.info(
        "onPurchasesUpdated resultCode=%d debugMsg=%s", billingResult.getResponseCode(),
        billingResult.getDebugMessage());
    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
      // TODO: handle this case specially?
      log.info("TODO: handle ITEM_ALREADY_OWNED better.");
    }
    // TODO: handle service disconnected, etc?
    if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
      // TODO: have some kind of error handling?
      return;
    }

    if (purchases == null) {
      // TODO: this is also some kind of error.
      return;
    }

    for (Purchase purchase : purchases) {
      log.info("  purchase sku=%s %s", purchase.getSku(), purchase.toString());
      if (pendingPurchase != null && pendingPurchase.sku.equals(purchase.getSku())) {
        pendingPurchase.handler.onPurchaseComplete(purchase);
        pendingPurchase = null;
      }
    }
  }

  public void launchPurchaseFlow(Activity activity, String sku, PurchaseHandler handler) {
    if (EmpireManager.i.getEmpire().getPatreonLevel() == BaseEmpire.PatreonLevel.EMPIRE) {
      // If you're at the highest tier on Patreon, you get all purchases for free, so we'll just
      // ignore the purchase flow.
      handler.onPurchaseComplete(null);
      return;
    }

    if (pendingPurchase != null) {
      log.error("Cannot initiate another purchase while one is in progress.");
      // TODO: is this really an error?
      return;
    }
    pendingPurchase = new PendingPurchase(activity, sku, handler);
    if (!isSetupComplete) {
      // Just wait for setup, it'll automatically attempt to purchase the pending one.
      log.info("Waiting for setup to complete before purchasing.");
      return;
    }

    // check if we already own it
    Purchase.PurchasesResult alreadyOwned = billingClient.queryPurchases(sku);
    log.info(" alreadyOwned: %d", alreadyOwned.getResponseCode());
    /*
    Purchase purchase = checkNotNull(inventory).getPurchase(skuName);
    if (purchase != null) {
      log.debug("Already purchased a '%s', not purchasing again.", skuName);
      listener.onIabPurchaseFinished(new IabResult(IabHelper.BILLING_RESPONSE_RESULT_OK, null), purchase);
    } else {
      helper.launchPurchaseFlow(activity, skuName, REQUEST_CODE, listener);
    }
    */

    SkuDetailsParams params = SkuDetailsParams.newBuilder()
        .setSkusList(Lists.newArrayList(sku))
        .setType(BillingClient.SkuType.INAPP)
        .build();
    billingClient.querySkuDetailsAsync(params, (billingResult, skuDetailsList) -> {
      log.info(
          "querySkuDetails responseCode=%d debugMsg=%s",
          billingResult.getResponseCode(),
          billingResult.getDebugMessage());
      if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
        // TODO: some kind of error, handle it.
        return;
      }
      if (skuDetailsList == null) {
        // Some other weird kind of error...
        return;
      }

      SkuDetails skuDetails = skuDetailsList.get(0);
      BillingFlowParams purchaseParams = BillingFlowParams.newBuilder()
          .setSkuDetails(skuDetails)
          .build();
      billingClient.launchBillingFlow(activity, purchaseParams);
    });
  }

  /**
   * Consume the given {@link Purchase}.
   *
   * @param purchase The {@link Purchase} to consume.
   * @param handler A callback that's called on successful consume. Can be null if you don't care.
   */
  public void consume(Purchase purchase, @Nullable final ConsumeHandler handler) {
    if (purchase == null) {
      if (EmpireManager.i.getEmpire().getPatreonLevel() != BaseEmpire.PatreonLevel.EMPIRE) {
        // This is an error!
      }
      return;
    }

    ConsumeParams consumeParams = ConsumeParams.newBuilder()
        .setPurchaseToken(purchase.getPurchaseToken())
        .build();
    billingClient.consumeAsync(consumeParams, (billingResult, purchaseToken) -> {
      if (handler != null) {
        handler.onPurchaseConsumed();
      }
    });
  }

  public void close() {
    billingClient.endConnection();
  }

  private static class PendingPurchase {
    public Activity activity;
    public String sku;
    public PurchaseHandler handler;

    public PendingPurchase(
        Activity activity, String sku, PurchaseHandler handler) {
      this.activity = activity;
      this.sku = sku;
      this.handler = handler;
    }
  }
}
