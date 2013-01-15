package au.com.codeka.warworlds.ctrl;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
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
        if (isInEditMode()) {
            return;
        }

        if (context instanceof Activity) {
            Activity activity = (Activity)context;
            mAdView = new AdView(activity, AdSize.BANNER, "a14f533fbb3e1bd");

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                                                                       LayoutParams.WRAP_CONTENT);
            lp.gravity = Gravity.CENTER;
            mAdView.setLayoutParams(lp);

            this.addView(mAdView);

            refreshAd();
        }
    }

    public void refreshAd() {
        if (mAdView == null || isInEditMode()) {
            return;
        }

        AdRequest request = new AdRequest();
        request.addTestDevice("14DEBC42826F8B1AA4D5EC50BB5812B7"); // Galaxy Nexus
       // request.addTestDevice("E0B9ECF834A336BF19E9E6232133C876"); // Nexus S
        request.addTestDevice("E3E9BC57830668448015E1753F83BB44"); // Nexus One

        request.addTestDevice(AdRequest.TEST_EMULATOR);
        mAdView.loadAd(request);
    }
}
