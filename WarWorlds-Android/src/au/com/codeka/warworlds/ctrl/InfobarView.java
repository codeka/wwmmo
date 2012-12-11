package au.com.codeka.warworlds.ctrl;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import au.com.codeka.Cash;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.api.RequestManager;
import au.com.codeka.warworlds.api.RequestManager.RequestManagerState;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.MyEmpire;

/**
 * The "infobar" control displays the current empire name, your cash level
 * and few other indicators that we want visible (almost) everywhere.
 */
public class InfobarView extends FrameLayout
                         implements EmpireManager.EmpireFetchedHandler,
                                    RequestManager.RequestManagerStateChangedHandler {
    private Handler mHandler;
    private View mView;

    public InfobarView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mView = inflate(context, R.layout.infobar_ctrl, null);
        this.addView(mView);
    }

    @Override
    public void onEmpireFetched(Empire empire) {
        TextView cash = (TextView) mView.findViewById(R.id.cash);
        cash.setText(Cash.format(empire.getCash()));
    }

    @Override
    public void onStateChanged(final RequestManagerState state) {
        // this is not called on the UI, so we have to send a request to the
        // UI thread to update the UI
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                ProgressBar working = (ProgressBar) mView.findViewById(R.id.working);
/*
                if (Util.isDebug()) {
                    TextView empireName = (TextView) mView.findViewById(R.id.empire_name);
                    if (state.numInProgressRequests > 0) {
                        String str = String.format(Locale.ENGLISH, "%d %s",
                                                   state.numInProgressRequests,
                                                   state.lastUri);
                        empireName.setText(str);
                    } else {
                        String name = EmpireManager.getInstance().getEmpire().getDisplayName();
                        empireName.setText(name);
                    }
                }
*/
                if (state.numInProgressRequests > 0) {
                    working.setVisibility(View.VISIBLE);
                } else {
                    working.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    public void onAttachedToWindow() {
        if (!isInEditMode()) {
            mHandler = new Handler();

            MyEmpire empire = EmpireManager.getInstance().getEmpire();

            TextView empireName = (TextView) mView.findViewById(R.id.empire_name);
            empireName.setText(empire.getDisplayName());

            TextView cash = (TextView) mView.findViewById(R.id.cash);
            cash.setText(Cash.format(empire.getCash()));

            EmpireManager.getInstance()
                         .addEmpireUpdatedListener(empire.getKey(), this);
            RequestManager.addRequestManagerStateChangedHandler(this);
        }
    }

    @Override
    public void onDetachedFromWindow() {
        if (!isInEditMode()) {
            EmpireManager.getInstance().removeEmpireUpdatedListener(this);
            RequestManager.removeRequestManagerStateChangedHandler(this);
        }
    }
}
