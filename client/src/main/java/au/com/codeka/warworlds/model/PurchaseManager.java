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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import au.com.codeka.common.Log;
import au.com.codeka.common.model.BaseEmpire;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.Util;

public class PurchaseManager implements PurchasesUpdatedListener {
  private static final Log log = new Log("PurchaseManager");
  public static PurchaseManager i = new PurchaseManager();

  private boolean isSetupComplete = false;

  public interface PurchaseHandler {
    void onPurchaseComplete(BillingResult billingResult, Purchase purchase);
  }

  public interface ConsumeHandler {
    void onPurchaseConsumed(BillingResult billingResult, Purchase purchase);
  }

  private PurchaseManager() {
  }

  // TODO: we should probably encrypt this some how... it really only allows people to give us
  // money though, so maybe not?
  private static final String sPublicKey =
      "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAuM1UqmzRXCwRr5" +
      "qcjqvBd+YbktM8fJoG5XUuGbRWErDVvx9gZ4TYju3jts702F9axBIH7VqQ5ARFO+AFJvv/AFeHPpT21VQu0o+" +
      "cHMZCnnaQmCnGeZE6udHfwsRYGnu35ReReKg7hbSHEIJ6I24uIjLqMNar34sKYCCqaE6IxlbQxYjK508nwsaK" +
      "dlAKtgymQkRGgspbmj5UW4B72drUt2kWPdRNw3RBfZBthTjm/6fUkPIxFpV8Ec/5Ty/z6Vn+VglTyE8xYaxPd" +
      "q+5JjWgA8oiiBFItNppBYl3ojNS9kBsYYmHJM4UlkwRSrc8f3HIIiZFYva4OR/ms2fWJ/kDzQIDAQAB";

  private BillingClient billingClient;
  private final HashMap<String, PendingPurchase> pendingPurchases = new HashMap<>();

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

        for (PendingPurchase pending : pendingPurchases.values()) {
          launchPurchaseFlow(pending.activity, pending.sku, pending.handler);
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
    for (Purchase purchase : purchases) {
      log.info("  purchase sku=%s %s", purchase.getSku(), purchase.toString());
      PendingPurchase pendingPurchase = pendingPurchases.get(purchase.getSku());
      if (pendingPurchase != null) {
        pendingPurchase.handler.onPurchaseComplete(billingResult, purchase);
      }
    }
  }

  public void launchPurchaseFlow(Activity activity, String sku, PurchaseHandler handler) {
    if (!isSetupComplete) {
      waitForSetup(activity, sku, handler);
    }

    if (EmpireManager.i.getEmpire().getPatreonLevel() == BaseEmpire.PatreonLevel.EMPIRE) {
      // If you're at the highest tier on Patreon, you get all purchases for free, so we'll just
      // ignore the purchase flow.
      handler.onPurchaseComplete(
          BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.OK).build(),
          null);
      return;
    }

    PendingPurchase pendingPurchase = new PendingPurchase(activity, sku, handler);
    pendingPurchases.put(sku, pendingPurchase);

    // check if we already own it
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

  private void waitForSetup(Activity activity, String sku, PurchaseHandler listener) {
    if (isSetupComplete) {
      launchPurchaseFlow(activity, sku, listener);
    }

    pendingPurchases.put(sku, new PendingPurchase(activity, sku, listener));
  }

  public void consume(Purchase purchase, final ConsumeHandler handler) {
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
      handler.onPurchaseConsumed(billingResult, purchase);
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
