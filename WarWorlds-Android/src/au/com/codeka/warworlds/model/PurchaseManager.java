package au.com.codeka.warworlds.model;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import au.com.codeka.warworlds.Util;
import au.com.codeka.warworlds.model.billing.IabException;
import au.com.codeka.warworlds.model.billing.IabHelper;
import au.com.codeka.warworlds.model.billing.IabResult;
import au.com.codeka.warworlds.model.billing.Inventory;
import au.com.codeka.warworlds.model.billing.Purchase;

public class PurchaseManager {
    private static final Logger log = LoggerFactory.getLogger(PurchaseManager.class);
    private static PurchaseManager sInstance = new PurchaseManager();
    public static PurchaseManager getInstance() {
        return sInstance;
    }
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
    }

    private IabHelper mHelper;
    private IabResult mSetupResult;
    private Inventory mInventory;

    public void setup(Context context) {
        mHelper = new IabHelper(context, sPublicKey);
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
        if (!mSetupResult.isSuccess()) {
            throw new IabException(mSetupResult);
        }
        return mInventory;
    }

    public void launchPurchaseFlow(Activity activity, String sku,
            IabHelper.OnIabPurchaseFinishedListener listener) throws IabException {
        if (!mSetupResult.isSuccess()) {
            throw new IabException(mSetupResult);
        }

        String skuName = Util.getProperties().getProperty("iap."+sku);

        // check if we already own it
        Purchase purchase = mInventory.getPurchase(skuName);
        if (purchase != null) {
            log.debug("Already purchased a '"+skuName+"', not purchasing again.");
            listener.onIabPurchaseFinished(new IabResult(IabHelper.BILLING_RESPONSE_RESULT_OK, null), purchase);
        } else {
            mHelper.launchPurchaseFlow(activity, skuName, REQUEST_CODE, listener);
        }
    }

    public void consume(Purchase purchase, IabHelper.OnConsumeFinishedListener listener) {
        mHelper.consumeAsync(purchase, listener);
    }

    public void close() {
        if (mHelper != null)
            mHelper.dispose();
        mHelper = null;
    }
}
