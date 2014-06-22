package au.com.codeka.warworlds.ctrl;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import au.com.codeka.BackgroundRunner;
import au.com.codeka.common.Log;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.PurchaseManager;
import au.com.codeka.warworlds.model.billing.IabException;
import au.com.codeka.warworlds.model.billing.IabHelper;
import au.com.codeka.warworlds.model.billing.IabResult;
import au.com.codeka.warworlds.model.billing.Inventory;
import au.com.codeka.warworlds.model.billing.Purchase;
import au.com.codeka.warworlds.model.billing.SkuDetails;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;


/**
 * This is our subclass of \c AdView that adds the "standard" properties automatically.
 */
public class BannerAdView extends FrameLayout {
    private static final Log log = new Log("BannerAdView");
    private AdView mAdView;
    private Button mRemoveAdsButton;
    private Context mContext;
    private Handler mHandler;

    private static boolean sAdsRemoved;
    private static DateTime sRemoveAdsShown;

    public BannerAdView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        if (isInEditMode()) {
            return;
        }

        if (!sAdsRemoved) {
            try {
                Inventory inventory = PurchaseManager.i.getInventory();
                if (inventory != null) {
                    sAdsRemoved = inventory.hasPurchase("remove_ads");
                }
            } catch(IabException e) {
                sAdsRemoved = false;
            }
        }

        mHandler = new Handler();
        setup();
    }

    public void refreshAd() {
        if (mAdView == null || isInEditMode()) {
            return;
        }

        AdRequest request = new AdRequest.Builder()
            .addTestDevice("14DEBC42826F8B1AA4D5EC50BB5812B7") // Galaxy Nexus
            .addTestDevice("E3E9BC57830668448015E1753F83BB44") // Nexus One
            .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
            .build();

        mAdView.loadAd(request);
    }

    public static void removeAds() {
        sAdsRemoved = true;
    }

    public static boolean isAdVisible() {
        return !sAdsRemoved;
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        if (isInEditMode()) {
            return;
        }

        if (visibility == View.VISIBLE) {
            setup();
        }
    }

    private void setup() {
        if (sAdsRemoved) {
            setVisibility(View.GONE);
            return;
        }

        boolean doAdsSetup = (sRemoveAdsShown != null);
        if (doAdsSetup) {
            DateTime now = DateTime.now(DateTimeZone.UTC);
            if (now.compareTo(sRemoveAdsShown.plusSeconds(45)) > 0) {
                doAdsSetup = false;
            }
        }
        if (doAdsSetup) {
            setupAd();
        } else {
            setupRemoveAdsButton();
        }
    }

    private void setupAd() {
        mAdView = new AdView((Activity) mContext);
        mAdView.setAdSize(AdSize.BANNER);
        mAdView.setAdUnitId("a14f533fbb3e1bd");

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                                                                   LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER;
        mAdView.setLayoutParams(lp);

        addView(mAdView);
        refreshAd();
    }

    private void setupRemoveAdsButton() {
        mRemoveAdsButton = new Button(mContext);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(250, LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER;
        mRemoveAdsButton.setLayoutParams(lp);
        mRemoveAdsButton.setText("Remove Ads");
        mRemoveAdsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRemoveAdsClick();
            }
        });

        addView(mRemoveAdsButton);

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sRemoveAdsShown = DateTime.now(DateTimeZone.UTC);
                removeAllViews();
                setupAd();
            }
        }, 7000);
    }

    private void onRemoveAdsClick() {
        try {
            PurchaseManager.i.launchPurchaseFlow((Activity) mContext, "remove_ads",
                        new IabHelper.OnIabPurchaseFinishedListener() {
                @Override
                public void onIabPurchaseFinished(IabResult result, final Purchase purchase) {
                    boolean isSuccess = result.isSuccess();
                    if (result.isFailure() && result.getResponse() == IabHelper.BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED) {
                        // if they've already purchased a star-renamed, but not reclaimed it, then
                        // we let them through anyway.
                        isSuccess = true;
                    }

                    if (isSuccess) {
                        sAdsRemoved = true;
                        setVisibility(View.GONE);

                        // also post to the server so that we can save the fact that you've bought it
                        if (purchase != null) {
                            postRemovalToServer(purchase);
                        }
                    }
                }
            });
        } catch (IabException e) {
            log.error("Couldn't get SKU details!", e);
            return;
        }
    }

    private void postRemovalToServer(final Purchase purchase) {
        new BackgroundRunner<Boolean>() {
            @Override
            protected Boolean doInBackground() {
                String price = "???";
                SkuDetails sku = null;
                try {
                    sku = PurchaseManager.i.getInventory().getSkuDetails(purchase.getSku());
                } catch (IabException e1) {
                }
                if (sku != null) {
                    price = sku.getPrice();
                }

                String url = "empires/" + EmpireManager.i.getEmpire().getKey() + "/ads";

                Messages.EmpireAdsRemoveRequest pb = Messages.EmpireAdsRemoveRequest.newBuilder()
                        .setPurchaseInfo(Messages.PurchaseInfo.newBuilder()
                                .setSku(purchase.getSku())
                                .setOrderId(purchase.getOrderId())
                                .setPrice(price)
                                .setToken(purchase.getToken())
                                .setDeveloperPayload(purchase.getDeveloperPayload()))
                        .build();
                try {
                    ApiClient.putProtoBuf(url, pb, null);
                } catch(Exception e) {
                    log.error("Error saving ads remove request!", e);
                }

                return true;
            }

            @Override
            protected void onComplete(Boolean sectors) {
            }
        }.execute();
    }
}
