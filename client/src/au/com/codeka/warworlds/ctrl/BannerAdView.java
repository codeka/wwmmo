package au.com.codeka.warworlds.ctrl;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;

import au.com.codeka.warworlds.model.PurchaseManager;
import au.com.codeka.warworlds.model.billing.IabException;
import au.com.codeka.warworlds.model.billing.IabHelper;
import au.com.codeka.warworlds.model.billing.IabResult;
import au.com.codeka.warworlds.model.billing.Inventory;
import au.com.codeka.warworlds.model.billing.Purchase;

/**
 * This is our subclass of \c AdView that adds the "standard" properties automatically.
 */
public class BannerAdView extends FrameLayout {
    private static final Logger log = LoggerFactory.getLogger(BannerAdView.class);
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

        AdRequest request = new AdRequest();
        request.addTestDevice("14DEBC42826F8B1AA4D5EC50BB5812B7"); // Galaxy Nexus
        request.addTestDevice("E3E9BC57830668448015E1753F83BB44"); // Nexus One

        request.addTestDevice(AdRequest.TEST_EMULATOR);
        mAdView.loadAd(request);
    }

    public static void removeAds() {
        sAdsRemoved = true;
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
        mAdView = new AdView((Activity) mContext, AdSize.BANNER, "a14f533fbb3e1bd");

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
                public void onIabPurchaseFinished(IabResult result, final Purchase info) {
                    boolean isSuccess = result.isSuccess();
                    if (result.isFailure() && result.getResponse() == IabHelper.BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED) {
                        // if they've already purchased a star-renamed, but not reclaimed it, then
                        // we let them through anyway.
                        isSuccess = true;
                    }

                    if (isSuccess) {
                        sAdsRemoved = true;
                        setVisibility(View.GONE);
                    }
                }
            });

        } catch (IabException e) {
            log.error("Couldn't get SKU details!", e);
            return;
        }

    }
}
