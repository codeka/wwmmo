package au.com.codeka.warworlds;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;

/**
 * This is our subclass of \c AdView that adds the "standard" properties automatically.
 */
public class BannerAdView extends FrameLayout {
    private AdView mAdView;

    public BannerAdView(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (context instanceof Activity) {
            Activity activity = (Activity)context;
            mAdView = new AdView(activity, AdSize.BANNER, "a14f533fbb3e1bd");
            this.addView(mAdView);

            refreshAd();
        }
    }

    public void refreshAd() {
        if (mAdView == null) {
            return;
        }

        AdRequest request = new AdRequest();
        request.addTestDevice("6454C8B8645CEC770A284D3C7D2F58DE");
        request.addTestDevice("14DEBC42826F8B1AA4D5EC50BB5812B7");
        request.addTestDevice("B5F11B468A1F5906D68ECAE779BF0237");
        request.addTestDevice("CF95DC53F383F9A836FD749F3EF439CD");
        mAdView.loadAd(request);
    }
}
