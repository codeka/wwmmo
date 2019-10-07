package au.com.codeka.warworlds.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONException;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import au.com.codeka.common.Log;
import au.com.codeka.common.model.BaseEmpire;
import au.com.codeka.warworlds.App;
import au.com.codeka.warworlds.Util;
import au.com.codeka.warworlds.model.billing.IabException;
import au.com.codeka.warworlds.model.billing.IabHelper;
import au.com.codeka.warworlds.model.billing.IabResult;
import au.com.codeka.warworlds.model.billing.Inventory;
import au.com.codeka.warworlds.model.billing.Purchase;

import static androidx.core.util.Preconditions.checkNotNull;

public class PurchaseManager {
  private static final Log log = new Log("PurchaseManager");
  public static PurchaseManager i = new PurchaseManager();

  private PurchaseManager() {
  }

  // TODO: we should probably encrypt this some how...
  private static String sPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAuM1UqmzRXCwRr5" +
      "qcjqvBd+YbktM8fJoG5XUuGbRWErDVvx9gZ4TYju3jts702F9axBIH7VqQ5ARFO+AFJvv/AFeHPpT21VQu0o+" +
      "cHMZCnnaQmCnGeZE6udHfwsRYGnu35ReReKg7hbSHEIJ6I24uIjLqMNar34sKYCCqaE6IxlbQxYjK508nwsaK" +
      "dlAKtgymQkRGgspbmj5UW4B72drUt2kWPdRNw3RBfZBthTjm/6fUkPIxFpV8Ec/5Ty/z6Vn+VglTyE8xYaxPd" +
      "q+5JjWgA8oiiBFItNppBYl3ojNS9kBsYYmHJM4UlkwRSrc8f3HIIiZFYva4OR/ms2fWJ/kDzQIDAQAB";

  private static int REQUEST_CODE = 6732; // random big number that hopefully won't conflict

  private static ArrayList<String> sAllSkus;

  {
    sAllSkus = new ArrayList<>();
    sAllSkus.add("star_rename");
    sAllSkus.add("remove_ads");
    sAllSkus.add("rename_empire");
    sAllSkus.add("reset_empire_small");
    sAllSkus.add("reset_empire_big");
    sAllSkus.add("decorate_empire");
  }

  private IabHelper helper;
  @Nullable private IabResult setupResult;
  @Nullable private Inventory inventory;

  private Set<PendingPurchase> pendingPurchases = new HashSet<>();

  public void setup() {
    // try to load the inventory from SharedPreferences first, so that we don't have to wait
    // on the play store...
    try {
      SharedPreferences prefs = Util.getSharedPreferences();
      String json = prefs.getString("au.com.codeka.warworlds.PurchaseInventory", null);
      if (json != null) {
        inventory = Inventory.fromJson(json);
        if (Util.isDebug()) {
          inventory.erasePurchase("remove_ads");
        }
      }
    } catch (JSONException e) {
      // ignore... for now
    }

    helper = new IabHelper(App.i, sPublicKey);
    helper.enableDebugLogging(true, "In-AppBilling");
    helper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
      @Override
      public void onIabSetupFinished(IabResult result) {
        setupResult = result;
        if (setupResult.isSuccess()) {
          helper.queryInventoryAsync(true, sAllSkus, new IabHelper.QueryInventoryFinishedListener() {
            @Override
            public void onQueryInventoryFinished(IabResult result, @Nullable Inventory inv) {
              if (result.isSuccess()) {
                inventory = checkNotNull(inv);
                if (Util.isDebug()) {
                  inventory.erasePurchase("remove_ads");
                }

                try {
                  SharedPreferences prefs = Util.getSharedPreferences();
                  prefs.edit().putString("au.com.codeka.warworlds.PurchaseInventory", inv.toJson())
                      .apply();
                } catch (JSONException e) {
                  // ignore... for now
                }

                for (PendingPurchase pending : pendingPurchases) {
                  try {
                    launchPurchaseFlow(pending.activity, pending.sku, pending.listener);
                  } catch(IabException e) {
                    log.error("Error launching pending purchase.", e);
                  }
                }
                pendingPurchases.clear();
              }
            }
          });
        }
      }
    });
  }

  /**
   * You must call this from any Activity where you call launchPurchaseFlow from.
   */
  public boolean onActivityResult(int requestCode, int resultCode, Intent intent) {
    if (requestCode == REQUEST_CODE) {
      helper.handleActivityResult(requestCode, resultCode, intent);
      return true;
    }

    return false;
  }

  public Inventory getInventory() throws IabException {
    if (inventory != null) {
      return inventory;
    }
    if (setupResult != null && !setupResult.isSuccess()) {
      throw new IabException(setupResult);
    }
    return inventory;
  }

  public void launchPurchaseFlow(
      Activity activity,
      String sku,
      IabHelper.OnIabPurchaseFinishedListener listener) throws IabException {
    if (EmpireManager.i.getEmpire().getPatreonLevel() == BaseEmpire.PatreonLevel.EMPIRE) {
      // If you're at the highest tier on Patreon, you get all purchases for free, so we'll just
      // ignore the purchase flow.
      listener.onIabPurchaseFinished(
          new IabResult(IabHelper.BILLING_RESPONSE_RESULT_OK, null), null);
      return;
    }

    if (setupResult == null) {
      waitForSetup(activity, sku, listener);
      return;
    }

    if (!setupResult.isSuccess()) {
      throw new IabException(setupResult);
    }

    String skuName = Util.getProperties().getProperty("iap." + sku);

    // check if we already own it
    Purchase purchase = checkNotNull(inventory).getPurchase(skuName);
    if (purchase != null) {
      log.debug("Already purchased a '%s', not purchasing again.", skuName);
      listener.onIabPurchaseFinished(new IabResult(IabHelper.BILLING_RESPONSE_RESULT_OK, null), purchase);
    } else {
      helper.launchPurchaseFlow(activity, skuName, REQUEST_CODE, listener);
    }
  }

  private void waitForSetup(Activity activity, String sku,
                            IabHelper.OnIabPurchaseFinishedListener listener) throws IabException {
    if (setupResult != null) {
      launchPurchaseFlow(activity, sku, listener);
    }

    pendingPurchases.add(new PendingPurchase(activity, sku, listener));
  }

  public void consume(Purchase purchase, final IabHelper.OnConsumeFinishedListener listener) {
    if (purchase == null) {
      if (EmpireManager.i.getEmpire().getPatreonLevel() != BaseEmpire.PatreonLevel.EMPIRE) {
        // This is an error!
      }
      return;
    }

    helper.consumeAsync(purchase, new IabHelper.OnConsumeFinishedListener() {
      @Override
      public void onConsumeFinished(Purchase purchase, IabResult result) {
        listener.onConsumeFinished(purchase, result);

        // we'll want to refresh the inventory as well, now that we've consumed something
        helper.queryInventoryAsync(true, sAllSkus, new IabHelper.QueryInventoryFinishedListener() {
          @Override
          public void onQueryInventoryFinished(IabResult result, Inventory inv) {
            if (result.isSuccess()) {
              inventory = inv;
            }
          }
        });
      }
    });
  }

  public void close() {
    if (helper != null)
      helper.dispose();
    helper = null;
  }

  private static class PendingPurchase {
    public Activity activity;
    public String sku;
    public IabHelper.OnIabPurchaseFinishedListener listener;

    public PendingPurchase(
        Activity activity, String sku, IabHelper.OnIabPurchaseFinishedListener listener) {
      this.activity = activity;
      this.sku = sku;
      this.listener = listener;
    }
  }
}
