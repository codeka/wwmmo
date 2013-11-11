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

    public void hideEmpireName() {
        mView.findViewById(R.id.empire_name).setVisibility(View.GONE);
    }

    @Override
    public void onEmpireFetched(Empire empire) {
        MyEmpire myEmpire = EmpireManager.i.getEmpire();
        if (myEmpire != null && myEmpire.getKey().equals(empire.getKey())) {
            TextView cash = (TextView) mView.findViewById(R.id.cash);
            cash.setText(Cash.format(empire.getCash()));

            TextView empireName = (TextView) mView.findViewById(R.id.empire_name);
            empireName.setText(empire.getDisplayName());
        }
    }

    @Override
    public void onStateChanged() {
        if (isInEditMode()) {
            return;
        }

        // this is not called on the UI, so we have to send a request to the
        // UI thread to update the UI
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                ProgressBar working = (ProgressBar) mView.findViewById(R.id.working);

                RequestManagerState state = RequestManager.getCurrentState();
                // We will always have one busy request for the notification long-poll, but
                // that one doesn't really count.
                if (state.numInProgressRequests > 1) {
                    working.setVisibility(View.VISIBLE);
                } else {
                    working.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    public void onAttachedToWindow() {
        if (isInEditMode()) {
            return;
        }
        mHandler = new Handler();

        EmpireManager.i.addEmpireUpdatedListener(null, this);
        RequestManager.addRequestManagerStateChangedHandler(this);

        refreshEmpire();
    }

    @Override
    public void onDetachedFromWindow() {
        if (isInEditMode()) {
            return;
        }

        EmpireManager.i.removeEmpireUpdatedListener(this);
        RequestManager.removeRequestManagerStateChangedHandler(this);
    }

    private void refreshEmpire() {
        MyEmpire empire = EmpireManager.i.getEmpire();
        if (empire != null) {
            onEmpireFetched(empire);
        }

        // set up the initial state
        onStateChanged();
    }
}
