package au.com.codeka.warworlds.model;

import java.util.ArrayList;

import org.json.JSONException;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import au.com.codeka.common.Log;
import au.com.codeka.warworlds.App;
import au.com.codeka.warworlds.Util;
import au.com.codeka.warworlds.model.billing.IabException;
import au.com.codeka.warworlds.model.billing.IabHelper;
import au.com.codeka.warworlds.model.billing.IabResult;
import au.com.codeka.warworlds.model.billing.Inventory;
import au.com.codeka.warworlds.model.billing.Purchase;

public class PurchaseManager {
    private static final Log log = new Log("PurchaseManager");
    public static PurchaseManager i = new PurchaseManager();

    private PurchaseManager() {
    }

    // TODO: we should probably encrypt this some how...
    private static String sPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAuM1UqmzRXCwRr5"+
            "qcjqvBd+YbktM8fJoG5XUuGbRWErDVvx9gZ4TYju3jts702F9axBIH7VqQ5ARFO+AFJvv/AFeHPpT21VQu0o+"+
            "cHMZCnnaQmCnGeZE6udHfwsRYGnu35ReReKg7hbSHEIJ6I24uIjLqMNar34sKYCCqaE6IxlbQxYjK508nwsaK"+
            "dlAKtgymQkRGgspbmj5UW4B72drUt2kWPdRNw3RBfZBthTjm/6fUkPIxFpV8Ec/5Ty/z6Vn+VglTyE8xYaxPd"+
            "q+5JjWgA8oiiBFItNppBYl3ojNS9kBsYYmHJM4UlkwRSrc8f3HIIiZFYva4OR/ms2fWJ/kDzQIDAQAB";

    private static int REQUEST_CODE = 67468732; // random big number that hopefully won't conflict

    private static ArrayList<String> sAllSkus;
    {
        sAllSkus = new ArrayList<String>();
        sAllSkus.add("star_rename");
        sAllSkus.add("remove_ads");
        sAllSkus.add("rename_empire");
        sAllSkus.add("reset_empire_small");
        sAllSkus.add("reset_empire_big");
        sAllSkus.add("decorate_empire");
    }

    private IabHelper mHelper;
    private IabResult mSetupResult;
    private Inventory mInventory;

    public void setup() {
        // try to load the inventory from SharedPreferences first, so that we don't have to wait
        // on the play store...
        try {
            SharedPreferences prefs = Util.getSharedPreferences();
            String json = prefs.getString("au.com.codeka.warworlds.PurchaseInventory", null);
            if (json != null) {
                mInventory = Inventory.fromJson(json);
                if (Util.isDebug()) {
                    mInventory.erasePurchase("remove_ads");
                }
            }
        } catch (JSONException e) {
            // ignore... for now
        }

        mHelper = new IabHelper(App.i, sPublicKey);
        mHelper.enableDebugLogging(true, "In-AppBilling");
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            @Override
            public void onIabSetupFinished(IabResult result) {
                mSetupResult = result;
                if (mSetupResult.isSuccess()) {
                    mHelper.queryInventoryAsync(true, sAllSkus, new IabHelper.QueryInventoryFinishedListener() {
                        @Override
                        public void onQueryInventoryFinished(IabResult result, Inventory inv) {
                            if (result.isSuccess()) {
                                mInventory = inv;
                                if (Util.isDebug()) {
                                    mInventory.erasePurchase("remove_ads");
                                }

                                try {
                                    SharedPreferences prefs = Util.getSharedPreferences();
                                    prefs.edit().putString("au.com.codeka.warworlds.PurchaseInventory", inv.toJson())
                                         .commit();
                                } catch (JSONException e) {
                                    // ignore... for now
                                }
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
            mHelper.handleActivityResult(requestCode, resultCode, intent);
            return true;
        }

        return false;
    }

    public Inventory getInventory() throws IabException {
        if (mInventory != null) {
            return mInventory;
        }
        if (mSetupResult != null && !mSetupResult.isSuccess()) {
            throw new IabException(mSetupResult);
        }
        return mInventory;
    }

    public void launchPurchaseFlow(Activity activity, String sku,
            IabHelper.OnIabPurchaseFinishedListener listener) throws IabException {
        if (mSetupResult == null) {
            waitForSetup(activity, sku, listener);
            return;
        }

        if (!mSetupResult.isSuccess()) {
            throw new IabException(mSetupResult);
        }

        String skuName = Util.getProperties().getProperty("iap."+sku);

        // check if we already own it
        Purchase purchase = mInventory.getPurchase(skuName);
        if (purchase != null) {
            log.debug("Already purchased a '%s', not purchasing again.", skuName);
            listener.onIabPurchaseFinished(new IabResult(IabHelper.BILLING_RESPONSE_RESULT_OK, null), purchase);
        } else {
            mHelper.launchPurchaseFlow(activity, skuName, REQUEST_CODE, listener);
        }
    }

    private void waitForSetup(Activity activity, String sku,
            IabHelper.OnIabPurchaseFinishedListener listener) throws IabException {
        // TODO
    }

    public void consume(Purchase purchase, final IabHelper.OnConsumeFinishedListener listener) {
        mHelper.consumeAsync(purchase, new IabHelper.OnConsumeFinishedListener() {
            @Override
            public void onConsumeFinished(Purchase purchase, IabResult result) {
                listener.onConsumeFinished(purchase, result);

                // we'll want to refresh the inventory as well, now that we've consumed something
                mHelper.queryInventoryAsync(true, sAllSkus, new IabHelper.QueryInventoryFinishedListener() {
                    @Override
                    public void onQueryInventoryFinished(IabResult result, Inventory inv) {
                        if (result.isSuccess()) {
                            mInventory = inv;
                        }
                    }
                });
            }
        });
    }

    public void close() {
        if (mHelper != null)
            mHelper.dispose();
        mHelper = null;
    }
}
